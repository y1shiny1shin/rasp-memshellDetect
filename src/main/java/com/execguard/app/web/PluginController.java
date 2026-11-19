package com.execguard.app.web;

import com.execguard.app.plugin.CommandPlugin;
import com.execguard.app.plugin.PluginHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Proxy;

/**
 * JDK 动态代理场景，预期 ALLOW。
 */
@RestController
@RequestMapping("/api/plugin")
public class PluginController {
    private final PluginHandler pluginHandler;

    public PluginController(PluginHandler pluginHandler) {
        this.pluginHandler = pluginHandler;
    }

    @GetMapping("/run")
    public String run() {
        CommandPlugin plugin = (CommandPlugin) Proxy.newProxyInstance(
                CommandPlugin.class.getClassLoader(),
                new Class<?>[]{CommandPlugin.class},
                pluginHandler);
        return plugin.run();
    }
}
