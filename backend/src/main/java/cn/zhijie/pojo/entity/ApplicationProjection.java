package cn.zhijie.pojo.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

// 数据库实体或关联查询投影，仅用于持久化与业务层。
public class ApplicationProjection {

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

    private UUID certificateId;

    public UUID getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(UUID value) {
        this.certificateId = value;
    }

    private String holderNameEncrypted;

    public String getHolderNameEncrypted() {
        return holderNameEncrypted;
    }

    public void setHolderNameEncrypted(String value) {
        this.holderNameEncrypted = value;
    }

    private String numberType;

    public String getNumberType() {
        return numberType;
    }

    public void setNumberType(String value) {
        this.numberType = value;
    }

    private String numberEncrypted;

    public String getNumberEncrypted() {
        return numberEncrypted;
    }

    public void setNumberEncrypted(String value) {
        this.numberEncrypted = value;
    }

    private String numberHash;

    public String getNumberHash() {
        return numberHash;
    }

    public void setNumberHash(String value) {
        this.numberHash = value;
    }

    private LocalDate obtainedOn;

    public LocalDate getObtainedOn() {
        return obtainedOn;
    }

    public void setObtainedOn(LocalDate value) {
        this.obtainedOn = value;
    }

    private UUID attachmentId;

    public UUID getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(UUID value) {
        this.attachmentId = value;
    }

    private UUID auxiliaryId;

    public UUID getAuxiliaryId() {
        return auxiliaryId;
    }

    public void setAuxiliaryId(UUID value) {
        this.auxiliaryId = value;
    }

    private String note;

    public String getNote() {
        return note;
    }

    public void setNote(String value) {
        this.note = value;
    }

    private Integer revision;

    public Integer getRevision() {
        return revision;
    }

    public void setRevision(Integer value) {
        this.revision = value;
    }

    private String autoResult;

    public String getAutoResult() {
        return autoResult;
    }

    public void setAutoResult(String value) {
        this.autoResult = value;
    }

    private JsonNode autoDetails;

    public JsonNode getAutoDetails() {
        return autoDetails;
    }

    public void setAutoDetails(JsonNode value) {
        this.autoDetails = value;
    }

    private String officialResult;

    public String getOfficialResult() {
        return officialResult;
    }

    public void setOfficialResult(String value) {
        this.officialResult = value;
    }

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        this.status = value;
    }

    private Instant createdAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        this.createdAt = value;
    }

    private Instant updatedAt;

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant value) {
        this.updatedAt = value;
    }

    private String certificateName;

    public String getCertificateName() {
        return certificateName;
    }

    public void setCertificateName(String value) {
        this.certificateName = value;
    }

    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String value) {
        this.username = value;
    }
}
