package com.jp.chatapi.chat.modal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
public class User implements UserDetails {

    private Integer id;
    private String username;   // JSON → "username"
    private String email;      // JSON → "email"

    private String passwordHash;

    private String avatarUrl;
    private boolean emailVerified;
    private String verificationToken;
    private String resetToken;
    private LocalDateTime resetTokenExpiry;
    private LocalDateTime createdAt;

    public User() {}

    // --------- JSON: accept "password" and map to passwordHash ---------
    @JsonProperty("password")
    public void setRawPassword(String rawPassword) {
        this.passwordHash = rawPassword;
    }

    // --------- Spring Security ---------
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email; // login with email
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
