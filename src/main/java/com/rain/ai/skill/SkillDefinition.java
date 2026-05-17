package com.rain.ai.skill;

import java.util.List;

public record SkillDefinition(
        String name,
        String description,
        List<String> toolNames
) {
}
