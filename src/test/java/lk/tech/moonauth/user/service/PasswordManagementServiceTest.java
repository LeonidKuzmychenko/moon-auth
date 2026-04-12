package lk.tech.moonauth.user.service;

import lk.tech.moonauth.auth.dto.ResetPasswordRequest;
import lk.tech.moonauth.email.service.EmailService;
import lk.tech.moonauth.token.entity.PasswordResetToken;
import lk.tech.moonauth.token.repository.PasswordResetTokenRepository;
import lk.tech.moonauth.token.repository.RefreshTokenRepository;
import lk.tech.moonauth.user.entity.User;
import lk.tech.moonauth.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordManagementServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordManagementService passwordManagementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(passwordManagementService, "passwordResetExpirationMs", 1800000L);
    }

    @Test
    void requestPasswordReset_Success() {
        User user = User.builder().email("test@example.com").build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        passwordManagementService.requestPasswordReset("test@example.com");

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void resetPassword_Success() {
        User user = User.builder().email("test@example.com").build();
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash("token")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();
        ResetPasswordRequest request = new ResetPasswordRequest("token", "NewPassword123!");

        when(passwordResetTokenRepository.findByTokenHash("token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");

        passwordManagementService.resetPassword(request);

        assertEquals("hashed-password", user.getPasswordHash());
        assertTrue(token.isUsed());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
        verify(refreshTokenRepository).deleteAllByUser(user);
    }
}
