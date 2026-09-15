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

@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
@Service
public class CatalogService {

    private final CertificateMapper certificateMapper;
    private final Audit audit;

    public CatalogService(CertificateMapper certificateMapper, Audit audit) {
        this.certificateMapper = certificateMapper;
        this.audit = audit;
    }

    @Transactional
    public IdResponse certificate(Actor a, CertificateRequest p) {
        a.require("ADMIN", "EDITOR");
        UUID id = p.id() != null ? p.id() : UUID.randomUUID();
        int level = p.level();
        check(level >= 1 && level <= 3, "证书级别无效");
        certificateMapper.saveCertificate(
            new SaveCertificateCommand(
                id,
                required(p.code(), "code"),
                required(p.name(), "name"),
                required(p.specialty(), "specialty"),
                level
            )
        );
        audit.log(a, "CATALOG_SAVED", id, "保存证书目录；已有证书级别不变");
        return new IdResponse(id);
    }
}
