package cn.zhijie.pojo.query;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

public record InsertReviewCommand(
    UUID id,
    UUID applicationId,
    UUID reviewer,
    String action,
    String reason
) {}
