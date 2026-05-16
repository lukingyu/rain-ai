package com.rain.ai.task;

import java.time.Instant;
import java.util.UUID;

public record AgentTask(
        UUID id,
        String workspaceId,
        TaskType taskType,
        UUID aggregateId,
        TaskStatus status,
        String payload,
        String result,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
