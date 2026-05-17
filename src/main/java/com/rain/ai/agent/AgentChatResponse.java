package com.rain.ai.agent;

import com.rain.ai.skill.SkillExecutionResponse;
import com.rain.ai.tool.ToolExecutionResponse;

import java.util.Map;

public record AgentChatResponse(
        String sessionId,
        String message,
        String plannerType,
        String selectedType,
        String selectedToolName,
        String selectedSkillName,
        Map<String, Object> arguments,
        ToolExecutionResponse toolExecution,
        SkillExecutionResponse skillExecution,
        String finalAnswer
) {
}
