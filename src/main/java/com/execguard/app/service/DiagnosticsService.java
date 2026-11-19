package com.execguard.app.service;

import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 正常但不在白名单里的业务类：诊断探测。
 * 预期 WARN（strict=false 时不拦截）。
 */
@Service
public class DiagnosticsService {
    public String ping() {
        try {
            Process process = new ProcessBuilder("echo", "diagnostics: ping")
                    .inheritIO()
                    .start();
            process.waitFor();
            return "ping ok";
        } catch (IOException e) {
            throw new IllegalStateException("diagnostics failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("diagnostics interrupted", e);
        }
    }
}
