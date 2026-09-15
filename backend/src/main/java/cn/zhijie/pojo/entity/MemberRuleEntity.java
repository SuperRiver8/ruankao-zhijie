package cn.zhijie.pojo.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.UUID;

// 数据库实体或关联查询投影，仅用于持久化与业务层。
public class MemberRuleEntity {

    private Integer level;

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer value) {
        this.level = value;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    private String icon;

    public String getIcon() {
        return icon;
    }

    public void setIcon(String value) {
        this.icon = value;
    }

    private Integer sortOrder;

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer value) {
        this.sortOrder = value;
    }

    private JsonNode entitlements;

    public JsonNode getEntitlements() {
        return entitlements;
    }

    public void setEntitlements(JsonNode value) {
        this.entitlements = value;
    }
}
