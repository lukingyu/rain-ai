package com.rain.ai.rag;

import java.util.List;

public record RagGroundingEvaluation(
        boolean grounded,
        String conclusion,
        List<String> unsupportedClaims
) {

    public static RagGroundingEvaluation unavailable(String reason) {
        return new RagGroundingEvaluation(false, reason, List.of());
    }
}
