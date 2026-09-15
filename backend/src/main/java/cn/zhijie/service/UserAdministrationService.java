package cn.zhijie.service;

import static cn.zhijie.util.Support.*;

import cn.zhijie.dao.*;
import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.query.*;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.util.ResponseMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
@Service
public class UserAdministrationService {

    private final UserMapper userMapper;
    private final Audit audit;

    public UserAdministrationService(UserMapper userMapper, Audit audit) {
        this.userMapper = userMapper;
        this.audit = audit;
    }

    public PageResponse<UserResponse> users(Actor a, PageQuery query) {
        a.require("ADMIN", "REVIEWER");
        return PageResponse.of(
            userMapper.usersPage(query).stream().map(ResponseMapper::user).toList(),
            userMapper.usersCount(query),
            query
        );
    }

    @Transactional
    public void user(Actor a, UUID id, UserUpdateRequest p) {
        a.require("ADMIN");
        found(userMapper.user(id));
        userMapper.updateUser(new UpdateUserCommand(id, "USER", p.enabled()));
        audit.log(a, "CUSTOMER_STATUS_CHANGED", id, "启用状态：" + p.enabled());
    }
}
