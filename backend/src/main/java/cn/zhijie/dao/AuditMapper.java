package cn.zhijie.dao;

import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import java.util.*;
import org.apache.ibatis.annotations.Param;

public interface AuditMapper {
    void audit(AuditCommand p);
    List<AuditEntity> audits();
    List<AuditEntity> auditsPage(@Param("query") PageQuery query);
    long auditsCount(@Param("query") PageQuery query);
}
