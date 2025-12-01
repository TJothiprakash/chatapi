package com.jp.chatapi.security.controller;

import com.jp.chatapi.chat.modal.User;
import com.jp.chatapi.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class UserRegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(UserRegistrationController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        logger.info("➡️ Entering user registration API");

        try {
            // Validate username & email
            if (user.getUsername() == null || user.getEmail() == null) {
                return ResponseEntity.badRequest().body("username and email are required");
            }

            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                logger.warn("Email already registered: {}", user.getEmail());
                return ResponseEntity.badRequest().body("Email is already registered.");
            }

            // Encode password (passwordHash contains raw password from JSON)
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

            // Save into database
            int generatedId = userRepository.save(user);
            user.setId(generatedId);

            logger.info("✅ Registration success. User ID: {}", generatedId);

            return ResponseEntity.ok("User registered successfully.");

        } catch (Exception e) {
            logger.error("❌ Registration failed for email: {}", user.getEmail(), e);
            return ResponseEntity.internalServerError().body("Registration failed due to server error.");
        }
    }
}
