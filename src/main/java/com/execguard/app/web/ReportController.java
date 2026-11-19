package com.execguard.app.web;

import com.execguard.app.service.AsyncReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * lambda 线程场景，预期 ALLOW。
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {
    private final AsyncReportService asyncReportService;

    public ReportController(AsyncReportService asyncReportService) {
        this.asyncReportService = asyncReportService;
    }

    @GetMapping("/async")
    public String async() throws InterruptedException {
        return asyncReportService.runAsync();
    }
}
