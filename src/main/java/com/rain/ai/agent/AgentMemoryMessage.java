package com.rain.ai.agent;

import java.time.Instant;

public record AgentMemoryMessage(
        String role,
        String content,
        Instant createdAt
) {
}
