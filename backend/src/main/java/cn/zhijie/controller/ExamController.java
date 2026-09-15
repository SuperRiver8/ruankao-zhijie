package cn.zhijie.controller;

import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.query.PageQuery;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.ExamService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@org.springframework.security.access.prepost.PreAuthorize("hasAuthority('IDENTITY_CUSTOMER')")
@RequestMapping("/api")
public class ExamController {

    private final ExamService application;

    public ExamController(ExamService application) {
        this.application = application;
    }

    @PostMapping("/exams/start/{paperId}")
    ApiResponse<ExamResponse> start(@AuthenticationPrincipal Actor a, @PathVariable UUID paperId) {
        return ApiResponse.ok(application.start(a, paperId));
    }

    @PostMapping("/practice")
    ApiResponse<ExamResponse> practice(
        @AuthenticationPrincipal Actor a,
        @Valid @RequestBody PracticeRequest p
    ) {
        return ApiResponse.ok(application.practice(a, p));
    }

    @PostMapping("/exams/{id}/self-grade")
    ApiResponse<ExamResponse> selfGrade(
        @AuthenticationPrincipal Actor a,
        @PathVariable UUID id,
        @Valid @RequestBody SelfGradeRequest p
    ) {
        return ApiResponse.ok(application.selfGrade(a, id, p));
    }

    @GetMapping("/exams")
    ApiResponse<PageResponse<ExamSummaryResponse>> list(
        @AuthenticationPrincipal Actor a,
        @Valid @ModelAttribute PageQuery query
    ) {
        return ApiResponse.ok(application.list(a, query));
    }

    @GetMapping("/exams/{id}")
    ApiResponse<ExamResponse> read(@AuthenticationPrincipal Actor a, @PathVariable UUID id) {
        return ApiResponse.ok(application.read(a, id));
    }

    @PutMapping("/exams/{id}/answers")
    ApiResponse<SaveAnswersResponse> save(
        @AuthenticationPrincipal Actor a,
        @PathVariable UUID id,
        @Valid @RequestBody AnswersRequest p
    ) {
        return ApiResponse.ok(application.save(a, id, p));
    }

    @PostMapping("/exams/{id}/submit")
    ApiResponse<ExamResponse> submit(@AuthenticationPrincipal Actor a, @PathVariable UUID id) {
        return ApiResponse.ok(application.submit(a, id));
    }
}
