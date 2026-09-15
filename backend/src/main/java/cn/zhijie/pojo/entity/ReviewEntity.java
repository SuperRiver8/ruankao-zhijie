package cn.zhijie.pojo.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

// 数据库实体或关联查询投影，仅用于持久化与业务层。
public class ReviewEntity {

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

    private String action;

    public String getAction() {
        return action;
    }

    public void setAction(String value) {
        this.action = value;
    }

    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String value) {
        this.reason = value;
    }

    private Instant createdAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        this.createdAt = value;
    }
}
