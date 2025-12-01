package com.jp.chatapi.security.controller;


import com.jp.chatapi.chat.modal.User;
import com.jp.chatapi.security.dto.ForgotPasswordRequest;
import com.jp.chatapi.security.redis.resetpassword.ResetPasswordService;
import com.jp.chatapi.security.repository.UserRepository;
import com.jp.chatapi.security.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class PasswordResetController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ResetPasswordService resetPasswordService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public PasswordResetController(UserRepository userRepository,
                                   EmailService emailService,
                                   ResetPasswordService resetPasswordService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.resetPasswordService = resetPasswordService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        logger.info("➡️ Forgot password request received for email: {}", request.getEmail());

        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if (optionalUser.isEmpty()) {
            logger.warn("No user found with email: {}", request.getEmail());
            return ResponseEntity.badRequest().body("User with email not found");
        }

        User user = optionalUser.get();
        String resetToken = UUID.randomUUID().toString();
        logger.debug("Generated reset token {} for user {}", resetToken, user.getEmail());

        // Save token in Redis with TTL
        resetPasswordService.storeResetToken(resetToken, user.getEmail());
        logger.info("Stored reset token in Redis for user: {}", user.getEmail());

        // Send email with token
        String resetLink = "http://localhost:8080/auth/reset-password?token=" + resetToken;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        logger.info("Sent password reset email to {}", user.getEmail());

        return ResponseEntity.ok("Reset link sent to email.");
    }

    @GetMapping("/reset-password")
    public ResponseEntity<String> showResetPasswordForm(@RequestParam("token") String token) {
        logger.info("➡️ GET reset password request received with token {}", token);

        String email = resetPasswordService.getEmailByToken(token);
        if (email == null) {
            logger.warn("Invalid or expired reset token: {}", token);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<h3>Invalid or expired reset token</h3>");
        }

        logger.info("Valid reset token {} for email {}", token, email);

        String htmlForm = """
                <html>
                  <body>
                    <h3>Reset your password</h3>
                    <form method="POST" action="/auth/reset-password?token=%s">
                      <input type="password" name="newPassword" placeholder="Enter new password" required/>
                      <button type="submit">Reset Password</button>
                    </form>
                  </body>
                </html>
                """.formatted(token);

        logger.debug("Returning HTML reset form for user {}", email);
        return ResponseEntity.ok().body(htmlForm);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestParam("token") String token,
            @RequestParam("newPassword") String newPassword
    ) {
        logger.info("➡️ POST reset password request received with token {}", token);

        String email = resetPasswordService.getEmailByToken(token);
        if (email == null) {
            logger.warn("Invalid or expired reset token: {}", token);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired token");
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            logger.error("No user found for email {} from valid token {}", email, token);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found");
        }

        User user = optionalUser.get();
        user.setPasswordHash(passwordEncoder.encode(user.getPassword()));

//        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logger.info("Password updated successfully for user {}", email);

        resetPasswordService.invalidateToken(token);
        logger.debug("Invalidated reset token {} for user {}", token, email);

        return ResponseEntity.ok("Password reset successful");
    }
}
