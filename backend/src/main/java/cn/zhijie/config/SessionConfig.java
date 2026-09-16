package cn.zhijie.config;

import cn.zhijie.security.*;
import java.time.*;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ SessionProperties.class, CaptchaProperties.class })
public class SessionConfig {

    static boolean redisSelected(Environment environment) {
        var mode = Binder.get(environment)
            .bind("app.session.store", SessionProperties.Store.class)
            .orElse(SessionProperties.Store.AUTO);
        String host = environment.getProperty(
            "spring.data.redis.host",
            environment.getProperty("REDIS_HOST", "")
        );
        boolean configured = StringUtils.hasText(host);
        boolean redis =
            mode == SessionProperties.Store.REDIS ||
            (mode == SessionProperties.Store.AUTO && configured);
        if (redis && !configured) throw new IllegalStateException(
            "Redis 模式必须配置 REDIS_HOST 或 spring.data.redis.host"
        );
        return redis;
    }

    static class RedisCondition implements Condition {

        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return redisSelected(context.getEnvironment());
        }
    }

    static class MemoryCondition implements Condition {

        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return !redisSelected(context.getEnvironment());
        }
    }

    @Bean(destroyMethod = "close")
    @Conditional(MemoryCondition.class)
    MemorySessionStore memorySessionStore() {
        org.slf4j.LoggerFactory.getLogger(SessionConfig.class).info(
            "使用本地内存会话，重启后需要重新登录"
        );
        return new MemorySessionStore(Clock.systemUTC());
    }

    @Configuration(proxyBeanMethods = false)
    @Conditional(RedisCondition.class)
    @EnableConfigurationProperties(RedisProperties.class)
    static class RedisConfig {

        @Bean
        LettuceConnectionFactory redisConnectionFactory(
            RedisProperties properties,
            Environment environment
        ) {
            String host = environment.getProperty(
                "spring.data.redis.host",
                environment.getProperty("REDIS_HOST", "")
            );
            var server = new RedisStandaloneConfiguration(host.trim(), properties.getPort());
            server.setDatabase(properties.getDatabase());
            server.setUsername(properties.getUsername());
            if (StringUtils.hasText(properties.getPassword())) server.setPassword(
                properties.getPassword()
            );
            var client = LettuceClientConfiguration.builder()
                .commandTimeout(
                    properties.getTimeout() == null
                        ? Duration.ofSeconds(3)
                        : properties.getTimeout()
                );
            if (properties.getSsl().isEnabled()) client.useSsl();
            return new LettuceConnectionFactory(server, client.build());
        }

        @Bean
        StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connection) {
            return new StringRedisTemplate(connection);
        }

        @Bean
        RedisSessionStore redisSessionStore(StringRedisTemplate redis) {
            return new RedisSessionStore(redis);
        }
    }
}
