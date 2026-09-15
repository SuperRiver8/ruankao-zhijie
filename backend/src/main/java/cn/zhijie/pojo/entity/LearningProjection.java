package cn.zhijie.pojo.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

// 数据库实体或关联查询投影，仅用于持久化与业务层。
public class LearningProjection {

    private UUID userId;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID value) {
        this.userId = value;
    }

    private UUID entityId;

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID value) {
        this.entityId = value;
    }

    private Integer version;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer value) {
        this.version = value;
    }

    private Boolean favorite;

    public Boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(Boolean value) {
        this.favorite = value;
    }

    private Boolean wrong;

    public Boolean getWrong() {
        return wrong;
    }

    public void setWrong(Boolean value) {
        this.wrong = value;
    }

    private Integer progress;

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer value) {
        this.progress = value;
    }

    private Instant updatedAt;

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant value) {
        this.updatedAt = value;
    }

    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        this.title = value;
    }
}
