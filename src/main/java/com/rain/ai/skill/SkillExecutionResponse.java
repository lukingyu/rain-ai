package com.rain.ai.skill;

import java.util.List;
import java.util.UUID;

public record SkillExecutionResponse(
        UUID taskId,
        String skillName,
        String status,
        String summary,
        List<SkillStepResult> steps
) {
}
