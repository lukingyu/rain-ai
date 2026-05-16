package com.rain.ai.embedding;

import java.time.Instant;
import java.util.UUID;

public record EmbeddingRecord(
        UUID id,
        String workspaceId,
        UUID knowledgeBaseId,
        UUID documentId,
        UUID chunkId,
        String embeddingModel,
        String embedding,
        Instant createdAt
) {
}
