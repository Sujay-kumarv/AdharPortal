package com.adhar.newapp.controller;

import com.adhar.newapp.model.User;
import com.adhar.newapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.adhar.newapp.service.AadhaarService aadhaarService;

    @Autowired
    private com.adhar.newapp.util.JwtProvider jwtProvider;

    private final Path rootLocation = Paths.get("uploads");

    public UserController() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage!", e);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @RequestParam("fullName") String fullName,
            @RequestParam("dob") String dob,
            @RequestParam("gender") String gender,
            @RequestParam("mobile") String mobile,
            @RequestParam("email") String email,
            @RequestParam("addressLine1") String addressLine1,
            @RequestParam("addressLine2") String addressLine2,
            @RequestParam("city") String city,
            @RequestParam("state") String state,
            @RequestParam("pincode") String pincode,
            @RequestParam("password") String password,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "idProof", required = false) MultipartFile idProof,
            @RequestParam(value = "signature", required = false) MultipartFile signature) {
        try {
            User user = new User();
            user.setFullName(fullName);
            user.setDob(dob);
            user.setGender(gender);
            user.setMobile(mobile);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setAddressLine1(addressLine1);
            user.setAddressLine2(addressLine2);
            user.setCity(city);
            user.setState(state);
            user.setPincode(pincode);

            if (photo != null && !photo.isEmpty())
                user.setPhotoPath(saveFile(photo));
            if (idProof != null && !idProof.isEmpty())
                user.setIdProofPath(saveFile(idProof));
            if (signature != null && !signature.isEmpty())
                user.setSignaturePath(saveFile(signature));

            System.out.println("DEBUG: Saving user: " + user.getEmail());
            userRepository.save(user);
            System.out.println("DEBUG: User saved successfully: " + user.getId());
            return ResponseEntity.ok(Map.of("message", "Registration Successful!"));

        } catch (Exception e) {
            System.err.println("ERROR: Registration Failed for email " + email);
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        System.out.println("DEBUG: /api/users/me called with token: " + (token != null ? "PRESENT" : "NULL"));
        if (token != null && token.startsWith("Bearer ")) {
            try {
                String jwt = token.substring(7);
                String email = jwtProvider.extractUsername(jwt);
                System.out.println("DEBUG: Extracted email: " + email);
                java.util.Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    System.out.println("DEBUG: User found: " + user.getId());
                    user.setPassword(null); // Mask password
                    return ResponseEntity.ok(user);
                } else {
                    System.out.println("DEBUG: User not found for email: " + email);
                    return ResponseEntity.status(404).body(Map.of("error", "User not found"));
                }
            } catch (Exception e) {
                System.out.println("DEBUG: Token validation failed: " + e.getMessage());
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or Expired Token"));
            }
        }
        System.out.println("DEBUG: Invalid or missing token");
        return ResponseEntity.status(401).build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> statusMap) {
        return userRepository.findById(id).map(user -> {
            String newStatus = statusMap.get("status");
            user.setStatus(newStatus);

            if ("APPROVED".equalsIgnoreCase(newStatus)) {
                aadhaarService.generateAadhaar(user);
            }

            userRepository.save(user);
            return ResponseEntity.ok(user);
        }).orElse(ResponseEntity.notFound().build());
    }

    // Serve uploaded files
    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                String contentType = null;
                try {
                    contentType = Files.probeContentType(file);
                } catch (IOException e) {
                    System.out.println("DEBUG: Could not probe content type for " + filename);
                }

                // Fallback based on extension if probe fails
                if (contentType == null) {
                    String lowerFilename = filename.toLowerCase();
                    if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
                        contentType = "image/jpeg";
                    } else if (lowerFilename.endsWith(".png")) {
                        contentType = "image/png";
                    } else if (lowerFilename.endsWith(".pdf")) {
                        contentType = "application/pdf";
                    } else {
                        contentType = "application/octet-stream";
                    }
                }

                System.out.println("DEBUG: Serving file " + filename + " with Content-Type: " + contentType);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private String saveFile(MultipartFile file) throws IOException {
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Files.copy(file.getInputStream(), rootLocation.resolve(filename));
        return filename;
    }
}
