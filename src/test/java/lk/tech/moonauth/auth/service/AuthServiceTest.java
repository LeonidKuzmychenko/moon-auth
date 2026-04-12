package lk.tech.moonauth.auth.service;

import lk.tech.moonauth.auth.dto.LoginRequest;
import lk.tech.moonauth.auth.dto.TokenResponse;
import lk.tech.moonauth.rateLimit.service.BruteForceService;
import lk.tech.moonauth.security.jwt.JwtUtils;
import lk.tech.moonauth.token.entity.RefreshToken;
import lk.tech.moonauth.token.repository.RefreshTokenRepository;
import lk.tech.moonauth.user.entity.Role;
import lk.tech.moonauth.user.entity.User;
import lk.tech.moonauth.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private BruteForceService bruteForceService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", 604800000L);
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("test@example.com", "Password123!");
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build();

        when(bruteForceService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jwtUtils.generateAccessToken(any())).thenReturn("access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        TokenResponse response = authService.login(request, "127.0.0.1", "test-agent");

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(bruteForceService, times(2)).loginSucceeded(anyString());
    }

    @Test
    void login_Blocked() {
        LoginRequest request = new LoginRequest("test@example.com", "Password123!");
        when(bruteForceService.isBlocked(anyString())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.login(request, "127.0.0.1", "test-agent"));
    }

    @Test
    void login_Unverified() {
        LoginRequest request = new LoginRequest("test@example.com", "Password123!");
        User user = User.builder()
                .email("test@example.com")
                .enabled(true)
                .emailVerified(false)
                .build();

        when(bruteForceService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class, () -> authService.login(request, "127.0.0.1", "test-agent"));
    }
}
