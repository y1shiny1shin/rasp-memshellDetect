package com.execguard.agent;

/**
 * 必须能放进 bootstrap classpath。
 *
 * java.lang.ProcessImpl 是 bootstrap 类，ASM 注入到它里面的代码只能调用
 * bootstrap 可见的类。agent 通过 manifest 的 Boot-Class-Path 把这个 helper
 * jar 加进 bootstrap，然后把自己的回调注册到这里。
 *
 * 注意：agent 和 helper 的类同属 com.execguard.agent 包，但由不同类加载器
 * 加载（bootstrap 和 system），属于跨加载器的 split package。两边只能通过
 * public 成员交互，不要在这里依赖 package-private 的 agent 类。
 */
public final class ExecBridge {
    public interface Callback {
        void beforeStart(String[] command);
    }

    private static volatile Callback callback;

    private ExecBridge() {
    }

    public static void setCallback(Callback cb) {
        callback = cb;
    }

    public static void beforeStart(String[] command) {
        Callback cb = callback;
        if (cb != null) {
            cb.beforeStart(command);
        }
    }
}
