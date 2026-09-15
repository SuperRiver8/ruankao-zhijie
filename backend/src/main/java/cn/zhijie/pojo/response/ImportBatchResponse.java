package cn.zhijie.pojo.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

// 接口响应实体，仅声明允许对外返回的字段。
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImportBatchResponse(
    UUID id,
    UUID ownerId,
    String idempotencyKey,
    String checksum,
    String status,
    JsonNode payload,
    JsonNode report,
    Instant createdAt,
    Boolean allowUpdates
) {}
