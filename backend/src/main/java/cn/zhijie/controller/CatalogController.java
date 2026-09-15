package cn.zhijie.controller;

import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService application;

    public CatalogController(CatalogService application) {
        this.application = application;
    }

    @PostMapping("/admin/certificates")
    ApiResponse<IdResponse> certificate(
        @AuthenticationPrincipal Actor a,
        @Valid @RequestBody CertificateRequest p
    ) {
        return ApiResponse.ok(application.certificate(a, p));
    }
}
