package com.rain.ai.agent.memory;

public record ConversationSummary(
        String conversationId,
        String summary,
        long summarizedMessageId
) {
}
