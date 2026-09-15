package cn.zhijie.security;

import cn.zhijie.pojo.SessionRecord;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MemorySessionStore implements SessionStore, RateLimiter, MemberCache, AutoCloseable {

    private final ConcurrentHashMap<String, SessionRecord> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final Clock clock;
    private final ScheduledExecutorService cleaner;

    private record Counter(long value, Instant expiresAt) {}

    public MemorySessionStore(Clock clock) {
        this.clock = clock;
        cleaner = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "local-session-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        cleaner.scheduleWithFixedDelay(this::cleanup, 60, 60, TimeUnit.SECONDS);
    }

    public void create(String id, SessionRecord session, Duration ttl) {
        sessions.put(
            id,
            new SessionRecord(
                session.identityType(),
                session.userId(),
                session.permissionVersion(),
                session.refreshDigest(),
                clock.instant().plus(ttl)
            )
        );
    }

    public Optional<SessionRecord> find(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(
            sessions.computeIfPresent(id, (key, value) ->
                value.expiresAt().isAfter(clock.instant()) ? value : null
            )
        );
    }

    public boolean rotate(String id, String expectedDigest, String nextDigest, Duration ttl) {
        AtomicBoolean rotated = new AtomicBoolean();
        sessions.computeIfPresent(id, (key, value) -> {
            if (!value.expiresAt().isAfter(clock.instant())) return null;
            if (!value.refreshDigest().equals(expectedDigest)) return value;
            rotated.set(true);
            return new SessionRecord(
                value.identityType(),
                value.userId(),
                value.permissionVersion(),
                nextDigest,
                clock.instant().plus(ttl)
            );
        });
        return rotated.get();
    }

    public void revoke(String id) {
        sessions.remove(id);
    }

    public long increment(String key, Duration window) {
        return counters
            .compute(key, (name, value) ->
                value == null || !value.expiresAt().isAfter(clock.instant())
                    ? new Counter(1, clock.instant().plus(window))
                    : new Counter(value.value() + 1, value.expiresAt())
            )
            .value();
    }

    public void cleanup() {
        Instant now = clock.instant();
        sessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        counters.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    // 本地会员资料直接读取数据库，没有需要清理的会员缓存。
    public void invalidate(UUID userId) {}

    public void close() {
        cleaner.shutdownNow();
        sessions.clear();
        counters.clear();
    }
}
