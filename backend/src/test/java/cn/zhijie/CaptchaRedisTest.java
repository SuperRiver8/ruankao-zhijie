package cn.zhijie;

import static org.junit.jupiter.api.Assertions.*;

import cn.zhijie.security.RedisSessionStore;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

// 显式提供测试 Redis 端口时才连接；只创建并清理随机测试键，绝不清空数据库。
@EnabledIfEnvironmentVariable(named = "CAPTCHA_TEST_REDIS_PORT", matches = "[0-9]+")
class CaptchaRedisTest {

    @Test
    void rollingFailuresAndAtomicChallengeConsumption() throws Exception {
        var config = new RedisStandaloneConfiguration(
            System.getenv().getOrDefault("CAPTCHA_TEST_REDIS_HOST", "127.0.0.1"),
            Integer.parseInt(System.getenv("CAPTCHA_TEST_REDIS_PORT"))
        );
        if (System.getenv("CAPTCHA_TEST_REDIS_PASSWORD") != null) config.setPassword(
            System.getenv("CAPTCHA_TEST_REDIS_PASSWORD")
        );
        var connection = new LettuceConnectionFactory(config);
        connection.afterPropertiesSet();
        connection.start();
        String key = "test-" + UUID.randomUUID();
        var store = new RedisSessionStore(new StringRedisTemplate(connection));
        try (var pool = Executors.newFixedThreadPool(8)) {
            assertEquals(0, store.failures(key, Duration.ofSeconds(2), false));
            assertEquals(1, store.failures(key, Duration.ofSeconds(2), true));
            assertEquals(2, store.failures(key, Duration.ofSeconds(2), true));
            assertEquals(2, store.failures(key, Duration.ofSeconds(2), false));
            Thread.sleep(2100);
            assertEquals(0, store.failures(key, Duration.ofSeconds(2), false));
            store.failures(key, Duration.ofSeconds(2), true);
            store.clearFailures(key);
            assertEquals(0, store.failures(key, Duration.ofSeconds(2), false));
            store.saveChallenge(key, "bound:123", Duration.ofSeconds(2));
            var tasks = new ArrayList<Callable<Optional<String>>>();
            for (int i = 0; i < 8; i++) tasks.add(() -> store.consumeChallenge(key));
            int consumed = 0;
            for (var result : pool.invokeAll(tasks)) if (result.get().isPresent()) consumed++;
            assertEquals(1, consumed);
            store.saveChallenge(key, "bound:123", Duration.ofMillis(100));
            Thread.sleep(150);
            assertTrue(store.consumeChallenge(key).isEmpty());
        } finally {
            try {
                store.clearFailures(key);
                store.consumeChallenge(key);
            } finally {
                connection.destroy();
            }
        }
    }
}
