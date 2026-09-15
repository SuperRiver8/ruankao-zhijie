package cn.zhijie.pojo.query;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

public record InsertAttachmentCommand(
    UUID id,
    UUID ownerId,
    UUID adminOwnerId,
    String name,
    String key,
    String type,
    Long size,
    String checksum,
    String access
) {}
