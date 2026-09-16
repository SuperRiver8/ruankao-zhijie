package cn.zhijie.controller;

import cn.zhijie.pojo.request.CaptchaChallengeRequest;
import cn.zhijie.pojo.response.*;
import cn.zhijie.security.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/captcha")
public class CaptchaController {

    private final CaptchaService captcha;
    private final ClientIpResolver ips;

    public CaptchaController(CaptchaService captcha, ClientIpResolver ips) {
        this.captcha = captcha;
        this.ips = ips;
    }

    @PostMapping("/challenge")
    public ResponseEntity<ApiResponse<CaptchaChallengeResponse>> challenge(
        @Valid @RequestBody CaptchaChallengeRequest request,
        HttpServletRequest http
    ) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(ApiResponse.ok(captcha.challenge(request, ips.resolve(http))));
    }
}
