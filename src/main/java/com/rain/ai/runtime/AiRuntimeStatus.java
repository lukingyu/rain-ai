package com.rain.ai.runtime;

public record AiRuntimeStatus(
        AiProviderStatus chat,
        AiProviderStatus embedding
) {
}
