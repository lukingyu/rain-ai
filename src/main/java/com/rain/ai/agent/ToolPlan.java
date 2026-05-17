package com.rain.ai.agent;

import java.util.Map;

public record ToolPlan(
        String toolName,
        Map<String, Object> arguments
) {
}
