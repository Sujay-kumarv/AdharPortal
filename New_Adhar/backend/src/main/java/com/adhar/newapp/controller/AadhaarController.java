package com.adhar.newapp.controller;

import com.adhar.newapp.model.User;
import com.adhar.newapp.repository.UserRepository;
import com.adhar.newapp.service.AadhaarService;
import com.adhar.newapp.util.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Optional;

@RestController
@RequestMapping("/api/newaadhaar")
public class AadhaarController {

    @Autowired
    private AadhaarService aadhaarService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @GetMapping("/download/{userId}")
    public ResponseEntity<Resource> downloadAadhaar(@PathVariable Long userId,
            @RequestHeader("Authorization") String token) {
        // 1. Validate User
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String requestEmail = jwtProvider.extractUsername(token.substring(7));
        Optional<User> requestUserOpt = userRepository.findByEmail(requestEmail);

        if (requestUserOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        User requestUser = requestUserOpt.get();
        boolean isAdmin = requestUser.getRoles().contains("ROLE_ADMIN");

        // 2. Check Permissions (Admin or Self)
        if (!isAdmin && !requestUser.getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        // 3. Get Target User and File
        Optional<User> targetUserOpt = userRepository.findById(userId);
        if (targetUserOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User targetUser = targetUserOpt.get();
        if (targetUser.getAadhaarPdfPath() == null) {
            return ResponseEntity.notFound().build(); // Not generated yet
        }

        try {
            Path filePath = aadhaarService.getPdfPath(targetUser.getAadhaarPdfPath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
