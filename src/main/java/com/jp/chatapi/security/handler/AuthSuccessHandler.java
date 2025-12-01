package com.jp.chatapi.security.handler;

import com.jp.chatapi.security.jwt.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtUtil jwtService;
    private final Logger logger = LoggerFactory.getLogger("AuthSuccessHandler.class");
    public AuthSuccessHandler(JwtUtil jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.Authentication authentication) throws IOException, ServletException {
        UserDetails user = (UserDetails) authentication.getPrincipal();
        String jwt = jwtService.generateToken(String.valueOf(user));
        logger.info("Generated cookie is "+jwt);
        ResponseCookie cookie = ResponseCookie.from("jwt", jwt)
//                .httpOnly(true)
//                .path("/")
//                .maxAge(86400)
//                .build();
                .httpOnly(true)
                .secure(true)           // must be HTTPS for SameSite=None
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("None")       // allow cross-origin requests
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.getWriter().write("Login successful");

    }
}
