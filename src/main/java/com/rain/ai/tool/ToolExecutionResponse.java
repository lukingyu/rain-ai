package com.rain.ai.tool;

import java.util.UUID;

public record ToolExecutionResponse(
        UUID taskId,
        String toolName,
        String status,
        Object result
) {
}
