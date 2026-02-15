package com.adhar.newapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimiterService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final int MAX_ATTEMPTS = 50; // Increased for development
    private static final long WINDOW_MINUTES = 15;
    private static final long LOCKOUT_MINUTES = 30;

    public boolean isBlocked(String key) {
        String lockoutKey = "lockout:" + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockoutKey));
    }

    public boolean allowRequest(String key) {
        if (isBlocked(key)) {
            return false;
        }

        String rateKey = "rate:" + key;
        Long attempts = redisTemplate.opsForValue().increment(rateKey);

        if (attempts != null && attempts == 1) {
            redisTemplate.expire(rateKey, Duration.ofMinutes(WINDOW_MINUTES));
        }

        if (attempts != null && attempts > MAX_ATTEMPTS) {
            // Block user
            redisTemplate.opsForValue().set("lockout:" + key, "LOCKED", Duration.ofMinutes(LOCKOUT_MINUTES));
            return false;
        }

        return true;
    }
}
