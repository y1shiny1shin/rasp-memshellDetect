package com.execguard.app.tooling;

/**
 * 通过 Attach API 把 agent 加载进指定 JVM。
 *
 * 用法：
 *   AttachMain <pid> <agentJar> [configPath]
 *
 * 不传 configPath 时，agent 使用 jar 内置的 rasp.properties。
 * 用反射调用 com.sun.tools.attach.VirtualMachine，编译期不依赖 tools.jar
 * （JDK 9+ 已没有 tools.jar）。JDK 8 运行时仍需要把
 * $JAVA_HOME/lib/tools.jar 放进 classpath，run.sh 已处理。
 */
public final class AttachMain {
    private AttachMain() {
    }

    public static void main(String[] args) throws Exception {
        String pid = args[0];
        String agentJar = args[1];
        String config = args.length >= 3 ? args[2] : null;

        System.out.println("[attach] attaching to " + pid);
        Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
        Object vm = vmClass.getMethod("attach", String.class).invoke(null, pid);
        try {
            if (config != null && !config.trim().isEmpty()) {
                vmClass.getMethod("loadAgent", String.class, String.class)
                        .invoke(vm, agentJar, config);
            } else {
                vmClass.getMethod("loadAgent", String.class).invoke(vm, agentJar);
            }
        } finally {
            vmClass.getMethod("detach").invoke(vm);
        }
        System.out.println("[attach] agent loaded");
    }
}
