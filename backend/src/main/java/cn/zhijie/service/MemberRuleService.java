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

@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN')")
@Service
public class MemberRuleService {

    private final MembershipMapper membershipMapper;
    private final Audit audit;

    public MemberRuleService(MembershipMapper membershipMapper, Audit audit) {
        this.membershipMapper = membershipMapper;
        this.audit = audit;
    }

    @Transactional
    public void rule(Actor a, int level, MemberRuleRequest p) {
        a.require("ADMIN");
        check(level >= 0 && level <= 3, "等级无效");
        membershipMapper.saveRule(
            new SaveRuleCommand(
                level,
                required(p.name(), "name"),
                required(p.icon(), "icon"),
                p.sort(),
                p.entitlements() == null ? JSON.createObjectNode() : p.entitlements()
            )
        );
        audit.log(a, "MEMBER_RULE_SAVED", null, "等级 " + level);
    }

    public List<MemberRuleResponse> list(Actor actor) {
        actor.require("ADMIN");
        return membershipMapper.rules().stream().map(ResponseMapper::memberRule).toList();
    }
}
