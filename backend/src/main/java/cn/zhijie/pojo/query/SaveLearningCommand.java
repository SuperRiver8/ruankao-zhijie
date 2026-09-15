package cn.zhijie.pojo.query;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

public record SaveLearningCommand(
    UUID userId,
    UUID entityId,
    Integer version,
    Boolean favorite,
    Boolean wrong,
    Integer progress
) {}
