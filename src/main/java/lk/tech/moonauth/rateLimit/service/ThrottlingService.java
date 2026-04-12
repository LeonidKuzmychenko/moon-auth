package lk.tech.moonauth.rateLimit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ThrottlingService {

    private final StringRedisTemplate redisTemplate;

    private static final String THROTTLE_PREFIX = "throttle:";

    public boolean isThrottled(String key, String action) {
        return redisTemplate.hasKey(THROTTLE_PREFIX + action + ":" + key);
    }

    public void throttle(String key, String action, long durationMs) {
        redisTemplate.opsForValue().set(THROTTLE_PREFIX + action + ":" + key, "throttled", durationMs, TimeUnit.MILLISECONDS);
    }
}
