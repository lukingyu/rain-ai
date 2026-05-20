package com.rain.ai.knowledge;

import java.util.UUID;

public record DocumentIngestionMessage(
        UUID documentId,
        UUID knowledgeBaseId,
        String storagePath
) {
}
