package com.rain.ai.rag;

import org.springframework.ai.document.Document;

import java.util.List;

public record ChunkRetrievalResult(
        List<Document> documents,
        String strategy
) {
}
