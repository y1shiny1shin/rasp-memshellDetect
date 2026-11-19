package com.execguard.app.plugin;

/**
 * 插件扩展点：应用通过 JDK 动态代理调用插件实现。
 */
public interface CommandPlugin {
    String run();
}
