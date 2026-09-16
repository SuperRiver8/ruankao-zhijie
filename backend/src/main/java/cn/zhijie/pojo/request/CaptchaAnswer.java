package cn.zhijie.pojo.request;

import jakarta.validation.constraints.*;

public record CaptchaAnswer(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String challengeId,
    @NotNull @DecimalMin("0") @DecimalMax("272") Double offsetX
) {}
