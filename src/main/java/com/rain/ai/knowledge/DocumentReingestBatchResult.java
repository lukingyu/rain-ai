package com.rain.ai.knowledge;

import java.util.List;
import java.util.UUID;

public record DocumentReingestBatchResult(
        UUID knowledgeBaseId,
        int submittedCount,
        List<KnowledgeDocument> documents
) {
}
