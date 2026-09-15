package cn.zhijie.service;

import static cn.zhijie.util.Support.*;

import cn.zhijie.dao.CertificateMapper;
import cn.zhijie.dao.LearningMapper;
import cn.zhijie.dao.MembershipMapper;
import cn.zhijie.dao.UserMapper;
import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.service.AuthService;
import cn.zhijie.util.ResponseMapper;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final CertificateMapper certificateMapper;
    private final MembershipMapper membershipMapper;
    private final LearningMapper learningMapper;

    public UserService(
        UserMapper userMapper,
        CertificateMapper certificateMapper,
        MembershipMapper membershipMapper,
        LearningMapper learningMapper
    ) {
        this.userMapper = userMapper;
        this.certificateMapper = certificateMapper;
        this.membershipMapper = membershipMapper;
        this.learningMapper = learningMapper;
    }

    public ProfileResponse me(Actor a) {
        a.requireCustomer();
        var u = userMapper.user(a.id());
        return new ProfileResponse(
            a.id(),
            u.getUsername(),
            a.role(),
            u.getIdentityVerified(),
            u.getMemberLevel(),
            membershipMapper.badges(a.id()).stream().map(ResponseMapper::badge).toList(),
            learningMapper.targets(a.id()).stream().map(ResponseMapper::certificate).toList(),
            membershipMapper.rules().stream().map(ResponseMapper::memberRule).toList(),
            membershipMapper
                .levelHistories(a.id())
                .stream()
                .map(ResponseMapper::levelHistory)
                .toList()
        );
    }

    public List<CertificateResponse> certificates() {
        return certificateMapper.certificates().stream().map(ResponseMapper::certificate).toList();
    }

    public void target(Actor a, UUID id) {
        a.requireCustomer();
        found(certificateMapper.certificate(id));
        learningMapper.addTarget(new AddTargetCommand(a.id(), id));
    }

    public void untarget(Actor a, UUID id) {
        a.requireCustomer();
        learningMapper.removeTarget(new RemoveTargetCommand(a.id(), id));
    }
}
