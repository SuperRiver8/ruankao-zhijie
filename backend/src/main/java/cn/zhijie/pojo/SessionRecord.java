package cn.zhijie.pojo;

import java.time.Instant;
import java.util.UUID;

// 仅在服务器保存刷新令牌摘要，不保存令牌明文。
public record SessionRecord(
    IdentityType identityType,
    UUID userId,
    int permissionVersion,
    String refreshDigest,
    Instant expiresAt
) {}
