package com.rain.ai.knowledge;

import java.util.UUID;

public record KnowledgeDocument(
        UUID id,
        UUID knowledgeBaseId,
        String originalFilename,
        String storagePath,
        DocumentStatus status,
        String errorMessage
) {
}
