package lk.tech.moonauth.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lk.tech.moonauth.auth.dto.*;
import lk.tech.moonauth.auth.mapper.AuthMapper;
import lk.tech.moonauth.auth.service.AuthService;
import lk.tech.moonauth.rateLimit.service.ThrottlingService;
import lk.tech.moonauth.user.entity.User;
import lk.tech.moonauth.user.repository.UserRepository;
import lk.tech.moonauth.user.service.PasswordManagementService;
import lk.tech.moonauth.user.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;
    private final PasswordManagementService passwordManagementService;
    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final ThrottlingService throttlingService;

    @Value("${app.security.throttling.email-resend-ms:60000}")
    private long emailResendThrottleMs;

    @Value("${app.security.throttling.password-reset-ms:300000}")
    private long passwordResetThrottleMs;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        registrationService.register(request);
        return ResponseEntity.ok(Map.of("message", "User registered. Please check your email for confirmation."));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.login(request, ipAddress, userAgent));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.refresh(request, ipAddress, userAgent));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logoutAll(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(authMapper.toUserResponse(user));
    }

    @PostMapping("/confirm-email")
    public ResponseEntity<Map<String, String>> confirmEmail(@RequestParam String token) {
        registrationService.confirmEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email confirmed successfully."));
    }

    @PostMapping("/resend-confirmation")
    public ResponseEntity<Map<String, String>> resendConfirmation(@RequestParam String email) {
        if (throttlingService.isThrottled(email, "resend-confirmation")) {
            throw new RuntimeException("Please wait before requesting another confirmation email.");
        }
        registrationService.resendConfirmation(email);
        throttlingService.throttle(email, "resend-confirmation", emailResendThrottleMs);
        return ResponseEntity.ok(Map.of("message", "Confirmation email sent."));
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestParam String email) {
        if (throttlingService.isThrottled(email, "password-reset")) {
            // Still return OK to avoid leaking info
            return ResponseEntity.ok(Map.of("message", "If an account exists with this email, you will receive a password reset link shortly."));
        }
        passwordManagementService.requestPasswordReset(email);
        throttlingService.throttle(email, "password-reset", passwordResetThrottleMs);
        return ResponseEntity.ok(Map.of("message", "If an account exists with this email, you will receive a password reset link shortly."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordManagementService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        passwordManagementService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
