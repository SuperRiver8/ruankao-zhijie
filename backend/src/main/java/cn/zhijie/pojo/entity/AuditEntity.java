package cn.zhijie.pojo.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

// 数据库实体或关联查询投影，仅用于持久化与业务层。
public class AuditEntity {

    private UUID adminActorId;

    public UUID getAdminActorId() {
        return adminActorId;
    }

    public void setAdminActorId(UUID value) {
        adminActorId = value;
    }

    private UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    private UUID actorId;

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID value) {
        this.actorId = value;
    }

    private String action;

    public String getAction() {
        return action;
    }

    public void setAction(String value) {
        this.action = value;
    }

    private UUID targetId;

    public UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(UUID value) {
        this.targetId = value;
    }

    private String detail;

    public String getDetail() {
        return detail;
    }

    public void setDetail(String value) {
        this.detail = value;
    }

    private Instant createdAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        this.createdAt = value;
    }
}
