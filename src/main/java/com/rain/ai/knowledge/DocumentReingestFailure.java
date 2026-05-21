package com.rain.ai.knowledge;

import java.util.UUID;

public record DocumentReingestFailure(
        UUID documentId,
        String reason
) {
}
