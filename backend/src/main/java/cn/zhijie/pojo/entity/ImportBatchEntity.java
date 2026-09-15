package cn.zhijie.pojo.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

// 数据库实体或关联查询投影，仅用于持久化与业务层。
public class ImportBatchEntity {

    private UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    private UUID ownerId;

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID value) {
        this.ownerId = value;
    }

    private String idempotencyKey;

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String value) {
        this.idempotencyKey = value;
    }

    private String checksum;

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String value) {
        this.checksum = value;
    }

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        this.status = value;
    }

    private JsonNode payload;

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode value) {
        this.payload = value;
    }

    private JsonNode report;

    public JsonNode getReport() {
        return report;
    }

    public void setReport(JsonNode value) {
        this.report = value;
    }

    private Instant createdAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        this.createdAt = value;
    }

    private Boolean allowUpdates;

    public Boolean getAllowUpdates() {
        return allowUpdates;
    }

    public void setAllowUpdates(Boolean value) {
        this.allowUpdates = value;
    }
}
