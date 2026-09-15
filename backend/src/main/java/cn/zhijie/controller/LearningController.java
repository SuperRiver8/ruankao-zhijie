package cn.zhijie.controller;

import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.query.PageQuery;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.LearningService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@org.springframework.security.access.prepost.PreAuthorize("hasAuthority('IDENTITY_CUSTOMER')")
@RequestMapping("/api")
public class LearningController {

    private final LearningService application;

    public LearningController(LearningService application) {
        this.application = application;
    }

    @GetMapping("/learning")
    ApiResponse<PageResponse<LearningResponse>> learning(
        @AuthenticationPrincipal Actor a,
        @Valid @ModelAttribute PageQuery query
    ) {
        return ApiResponse.ok(application.learning(a, query));
    }

    @PutMapping("/learning/{id}")
    ApiResponse<Void> learning(
        @AuthenticationPrincipal Actor a,
        @PathVariable UUID id,
        @Valid @RequestBody LearningRequest p
    ) {
        application.learning(a, id, p);
        return ApiResponse.ok(null);
    }
}
