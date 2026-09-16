package cn.zhijie.security;

public class CaptchaException extends RuntimeException {

    private final String code;

    public CaptchaException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static CaptchaException required() {
        return new CaptchaException("CAPTCHA_REQUIRED", "请完成滑块拼图验证后重试");
    }
}
