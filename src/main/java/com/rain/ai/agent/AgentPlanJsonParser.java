package com.rain.ai.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AgentPlanJsonParser {

    private static final TypeReference<Map<String, Object>> PLAN_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public AgentPlanJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AgentPlan parse(String content) {
        Map<String, Object> values = readJson(stripMarkdownFence(content));
        AgentPlanType type = parseType(values.get("type"));
        String name = requiredString(values, "name");
        Map<String, Object> arguments = readArguments(values.get("arguments"));
        return new AgentPlan(type, name, arguments);
    }

    private String stripMarkdownFence(String content) {
        String value = content == null ? "" : content.trim();
        if (!value.startsWith("```")) {
            return value;
        }
        int firstLineEnd = value.indexOf('\n');
        int lastFence = value.lastIndexOf("```");
        if (firstLineEnd < 0 || lastFence <= firstLineEnd) {
            throw new BizException(ErrorCode.参数错误, "模型规划结果不是合法 JSON");
        }
        return value.substring(firstLineEnd + 1, lastFence).trim();
    }

    private Map<String, Object> readJson(String content) {
        try {
            return objectMapper.readValue(content, PLAN_TYPE);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.参数错误, "模型规划结果 JSON 解析失败");
        }
    }

    private AgentPlanType parseType(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            throw new BizException(ErrorCode.参数错误, "模型规划缺少字段：type");
        }
        try {
            return AgentPlanType.valueOf(String.valueOf(value));
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.参数错误, "模型规划类型只允许 TOOL 或 SKILL");
        }
    }

    private String requiredString(Map<String, Object> values, String name) {
        Object value = values.get(name);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new BizException(ErrorCode.参数错误, "模型规划缺少字段：" + name);
        }
        return String.valueOf(value);
    }

    private Map<String, Object> readArguments(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (value instanceof Map<?, ?> mapValue) {
            return mapValue.entrySet()
                    .stream()
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(
                            entry -> String.valueOf(entry.getKey()),
                            Map.Entry::getValue
                    ));
        }
        throw new BizException(ErrorCode.参数错误, "模型规划 arguments 必须是 JSON 对象");
    }
}
