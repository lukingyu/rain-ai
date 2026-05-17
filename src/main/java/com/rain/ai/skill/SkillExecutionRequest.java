package com.rain.ai.skill;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record SkillExecutionRequest(
        @NotBlank(message = "技能名称不能为空")
        String skillName,
        Map<String, Object> arguments
) {
    public Map<String, Object> safeArguments() {
        if (arguments == null) {
            return Map.of();
        }
        return arguments;
    }
}
