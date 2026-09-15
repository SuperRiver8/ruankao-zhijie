package cn.zhijie.dao;

import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import java.util.*;

public interface MembershipMapper {
    List<MemberRuleEntity> rules();
    void saveRule(SaveRuleCommand p);
    List<BadgeProjection> badges(UUID id);
    void grantCertificate(GrantCertificateCommand p);
    void revokeCertificate(UUID id);
    int calculatedLevel(UUID id);
    void setLevel(SetLevelCommand p);
    void levelHistory(LevelHistoryCommand p);
    List<LevelHistoryEntity> levelHistories(UUID id);
}
