package cn.zhijie.pojo.response;

import cn.zhijie.util.TraceContext;

public record ApiResponse<T>(String code, String message, T data, String traceId) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "成功", data, TraceContext.current());
    }
    public static ApiResponse<Void> failure(String code, String message) {
        return new ApiResponse<>(code, message, null, TraceContext.current());
    }
}
