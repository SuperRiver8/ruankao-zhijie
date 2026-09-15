package cn.zhijie.pojo.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

// 数据库实体或关联查询投影，仅用于持久化与业务层。
public class LevelHistoryEntity {

    private UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    private UUID userId;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID value) {
        this.userId = value;
    }

    private Integer oldLevel;

    public Integer getOldLevel() {
        return oldLevel;
    }

    public void setOldLevel(Integer value) {
        this.oldLevel = value;
    }

    private Integer newLevel;

    public Integer getNewLevel() {
        return newLevel;
    }

    public void setNewLevel(Integer value) {
        this.newLevel = value;
    }

    private UUID applicationId;

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID value) {
        this.applicationId = value;
    }

    private Instant createdAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        this.createdAt = value;
    }
}
