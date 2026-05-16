package com.rain.ai.knowledge;

import java.time.Instant;
import java.util.UUID;

public record DocumentChunk(
        UUID id,
        String workspaceId,
        UUID knowledgeBaseId,
        UUID documentId,
        int chunkIndex,
        String content,
        int tokenCount,
        String metadata,
        Instant createdAt
) {
}
