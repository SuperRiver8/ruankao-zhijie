package cn.zhijie.pojo.request;

import jakarta.validation.constraints.*;

public record CaptchaChallengeRequest(
    @NotNull Scene scene,
    @NotBlank @Size(max = 40) String username
) {
    public enum Scene {
        CUSTOMER_LOGIN,
        ADMIN_LOGIN,
        CUSTOMER_REGISTER,
    }
}
