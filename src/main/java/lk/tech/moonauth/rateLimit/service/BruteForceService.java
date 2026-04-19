package lk.tech.moonauth.rateLimit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BruteForceService {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.security.brute-force.max-attempts}")
    private int maxAttempts;

    @Value("${app.security.brute-force.lock-duration-ms}")
    private long lockDurationMs;

    private static final String LOGIN_ATTEMPT_PREFIX = "login_attempt:";
    private static final String LOGIN_LOCK_PREFIX = "login_lock:";

    public void loginFailed(String key) {
        String attemptKey = LOGIN_ATTEMPT_PREFIX + key;
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptKey, 15, TimeUnit.MINUTES);
        }

        if (attempts != null && attempts >= maxAttempts) {
            redisTemplate.opsForValue().set(LOGIN_LOCK_PREFIX + key, "locked", lockDurationMs, TimeUnit.MILLISECONDS);
        }
    }

    public void loginSucceeded(String key) {
        redisTemplate.delete(LOGIN_ATTEMPT_PREFIX + key);
        redisTemplate.delete(LOGIN_LOCK_PREFIX + key);
    }

    public boolean isBlocked(String key) {
        return redisTemplate.hasKey(LOGIN_LOCK_PREFIX + key);
    }
}
