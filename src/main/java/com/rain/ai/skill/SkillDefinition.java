package com.rain.ai.skill;

import com.rain.ai.tool.ToolParameter;

import java.util.List;

public record SkillDefinition(
        String name,
        String description,
        List<String> toolNames,
        List<ToolParameter> parameters
) {
    public SkillDefinition(String name, String description, List<String> toolNames) {
        this(name, description, toolNames, List.of());
    }
}
