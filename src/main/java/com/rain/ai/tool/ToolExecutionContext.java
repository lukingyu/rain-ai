package com.rain.ai.tool;

import java.util.Map;

public record ToolExecutionContext(
        Map<String, Object> arguments
) {
}
