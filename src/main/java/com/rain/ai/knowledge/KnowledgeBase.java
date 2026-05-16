package com.rain.ai.knowledge;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeBase(
        UUID id,
        String workspaceId,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
