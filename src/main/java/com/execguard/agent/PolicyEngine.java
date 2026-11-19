package com.execguard.agent;

import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 判定逻辑：
 *   1. 拿当前线程的 Class<?>[] 调用栈
 *   2. 逐层分类：agent/JDK 跳过；代理/lambda 透明；其他类查部署文件和出生记录
 *   3. 只要栈里有一个“来路不明”或“字节被换过”的类 -> BLOCK（不管白名单）
 *   4. 没有来路不明的类，再看栈里有没有白名单业务类 -> ALLOW
 *   5. 都没有 -> WARN（可信，但未授权）
 */
final class PolicyEngine {
    private static final String[] AGENT_PREFIXES = {"com.execguard.agent."};

    private final ExecPolicy policy;
    private final BirthRecorder recorder;

    PolicyEngine(ExecPolicy policy, BirthRecorder recorder) {
        this.policy = policy;
        this.recorder = recorder;
    }

    void beforeStart(String[] command) {
        Class<?>[] context = ClassStack.INSTANCE.classes();
        Decision decision = analyze(context);

        StackTraceElement[] trace = new Throwable().getStackTrace();
        report(command, decision, trace);

        boolean shouldBlock = decision.block
                || (policy.strict() && decision.trustedButNotWhitelisted);
        if (policy.mode() == ExecPolicy.Mode.BLOCK && shouldBlock) {
            throw new SecurityException("[rasp-memshell] blocked: " + decision.reason);
        }
    }

    private Decision analyze(Class<?>[] context) {
        List<Frame> frames = new ArrayList<Frame>();
        boolean hasUntrusted = false;
        boolean hasTampered = false;
        boolean hasWhitelisted = false;
        boolean hasTrusted = false;
        String reason = "";

        for (Class<?> clazz : context) {
            if (clazz == null) {
                continue;
            }
            String name = clazz.getName();

            if (isAgentClass(name)) {
                frames.add(new Frame(clazz, "agent-skip"));
                continue;
            }
            if (clazz.getClassLoader() == null) {
                frames.add(new Frame(clazz, "jdk/trusted"));
                continue;
            }
            if (Proxy.isProxyClass(clazz)) {
                frames.add(new Frame(clazz, "proxy/transparent"));
                continue;
            }
            // JDK8/11/17: demo.Main$$Lambda$8/0x...
            // JDK21+:    demo.Main$$Lambda/0x...
            if (name.contains("$$Lambda$") || name.contains("$$Lambda/")) {
                frames.add(new Frame(clazz, "lambda/transparent"));
                continue;
            }
            if (name.contains("$$EnhancerBy") || name.contains("$$FastClassBy")) {
                frames.add(new Frame(clazz, "generated/transparent(demo)"));
                continue;
            }

            ClassRecord record = recorder.get(clazz.getClassLoader(), name);
            URL resource = clazz.getClassLoader()
                    .getResource(name.replace('.', '/') + ".class");

            if (resource == null) {
                frames.add(new Frame(clazz, "UNTRUSTED (no artifact, no resource)"));
                hasUntrusted = true;
                reason = "调用栈中存在没有部署文件来源的类: " + name;
                continue;
            }

            if (record == null) {
                frames.add(new Frame(clazz, "trusted? (resource exists, no birth record)"));
                hasTrusted = true;
            } else {
                String resourceHash = BirthRecorder.hashResource(resource);
                if (resourceHash.equals(record.hashHex)) {
                    frames.add(new Frame(clazz, "TRUSTED (artifact hash match)"));
                    hasTrusted = true;
                } else {
                    frames.add(new Frame(clazz, "TAMPERED (artifact hash mismatch)"));
                    hasTampered = true;
                    reason = "类字节与部署文件不一致: " + name;
                }
            }

            if (policy.isWhitelisted(name)) {
                hasWhitelisted = true;
            }
        }

        if (hasUntrusted || hasTampered) {
            return new Decision(true, false, false, reason, frames);
        }
        if (hasWhitelisted) {
            return new Decision(false, true, false, "白名单业务类执行命令", frames);
        }
        if (hasTrusted) {
            return new Decision(false, false, true, "可信类执行命令，但不在白名单", frames);
        }
        return new Decision(false, false, true,
                "没有找到可信的业务类（可能只有代理/生成类）", frames);
    }

    private static boolean isAgentClass(String name) {
        for (String prefix : AGENT_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void report(String[] command, Decision decision, StackTraceElement[] trace) {
        String verdict;
        if (decision.block) {
            verdict = policy.mode() == ExecPolicy.Mode.BLOCK
                    ? "BLOCK" : "ALERT(would-block)";
        } else if (decision.allowed) {
            verdict = "ALLOW";
        } else {
            verdict = (policy.strict() && policy.mode() == ExecPolicy.Mode.BLOCK)
                    ? "BLOCK(not-whitelisted)" : "WARN";
        }

        System.out.println("[rasp-memshell] cmd=" + join(command) + " -> " + verdict);
        if (!decision.reason.isEmpty()) {
            System.out.println("[rasp-memshell]   reason: " + decision.reason);
        }
        System.out.println("[rasp-memshell]   stack:");
        for (StackTraceElement element : trace) {
            System.out.println("[rasp-memshell]     at " + element);
        }
        System.out.println("[rasp-memshell]   class frames:");
        int i = 0;
        for (Frame frame : decision.frames) {
            System.out.println("[rasp-memshell]     #" + (i++) + " " + frame.clazz.getName()
                    + " [" + frame.label + "] loader=" + loaderName(frame.clazz));
        }
    }

    private static String join(String[] command) {
        if (command == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : command) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(part);
        }
        return sb.toString();
    }

    private static String loaderName(Class<?> clazz) {
        ClassLoader loader = clazz.getClassLoader();
        return loader == null ? "bootstrap" : loader.getClass().getName();
    }

    private static final class Frame {
        final Class<?> clazz;
        final String label;

        Frame(Class<?> clazz, String label) {
            this.clazz = clazz;
            this.label = label;
        }
    }

    private static final class Decision {
        final boolean block;
        final boolean allowed;
        final boolean trustedButNotWhitelisted;
        final String reason;
        final List<Frame> frames;

        Decision(boolean block, boolean allowed, boolean trustedButNotWhitelisted,
                 String reason, List<Frame> frames) {
            this.block = block;
            this.allowed = allowed;
            this.trustedButNotWhitelisted = trustedButNotWhitelisted;
            this.reason = reason;
            this.frames = frames;
        }
    }
}
