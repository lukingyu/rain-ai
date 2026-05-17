package com.rain.ai.runtime;

public record AiProviderStatus(
        String provider,
        boolean configured,
        boolean modelBeanAvailable,
        String baseUrl,
        String model,
        Integer dimensions
) {
}
