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
public class MemberRuleController {

    private final MemberRuleService application;

    public MemberRuleController(MemberRuleService application) {
        this.application = application;
    }

    @PutMapping("/admin/member-rules/{level}")
    ApiResponse<Void> rule(
        @AuthenticationPrincipal Actor a,
        @PathVariable int level,
        @Valid @RequestBody MemberRuleRequest p
    ) {
        application.rule(a, level, p);
        return ApiResponse.ok(null);
    }

    @GetMapping("/admin/member-rules")
    ApiResponse<List<MemberRuleResponse>> list(@AuthenticationPrincipal Actor actor) {
        return ApiResponse.ok(application.list(actor));
    }
}
