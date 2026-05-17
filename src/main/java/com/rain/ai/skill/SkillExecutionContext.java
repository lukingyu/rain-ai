package com.rain.ai.skill;

import java.util.Map;

public record SkillExecutionContext(
        Map<String, Object> arguments
) {
}
