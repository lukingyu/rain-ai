package com.rain.ai.agent;

import com.rain.ai.tool.ToolExecutionResponse;

import java.util.List;

public record AiFunctionCallingResult(
        String finalAnswer,
        List<ToolExecutionResponse> toolExecutions
) {
}
