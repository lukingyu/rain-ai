package com.rain.ai.rag;

import java.util.UUID;

public record PromptContextSegment(
        int citationIndex,
        UUID documentId,
        int chunkIndex,
        String content,
        int originalTokenCount,
        int usedTokenCount,
        boolean truncated
) {
}
