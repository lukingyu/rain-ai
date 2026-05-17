package com.rain.ai.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AgentSessionMemoryService {

    private static final int MAX_MESSAGES = 20;
    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "rain-ai:agent:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AgentSessionMemoryService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return sessionId;
    }

    public List<AgentMemoryMessage> recentMessages(String sessionId) {
        List<String> values = redisTemplate.opsForList().range(key(sessionId), 0, -1);
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::fromJson)
                .toList();
    }

    public void appendUserMessage(String sessionId, String content) {
        append(sessionId, new AgentMemoryMessage("user", content, Instant.now()));
    }

    public void appendAssistantMessage(String sessionId, String content) {
        append(sessionId, new AgentMemoryMessage("assistant", content, Instant.now()));
    }

    private void append(String sessionId, AgentMemoryMessage message) {
        String key = key(sessionId);
        redisTemplate.opsForList().rightPush(key, toJson(message));
        redisTemplate.opsForList().trim(key, -MAX_MESSAGES, -1);
        redisTemplate.expire(key, SESSION_TTL);
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId + ":messages";
    }

    private String toJson(AgentMemoryMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("会话记忆序列化失败", exception);
        }
    }

    private AgentMemoryMessage fromJson(String value) {
        try {
            return objectMapper.readValue(value, AgentMemoryMessage.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("会话记忆反序列化失败", exception);
        }
    }
}
