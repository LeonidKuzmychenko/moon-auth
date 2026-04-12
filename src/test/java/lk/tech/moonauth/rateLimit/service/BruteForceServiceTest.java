package lk.tech.moonauth.rateLimit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BruteForceServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private BruteForceService bruteForceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(bruteForceService, "maxAttempts", 5);
        ReflectionTestUtils.setField(bruteForceService, "lockDurationMs", 900000L);
    }

    @Test
    void loginFailed_IncrementsAttempts() {
        String key = "test@example.com";
        when(valueOperations.increment(anyString())).thenReturn(1L);

        bruteForceService.loginFailed(key);

        verify(valueOperations).increment("login_attempt:" + key);
        verify(redisTemplate).expire(eq("login_attempt:" + key), anyLong(), any(TimeUnit.class));
    }

    @Test
    void loginFailed_LocksWhenMaxReached() {
        String key = "test@example.com";
        when(valueOperations.increment(anyString())).thenReturn(5L);

        bruteForceService.loginFailed(key);

        verify(valueOperations).set(eq("login_lock:" + key), eq("locked"), anyLong(), any(TimeUnit.class));
    }

    @Test
    void isBlocked_ChecksRedis() {
        String key = "test@example.com";
        when(redisTemplate.hasKey("login_lock:" + key)).thenReturn(true);

        assertTrue(bruteForceService.isBlocked(key));
    }

    @Test
    void loginSucceeded_DeletesKeys() {
        String key = "test@example.com";

        bruteForceService.loginSucceeded(key);

        verify(redisTemplate).delete("login_attempt:" + key);
        verify(redisTemplate).delete("login_lock:" + key);
    }
}
