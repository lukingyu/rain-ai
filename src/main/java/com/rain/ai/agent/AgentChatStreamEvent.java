package com.rain.ai.agent;

import com.rain.ai.rag.RagCitation;

import java.util.List;

public record AgentChatStreamEvent(
        String type,
        String sessionId,
        String content,
        List<RagCitation> citations
) {

    public static AgentChatStreamEvent session(String sessionId) {
        return new AgentChatStreamEvent("session", sessionId, "", List.of());
    }

    public static AgentChatStreamEvent delta(String sessionId, String content) {
        return new AgentChatStreamEvent("delta", sessionId, content, List.of());
    }

    public static AgentChatStreamEvent citations(String sessionId, List<RagCitation> citations) {
        return new AgentChatStreamEvent("citations", sessionId, "", citations);
    }

    public static AgentChatStreamEvent done(String sessionId) {
        return new AgentChatStreamEvent("done", sessionId, "", List.of());
    }

    public static AgentChatStreamEvent error(String sessionId, String message) {
        return new AgentChatStreamEvent("error", sessionId, message, List.of());
    }
}
