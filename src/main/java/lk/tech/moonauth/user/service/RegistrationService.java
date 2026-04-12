package lk.tech.moonauth.user.service;

import lk.tech.moonauth.auth.dto.RegisterRequest;
import lk.tech.moonauth.email.service.EmailService;
import lk.tech.moonauth.token.entity.EmailVerificationToken;
import lk.tech.moonauth.token.repository.EmailVerificationTokenRepository;
import lk.tech.moonauth.user.entity.Role;
import lk.tech.moonauth.user.entity.User;
import lk.tech.moonauth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.email-verification.expiration-ms:86400000}")
    private long emailVerificationExpirationMs;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(false)
                .emailVerified(false)
                .build();

        userRepository.save(user);
        sendVerificationEmail(user);
    }

    @Transactional
    public void confirmEmail(String tokenHash) {
        System.out.println("1");
        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        System.out.println("2");
        if (token.isUsed() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired or used");
        }

        System.out.println("3");

        User user = token.getUser();
        user.setEnabled(true);
        user.setEmailVerified(true);
        userRepository.save(user);

        System.out.println("4");
        token.setUsed(true);
        emailVerificationTokenRepository.save(token);
        System.out.println("5");
    }

    @Transactional
    public void resendConfirmation(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email already verified");
        }

        sendVerificationEmail(user);
    }

    private void sendVerificationEmail(User user) {
        String tokenHash = UUID.randomUUID().toString();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusNanos(emailVerificationExpirationMs * 1_000_000))
                .build();
        emailVerificationTokenRepository.save(token);
        emailService.sendVerificationEmail(user.getEmail(), tokenHash);
    }
}
