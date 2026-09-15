package cn.zhijie.pojo.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

// 接口响应实体，仅声明允许对外返回的字段。
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VerificationResponse(
    UUID id,
    UUID applicationId,
    UUID reviewerId,
    String result,
    String officialSource,
    String evidence,
    String ownershipEvidence,
    Instant createdAt
) {}
