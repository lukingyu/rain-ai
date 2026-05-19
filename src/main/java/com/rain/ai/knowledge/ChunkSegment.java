package com.rain.ai.knowledge;

public record ChunkSegment(
        String content,
        int tokenCount,
        String sectionTitle,
        int charStart,
        int charEnd,
        String boundary,
        String contentHash
) {
}
