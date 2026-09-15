package cn.zhijie.controller;

import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.query.PageQuery;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.MembershipService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MembershipController {

    private final MembershipService service;

    public MembershipController(MembershipService service) {
        this.service = service;
    }

    @PostMapping("/certification/applications")
    ApiResponse<IdResponse> submit(
        @AuthenticationPrincipal Actor a,
        @Valid @RequestBody ApplicationRequest p
    ) {
        return ApiResponse.ok(service.submit(a, null, p));
    }

    @PutMapping("/certification/applications/{id}")
    ApiResponse<IdResponse> resubmit(
        @AuthenticationPrincipal Actor a,
        @PathVariable UUID id,
        @Valid @RequestBody ApplicationRequest p
    ) {
        return ApiResponse.ok(service.submit(a, id, p));
    }

    @GetMapping("/certification/applications")
    ApiResponse<PageResponse<ApplicationResponse>> list(
        @AuthenticationPrincipal Actor a,
        @Valid @ModelAttribute PageQuery query
    ) {
        return ApiResponse.ok(service.list(a, false, query));
    }

    @GetMapping("/certification/applications/{id}")
    ApiResponse<ApplicationResponse> detail(
        @AuthenticationPrincipal Actor a,
        @PathVariable UUID id
    ) {
        return ApiResponse.ok(service.detail(a, id));
    }

    @GetMapping("/admin/certifications")
    ApiResponse<PageResponse<ApplicationResponse>> queue(
        @AuthenticationPrincipal Actor a,
        @Valid @ModelAttribute PageQuery query
    ) {
        return ApiResponse.ok(service.list(a, true, query));
    }

    @PostMapping("/admin/certifications/{id}/review")
    ApiResponse<Void> review(
        @AuthenticationPrincipal Actor a,
        @PathVariable UUID id,
        @Valid @RequestBody CertificateReviewRequest p
    ) {
        service.review(a, id, p);
        return ApiResponse.ok(null);
    }

    @PostMapping("/admin/users/{id}/identity")
    ApiResponse<Void> identity(
        @AuthenticationPrincipal Actor a,
        @PathVariable UUID id,
        @Valid @RequestBody IdentityRequest p
    ) {
        service.identity(a, id, p);
        return ApiResponse.ok(null);
    }
}
