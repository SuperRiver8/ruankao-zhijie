package cn.zhijie.pojo.query;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

public record InsertAttemptCommand(
    UUID id,
    UUID userId,
    UUID paperId,
    JsonNode snapshot,
    Instant deadline
) {}
