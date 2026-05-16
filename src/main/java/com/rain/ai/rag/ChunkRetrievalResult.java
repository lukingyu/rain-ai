package com.rain.ai.rag;

import com.rain.ai.knowledge.DocumentChunk;

import java.util.List;

public record ChunkRetrievalResult(
        List<DocumentChunk> chunks,
        String strategy
) {
}
