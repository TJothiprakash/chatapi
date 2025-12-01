package com.jp.chatapi.security.filter;

import com.jp.chatapi.security.jwt.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        logger.info("Incoming request: {} {}", request.getMethod(), path);

        // ✅ Skip JWT validation for public endpoints
        if (path.startsWith("/auth")) {
            logger.debug("Skipping JWT validation for public endpoint: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;

        // ✅ 1️⃣ Try Authorization header first
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            logger.debug("JWT found in Authorization header");
        } else {
            logger.debug("No JWT found in Authorization header");
        }

        // ✅ 2️⃣ If not present, fallback to HttpOnly cookie
        if (token == null && request.getCookies() != null) {
            token = Arrays.stream(request.getCookies())
                    .filter(c -> "jwt".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);

            if (token != null) {
                logger.debug("JWT found in cookie");
            } else {
                logger.debug("No JWT found in cookie");
            }
        }

        try {
            if (token != null && jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmailFromToken(token);
                logger.info("JWT validated successfully for user: {}", email);

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.debug("User authenticated in SecurityContext: {}", email);
            } else {
                logger.warn("JWT is missing or invalid for request: {}", path);
            }
        } catch (ExpiredJwtException e) {
            logger.warn("JWT expired: {}", e.getMessage());

            // 🧹 Clear expired cookie
            Cookie clear = new Cookie("jwt", null);
            clear.setHttpOnly(true);
            clear.setPath("/");
            clear.setMaxAge(0);
            response.addCookie(clear);

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (Exception e) {
            logger.error("Error validating JWT: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
