package cn.zhijie.pojo.request;

import jakarta.validation.constraints.*;

public record AdminUpdateRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,40}") String username,
    @NotBlank @Pattern(regexp = "ADMIN|EDITOR|REVIEWER") String role,
    @NotNull Boolean enabled
) {}
