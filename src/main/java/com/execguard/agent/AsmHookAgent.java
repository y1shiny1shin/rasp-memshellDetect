package com.execguard.agent;

import java.lang.instrument.Instrumentation;

/**
 * rasp-memshellDetect agent。
 *
 * 默认使用 agent jar 内置的 rasp.properties；需要覆盖时可以用：
 *   java -javaagent:rasp-memshellDetect-agent.jar -Drasp.config=/path/rasp.properties ...
 *   VirtualMachine.attach(pid).loadAgent(agentJar, "/path/rasp.properties");
 */
public final class AsmHookAgent {
    private static final String ASM_VERSION = "9.9.1";

    private AsmHookAgent() {
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        install(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        install(agentArgs, inst);
    }

    private static void install(String agentArgs, Instrumentation inst) {
        String configPath = (agentArgs != null && !agentArgs.trim().isEmpty())
                ? agentArgs.trim()
                : System.getProperty("rasp.config");

        System.out.println("[rasp-memshell] 启动；ASM=" + ASM_VERSION
                + " (" + AsmHookAgent.class.getProtectionDomain()
                .getCodeSource().getLocation() + ")");

        ExecPolicy policy = ExecPolicy.load(configPath);
        BirthRecorder recorder = new BirthRecorder();
        inst.addTransformer(recorder, true);

        ExecBridge.setCallback(new HookCallback(new PolicyEngine(policy, recorder)));

        try {
            inst.addTransformer(new ProcessImplTransformer(), true);
            Class<?> processImpl = Class.forName("java.lang.ProcessImpl");
            System.out.println("[rasp-memshell] ProcessImpl modifiable="
                    + inst.isModifiableClass(processImpl));
            if (inst.isModifiableClass(processImpl)) {
                inst.retransformClasses(processImpl);
                System.out.println("[rasp-memshell] ProcessImpl.start hook 已安装");
            }
        } catch (Throwable t) {
            System.out.println("[rasp-memshell] 安装 ProcessImpl hook 失败: " + t);
            t.printStackTrace(System.out);
        }

        System.out.println("[rasp-memshell] 白名单=" + policy.whitelist()
                + "；模式=" + policy.mode()
                + "；strict=" + policy.strict());
    }
}
