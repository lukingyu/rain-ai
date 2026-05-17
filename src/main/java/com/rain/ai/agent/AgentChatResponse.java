package com.rain.ai.agent;

import com.rain.ai.tool.ToolExecutionResponse;

import java.util.Map;

public record AgentChatResponse(
        String message,
        String selectedToolName,
        Map<String, Object> arguments,
        ToolExecutionResponse toolExecution,
        String finalAnswer
) {
}
