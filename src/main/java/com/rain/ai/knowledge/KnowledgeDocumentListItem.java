package com.rain.ai.knowledge;

import java.util.UUID;

public record KnowledgeDocumentListItem(
        UUID id,
        String originalFilename,
        DocumentStatus status,
        String errorMessage
) {

    public static KnowledgeDocumentListItem from(KnowledgeDocument document) {
        return new KnowledgeDocumentListItem(
                document.id(),
                document.originalFilename(),
                document.status(),
                document.errorMessage()
        );
    }
}
