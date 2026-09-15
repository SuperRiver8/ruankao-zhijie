package cn.zhijie.pojo.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

// 数据库实体或关联查询投影，仅用于持久化与业务层。
public class AttachmentEntity {

    private UUID adminOwnerId;

    public UUID getAdminOwnerId() {
        return adminOwnerId;
    }

    public void setAdminOwnerId(UUID value) {
        adminOwnerId = value;
    }

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

    private String originalName;

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String value) {
        this.originalName = value;
    }

    private String storageKey;

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String value) {
        this.storageKey = value;
    }

    private String mediaType;

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String value) {
        this.mediaType = value;
    }

    private Long size;

    public Long getSize() {
        return size;
    }

    public void setSize(Long value) {
        this.size = value;
    }

    private String checksum;

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String value) {
        this.checksum = value;
    }

    private String accessLevel;

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String value) {
        this.accessLevel = value;
    }

    private Instant createdAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        this.createdAt = value;
    }
}
