package cn.zhijie.pojo.query;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

public record AuditCommand(
    UUID id,
    UUID actor,
    UUID adminActor,
    String action,
    UUID target,
    String detail
) {}
