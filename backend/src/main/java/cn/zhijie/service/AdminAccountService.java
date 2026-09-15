package cn.zhijie.service;

import static cn.zhijie.util.Support.*;

import cn.zhijie.dao.AdminUserMapper;
import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.entity.AdminUserEntity;
import cn.zhijie.pojo.query.*;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountService {

    private final AdminUserMapper users;
    private final Audit audit;

    public AdminAccountService(AdminUserMapper users, Audit audit) {
        this.users = users;
        this.audit = audit;
    }

    private AdminResponse response(AdminUserEntity user) {
        return new AdminResponse(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            user.getEnabled(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    public AdminResponse me(Actor actor) {
        actor.require("ADMIN", "EDITOR", "REVIEWER");
        return response(found(users.user(actor.id())));
    }

    public PageResponse<AdminResponse> list(Actor actor, AdminQuery query) {
        actor.require("ADMIN");
        return PageResponse.of(
            users.page(query).stream().map(this::response).toList(),
            users.count(query),
            query
        );
    }

    @Transactional
    public IdResponse create(Actor actor, AdminCreateRequest request) {
        actor.require("ADMIN");
        check(
            request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72,
            "密码不能超过72字节"
        );
        UUID id = UUID.randomUUID();
        users.insert(
            new SaveAdminCommand(
                id,
                request.username(),
                new BCryptPasswordEncoder().encode(request.password()),
                request.role(),
                request.enabled()
            )
        );
        audit.log(actor, "ADMIN_ACCOUNT_CREATED", id, "创建后台账号，角色：" + request.role());
        return new IdResponse(id);
    }

    @Transactional
    public void update(Actor actor, UUID id, AdminUpdateRequest request) {
        actor.require("ADMIN");
        // 固定顺序锁定后台账户，串行检查最后一个管理员。
        var accounts = users.lockAccounts();
        var current = accounts
            .stream()
            .filter(user -> user.getId().equals(actor.id()))
            .findFirst()
            .orElseThrow();
        check(
            Boolean.TRUE.equals(current.getEnabled()) && "ADMIN".equals(current.getRole()),
            "当前账户已无管理权限"
        );
        var target = found(
            accounts.stream().filter(user -> user.getId().equals(id)).findFirst().orElse(null)
        );
        if (actor.id().equals(id)) check(
            target.getRole().equals(request.role()) && request.enabled(),
            "不能修改自己的角色或禁用自己"
        );
        long remaining = accounts
            .stream()
            .filter(
                user ->
                    !user.getId().equals(id) &&
                    Boolean.TRUE.equals(user.getEnabled()) &&
                    "ADMIN".equals(user.getRole())
            )
            .count();
        check(
            remaining > 0 || (request.enabled() && "ADMIN".equals(request.role())),
            "必须保留至少一个有效管理员"
        );
        users.update(
            new SaveAdminCommand(id, request.username(), null, request.role(), request.enabled())
        );
        audit.log(
            actor,
            "ADMIN_ACCOUNT_UPDATED",
            id,
            "修改后台账号，角色：" + request.role() + "，启用：" + request.enabled()
        );
    }
}
