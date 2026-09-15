package cn.zhijie.pojo.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

// 数据库实体或关联查询投影，仅用于持久化与业务层。
public class CertificateEntity {

    private UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String value) {
        this.code = value;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    private String examSystem;

    public String getExamSystem() {
        return examSystem;
    }

    public void setExamSystem(String value) {
        this.examSystem = value;
    }

    private String specialty;

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String value) {
        this.specialty = value;
    }

    private Integer level;

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer value) {
        this.level = value;
    }

    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean value) {
        this.enabled = value;
    }
}
