package cn.zhijie.pojo;

import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// 当前登录身份，只用于服务端鉴权，不作为用户资料响应。
public record Actor(UUID id, String role, String session, IdentityType identityType) {
    public boolean isAdmin() {
        return identityType == IdentityType.ADMIN;
    }
    public boolean ownsCustomer(UUID userId) {
        return identityType == IdentityType.CUSTOMER && id.equals(userId);
    }
    public void requireCustomer() {
        if (identityType != IdentityType.CUSTOMER) throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "仅限用户端账户操作"
        );
    }
    public void require(String... roles) {
        if (!isAdmin() || !Set.of(roles).contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无操作权限");
        }
    }
}
