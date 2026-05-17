package com.rain.ai.skill;

public interface SkillHandler {

    SkillDefinition definition();

    SkillExecutionOutcome execute(SkillExecutionContext context);
}
