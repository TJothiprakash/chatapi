package com.jp.chatapi.security.redis.resetpassword;

import com.jp.chatapi.security.redis.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ResetPasswordService {

    private static final Logger logger = LoggerFactory.getLogger(ResetPasswordService.class);
    private static final long TOKEN_TTL_MINUTES = 15;

    @Autowired
    private RedisService redisService;

    public void storeResetToken(String token, String email) {
        String key = "reset_token:" + token;
        redisService.set(key, email, TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        logger.info("Stored reset token in Redis: {} for email: {} (TTL={} minutes)", token, email, TOKEN_TTL_MINUTES);
    }

    public String getEmailByToken(String token) {
        String key = "reset_token:" + token;
        String email = redisService.get(key);
        if (email != null) {
            logger.info("Found email '{}' for token '{}'", email, token);
        } else {
            logger.warn("No email found for token '{}'", token);
        }
        return email;
    }

    public void invalidateToken(String token) {
        String key = "reset_token:" + token;
        redisService.delete(key);
        logger.info("Invalidated reset token '{}'", token);
    }
}
