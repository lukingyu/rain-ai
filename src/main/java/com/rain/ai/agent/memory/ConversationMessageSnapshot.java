package com.rain.ai.agent.memory;

public record ConversationMessageSnapshot(
        long id,
        String messageType,
        String content
) {
}
