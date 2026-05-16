package com.rain.ai.knowledge;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeDocument(
        UUID id,
        String workspaceId,
        UUID knowledgeBaseId,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String storagePath,
        DocumentStatus status,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
