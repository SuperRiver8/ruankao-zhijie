package cn.zhijie.security;

import java.time.Duration;
import java.util.Optional;

public interface CaptchaStore {
    long failures(String key, Duration window, boolean record);
    void clearFailures(String key);
    void saveChallenge(String id, String value, Duration ttl);
    Optional<String> consumeChallenge(String id);
}
