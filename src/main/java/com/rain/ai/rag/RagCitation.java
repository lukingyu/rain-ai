package com.rain.ai.rag;

public record RagCitation(
        String documentId,
        int chunkIndex,
        String content
) {
}
