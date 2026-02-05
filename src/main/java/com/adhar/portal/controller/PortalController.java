package com.adhar.portal.controller;

import com.adhar.portal.model.User;
import com.adhar.portal.service.QlikSenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
public class PortalController {

    @Autowired
    private QlikSenseService qlikSenseService;

    // Store User objects mapped by username
    private Map<String, User> users = new HashMap<>();

    public PortalController() {
        // Default demo user
        users.put("user", new User("user", "pass", "Demo User", "1111-2222-3333", "2000-01-01", "123 Demo Street"));
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("qlikStatus", qlikSenseService.getConnectionStatus());
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String registered,
            @RequestParam(required = false) String reset, Model model) {
        if (registered != null) {
            model.addAttribute("success", "Registration successful! Please login.");
        }
        if (reset != null) {
            model.addAttribute("success", "Password reset successful! Please login with your new password.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model) {
        if (!users.containsKey(username)) {
            model.addAttribute("error", "Username not found. Please register.");
            return "login";
        }

        if (!users.get(username).getPassword().equals(password)) {
            model.addAttribute("error", "Incorrect password. Please try again.");
            return "login";
        }

        return "redirect:/dashboard?user=" + username;
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String username, @RequestParam String newPassword, Model model) {
        if (!users.containsKey(username)) {
            model.addAttribute("error", "Username not found.");
            return "forgot-password";
        }

        User user = users.get(username);
        user.setPassword(newPassword); // In a real app, this should be hashed
        // users.put(username, user); // Not strictly needed as object is mutable and in
        // map, but good for clarity

        return "redirect:/login?reset=true";
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "Guest") String user, Model model) {
        User userObj = users.get(user);
        if (userObj != null) {
            model.addAttribute("username", userObj.getUsername());
            model.addAttribute("fullName", userObj.getFullName());
            model.addAttribute("aadharNumber", userObj.getAadharNumber());
            model.addAttribute("dob", userObj.getDob());
            model.addAttribute("address", userObj.getAddress());
            model.addAttribute("userStatus", "Verified Resident");
        } else {
            model.addAttribute("username", user);
            model.addAttribute("fullName", "N/A");
            model.addAttribute("aadharNumber", "N/A");
            model.addAttribute("dob", "N/A");
            model.addAttribute("address", "N/A");
            model.addAttribute("userStatus", "Guest Access");
        }
        model.addAttribute("qlikMessage", qlikSenseService.getAnalysisData());
        return "dashboard";
    }

    @GetMapping("/register")
    public String register(Model model) {
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String fullName,
            @RequestParam String aadharNumber,
            @RequestParam String dob,
            @RequestParam String address,
            Model model) {

        // Retain input values
        model.addAttribute("username", username);
        model.addAttribute("fullName", fullName);
        model.addAttribute("aadharNumber", aadharNumber);
        model.addAttribute("dob", dob);
        model.addAttribute("address", address);

        // Validation Logic
        if (users.containsKey(username)) {
            model.addAttribute("error", "Username '" + username + "' is already taken.");
            return "register";
        }

        if (password.length() < 4) {
            model.addAttribute("error", "Password must be at least 4 characters.");
            return "register";
        }

        // Simple regex check for 12 digits
        if (!aadharNumber.matches("\\d{12}")) {
            model.addAttribute("error", "Aadhar Number must be exactly 12 digits.");
            return "register";
        }

        User newUser = new User(username, password, fullName, aadharNumber, dob, address);
        users.put(username, newUser);
        return "redirect:/login?registered=true";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/";
    }
}
