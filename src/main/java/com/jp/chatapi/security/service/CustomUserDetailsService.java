package com.jp.chatapi.security.service;

import com.jp.chatapi.chat.modal.User;
import com.jp.chatapi.security.jwt.JwtUtil;
import com.jp.chatapi.security.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return user; // now safe, because User implements UserDetails
    }

    public String validateTokenAndGetUsername(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("Missing token");
        }

        if (!jwtUtil.validateToken(token)) {
            throw new JwtException("Invalid or expired token");
        }

        String username = jwtUtil.getEmailFromToken(token);

        // optional DB existence check
        // if (!userRepository.existsByEmail(username)) {
        //     throw new JwtException("User not found");
        // }

        return username;
    }
}
