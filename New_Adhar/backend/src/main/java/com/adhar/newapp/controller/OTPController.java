package com.adhar.newapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/otp")
@CrossOrigin(origins = "http://localhost:5173")
public class OTPController {

    private final Map<String, String> otpStorage = new java.util.concurrent.ConcurrentHashMap<>();

    @PostMapping("/send")
    public ResponseEntity<?> sendOTP(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }
        
        // Generate 6-digit OTP
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        otpStorage.put(email, otp);
        
        System.out.println("OTP for " + email + ": " + otp); // Log for debugging

        // Return OTP in response for simulation
        return ResponseEntity.ok(Map.of("message", "OTP sent to " + email, "otp", otp));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOTP(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        
        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and OTP are required"));
        }

        if (otp.equals(otpStorage.get(email))) {
            otpStorage.remove(email); // Clear OTP after success
            return ResponseEntity.ok(Map.of("message", "OTP Verified Successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid OTP"));
        }
    }
}
