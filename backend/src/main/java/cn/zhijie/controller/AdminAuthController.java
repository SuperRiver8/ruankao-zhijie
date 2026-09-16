package cn.zhijie.controller;

import cn.zhijie.pojo.*;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AuthService auth;
    private final AdminAccountService accounts;
    private final cn.zhijie.security.ClientIpResolver ips;

    public AdminAuthController(
        AuthService auth,
        AdminAccountService accounts,
        cn.zhijie.security.ClientIpResolver ips
    ) {
        this.auth = auth;
        this.accounts = accounts;
        this.ips = ips;
    }

    @PostMapping("/auth/login")
    public ApiResponse<TokenResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest http
    ) {
        return ApiResponse.ok(auth.login(IdentityType.ADMIN, request, ips.resolve(http)));
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(auth.refresh(IdentityType.ADMIN, request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal Actor actor) {
        auth.logout(actor);
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<AdminResponse> me(@AuthenticationPrincipal Actor actor) {
        return ApiResponse.ok(accounts.me(actor));
    }

    @PutMapping("/me/account")
    public ApiResponse<Void> account(
        @AuthenticationPrincipal Actor actor,
        @Valid @RequestBody AccountRequest request
    ) {
        auth.updateAccount(actor, request);
        return ApiResponse.ok(null);
    }
}
