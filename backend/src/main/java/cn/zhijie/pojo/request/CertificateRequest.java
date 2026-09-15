package cn.zhijie.pojo.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import java.util.UUID;

// 接口请求实体，可选字段为空时不传入业务参数。
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CertificateRequest(
    UUID id,
    @NotBlank String code,
    @NotBlank String name,
    @NotBlank String specialty,
    @NotNull @Min(1) @Max(3) Integer level
) {}
