package com.adhar.newapp.service;

import com.adhar.newapp.model.AuditLog;
import com.adhar.newapp.model.RefreshToken;
import com.adhar.newapp.model.User;
import com.adhar.newapp.repository.AuditLogRepository;
import com.adhar.newapp.repository.PasswordResetTokenRepository;
import com.adhar.newapp.repository.RefreshTokenRepository;
import com.adhar.newapp.repository.UserRepository;
import com.adhar.newapp.util.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RateLimiterService rateLimiterService;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired
    private EmailService emailService;

    // Temporary storage for MFA sessions (In-Memory for now, should be Redis)
    private final java.util.Map<String, String> mfaSessions = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, String> mfaCodes = new java.util.concurrent.ConcurrentHashMap<>();

    public com.adhar.newapp.dto.AuthResponse initiateLogin(String email, String password, String ipAddress) {
        System.out.println("DEBUG: initiateLogin called for email: " + email);
        if (!rateLimiterService.allowRequest(ipAddress)) {
            System.out.println("DEBUG: Rate limit exceeded for IP: " + ipAddress);
            logAudit(null, "LOGIN_BLOCKED", ipAddress, "Rate Limit Exceeded");
            throw new RuntimeException("Too many login attempts. Please try again later.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    System.out.println("DEBUG: User not found for email: " + email);
                    return new RuntimeException("Invalid Credentials");
                });

        System.out.println("DEBUG: User found: " + user.getId() + ", Email: " + user.getEmail());
        System.out.println("DEBUG: User Roles: " + user.getRoles()); // Add this line

        if (!passwordEncoder.matches(password, user.getPassword())) {
            System.out.println("DEBUG: Password mismatch for user: " + email);
            logAudit(user.getId(), "LOGIN_FAIL", ipAddress, "Bad Password");
            throw new RuntimeException("Invalid Credentials");
        }

        System.out.println("DEBUG: Password matched. MFA Enabled: " + user.isMfaEnabled());

        // DIRECT LOGIN - NO MFA
        String accessToken = jwtProvider.generateAccessToken(user.getEmail(), user.getRoles());
        RefreshToken refreshToken = createRefreshToken(user);

        logAudit(user.getId(), "LOGIN_SUCCESS", ipAddress, "Login Successful (MFA Disabled)");

        return new com.adhar.newapp.dto.AuthResponse(accessToken, refreshToken.getToken(), user.getRoles());
    }

    // verifyMfa is no longer needed but kept to avoid compilation errors if
    // referenced elsewhere (though we should remove it if possible)
    public com.adhar.newapp.dto.AuthResponse verifyMfa(String sessionId, String code) {
        throw new RuntimeException("MFA is disabled.");
    }

    @Transactional
    public com.adhar.newapp.dto.AuthResponse refreshToken(String requestRefreshToken) {
        return refreshTokenRepository.findByToken(requestRefreshToken)
                .map(token -> {
                    if (token.getExpiryDate().isBefore(Instant.now()) || token.isRevoked()) {
                        refreshTokenRepository.delete(token); // Revoke/Delete
                        throw new RuntimeException("Refresh token was expired. Please make a new signin request");
                    }
                    return token;
                })
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtProvider.generateAccessToken(user.getEmail(), user.getRoles());
                    // Rotate Refresh Token
                    refreshTokenRepository.deleteByUser(user); // Invalidate old
                    RefreshToken newRefToken = createRefreshToken(user);
                    return new com.adhar.newapp.dto.AuthResponse(accessToken, newRefToken.getToken(), user.getRoles());
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(new RefreshToken());

        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(1000 * 60 * 60 * 24 * 7)); // 7 days
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    private void logAudit(Long userId, String action, String ip, String details) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setIpAddress(ip);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Create Token
        com.adhar.newapp.model.PasswordResetToken token = new com.adhar.newapp.model.PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(java.time.LocalDateTime.now().plusHours(1)); // 1 hour expiry
        passwordResetTokenRepository.save(token);

        // Send Email
        String resetLink = "http://localhost:5173/reset_password.html?token=" + token.getToken();
        String subject = "Password Reset Request";
        String body = "To reset your password, click the link below:\n" + resetLink;

        try {
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            System.err.println("❌ FAILED TO SEND RESET EMAIL: " + e.getMessage());
            System.out.println("⚠ DEBUG MODE: Click this link to reset password: " + resetLink);
        }
    }

    public void resetPassword(String token, String newPassword) {
        com.adhar.newapp.model.PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid Token"));

        if (resetToken.isExpired()) {
            throw new RuntimeException("Token Expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
    }
}
