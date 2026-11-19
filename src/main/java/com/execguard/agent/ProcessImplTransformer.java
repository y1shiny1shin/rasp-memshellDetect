package com.execguard.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * 在 java.lang.ProcessImpl.start(String[], Map, String, Redirect[], boolean)
 * 方法入口插入：
 *
 *     com.execguard.agent.ExecBridge.beforeStart(cmdarray);
 *
 * 为什么 hook 这里：
 *   - Runtime.exec / ProcessBuilder.start 最终都会调用 ProcessImpl.start
 *   - 反射直接调 ProcessImpl.start 也会经过这里
 *   - 能拿到完整的 String[] 命令行
 *
 * 只插入一条入口调用，不新增分支，所以不需要重算 frames。
 */
public final class ProcessImplTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!"java/lang/ProcessImpl".equals(className)) {
            return null;
        }

        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc,
                                                 String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                    if ("start".equals(name) && desc.startsWith("([Ljava/lang/String;")) {
                        return new MethodVisitor(Opcodes.ASM9, mv) {
                            @Override
                            public void visitCode() {
                                super.visitCode();
                                super.visitVarInsn(Opcodes.ALOAD, 0);
                                super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                        "com/execguard/agent/ExecBridge",
                                        "beforeStart",
                                        "([Ljava/lang/String;)V",
                                        false);
                            }
                        };
                    }
                    return mv;
                }
            };
            cr.accept(cv, 0);
            return cw.toByteArray();
        } catch (Throwable t) {
            System.out.println("[rasp-memshell] transform ProcessImpl failed: " + t);
            return null;
        }
    }
}
