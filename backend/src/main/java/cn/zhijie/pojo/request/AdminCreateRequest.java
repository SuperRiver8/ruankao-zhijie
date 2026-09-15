package cn.zhijie.pojo.request;

import jakarta.validation.constraints.*;

public record AdminCreateRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,40}") String username,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank @Pattern(regexp = "ADMIN|EDITOR|REVIEWER") String role,
    @NotNull Boolean enabled
) {}
