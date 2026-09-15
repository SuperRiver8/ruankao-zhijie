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
public class AuditQueryService {

    private final AuditMapper auditMapper;

    public AuditQueryService(AuditMapper auditMapper) {
        this.auditMapper = auditMapper;
    }

    public PageResponse<AuditResponse> audits(Actor a, PageQuery query) {
        a.require("ADMIN");
        return PageResponse.of(
            auditMapper.auditsPage(query).stream().map(ResponseMapper::audit).toList(),
            auditMapper.auditsCount(query),
            query
        );
    }
}
