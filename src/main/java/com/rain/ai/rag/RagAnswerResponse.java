package com.rain.ai.rag;

import java.util.List;
import java.util.UUID;

public record RagAnswerResponse(
        UUID knowledgeBaseId,
        String question,
        String answer,
        List<RagCitation> citations,
        boolean usedModel,
        String retrievalSummary
) {
}
