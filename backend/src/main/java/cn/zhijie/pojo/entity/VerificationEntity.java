package cn.zhijie.pojo.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

// 数据库实体或关联查询投影，仅用于持久化与业务层。
public class VerificationEntity {

    private UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    private UUID applicationId;

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID value) {
        this.applicationId = value;
    }

    private UUID reviewerId;

    public UUID getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(UUID value) {
        this.reviewerId = value;
    }

    private String result;

    public String getResult() {
        return result;
    }

    public void setResult(String value) {
        this.result = value;
    }

    private String officialSource;

    public String getOfficialSource() {
        return officialSource;
    }

    public void setOfficialSource(String value) {
        this.officialSource = value;
    }

    private String evidence;

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String value) {
        this.evidence = value;
    }

    private String ownershipEvidence;

    public String getOwnershipEvidence() {
        return ownershipEvidence;
    }

    public void setOwnershipEvidence(String value) {
        this.ownershipEvidence = value;
    }

    private Instant createdAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        this.createdAt = value;
    }
}
