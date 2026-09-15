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
public class UserAdministrationController {

    private final UserAdministrationService application;

    public UserAdministrationController(UserAdministrationService application) {
        this.application = application;
    }

    @GetMapping("/admin/users")
    ApiResponse<PageResponse<UserResponse>> users(
        @AuthenticationPrincipal Actor a,
        @Valid @ModelAttribute PageQuery query
    ) {
        return ApiResponse.ok(application.users(a, query));
    }

    @PutMapping("/admin/users/{id}")
    ApiResponse<Void> user(
        @AuthenticationPrincipal Actor a,
        @PathVariable UUID id,
        @Valid @RequestBody UserUpdateRequest p
    ) {
        application.user(a, id, p);
        return ApiResponse.ok(null);
    }
}
