package com.execguard.agent;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 只记录，不改字节码。
 * 注册后，每个类定义/重定义时被调用一次，记录：
 *   类名 + 加载器 + classfileBuffer SHA-256 + 定义调用栈 + 是否 redefine
 *
 * 执行命令时，用这里的 hash 和加载器能读到的部署文件 hash 对比。
 * 注意：这个对比只能当辅助信号，见 README。
 */
public final class BirthRecorder implements ClassFileTransformer {
    private final ConcurrentHashMap<ClassLoader, ConcurrentHashMap<String, ClassRecord>> records =
            new ConcurrentHashMap<ClassLoader, ConcurrentHashMap<String, ClassRecord>>();

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        try {
            if (className == null || loader == null) {
                return null; // bootstrap 类单独按可信处理
            }
            String dotted = className.replace('/', '.');
            if (dotted.startsWith("com.execguard.agent.")) {
                return null; // 不记录 agent / helper
            }

            StackTraceElement[] stack = new Throwable().getStackTrace();
            List<String> birthStack = new ArrayList<String>();
            for (int i = 0; i < stack.length && i < 30; i++) {
                birthStack.add(stack[i].getClassName() + "." + stack[i].getMethodName());
            }

            String hash = sha256(classfileBuffer);
            ConcurrentHashMap<String, ClassRecord> byName = records.get(loader);
            if (byName == null) {
                byName = new ConcurrentHashMap<String, ClassRecord>();
                ConcurrentHashMap<String, ClassRecord> existing =
                        records.putIfAbsent(loader, byName);
                if (existing != null) {
                    byName = existing;
                }
            }

            ClassRecord old = byName.get(dotted);
            boolean redefined = classBeingRedefined != null;
            boolean changed = old != null && !old.hashHex.equals(hash);
            byName.put(dotted, new ClassRecord(
                    dotted, hash, redefined, changed,
                    loader.getClass().getName(), birthStack, System.currentTimeMillis()));
        } catch (Throwable ignored) {
            // 监控代码不能影响类加载
        }
        return null;
    }

    public ClassRecord get(ClassLoader loader, String className) {
        if (loader == null) {
            return null;
        }
        ConcurrentHashMap<String, ClassRecord> byName = records.get(loader);
        return byName == null ? null : byName.get(className);
    }

    static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    static byte[] readAll(InputStream in) throws Exception {
        try (InputStream input = in;
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = input.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    public static String hashResource(URL url) {
        try (InputStream in = url.openStream()) {
            return sha256(readAll(in));
        } catch (Exception e) {
            return "";
        }
    }
}
