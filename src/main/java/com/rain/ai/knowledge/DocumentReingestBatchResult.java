package com.rain.ai.knowledge;

import java.util.List;
import java.util.UUID;

public record DocumentReingestBatchResult(
        UUID knowledgeBaseId,
        int totalCount,
        int submittedCount,
        int failedCount,
        List<KnowledgeDocument> documents,
        List<DocumentReingestFailure> failures
) {
}
