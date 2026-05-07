package com.parkease.booking.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.URI;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.url:}")
    private String redisUrl;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean redisSslEnabled;

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String configuredAddress = redisUrl == null || redisUrl.isBlank()
                ? (redisSslEnabled ? "rediss://" : "redis://") + redisHost + ":" + redisPort
                : redisUrl;
        String password = resolvePassword(configuredAddress);
        config.useSingleServer()
                .setAddress(resolveAddress(configuredAddress))
                .setPassword(password)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(2);
        return Redisson.create(config);
    }

    private String resolveAddress(String address) {
        try {
            URI uri = URI.create(address);
            if (uri.getScheme() == null || uri.getHost() == null || uri.getPort() < 0) {
                return address;
            }
            return uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
        } catch (IllegalArgumentException e) {
            return address;
        }
    }

    private String resolvePassword(String address) {
        if (redisPassword != null && !redisPassword.isBlank()) {
            return redisPassword;
        }
        try {
            String userInfo = URI.create(address).getUserInfo();
            if (userInfo == null || userInfo.isBlank()) {
                return null;
            }
            int separator = userInfo.indexOf(':');
            return separator >= 0 ? userInfo.substring(separator + 1) : userInfo;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

// same as parking-service, Redisson client is used for distributed locks when creating bookings.
// this prevents two drivers from booking the same spot.
