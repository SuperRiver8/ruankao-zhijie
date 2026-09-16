package cn.zhijie.security;

import cn.zhijie.pojo.SessionRecord;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RedisSessionStore implements SessionStore, RateLimiter, MemberCache, CaptchaStore {

    private final StringRedisTemplate redis;

    public RedisSessionStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private <T> T execute(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (org.springframework.dao.DataAccessException error) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Redis 会话服务不可用，请稍后重试",
                error
            );
        }
    }

    public void create(String id, SessionRecord session, Duration ttl) {
        execute(() ->
            redis.execute(
                new DefaultRedisScript<>(
                    "redis.call('SET',KEYS[1],ARGV[1],'EX',ARGV[3]); redis.call('SET',KEYS[2],ARGV[2],'EX',ARGV[3]); return 1",
                    Long.class
                ),
                List.of("session:v2:" + id, "refresh:v2:" + id),
                session.identityType() + ":" + session.userId() + ":" + session.permissionVersion(),
                session.refreshDigest(),
                String.valueOf(ttl.toSeconds())
            )
        );
    }

    public Optional<SessionRecord> find(String id) {
        if (id == null) return Optional.empty();
        String value = execute(() ->
            redis.execute(
                new DefaultRedisScript<>(
                    "local s=redis.call('GET',KEYS[1]); local r=redis.call('GET',KEYS[2]); local ttl=math.min(redis.call('PTTL',KEYS[1]),redis.call('PTTL',KEYS[2])); if not s or not r or ttl<=0 then return nil end; return s..'|'..r..'|'..ttl",
                    String.class
                ),
                List.of("session:v2:" + id, "refresh:v2:" + id)
            )
        );
        if (value == null) return Optional.empty();
        String[] fields = value.split("\\|"), identity = fields[0].split(":");
        return Optional.of(
            new SessionRecord(
                cn.zhijie.pojo.IdentityType.valueOf(identity[0]),
                UUID.fromString(identity[1]),
                Integer.parseInt(identity[2]),
                fields[1],
                Instant.now().plusMillis(Long.parseLong(fields[2]))
            )
        );
    }

    public boolean rotate(String id, String expectedDigest, String nextDigest, Duration ttl) {
        return Long.valueOf(1).equals(
            execute(() ->
                redis.execute(
                    new DefaultRedisScript<>(
                        "if redis.call('GET',KEYS[1]) == ARGV[1] and redis.call('EXISTS',KEYS[2]) == 1 then redis.call('SET',KEYS[1],ARGV[2],'EX',ARGV[3]); redis.call('EXPIRE',KEYS[2],ARGV[3]); return 1 end; return 0",
                        Long.class
                    ),
                    List.of("refresh:v2:" + id, "session:v2:" + id),
                    expectedDigest,
                    nextDigest,
                    String.valueOf(ttl.toSeconds())
                )
            )
        );
    }

    public void revoke(String id) {
        execute(() -> redis.delete(List.of("session:v2:" + id, "refresh:v2:" + id)));
    }

    public long increment(String key, Duration window) {
        Long count = execute(() ->
            redis.execute(
                new DefaultRedisScript<>(
                    "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]); end; return n",
                    Long.class
                ),
                List.of("rate:" + key),
                String.valueOf(window.toSeconds())
            )
        );
        if (count == null) throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "限流服务不可用"
        );
        return count;
    }

    public void invalidate(UUID userId) {
        execute(() -> redis.delete("member:" + userId));
    }

    public long failures(String key, Duration window, boolean record) {
        // 使用 Redis 时钟，避免多实例时钟偏差；随机成员保留同毫秒的多次失败。
        Long result = execute(() ->
            redis.execute(
                new DefaultRedisScript<>(
                    "local t=redis.call('TIME'); local now=t[1]*1000+math.floor(t[2]/1000); " +
                    "redis.call('ZREMRANGEBYSCORE',KEYS[1],'-inf',now-ARGV[1]); " +
                    "if ARGV[2]=='1' then redis.call('ZADD',KEYS[1],now,ARGV[3]); " +
                    "redis.call('PEXPIRE',KEYS[1],ARGV[1]); end; " +
                    "return redis.call('ZCOUNT',KEYS[1],'-inf',now)",
                    Long.class
                ),
                List.of("captcha:fail:" + key),
                String.valueOf(window.toMillis()),
                record ? "1" : "0",
                UUID.randomUUID().toString()
            )
        );
        if (result == null) throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "验证码服务不可用"
        );
        return result;
    }

    public void clearFailures(String key) {
        execute(() -> redis.delete("captcha:fail:" + key));
    }

    public void saveChallenge(String id, String value, Duration ttl) {
        execute(() -> {
            redis.opsForValue().set("captcha:challenge:" + id, value, ttl);
            return true;
        });
    }

    public Optional<String> consumeChallenge(String id) {
        return Optional.ofNullable(
            execute(() ->
                redis.execute(
                    new DefaultRedisScript<>(
                        "local v=redis.call('GET',KEYS[1]); redis.call('DEL',KEYS[1]); return v",
                        String.class
                    ),
                    List.of("captcha:challenge:" + id)
                )
            )
        );
    }
}
