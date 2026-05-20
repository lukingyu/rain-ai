package com.rain.ai.agent;

import com.rain.ai.rag.RagCitation;

import java.util.List;
import java.util.UUID;

public record AgentChatResponse(
        String sessionId,
        String message,
        String agentMode,
        UUID knowledgeBaseId,
        String answer,
        List<RagCitation> citations
) {
}
