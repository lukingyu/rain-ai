package com.rain.ai.knowledge;

import java.util.UUID;

public record DocumentIngestionMessage(
        UUID taskId,
        UUID documentId,
        UUID knowledgeBaseId,
        String workspaceId,
        String storagePath
) {
}
