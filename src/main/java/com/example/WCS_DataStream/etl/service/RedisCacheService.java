package com.example.WCS_DataStream.etl.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public RedisCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public <T> T get(String namespace, String key, Class<T> clazz) {
        Object v = redisTemplate.opsForValue().get(namespacedKey(namespace, key));
        if (v == null) return null;
        if (clazz.isInstance(v)) return clazz.cast(v);
        try {
            if (v instanceof java.util.Map) {
                return objectMapper.convertValue(v, clazz);
            }
            if (v instanceof String s) {
                return objectMapper.readValue(s, clazz);
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    public void set(String namespace, String key, Object value) {
        redisTemplate.opsForValue().set(namespacedKey(namespace, key), value);
    }

    public void delete(String namespace, String key) {
        redisTemplate.delete(namespacedKey(namespace, key));
    }

    public void clearNamespace(String namespace) {
        String pattern = namespace + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String namespacedKey(String namespace, String key) {
        return namespace + ":" + Objects.toString(key, "null");
    }
} 