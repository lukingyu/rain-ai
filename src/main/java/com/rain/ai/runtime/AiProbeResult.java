package com.rain.ai.runtime;

public record AiProbeResult(
        String provider,
        boolean skipped,
        boolean success,
        String message,
        Object detail
) {

    public static AiProbeResult skipped(String provider, String message) {
        return new AiProbeResult(provider, true, false, message, null);
    }

    public static AiProbeResult success(String provider, Object detail) {
        return new AiProbeResult(provider, false, true, "调用成功", detail);
    }

    public static AiProbeResult failed(String provider, Exception exception) {
        return new AiProbeResult(
                provider,
                false,
                false,
                exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                null
        );
    }
}
