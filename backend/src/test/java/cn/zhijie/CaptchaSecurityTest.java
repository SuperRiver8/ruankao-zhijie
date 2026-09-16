package cn.zhijie;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.zhijie.config.CaptchaProperties;
import cn.zhijie.dao.*;
import cn.zhijie.pojo.IdentityType;
import cn.zhijie.pojo.entity.UserEntity;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.request.CaptchaChallengeRequest.Scene;
import cn.zhijie.security.*;
import cn.zhijie.service.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class CaptchaSecurityTest {

    static final CaptchaProperties CONFIG = new CaptchaProperties(120, 2, 120, 5, 30, 60);

    static class TestClock extends Clock {

        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        public Instant instant() {
            return now;
        }

        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    // 仅在测试中捕获答案，生产响应不公开答案。
    static class TestStore extends MemorySessionStore {

        String latestId;
        double latestX;

        TestStore(Clock clock) {
            super(clock);
        }

        public void saveChallenge(String id, String value, Duration ttl) {
            super.saveChallenge(id, value, ttl);
            latestId = id;
            latestX = Double.parseDouble(value.split(":")[1]);
        }

        CaptchaAnswer answer() {
            return new CaptchaAnswer(latestId, latestX);
        }
    }

    CaptchaAnswer issue(
        CaptchaService captcha,
        TestStore store,
        Scene scene,
        String name,
        String ip
    ) {
        var response = captcha.challenge(new CaptchaChallengeRequest(scene, name), ip);
        assertEquals(320, response.width());
        assertEquals(160, response.height());
        assertTrue(Base64.getDecoder().decode(response.background()).length > 100);
        return store.answer();
    }

    @Test
    void secondFailureRequiresCaptchaAndSuccessClearsIt() {
        try (var store = new TestStore(Clock.systemUTC())) {
            var captcha = new CaptchaService(store, store, CONFIG);
            var users = mock(UserMapper.class);
            var user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEnabled(true);
            user.setPermissionVersion(1);
            user.setPasswordHash(new BCryptPasswordEncoder().encode("correct-password"));
            when(users.userByName("tester")).thenReturn(user);
            var auth = new AuthService(
                users,
                mock(AdminUserMapper.class),
                mock(Audit.class),
                store,
                store,
                captcha,
                "test-only-signing-key-at-least-32-characters"
            );
            var wrong = new LoginRequest("tester", "wrong");
            assertEquals(
                401,
                assertThrows(ResponseStatusException.class, () ->
                    auth.login(IdentityType.CUSTOMER, wrong, "ip")
                )
                    .getStatusCode()
                    .value()
            );
            assertEquals(
                "CAPTCHA_REQUIRED",
                assertThrows(CaptchaException.class, () ->
                    auth.login(IdentityType.CUSTOMER, wrong, "ip")
                ).code()
            );
            clearInvocations(users);
            assertThrows(CaptchaException.class, () ->
                auth.login(
                    IdentityType.CUSTOMER,
                    new LoginRequest("tester", "correct-password"),
                    "ip"
                )
            );
            verifyNoInteractions(users);
            var answer = issue(captcha, store, Scene.CUSTOMER_LOGIN, "tester", "ip");
            assertThrows(CaptchaException.class, () ->
                auth.login(IdentityType.CUSTOMER, new LoginRequest("tester", "wrong", answer), "ip")
            );
            assertTrue(captcha.required(IdentityType.CUSTOMER, "tester", "ip"));
            assertEquals(
                "CAPTCHA_EXPIRED",
                assertThrows(CaptchaException.class, () ->
                    captcha.verify(Scene.CUSTOMER_LOGIN, "tester", "ip", answer)
                ).code()
            );
            var next = issue(captcha, store, Scene.CUSTOMER_LOGIN, "tester", "ip");
            assertNotNull(
                auth
                    .login(
                        IdentityType.CUSTOMER,
                        new LoginRequest("tester", "correct-password", next),
                        "ip"
                    )
                    .accessToken()
            );
            assertFalse(captcha.required(IdentityType.CUSTOMER, "tester", "ip"));
            assertThrows(CaptchaException.class, () ->
                auth.register(new LoginRequest("tester", "correct-password"), "ip")
            );
            verify(users, never()).insertUser(any());
        }
    }

    @Test
    void rollingWindowBoundaryAndIdentityIsolation() {
        var clock = new TestClock();
        try (var store = new TestStore(clock)) {
            var captcha = new CaptchaService(store, store, CONFIG);
            assertFalse(captcha.failed(IdentityType.CUSTOMER, "alice", "ip1"));
            clock.now = clock.now.plusSeconds(60);
            assertTrue(captcha.failed(IdentityType.CUSTOMER, "alice", "ip1"));
            assertFalse(captcha.required(IdentityType.ADMIN, "alice", "ip1"));
            assertFalse(captcha.required(IdentityType.CUSTOMER, "bob", "ip1"));
            assertFalse(captcha.required(IdentityType.CUSTOMER, "alice", "ip2"));
            clock.now = clock.now.plusSeconds(59);
            assertTrue(captcha.required(IdentityType.CUSTOMER, "alice", "ip1"));
            clock.now = clock.now.plusSeconds(1);
            assertFalse(captcha.required(IdentityType.CUSTOMER, "alice", "ip1"));
            assertTrue(captcha.failed(IdentityType.CUSTOMER, "alice", "ip1"));
        }
    }

    @Test
    void challengeBindingExpiryToleranceAndConsumption() {
        var clock = new TestClock();
        try (var store = new TestStore(clock)) {
            var captcha = new CaptchaService(store, store, CONFIG);
            var answer = issue(captcha, store, Scene.CUSTOMER_REGISTER, "alice", "ip");
            assertEquals(
                "CAPTCHA_INVALID",
                assertThrows(CaptchaException.class, () ->
                    captcha.verify(Scene.CUSTOMER_LOGIN, "alice", "ip", answer)
                ).code()
            );
            assertThrows(CaptchaException.class, () ->
                captcha.verify(Scene.CUSTOMER_REGISTER, "alice", "ip", answer)
            );
            for (String[] binding : List.of(
                new String[] { "bob", "ip" },
                new String[] { "alice", "other" }
            )) {
                var bound = issue(captcha, store, Scene.CUSTOMER_REGISTER, "alice", "ip");
                assertThrows(CaptchaException.class, () ->
                    captcha.verify(Scene.CUSTOMER_REGISTER, binding[0], binding[1], bound)
                );
            }
            var expired = issue(captcha, store, Scene.CUSTOMER_REGISTER, "alice", "ip");
            clock.now = clock.now.plusSeconds(120);
            assertEquals(
                "CAPTCHA_EXPIRED",
                assertThrows(CaptchaException.class, () ->
                    captcha.verify(Scene.CUSTOMER_REGISTER, "alice", "ip", expired)
                ).code()
            );
            var valid = issue(captcha, store, Scene.CUSTOMER_REGISTER, "alice", "ip");
            captcha.verify(
                Scene.CUSTOMER_REGISTER,
                "alice",
                "ip",
                new CaptchaAnswer(valid.challengeId(), valid.offsetX() + 5)
            );
            var invalid = issue(captcha, store, Scene.CUSTOMER_REGISTER, "alice", "ip");
            assertThrows(CaptchaException.class, () ->
                captcha.verify(
                    Scene.CUSTOMER_REGISTER,
                    "alice",
                    "ip",
                    new CaptchaAnswer(invalid.challengeId(), invalid.offsetX() + 6)
                )
            );
            assertThrows(CaptchaException.class, () ->
                captcha.verify(Scene.CUSTOMER_REGISTER, "alice", "ip", invalid)
            );
        }
    }

    @Test
    void challengeCanBeConsumedOnlyOnceConcurrentlyAndIssueIsLimited() throws Exception {
        try (
            var store = new TestStore(Clock.systemUTC());
            var pool = Executors.newFixedThreadPool(8)
        ) {
            var captcha = new CaptchaService(store, store, CONFIG);
            var answer = issue(captcha, store, Scene.ADMIN_LOGIN, "alice", "ip");
            var successes = new AtomicInteger();
            var tasks = new ArrayList<Callable<Void>>();
            for (int i = 0; i < 8; i++) tasks.add(() -> {
                try {
                    captcha.verify(Scene.ADMIN_LOGIN, "alice", "ip", answer);
                    successes.incrementAndGet();
                } catch (CaptchaException ignored) {}
                return null;
            });
            for (var result : pool.invokeAll(tasks)) result.get();
            assertEquals(1, successes.get());
            for (int i = 1; i < 30; i++) captcha.challenge(
                new CaptchaChallengeRequest(Scene.CUSTOMER_LOGIN, "bob"),
                "ip"
            );
            assertEquals(
                429,
                assertThrows(ResponseStatusException.class, () ->
                    captcha.challenge(new CaptchaChallengeRequest(Scene.ADMIN_LOGIN, "alice"), "ip")
                )
                    .getStatusCode()
                    .value()
            );
        }
    }

    @Test
    void onlyExplicitProxyMaySupplyClientIp() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("172.30.240.10");
        request.addHeader("X-Real-IP", "203.0.113.10");
        assertEquals("172.30.240.10", new ClientIpResolver("").resolve(request));
        assertEquals("203.0.113.10", new ClientIpResolver("172.30.240.10").resolve(request));
        request.setRemoteAddr("172.30.240.11");
        assertEquals("172.30.240.11", new ClientIpResolver("172.30.240.10").resolve(request));
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisFailureMustNotDisableCaptcha() {
        var redis = mock(org.springframework.data.redis.core.StringRedisTemplate.class);
        doThrow(new org.springframework.data.redis.RedisConnectionFailureException("offline"))
            .when(redis)
            .execute(
                any(org.springframework.data.redis.core.script.RedisScript.class),
                anyList(),
                any(Object[].class)
            );
        var store = new RedisSessionStore(redis);
        var captcha = new CaptchaService(store, store, CONFIG);
        assertEquals(
            503,
            assertThrows(ResponseStatusException.class, () ->
                captcha.required(IdentityType.CUSTOMER, "tester", "ip")
            )
                .getStatusCode()
                .value()
        );
        assertEquals(
            503,
            assertThrows(ResponseStatusException.class, () -> store.consumeChallenge("test"))
                .getStatusCode()
                .value()
        );
    }
}
