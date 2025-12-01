package com.jp.chatapi.security.repository;

import com.jp.chatapi.chat.modal.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> mapper = new RowMapper<>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User u = new User();

            u.setId(rs.getInt("id"));
            u.setUsername(rs.getString("username"));
            u.setEmail(rs.getString("email"));
            u.setPasswordHash(rs.getString("password_hash"));
            u.setAvatarUrl(rs.getString("avatar_url"));
            u.setEmailVerified(rs.getBoolean("email_verified"));
            u.setVerificationToken(rs.getString("verification_token"));
            u.setResetToken(rs.getString("reset_token"));

            if (rs.getTimestamp("reset_token_expiry") != null) {
                u.setResetTokenExpiry(rs.getTimestamp("reset_token_expiry").toLocalDateTime());
            }

            if (rs.getTimestamp("created_at") != null) {
                u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }

            return u;
        }
    };

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        return jdbcTemplate.query(sql, mapper, email).stream().findFirst();
    }

    public Optional<User> findByResetToken(String resetToken) {
        String sql = "SELECT * FROM users WHERE reset_token = ?";
        return jdbcTemplate.query(sql, mapper, resetToken).stream().findFirst();
    }

    public int save(User user) {
        String sql = """
                INSERT INTO users (username, email, password_hash)
                VALUES (?, ?, ?)
                RETURNING id
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash()
        );
    }
}
