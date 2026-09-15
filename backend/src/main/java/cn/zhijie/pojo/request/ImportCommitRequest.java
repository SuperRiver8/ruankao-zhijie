package cn.zhijie.pojo.request;

import com.fasterxml.jackson.annotation.JsonInclude;

// 接口请求实体，可选字段为空时不传入业务参数。
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImportCommitRequest(Boolean allowUpdates) {}
