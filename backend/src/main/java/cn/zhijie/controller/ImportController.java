package cn.zhijie.controller;

import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.query.PageQuery;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.ImportService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/imports")
public class ImportController {

    private final ImportService application;

    public ImportController(ImportService application) {
        this.application = application;
    }

    @GetMapping("/schema")
    JsonNode schema(@AuthenticationPrincipal Actor a) throws Exception {
        return application.schema(a);
    }

    @GetMapping("/dictionary")
    ImportDictionaryResponse dictionary(@AuthenticationPrincipal Actor a) {
        return application.dictionary(a);
    }

    @PostMapping
    ApiResponse<ImportBatchResponse> preview(
        @AuthenticationPrincipal Actor a,
        @RequestHeader("Idempotency-Key") String key,
        @RequestParam MultipartFile file
    ) throws Exception {
        return ApiResponse.ok(application.preview(a, key, file));
    }

    @GetMapping
    ApiResponse<PageResponse<ImportBatchResponse>> list(
        @AuthenticationPrincipal Actor a,
        @Valid @ModelAttribute PageQuery query
    ) {
        return ApiResponse.ok(application.list(a, query));
    }

    @PostMapping("/{id}/commit")
    ApiResponse<ImportBatchResponse> commit(
        @AuthenticationPrincipal Actor a,
        @PathVariable UUID id,
        @Valid @RequestBody ImportCommitRequest p
    ) {
        return ApiResponse.ok(application.commit(a, id, Boolean.TRUE.equals(p.allowUpdates())));
    }
}
