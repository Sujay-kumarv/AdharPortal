package com.adhar.newapp.config;

import com.adhar.newapp.model.User;
import com.adhar.newapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "sujaydevkumar1234@gmail.com";

            User admin = userRepository.findByEmail(adminEmail).orElse(new User());

            admin.setFullName("System Administrator");
            admin.setEmail(adminEmail);
            admin.setMobile("0000000000");
            admin.setDob("2000-01-01");
            admin.setGender("Other");
            admin.setAddressLine1("Admin HQ");
            admin.setCity("Admin City");
            admin.setState("Delhi");
            admin.setPincode("110001");
            admin.setPassword(passwordEncoder.encode("admin@123"));
            admin.setRoles("ROLE_ADMIN,ROLE_USER");
            admin.setStatus("APPROVED");

            userRepository.save(admin);
            System.out.println("✅ Admin User Synced: " + adminEmail);

            // Create Default User
            String userEmail = "user@example.com";
            if (userRepository.findByEmail(userEmail).isEmpty()) {
                User user = new User();
                user.setFullName("Test User");
                user.setEmail(userEmail);
                user.setMobile("9999999999");
                user.setDob("1995-01-01");
                user.setGender("Male");
                user.setAddressLine1("User Address");
                user.setCity("User City");
                user.setState("User State");
                user.setPincode("110002");
                user.setPassword(passwordEncoder.encode("password"));
                user.setRoles("ROLE_USER");
                user.setStatus("APPROVED");

                userRepository.save(user);
                System.out.println("✅ Default User Created: " + userEmail);
            }
        };
    }
}
