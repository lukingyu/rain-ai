package com.rain.ai.agent;

import com.rain.ai.tool.ToolExecutionResponse;

import java.util.Map;

public record AgentChatResponse(
        String sessionId,
        String message,
        String plannerType,
        String selectedToolName,
        Map<String, Object> arguments,
        ToolExecutionResponse toolExecution,
        String finalAnswer
) {
}
