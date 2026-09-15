package cn.zhijie.security;

import java.time.Duration;

public interface RateLimiter {
    long increment(String key, Duration window);
}
