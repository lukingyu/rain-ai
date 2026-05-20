package com.rain.ai.task;

import java.util.UUID;

public record AgentTask(
        UUID id,
        TaskType taskType,
        UUID aggregateId,
        TaskStatus status,
        String result,
        String errorMessage
) {
}
