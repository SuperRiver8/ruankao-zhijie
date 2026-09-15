package cn.zhijie.pojo.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

// 数据库实体或关联查询投影，仅用于持久化与业务层。
public class UserEntity {

    private UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String value) {
        this.username = value;
    }

    private String passwordHash;

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String value) {
        this.passwordHash = value;
    }

    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String value) {
        this.role = value;
    }

    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean value) {
        this.enabled = value;
    }

    private Integer permissionVersion;

    public Integer getPermissionVersion() {
        return permissionVersion;
    }

    public void setPermissionVersion(Integer value) {
        this.permissionVersion = value;
    }

    private Boolean identityVerified;

    public Boolean getIdentityVerified() {
        return identityVerified;
    }

    public void setIdentityVerified(Boolean value) {
        this.identityVerified = value;
    }

    private String identityNameEncrypted;

    public String getIdentityNameEncrypted() {
        return identityNameEncrypted;
    }

    public void setIdentityNameEncrypted(String value) {
        this.identityNameEncrypted = value;
    }

    private String identityEvidence;

    public String getIdentityEvidence() {
        return identityEvidence;
    }

    public void setIdentityEvidence(String value) {
        this.identityEvidence = value;
    }

    private Integer memberLevel;

    public Integer getMemberLevel() {
        return memberLevel;
    }

    public void setMemberLevel(Integer value) {
        this.memberLevel = value;
    }

    private Instant createdAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        this.createdAt = value;
    }
}
