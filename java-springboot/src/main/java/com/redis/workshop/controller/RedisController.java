package com.redis.workshop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RedisController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/")
    public Map<String, String> home() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Redis Spring Boot Workshop - Ready!");
        return response;
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        Map<String, String> response = new HashMap<>();
        try {
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            response.put("status", "connected");
            response.put("ping", pong);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PostMapping("/set/{key}")
    public Map<String, String> setValue(@PathVariable String key, @RequestBody String value) {
        Map<String, String> response = new HashMap<>();
        try {
            redisTemplate.opsForValue().set(key, value);
            response.put("status", "success");
            response.put("key", key);
            response.put("value", value);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/get/{key}")
    public Map<String, String> getValue(@PathVariable String key) {
        Map<String, String> response = new HashMap<>();
        try {
            String value = (String) redisTemplate.opsForValue().get(key);
            response.put("status", "success");
            response.put("key", key);
            response.put("value", value != null ? value : "null");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }
}

