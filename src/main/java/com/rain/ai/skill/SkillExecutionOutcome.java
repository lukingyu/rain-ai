package com.rain.ai.skill;

import java.util.List;

public record SkillExecutionOutcome(
        String summary,
        List<SkillStepResult> steps
) {
}
