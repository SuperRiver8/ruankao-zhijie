package cn.zhijie.controller;

import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.query.PageQuery;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.ContentService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ContentController {

    private final ContentService application;

    public ContentController(ContentService application) {
        this.application = application;
    }

    @GetMapping("/content")
    ApiResponse<PageResponse<ContentResponse>> list(
        @AuthenticationPrincipal Actor a,
        @RequestParam(required = false) String kind,
        @Valid @ModelAttribute PageQuery query
    ) {
        return ApiResponse.ok(application.list(a, kind, false, query));
    }

    @GetMapping("/admin/content")
    ApiResponse<PageResponse<ContentResponse>> adminList(
        @AuthenticationPrincipal Actor a,
        @RequestParam(required = false) String kind,
        @Valid @ModelAttribute PageQuery query
    ) {
        return ApiResponse.ok(application.list(a, kind, true, query));
    }

    @PostMapping("/admin/content")
    ApiResponse<SaveContentResponse> create(
        @AuthenticationPrincipal Actor a,
        @Valid @RequestBody ContentRequest p
    ) {
        return ApiResponse.ok(application.save(a, null, p));
    }

    @PutMapping("/admin/content/{id}")
    ApiResponse<SaveContentResponse> edit(
        @AuthenticationPrincipal Actor a,
        @PathVariable UUID id,
        @Valid @RequestBody ContentRequest p
    ) {
        return ApiResponse.ok(application.save(a, id, p));
    }

    @PostMapping("/admin/content/{id}/review")
    ApiResponse<Void> review(
        @AuthenticationPrincipal Actor a,
        @PathVariable UUID id,
        @Valid @RequestBody ContentReviewRequest p
    ) {
        application.publish(a, id, p);
        return ApiResponse.ok(null);
    }
}
