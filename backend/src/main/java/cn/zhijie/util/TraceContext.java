package cn.zhijie.util;

import org.slf4j.MDC;

public final class TraceContext {

    private TraceContext() {}

    public static String current() {
        return MDC.get("traceId");
    }
}
