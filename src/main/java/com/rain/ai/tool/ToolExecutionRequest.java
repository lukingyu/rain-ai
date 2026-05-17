package com.rain.ai.tool;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record ToolExecutionRequest(
        @NotBlank(message = "工具名称不能为空")
        String toolName,
        Map<String, Object> arguments
) {
    public Map<String, Object> safeArguments() {
        if (arguments == null) {
            return Map.of();
        }
        return arguments;
    }
}
