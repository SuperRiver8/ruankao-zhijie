package cn.zhijie.dao;

import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import java.util.*;
import org.apache.ibatis.annotations.Param;

public interface CertificationMapper {
    void insertApplication(InsertApplicationCommand p);
    ApplicationProjection application(UUID id);
    ApplicationProjection lockApplication(UUID id);
    List<ApplicationProjection> applications(@Param("userId") UUID userId);
    int duplicateCount(DuplicateCountCommand p);
    void snapshotApplication(UUID id);
    void resubmitApplication(ResubmitApplicationCommand p);
    void setApplicationState(SetApplicationStateCommand p);
    void insertVerification(InsertVerificationCommand p);
    List<VerificationEntity> verifications(UUID id);
    void insertReview(InsertReviewCommand p);
    List<ReviewEntity> reviews(UUID id);
    List<ApplicationProjection> applicationsPage(
        @Param("userId") UUID userId,
        @Param("query") PageQuery query
    );
    long applicationsCount(@Param("userId") UUID userId, @Param("query") PageQuery query);
}
