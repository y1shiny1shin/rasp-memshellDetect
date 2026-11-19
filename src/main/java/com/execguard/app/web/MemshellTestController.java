package com.execguard.app.web;

import com.execguard.app.memshell.ServletShellInjector;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 测试环境专用的“漏洞入口”。
 *
 * 模拟攻击者已经获得应用内代码执行能力（反序列化、表达式注入等），
 * 在请求线程里把 payloads 模块的 servlet 内存马注入当前 Tomcat。
 * 真实攻击中这一步由漏洞利用链完成，这里用 HTTP 入口方便复现。
 */
@RestController
@RequestMapping("/test")
public class MemshellTestController {
    @GetMapping("/inject")
    public String inject(HttpServletRequest request) throws Exception {
        return "servlet memshell injected at " + ServletShellInjector.inject(request);
    }
}
