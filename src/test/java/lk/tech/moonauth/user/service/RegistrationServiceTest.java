package lk.tech.moonauth.user.service;

import lk.tech.moonauth.auth.dto.RegisterRequest;
import lk.tech.moonauth.email.service.EmailService;
import lk.tech.moonauth.token.entity.EmailVerificationToken;
import lk.tech.moonauth.token.repository.EmailVerificationTokenRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(registrationService, "emailVerificationExpirationMs", 86400000L);
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest("test@example.com", "Password123!");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(userRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(emailVerificationTokenRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        registrationService.register(request);

        verify(userRepository).save(any(User.class));
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void confirmEmail_Success() {
        User user = User.builder().email("test@example.com").enabled(false).emailVerified(false).build();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .tokenHash("token")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .used(false)
                .build();

        when(emailVerificationTokenRepository.findByTokenHash("token")).thenReturn(Optional.of(token));

        registrationService.confirmEmail("token");

        assertTrue(user.isEnabled());
        assertTrue(user.isEmailVerified());
        assertTrue(token.isUsed());
        verify(userRepository).save(user);
        verify(emailVerificationTokenRepository).save(token);
    }

    @Test
    void confirmEmail_Expired() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .tokenHash("token")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .used(false)
                .build();

        when(emailVerificationTokenRepository.findByTokenHash("token")).thenReturn(Optional.of(token));

        assertThrows(RuntimeException.class, () -> registrationService.confirmEmail("token"));
    }
}
