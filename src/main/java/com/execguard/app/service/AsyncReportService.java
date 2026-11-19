package com.execguard.app.service;

import org.springframework.stereotype.Service;

/**
 * 异步报表：在线程里通过 lambda 调用白名单业务类。
 * lambda 类对检测逻辑透明，最终看到的宿主是 AsyncReportService，
 * 栈里还有 whitelisted 的 OrderExportService，预期 ALLOW。
 */
@Service
public class AsyncReportService {
    private final OrderExportService orderExportService;

    public AsyncReportService(OrderExportService orderExportService) {
        this.orderExportService = orderExportService;
    }

    public String runAsync() throws InterruptedException {
        Thread worker = new Thread(() -> orderExportService.export(), "report-worker");
        worker.start();
        worker.join();
        return "async report completed";
    }
}
