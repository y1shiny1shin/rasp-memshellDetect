package com.execguard.agent;

/**
 * 不安装 SecurityManager，只借用 SecurityManager.getClassContext() 拿 Class<?>[]。
 *
 * 实测 JDK 8/11/17/21/22/23 都可以直接 new + 调用，不需要
 * -Djava.security.manager=allow。
 *
 * JDK 24+ SecurityManager 被移除，这个方式不能再用；ASM hook 本身可以继续用，
 * 但栈采集要换成 JVMTI GetStackTrace + GetMethodDeclaringClass，或者
 * JDK9+ 的 StackWalker + 自己维护 name -> Class 索引。
 */
final class ClassStack extends SecurityManager {
    static final ClassStack INSTANCE = new ClassStack();

    private ClassStack() {
    }

    Class<?>[] classes() {
        return getClassContext();
    }
}
