package cn.zhijie.pojo.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// 接口响应实体，仅声明允许对外返回的字段。
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApplicationResponse(
    UUID id,
    UUID userId,
    UUID certificateId,
    String certificateName,
    String username,
    String holderName,
    String number,
    String numberType,
    LocalDate obtainedOn,
    UUID attachmentId,
    UUID auxiliaryId,
    String note,
    Integer revision,
    String autoResult,
    JsonNode autoDetails,
    String officialResult,
    String status,
    Instant createdAt,
    Instant updatedAt,
    List<ReviewResponse> reviews,
    List<VerificationResponse> verifications
) {}
