package com.execguard.agent;

import java.util.List;

/**
 * 类的“出生证明”：
 *   - 类定义/重定义时 classfileBuffer 的 SHA-256
 *   - 加载器
 *   - 定义调用栈
 *   - 是否 redefine / 字节是否变化
 */
public final class ClassRecord {
    public final String className;
    public final String hashHex;
    public final boolean redefined;
    public final boolean hashChanged;
    public final String loaderName;
    public final List<String> birthStack;
    public final long timestamp;

    public ClassRecord(String className, String hashHex, boolean redefined, boolean hashChanged,
                       String loaderName, List<String> birthStack, long timestamp) {
        this.className = className;
        this.hashHex = hashHex;
        this.redefined = redefined;
        this.hashChanged = hashChanged;
        this.loaderName = loaderName;
        this.birthStack = birthStack;
        this.timestamp = timestamp;
    }
}
