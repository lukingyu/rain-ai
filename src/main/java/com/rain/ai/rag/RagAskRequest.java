package com.rain.ai.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RagAskRequest(
        @NotNull(message = "知识库不能为空")
        UUID knowledgeBaseId,

        @NotBlank(message = "问题不能为空")
        @Size(max = 1000, message = "问题不能超过 1000 个字符")
        String question
) {
}
