package com.rain.ai.knowledge;

import com.rain.ai.task.AgentTask;

public record DocumentUploadResult(
        KnowledgeDocument document,
        AgentTask task
) {
}
