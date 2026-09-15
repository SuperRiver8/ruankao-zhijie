package cn.zhijie.pojo.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

// 接口请求实体，可选字段为空时不传入业务参数。
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberRuleRequest(
    @NotBlank String name,
    @NotBlank String icon,
    @NotNull Integer sort,
    JsonNode entitlements
) {}
