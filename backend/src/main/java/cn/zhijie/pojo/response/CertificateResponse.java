package cn.zhijie.pojo.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

// 接口响应实体，仅声明允许对外返回的字段。
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CertificateResponse(
    UUID id,
    String code,
    String name,
    String examSystem,
    String specialty,
    Integer level,
    Boolean enabled
) {}
