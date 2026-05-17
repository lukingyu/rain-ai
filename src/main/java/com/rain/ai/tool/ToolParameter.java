package com.rain.ai.tool;

public record ToolParameter(
        String name,
        String type,
        boolean required,
        String description
) {
}
