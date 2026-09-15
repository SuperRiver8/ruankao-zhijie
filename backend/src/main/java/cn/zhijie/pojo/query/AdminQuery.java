package cn.zhijie.pojo.query;

import jakarta.validation.constraints.*;

public class AdminQuery extends PageQuery {

    @Size(max = 80)
    private String username;

    @Pattern(regexp = "ADMIN|EDITOR|REVIEWER")
    private String role;

    private Boolean enabled;

    public String getUsername() {
        return username;
    }

    public void setUsername(String value) {
        username = value;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String value) {
        role = value;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean value) {
        enabled = value;
    }
}
