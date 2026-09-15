package cn.zhijie.pojo.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

// 接口响应实体，仅声明允许对外返回的字段。
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExamResponse(
    UUID id,
    UUID userId,
    UUID paperId,
    JsonNode snapshot,
    JsonNode answers,
    JsonNode result,
    Instant startedAt,
    Instant deadline,
    Instant submittedAt,
    String status,
    Instant serverTime
) {}
