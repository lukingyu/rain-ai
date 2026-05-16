package com.rain.ai.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKnowledgeBaseRequest(
        @NotBlank
        @Size(max = 120)
        String name,

        @Size(max = 1000)
        String description
) {
}
