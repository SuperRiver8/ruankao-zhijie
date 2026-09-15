package cn.zhijie.pojo.response;

import java.time.Instant;
import java.util.UUID;

public record AdminResponse(
    UUID id,
    String username,
    String role,
    Boolean enabled,
    Instant createdAt,
    Instant updatedAt
) {}
