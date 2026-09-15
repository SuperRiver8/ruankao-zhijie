package cn.zhijie.pojo.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

// 数据库实体或关联查询投影，仅用于持久化与业务层。
public class BadgeProjection {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    private Integer level;

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer value) {
        this.level = value;
    }

    private String specialty;

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String value) {
        this.specialty = value;
    }

    private Instant certifiedAt;

    public Instant getCertifiedAt() {
        return certifiedAt;
    }

    public void setCertifiedAt(Instant value) {
        this.certifiedAt = value;
    }
}
