package com.rain.ai.tool;

import java.util.List;

public record ToolDefinition(
        String name,
        String description,
        List<ToolParameter> parameters
) {
}
