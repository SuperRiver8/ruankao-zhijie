package cn.zhijie.pojo.query;

import java.util.UUID;

public record SaveAdminCommand(
    UUID id,
    String username,
    String password,
    String role,
    Boolean enabled
) {}
