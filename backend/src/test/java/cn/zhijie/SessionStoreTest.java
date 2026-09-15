package cn.zhijie;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.zhijie.config.SessionConfig;
import cn.zhijie.dao.UserMapper;
import cn.zhijie.pojo.IdentityType;
import cn.zhijie.pojo.SessionRecord;
import cn.zhijie.pojo.entity.UserEntity;
import cn.zhijie.pojo.request.LoginRequest;
import cn.zhijie.security.*;
import cn.zhijie.service.AuthService;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.data.redis.RedisHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class SessionStoreTest {

    static class MutableClock extends Clock {

        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        public Clock withZone(ZoneId zone) {
            return this;
        }

        public Instant instant() {
            return now;
        }
    }

    @Test
    void expirationRateWindowRevocationAndRestart() {
        var clock = new MutableClock();
        try (
            var store = new MemorySessionStore(clock);
            var restarted = new MemorySessionStore(clock)
        ) {
            store.create(
                "s",
                new SessionRecord(
                    IdentityType.CUSTOMER,
                    UUID.randomUUID(),
                    1,
                    "digest",
                    clock.instant()
                ),
                Duration.ofSeconds(10)
            );
            assertTrue(store.find("s").isPresent());
            assertTrue(restarted.find("s").isEmpty());
            assertEquals(1, store.increment("login", Duration.ofSeconds(10)));
            assertEquals(2, store.increment("login", Duration.ofSeconds(10)));
            clock.now = clock.now.plusSeconds(10);
            assertTrue(store.find("s").isEmpty());
            assertFalse(store.rotate("s", "digest", "next", Duration.ofDays(7)));
            assertEquals(1, store.increment("login", Duration.ofSeconds(10)));
            store.create(
                "s",
                new SessionRecord(
                    IdentityType.CUSTOMER,
                    UUID.randomUUID(),
                    1,
                    "digest",
                    clock.instant()
                ),
                Duration.ofSeconds(10)
            );
            store.revoke("s");
            assertTrue(store.find("s").isEmpty());
            store.cleanup();
        }
    }

    @Test
    void concurrentRefreshHasOneWinnerAndExtendsExpiration() throws Exception {
        var clock = new MutableClock();
        try (
            var store = new MemorySessionStore(clock);
            var pool = Executors.newFixedThreadPool(8)
        ) {
            store.create(
                "s",
                new SessionRecord(
                    IdentityType.CUSTOMER,
                    UUID.randomUUID(),
                    1,
                    "old",
                    clock.instant()
                ),
                Duration.ofSeconds(10)
            );
            clock.now = clock.now.plusSeconds(5);
            List<Callable<Boolean>> calls = new ArrayList<>();
            for (int i = 0; i < 16; i++) calls.add(() ->
                store.rotate("s", "old", "new", Duration.ofDays(7))
            );
            int winners = 0;
            for (var result : pool.invokeAll(calls)) if (result.get()) winners++;
            assertEquals(1, winners);
            assertEquals(
                clock.instant().plus(Duration.ofDays(7)),
                store.find("s").orElseThrow().expiresAt()
            );
            assertFalse(store.rotate("s", "old", "another", Duration.ofDays(7)));
        }
    }

    @Test
    void loginRefreshLogoutBanAndPermissionVersion() {
        UserMapper users = mock(UserMapper.class);
        var user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername("tester");
        user.setRole("USER");
        user.setEnabled(true);
        user.setPermissionVersion(1);
        user.setPasswordHash(new BCryptPasswordEncoder().encode("test-password"));
        when(users.userByName("tester")).thenReturn(user);
        when(users.user(user.getId())).thenReturn(user);
        try (var store = new MemorySessionStore(Clock.systemUTC())) {
            var auth = new AuthService(
                users,
                mock(cn.zhijie.dao.AdminUserMapper.class),
                mock(cn.zhijie.service.Audit.class),
                store,
                store,
                "test-only-signing-key-at-least-32-characters"
            );
            var login = auth.login(
                IdentityType.CUSTOMER,
                new LoginRequest("tester", "test-password"),
                "local"
            );
            var actor = auth.authenticate(login.accessToken());
            var refresh = auth.refresh(IdentityType.CUSTOMER, login.refreshToken());
            assertThrows(ResponseStatusException.class, () ->
                auth.refresh(IdentityType.CUSTOMER, login.refreshToken())
            );
            assertEquals(actor.id(), auth.authenticate(refresh.accessToken()).id());
            user.setEnabled(false);
            assertThrows(ResponseStatusException.class, () ->
                auth.authenticate(refresh.accessToken())
            );
            assertThrows(ResponseStatusException.class, () ->
                auth.refresh(IdentityType.CUSTOMER, refresh.refreshToken())
            );
            user.setEnabled(true);
            user.setPermissionVersion(2);
            assertThrows(ResponseStatusException.class, () ->
                auth.authenticate(refresh.accessToken())
            );
            var next = auth.login(
                IdentityType.CUSTOMER,
                new LoginRequest("tester", "test-password"),
                "local"
            );
            auth.logout(auth.authenticate(next.accessToken()));
            assertThrows(ResponseStatusException.class, () -> auth.authenticate(next.accessToken())
            );
            assertThrows(ResponseStatusException.class, () ->
                auth.refresh(IdentityType.CUSTOMER, next.refreshToken())
            );
            auth.rate("test", 1, 60);
            assertEquals(
                429,
                assertThrows(ResponseStatusException.class, () -> auth.rate("test", 1, 60))
                    .getStatusCode()
                    .value()
            );
        }
    }

    private ApplicationContextRunner context() {
        return new ApplicationContextRunner()
            .withUserConfiguration(SessionConfig.class)
            .withConfiguration(
                AutoConfigurations.of(RedisHealthContributorAutoConfiguration.class)
            );
    }

    @Test
    void memoryHasNoRedisComponentsOrHealthCheck() {
        context()
            .withPropertyValues("app.jobs-enabled=false")
            .run(ctx -> {
                assertNull(ctx.getStartupFailure());
                assertInstanceOf(MemorySessionStore.class, ctx.getBean(SessionStore.class));
                assertTrue(ctx.getBeansOfType(RedisConnectionFactory.class).isEmpty());
                assertTrue(ctx.getBeansOfType(StringRedisTemplate.class).isEmpty());
                assertFalse(ctx.containsBean("redisHealthContributor"));
            });
    }

    @Test
    void redisSelectionAndConfigurationGuards() {
        context()
            .withPropertyValues("spring.data.redis.host=localhost")
            .run(ctx -> {
                assertNull(ctx.getStartupFailure());
                assertInstanceOf(RedisSessionStore.class, ctx.getBean(SessionStore.class));
                assertTrue(ctx.containsBean("redisHealthContributor"));
            });
        context()
            .withPropertyValues("app.session.store=redis")
            .run(ctx -> assertNotNull(ctx.getStartupFailure()));
        context()
            .withPropertyValues("app.session.store=memory", "spring.data.redis.host=localhost")
            .run(ctx -> {
                assertNull(ctx.getStartupFailure());
                assertInstanceOf(MemorySessionStore.class, ctx.getBean(SessionStore.class));
            });
    }

    @Test
    void configuredRedisFailureIs503WithoutFallback() {
        var redis = mock(StringRedisTemplate.class);
        when(redis.delete(anyList())).thenThrow(
            new org.springframework.data.redis.RedisConnectionFailureException("offline")
        );
        var store = new RedisSessionStore(redis);
        assertEquals(
            503,
            assertThrows(ResponseStatusException.class, () -> store.revoke("s"))
                .getStatusCode()
                .value()
        );
    }
}
