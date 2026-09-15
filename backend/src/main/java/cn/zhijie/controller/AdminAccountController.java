package cn.zhijie.controller;

import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.query.AdminQuery;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.AdminAccountService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {

    private final AdminAccountService service;

    public AdminAccountController(AdminAccountService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminResponse>> list(
        @AuthenticationPrincipal Actor actor,
        @Valid @ModelAttribute AdminQuery query
    ) {
        return ApiResponse.ok(service.list(actor, query));
    }

    @PostMapping
    public ApiResponse<IdResponse> create(
        @AuthenticationPrincipal Actor actor,
        @Valid @RequestBody AdminCreateRequest request
    ) {
        return ApiResponse.ok(service.create(actor, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(
        @AuthenticationPrincipal Actor actor,
        @PathVariable UUID id,
        @Valid @RequestBody AdminUpdateRequest request
    ) {
        service.update(actor, id, request);
        return ApiResponse.ok(null);
    }
}
