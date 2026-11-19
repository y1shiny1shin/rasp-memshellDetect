package com.execguard.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 测试用的 Spring Boot 2 应用。
 *
 * 正常业务接口演示可信代码执行命令；/test/inject 是测试环境专用的
 * “漏洞入口”，用来把 payloads 模块里的 servlet 内存马真实注入当前 Tomcat。
 */
@SpringBootApplication
public class ShopApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }
}
