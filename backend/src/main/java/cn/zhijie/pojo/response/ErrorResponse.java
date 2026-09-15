package cn.zhijie.pojo.response;

import com.fasterxml.jackson.annotation.JsonInclude;

// 接口响应实体，仅声明允许对外返回的字段。
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String message) {}
