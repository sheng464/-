package com.example.authbackend.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public void setToken(String username, String token) {
        try {
            redisTemplate.opsForValue().set("token:" + username, token, 3600, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Redis setToken error: " + e.getMessage());
        }
    }
    
    public String getToken(String username) {
        try {
            Object result = redisTemplate.opsForValue().get("token:" + username);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            System.err.println("Redis getToken error: " + e.getMessage());
            return null;
        }
    }
    
    public void removeToken(String username) {
        try {
            redisTemplate.delete("token:" + username);
        } catch (Exception e) {
            System.err.println("Redis removeToken error: " + e.getMessage());
        }
    }
    
    public boolean hasToken(String username) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey("token:" + username));
        } catch (Exception e) {
            System.err.println("Redis hasToken error: " + e.getMessage());
            return false;
        }
    }
}
