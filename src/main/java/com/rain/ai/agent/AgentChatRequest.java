package com.rain.ai.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AgentChatRequest(
        String sessionId,

        UUID knowledgeBaseId,

        @NotBlank(message = "消息不能为空")
        @Size(max = 1000, message = "消息不能超过 1000 个字符")
        String message
) {
}
