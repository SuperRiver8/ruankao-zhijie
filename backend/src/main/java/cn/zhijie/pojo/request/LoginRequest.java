package cn.zhijie.pojo.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

// 接口请求实体，可选字段为空时不传入业务参数。
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginRequest(
    @NotBlank @Size(max = 40) String username,
    @NotBlank @Size(max = 100) String password,
    @Valid CaptchaAnswer captcha
) {
    public LoginRequest(String username, String password) {
        this(username, password, null);
    }
}
