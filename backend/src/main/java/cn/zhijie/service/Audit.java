package cn.zhijie.service;

import static cn.zhijie.util.Support.*;

import cn.zhijie.dao.AuditMapper;
import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import cn.zhijie.pojo.request.*;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class Audit {

    private final AuditMapper auditMapper;

    public Audit(AuditMapper auditMapper) {
        this.auditMapper = auditMapper;
    }

    public void log(cn.zhijie.pojo.Actor actor, String action, UUID target, String detail) {
        auditMapper.audit(
            new AuditCommand(
                UUID.randomUUID(),
                actor == null || actor.isAdmin() ? null : actor.id(),
                actor != null && actor.isAdmin() ? actor.id() : null,
                action,
                target,
                detail
            )
        );
    }
}
