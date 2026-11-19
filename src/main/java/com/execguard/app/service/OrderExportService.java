package com.execguard.app.service;

import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 正常业务类：订单导出。
 *
 * 真实系统里这类逻辑常常会调用外部命令（转换工具、报表程序等）。
 * src/main/resources/rasp.properties 把它放进白名单，预期 ALLOW。
 */
@Service
public class OrderExportService {
    public String export() {
        try {
            Process process = new ProcessBuilder("echo", "legit: order export completed")
                    .inheritIO()
                    .start();
            process.waitFor();
            return "order export completed";
        } catch (IOException e) {
            throw new IllegalStateException("order export failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("order export interrupted", e);
        }
    }
}
