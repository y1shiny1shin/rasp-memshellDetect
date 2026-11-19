package com.execguard.app.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查/首页，脚本用它判断应用是否就绪。
 */
@RestController
public class IndexController {
    @GetMapping("/")
    public String index() {
        return "shop-demo";
    }
}
