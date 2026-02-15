package com.adhar.newapp.controller;

import com.adhar.newapp.dto.AuthResponse;
import com.adhar.newapp.dto.LoginRequest;
import com.adhar.newapp.dto.MfaRequest;
import com.adhar.newapp.service.AuthService;
import com.adhar.newapp.util.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProvider jwtProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        System.out.println("DEBUG: Login request received for user: " + loginRequest.getUsername());
        System.out.println("DEBUG: Remote IP: " + request.getRemoteAddr());
        try {
            AuthResponse response = authService.initiateLogin(loginRequest.getUsername(), loginRequest.getPassword(),
                    request.getRemoteAddr());

            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", response.getRefreshToken())
                    .httpOnly(true)
                    .secure(false)
                    .path("/api/auth/refresh")
                    .maxAge(7 * 24 * 60 * 60)
                    .sameSite("Strict")
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(response);
        } catch (RuntimeException e) {
            System.out.println("DEBUG: Login failed: " + e.getMessage());
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Internal Server Error"));
        }
    }

    @PostMapping("/mfa")
    public ResponseEntity<?> verifyMfa(@RequestBody MfaRequest mfaRequest) {
        AuthResponse response = authService.verifyMfa(mfaRequest.getSessionId(), mfaRequest.getCode());

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", response.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/api/auth/refresh")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(name = "refresh_token") String refreshToken) {
        AuthResponse response = authService.refreshToken(refreshToken);

        ResponseCookie newRefreshCookie = ResponseCookie.from("refresh_token", response.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/api/auth/refresh")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newRefreshCookie.toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        ResponseCookie cleanCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/api/auth/refresh")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader(name = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = jwtProvider.extractUsername(token);
            if (jwtProvider.validateToken(token, username)) {
                return ResponseEntity.ok(Map.of("status", "VALID", "user", username));
            }
        }
        return ResponseEntity.status(401).body(Map.of("status", "INVALID"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam("email") String email) {
        try {
            authService.forgotPassword(email);
            return ResponseEntity.ok(Map.of("message", "Password reset link sent to your email."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");
            authService.resetPassword(token, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully. Please login."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
