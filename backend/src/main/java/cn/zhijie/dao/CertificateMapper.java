package cn.zhijie.dao;

import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import java.util.*;

public interface CertificateMapper {
    List<CertificateEntity> certificates();
    CertificateEntity certificate(UUID id);
    void saveCertificate(SaveCertificateCommand p);
}
