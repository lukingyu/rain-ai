package com.rain.ai.rag;

import java.util.UUID;

public record RagCitation(
        UUID documentId,
        int chunkIndex,
        String content
) {
}
