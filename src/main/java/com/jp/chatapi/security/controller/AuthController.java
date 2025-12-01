package com.jp.chatapi.security.controller;

import com.jp.chatapi.chat.modal.User;
import com.jp.chatapi.security.dto.LoginRequest;
import com.jp.chatapi.security.jwt.JwtUtil;
import com.jp.chatapi.security.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public AuthController(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            UserDetailsService userDetailsService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    // -------------------- LOGIN --------------------
    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request, HttpServletResponse response) {
        logger.info("Login request received for email: {}", request.getEmail());

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            logger.debug("Authentication success for user: {}", request.getEmail());

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

            String token = jwtUtil.generateToken(userDetails.getUsername());
            logger.info("JWT generated for user: {}", userDetails.getUsername());

            ResponseCookie cookie = ResponseCookie
                    .from("jwt", token)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .sameSite("Strict")
                    .maxAge(86400)
                    .build();

            response.addHeader("Set-Cookie", cookie.toString());

            return "{\"token\":\"" + token + "\"}";
        } catch (Exception e) {
            logger.error("Login failed for email: {}. Reason: {}", request.getEmail(), e.getMessage());
            throw e;
        }
    }

    // -------------------- LOGOUT --------------------
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);

        return ResponseEntity.ok("Logged out successfully");
    }

    // -------------------- STATUS --------------------
    @GetMapping("/status")
    public ResponseEntity<?> status(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        logger.info("Checking login status");

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing token");
            }

            String token = authHeader.substring(7);
            String userEmail = jwtUtil.getEmailFromToken(token);

            var userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            User user = userOpt.get();
            logger.info("Authenticated user ID: {}", user.getId());

            return ResponseEntity.ok(
                    Map.of(
                            "userId", user.getId(),
                            "email", user.getEmail(),
                            "username", user.getUsername()
                    )
            );

        } catch (JwtException e) {
            logger.error("Invalid token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
    }
}
