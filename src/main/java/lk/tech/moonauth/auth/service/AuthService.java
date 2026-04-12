package lk.tech.moonauth.auth.service;

import lk.tech.moonauth.auth.dto.*;
import lk.tech.moonauth.rateLimit.service.BruteForceService;
import lk.tech.moonauth.security.jwt.JwtUtils;
import lk.tech.moonauth.token.entity.RefreshToken;
import lk.tech.moonauth.token.repository.RefreshTokenRepository;
import lk.tech.moonauth.user.entity.User;
import lk.tech.moonauth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final BruteForceService bruteForceService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Transactional
    public TokenResponse login(LoginRequest request, String ipAddress, String userAgent) {
        if (bruteForceService.isBlocked(request.getEmail()) || bruteForceService.isBlocked(ipAddress)) {
            throw new RuntimeException("Too many login attempts. Please try again later.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            bruteForceService.loginFailed(request.getEmail());
            bruteForceService.loginFailed(ipAddress);
            throw new RuntimeException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled() || !user.isEmailVerified()) {
            throw new RuntimeException("Email not verified");
        }

        bruteForceService.loginSucceeded(request.getEmail());
        bruteForceService.loginSucceeded(ipAddress);

        String accessToken = jwtUtils.generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user, ipAddress, userAgent);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getTokenHash())
                .build();
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request, String ipAddress, String userAgent) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            // Reuse detection
            log.warn("Reuse detection: Refresh token {} was reused!", refreshToken.getTokenHash());
            revokeAllUserTokens(refreshToken.getUser());
            throw new RuntimeException("Invalid refresh token");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        User user = refreshToken.getUser();
        
        // Rotate token
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
        
        String newAccessToken = jwtUtils.generateAccessToken(user);
        RefreshToken newRefreshToken = createRefreshToken(user, ipAddress, userAgent);
        
        refreshToken.setReplacedByTokenHash(newRefreshToken.getTokenHash());
        refreshTokenRepository.save(refreshToken);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getTokenHash())
                .build();
    }

    @Transactional
    public void logout(String refreshTokenHash) {
        refreshTokenRepository.findByTokenHash(refreshTokenHash).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void logoutAll(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        revokeAllUserTokens(user);
    }

    private RefreshToken createRefreshToken(User user, String ipAddress, String userAgent) {
        String tokenHash = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusNanos(refreshTokenExpirationMs * 1_000_000))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    private void revokeAllUserTokens(User user) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserAndRevokedFalse(user);
        tokens.forEach(t -> {
            t.setRevoked(true);
            t.setRevokedAt(LocalDateTime.now());
        });
        refreshTokenRepository.saveAll(tokens);
    }
}
