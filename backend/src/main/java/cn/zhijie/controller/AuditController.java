package cn.zhijie.controller;

import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.query.PageQuery;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuditController {

    private final AuditQueryService application;

    public AuditController(AuditQueryService application) {
        this.application = application;
    }

    @GetMapping("/admin/audits")
    ApiResponse<PageResponse<AuditResponse>> audits(
        @AuthenticationPrincipal Actor a,
        @Valid @ModelAttribute PageQuery query
    ) {
        return ApiResponse.ok(application.audits(a, query));
    }
}
