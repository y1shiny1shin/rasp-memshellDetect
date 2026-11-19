package com.execguard.app.plugin;

import com.execguard.app.service.OrderExportService;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 正常 JDK 动态代理的 handler：来自部署目录，最终调用白名单业务类。
 * 代理壳对检测逻辑透明，handler 和目标类都可信，预期 ALLOW。
 */
@Component
public class PluginHandler implements InvocationHandler {
    private final OrderExportService orderExportService;

    public PluginHandler(OrderExportService orderExportService) {
        this.orderExportService = orderExportService;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        return orderExportService.export();
    }
}
