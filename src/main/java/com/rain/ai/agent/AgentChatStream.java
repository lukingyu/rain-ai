package com.rain.ai.agent;

import reactor.core.publisher.Flux;

public record AgentChatStream(
        String sessionId,
        Flux<String> content
) {
}
