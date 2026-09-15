package cn.zhijie.security;

import cn.zhijie.pojo.SessionRecord;
import java.time.Duration;
import java.util.Optional;

public interface SessionStore {
    void create(String id, SessionRecord session, Duration ttl);
    Optional<SessionRecord> find(String id);
    boolean rotate(String id, String expectedDigest, String nextDigest, Duration ttl);
    void revoke(String id);
}
