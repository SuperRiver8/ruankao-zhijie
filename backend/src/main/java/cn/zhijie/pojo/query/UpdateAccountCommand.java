package cn.zhijie.pojo.query;

import java.util.UUID;

public record UpdateAccountCommand(UUID id, String username, String password) {}
