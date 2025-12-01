package com.jp.chatapi.security.redis.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class RedisTestController {

    private static final Logger logger = LoggerFactory.getLogger(RedisTestController.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/redis/test")
    public String test() {
        logger.info("Setting key 'ping' with value 'pong' in Redis with 10 seconds TTL");
        redisTemplate.opsForValue().set("ping", "pong", 10, TimeUnit.SECONDS);

        String value = redisTemplate.opsForValue().get("ping");
        logger.info("Retrieved key 'ping' from Redis: {}", value);

        return value;
    }
}
