package com.execguard.agent;

final class HookCallback implements ExecBridge.Callback {
    private final PolicyEngine engine;

    HookCallback(PolicyEngine engine) {
        this.engine = engine;
    }

    @Override
    public void beforeStart(String[] command) {
        engine.beforeStart(command);
    }
}
