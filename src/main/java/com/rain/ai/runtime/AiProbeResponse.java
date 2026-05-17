package com.rain.ai.runtime;

public record AiProbeResponse(
        AiProbeResult chat,
        AiProbeResult embedding
) {
}
