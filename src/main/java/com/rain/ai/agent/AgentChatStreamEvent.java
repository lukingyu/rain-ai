package com.rain.ai.agent;

public record AgentChatStreamEvent(
        String sessionId,
        String content
) {
}
