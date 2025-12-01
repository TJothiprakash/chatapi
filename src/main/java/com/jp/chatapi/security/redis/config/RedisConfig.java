package com.jp.chatapi.security.redis.config;

import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.url}")
    private String redisUrl;

    @Value("${spring.data.redis.ssl.enabled:true}")
    private boolean useSsl;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config;
        RedisPassword password = RedisPassword.none();

        // Parse redis URL
        RedisURI redisURI = RedisURI.create(redisUrl);

        config = new RedisStandaloneConfiguration(redisURI.getHost(), redisURI.getPort());

        if (redisURI.getPassword() != null) {
            password = RedisPassword.of(new String(redisURI.getPassword()));
        }

        config.setPassword(password);

        LettuceClientConfiguration clientConfig =
                LettuceClientConfiguration.builder()
                        .useSsl() // always use SSL for Upstash
                        .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean
    public StringRedisTemplate redisTemplate(LettuceConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
