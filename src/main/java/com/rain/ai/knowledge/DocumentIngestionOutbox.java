package com.rain.ai.knowledge;

import java.util.UUID;

public record DocumentIngestionOutbox(
        long id,
        UUID documentId,
        UUID knowledgeBaseId,
        String storagePath,
        DocumentIngestionOutboxStatus status,
        int retryCount,
        String errorMessage
) {

    public DocumentIngestionMessage toMessage() {
        return new DocumentIngestionMessage(documentId, knowledgeBaseId, storagePath);
    }
}
