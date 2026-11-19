package com.execguard.app.web;

import com.execguard.app.service.OrderExportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 白名单业务：订单导出，预期 ALLOW。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderExportService orderExportService;

    public OrderController(OrderExportService orderExportService) {
        this.orderExportService = orderExportService;
    }

    @GetMapping("/export")
    public String export() {
        return orderExportService.export();
    }
}
