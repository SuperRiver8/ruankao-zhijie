package cn.zhijie.pojo.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

// 接口响应实体，仅声明允许对外返回的字段。
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExamSummaryResponse(
    UUID id,
    UUID paperId,
    String status,
    Instant startedAt,
    Instant deadline,
    JsonNode result
) {}
