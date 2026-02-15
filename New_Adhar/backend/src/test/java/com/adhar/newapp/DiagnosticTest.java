package com.adhar.newapp;

import com.adhar.newapp.model.User;
import com.adhar.newapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@SpringBootTest
public class DiagnosticTest {

    @Autowired
    private UserRepository userRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.adhar.newapp.service.EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void checkRenukaUser() {
        String email = "renuka@gmail.com";
        String rawPassword = "Renuka@123";

        System.out.println("---------------- DIAGNOSTIC START ----------------");
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            System.out.println("User FOUND: " + user.getFullName());
            System.out.println("ID: " + user.getId());
            System.out.println("Status: " + user.getStatus());
            System.out.println("Stored Hash: " + user.getPassword());

            boolean matches = passwordEncoder.matches(rawPassword, user.getPassword());
            System.out.println("Password Match Result: " + matches);

            if (!matches) {
                System.out.println("Trying to re-encode and match...");
                String newHash = passwordEncoder.encode(rawPassword);
                System.out.println("New Hash for 'Renuka@123': " + newHash);
                System.out.println("Matches New Hash: " + passwordEncoder.matches(rawPassword, newHash));

                // If it doesn't match the stored one, maybe the stored one was encoded with a
                // different encoder?
                // Or maybe it's plain text?
                if (rawPassword.equals(user.getPassword())) {
                    System.out.println("WARNING: Stored password is PLAIN TEXT!");
                }
            }
        } else {
            System.out.println("User NOT FOUND with email: " + email);
        }
        System.out.println("---------------- DIAGNOSTIC END ----------------");
    }
}
