package com.rain.ai.skill;

import java.util.UUID;

public record SkillStepResult(
        String stepName,
        String toolName,
        UUID taskId,
        String status,
        Object result
) {
}
