package cn.zhijie.pojo.request;

import jakarta.validation.constraints.*;

public record AccountRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,40}") String username,
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 8, max = 72) String newPassword
) {}
