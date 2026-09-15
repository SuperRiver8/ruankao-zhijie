package cn.zhijie.security;

import java.util.UUID;

public interface MemberCache {
    void invalidate(UUID userId);
}
