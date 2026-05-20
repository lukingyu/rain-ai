package com.rain.ai.rag;

import java.util.List;

public record PromptContext(
        List<PromptContextSegment> segments,
        int tokenBudget,
        int usedTokenCount,
        int truncatedChunkCount,
        int deduplicatedChunkCount,
        int omittedChunkCount
) {
}
