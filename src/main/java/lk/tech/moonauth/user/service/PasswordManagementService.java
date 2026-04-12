package lk.tech.moonauth.user.service;

import lk.tech.moonauth.auth.dto.ChangePasswordRequest;
import lk.tech.moonauth.auth.dto.ResetPasswordRequest;
import lk.tech.moonauth.email.service.EmailService;
import lk.tech.moonauth.token.entity.PasswordResetToken;
import lk.tech.moonauth.token.repository.PasswordResetTokenRepository;
import lk.tech.moonauth.token.repository.RefreshTokenRepository;
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
public class PasswordManagementService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.password-reset.expiration-ms:1800000}")
    private long passwordResetExpirationMs;

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String tokenHash = UUID.randomUUID().toString();
            PasswordResetToken token = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(tokenHash)
                    .expiresAt(LocalDateTime.now().plusNanos(passwordResetExpirationMs * 1_000_000))
                    .build();
            passwordResetTokenRepository.save(token);
            emailService.sendPasswordResetEmail(user.getEmail(), tokenHash);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (token.isUsed() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired or used");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        // Invalidate all sessions
        refreshTokenRepository.deleteAllByUser(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate all sessions
        refreshTokenRepository.deleteAllByUser(user);
    }
}
