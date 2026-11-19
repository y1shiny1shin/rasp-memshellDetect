package com.execguard.app.web;

import com.execguard.app.service.DiagnosticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 可信但不在白名单里的业务，预期 WARN。
 */
@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticsController {
    private final DiagnosticsService diagnosticsService;

    public DiagnosticsController(DiagnosticsService diagnosticsService) {
        this.diagnosticsService = diagnosticsService;
    }

    @GetMapping("/ping")
    public String ping() {
        return diagnosticsService.ping();
    }
}
