package cn.zhijie.pojo.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

// 接口响应实体，仅声明允许对外返回的字段。
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberRuleResponse(
    Integer level,
    String name,
    String icon,
    Integer sortOrder,
    JsonNode entitlements
) {}
