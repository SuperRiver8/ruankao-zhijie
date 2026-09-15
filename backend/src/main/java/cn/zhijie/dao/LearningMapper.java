package cn.zhijie.dao;

import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import java.util.*;
import org.apache.ibatis.annotations.Param;

public interface LearningMapper {
    List<CertificateEntity> targets(UUID id);
    void addTarget(AddTargetCommand p);
    void removeTarget(RemoveTargetCommand p);
    void saveLearning(SaveLearningCommand p);
    List<LearningProjection> learning(UUID id);
    List<LearningProjection> learningPage(@Param("id") UUID id, @Param("query") PageQuery query);
    long learningCount(@Param("id") UUID id, @Param("query") PageQuery query);
}
