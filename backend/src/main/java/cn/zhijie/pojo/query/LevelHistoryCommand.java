package cn.zhijie.pojo.query;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

public record LevelHistoryCommand(
    UUID id,
    UUID userId,
    Integer old,
    Integer newLevel,
    UUID applicationId
) {}
