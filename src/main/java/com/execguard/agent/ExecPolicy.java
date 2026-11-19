package com.execguard.agent;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/**
 * 策略配置：
 *   exec.whitelist=com.execguard.app.service.OrderExportService   # 允许执行命令的业务类，逗号分隔
 *   mode=block                                                  # alert 只记录；block 拦截
 *   strict=false                                                # true 时“可信但不在白名单”也拦
 */
public final class ExecPolicy {
    public enum Mode { ALERT, BLOCK }

    private final Set<String> whitelist;
    private final Mode mode;
    private final boolean strict;

    private ExecPolicy(Set<String> whitelist, Mode mode, boolean strict) {
        this.whitelist = whitelist;
        this.mode = mode;
        this.strict = strict;
    }

    public static ExecPolicy load(String path) {
        Properties props = new Properties();
        String source;

        if (path != null && !path.trim().isEmpty()) {
            Path file = Paths.get(path.trim());
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
                source = file.toString();
            } catch (Exception e) {
                System.out.println("[rasp-memshell] 读取配置失败: " + file + " -> " + e
                        + "，回退到内置配置");
                loadBundled(props);
                source = "classpath:rasp.properties";
            }
        } else {
            loadBundled(props);
            source = "classpath:rasp.properties";
        }

        System.out.println("[rasp-memshell] 配置来源: " + source);

        Set<String> whitelist = new LinkedHashSet<String>();
        String wl = props.getProperty("exec.whitelist", "");
        for (String item : wl.split(",")) {
            String name = item.trim();
            if (!name.isEmpty()) {
                whitelist.add(name);
            }
        }

        Mode mode = "block".equalsIgnoreCase(
                props.getProperty("mode", "alert").trim())
                ? Mode.BLOCK : Mode.ALERT;
        boolean strict = Boolean.parseBoolean(
                props.getProperty("strict", "false").trim());

        return new ExecPolicy(Collections.unmodifiableSet(whitelist), mode, strict);
    }

    private static void loadBundled(Properties props) {
        try (InputStream in = ExecPolicy.class.getResourceAsStream("/rasp.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            System.out.println("[rasp-memshell] 读取内置配置失败: " + e);
        }
    }

    public Set<String> whitelist() {
        return whitelist;
    }

    public Mode mode() {
        return mode;
    }

    public boolean strict() {
        return strict;
    }

    public boolean isWhitelisted(String className) {
        return whitelist.contains(className);
    }
}
