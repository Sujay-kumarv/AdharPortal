package com.adhar.newapp.controller;

import com.adhar.newapp.model.User;
import com.adhar.newapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/promote")
    public ResponseEntity<String> promoteUser(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setRoles("ROLE_ADMIN,ROLE_USER");
            userRepository.save(user);
            return ResponseEntity.ok("User " + email + " promoted to ADMIN.");
        }
        return ResponseEntity.badRequest().body("User not found.");
    }
}
