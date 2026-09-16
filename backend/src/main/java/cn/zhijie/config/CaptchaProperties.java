package cn.zhijie.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.captcha")
public record CaptchaProperties(
    @DefaultValue("120") @Min(1) int windowSeconds,
    @DefaultValue("2") @Min(1) int failureThreshold,
    @DefaultValue("120") @Min(1) int challengeTtlSeconds,
    @DefaultValue("5") @Min(0) @Max(10) int tolerance,
    @DefaultValue("30") @Min(1) int issueLimit,
    @DefaultValue("60") @Min(1) int issueWindowSeconds
) {}
