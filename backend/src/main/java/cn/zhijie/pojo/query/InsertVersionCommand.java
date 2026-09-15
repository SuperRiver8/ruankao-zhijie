package cn.zhijie.pojo.query;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

public record InsertVersionCommand(
    UUID id,
    Integer version,
    String title,
    JsonNode payload,
    String checksum,
    UUID actor
) {}
