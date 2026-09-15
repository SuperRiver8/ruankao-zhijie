package cn.zhijie.pojo.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

// 数据库实体或关联查询投影，仅用于持久化与业务层。
public class ContentProjection {

    private UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    private String kind;

    public String getKind() {
        return kind;
    }

    public void setKind(String value) {
        this.kind = value;
    }

    private String namespace;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String value) {
        this.namespace = value;
    }

    private String externalId;

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String value) {
        this.externalId = value;
    }

    private Integer currentVersion;

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Integer value) {
        this.currentVersion = value;
    }

    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        this.title = value;
    }

    private JsonNode payload;

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode value) {
        this.payload = value;
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

    private Integer version;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer value) {
        this.version = value;
    }
}
