package cn.zhijie.util;

import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.response.*;
import java.time.Instant;

// 显式白名单映射，持久化敏感字段不会直接序列化给客户端。
public final class ResponseMapper {

    private ResponseMapper() {}

    public static UserResponse user(UserEntity e) {
        return new UserResponse(
            e.getId(),
            e.getUsername(),
            e.getRole(),
            e.getEnabled(),
            e.getIdentityVerified(),
            e.getMemberLevel()
        );
    }

    public static CertificateResponse certificate(CertificateEntity e) {
        return new CertificateResponse(
            e.getId(),
            e.getCode(),
            e.getName(),
            e.getExamSystem(),
            e.getSpecialty(),
            e.getLevel(),
            e.getEnabled()
        );
    }

    public static MemberRuleResponse memberRule(MemberRuleEntity e) {
        return new MemberRuleResponse(
            e.getLevel(),
            e.getName(),
            e.getIcon(),
            e.getSortOrder(),
            e.getEntitlements()
        );
    }

    public static BadgeResponse badge(BadgeProjection e) {
        return new BadgeResponse(e.getName(), e.getLevel(), e.getSpecialty(), e.getCertifiedAt());
    }

    public static LevelHistoryResponse levelHistory(LevelHistoryEntity e) {
        return new LevelHistoryResponse(
            e.getId(),
            e.getUserId(),
            e.getOldLevel(),
            e.getNewLevel(),
            e.getApplicationId(),
            e.getCreatedAt()
        );
    }

    public static AuditResponse audit(AuditEntity e) {
        return new AuditResponse(
            e.getId(),
            e.getActorId(), e.getAdminActorId(),
            e.getAction(),
            e.getTargetId(),
            e.getDetail(),
            e.getCreatedAt()
        );
    }

    public static LearningResponse learning(LearningProjection e) {
        return new LearningResponse(
            e.getUserId(),
            e.getEntityId(),
            e.getVersion(),
            e.getFavorite(),
            e.getWrong(),
            e.getProgress(),
            e.getUpdatedAt(),
            e.getTitle()
        );
    }

    public static ContentResponse content(ContentProjection e) {
        return new ContentResponse(
            e.getId(),
            e.getKind(),
            e.getNamespace(),
            e.getExternalId(),
            e.getCurrentVersion(),
            e.getTitle(),
            e.getPayload(),
            e.getStatus(),
            e.getVersion()
        );
    }

    public static ImportBatchResponse importBatch(ImportBatchEntity e) {
        return new ImportBatchResponse(
            e.getId(),
            e.getOwnerId(),
            e.getIdempotencyKey(),
            e.getChecksum(),
            e.getStatus(),
            e.getPayload(),
            e.getReport(),
            e.getCreatedAt(),
            e.getAllowUpdates()
        );
    }

    public static ExamResponse exam(ExamEntity e) {
        return new ExamResponse(
            e.getId(),
            e.getUserId(),
            e.getPaperId(),
            e.getSnapshot(),
            e.getAnswers(),
            e.getResult(),
            e.getStartedAt(),
            e.getDeadline(),
            e.getSubmittedAt(),
            e.getStatus(),
            Instant.now()
        );
    }

    public static ExamSummaryResponse examSummary(ExamEntity e) {
        return new ExamSummaryResponse(
            e.getId(),
            e.getPaperId(),
            e.getStatus(),
            e.getStartedAt(),
            e.getDeadline(),
            e.getResult()
        );
    }

    public static ReviewResponse review(ReviewEntity e) {
        return new ReviewResponse(
            e.getId(),
            e.getApplicationId(),
            e.getReviewerId(),
            e.getAction(),
            e.getReason(),
            e.getCreatedAt()
        );
    }

    public static VerificationResponse verification(VerificationEntity e) {
        return new VerificationResponse(
            e.getId(),
            e.getApplicationId(),
            e.getReviewerId(),
            e.getResult(),
            e.getOfficialSource(),
            e.getEvidence(),
            e.getOwnershipEvidence(),
            e.getCreatedAt()
        );
    }
}
