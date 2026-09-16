package cn.zhijie.controller;

import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.IdentityType;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.AuthService;
import cn.zhijie.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService application;
    private final AuthService auth;
    private final cn.zhijie.security.ClientIpResolver ips;

    public AuthController(
        UserService application,
        AuthService auth,
        cn.zhijie.security.ClientIpResolver ips
    ) {
        this.application = application;
        this.auth = auth;
        this.ips = ips;
    }

    @PostMapping("/customer/auth/register")
    ApiResponse<TokenResponse> register(@Valid @RequestBody LoginRequest p, HttpServletRequest r) {
        return ApiResponse.ok(auth.register(p, ips.resolve(r)));
    }

    @PostMapping("/customer/auth/login")
    ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest p, HttpServletRequest r) {
        return ApiResponse.ok(auth.login(IdentityType.CUSTOMER, p, ips.resolve(r)));
    }

    @PostMapping("/customer/auth/refresh")
    ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest p) {
        return ApiResponse.ok(auth.refresh(IdentityType.CUSTOMER, p.refreshToken()));
    }

    @PostMapping("/customer/logout")
    ApiResponse<Void> logout(@AuthenticationPrincipal Actor a) {
        auth.logout(a);
        return ApiResponse.ok(null);
    }

    @PutMapping("/customer/me/account")
    ApiResponse<Void> account(
        @AuthenticationPrincipal Actor actor,
        @Valid @RequestBody AccountRequest request
    ) {
        auth.updateAccount(actor, request);
        return ApiResponse.ok(null);
    }

    @GetMapping("/customer/me")
    ApiResponse<ProfileResponse> me(@AuthenticationPrincipal Actor a) {
        return ApiResponse.ok(application.me(a));
    }

    @GetMapping("/public/certificates")
    ApiResponse<List<CertificateResponse>> certificates() {
        return ApiResponse.ok(application.certificates());
    }

    @PostMapping("/me/targets/{id}")
    ApiResponse<Void> target(@AuthenticationPrincipal Actor a, @PathVariable UUID id) {
        application.target(a, id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/me/targets/{id}")
    ApiResponse<Void> untarget(@AuthenticationPrincipal Actor a, @PathVariable UUID id) {
        application.untarget(a, id);
        return ApiResponse.ok(null);
    }
}
