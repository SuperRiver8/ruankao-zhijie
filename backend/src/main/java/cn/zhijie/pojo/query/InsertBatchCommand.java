package cn.zhijie.pojo.query;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

public record InsertBatchCommand(
    UUID id,
    UUID ownerId,
    String key,
    String checksum,
    JsonNode payload,
    JsonNode report
) {}
