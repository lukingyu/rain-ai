package com.rain.ai.runtime;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AiRuntimeProbeService {

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final AiRuntimeStatusService aiRuntimeStatusService;

    public AiRuntimeProbeService(
            ObjectProvider<ChatModel> chatModelProvider,
            ObjectProvider<EmbeddingModel> embeddingModelProvider,
            AiRuntimeStatusService aiRuntimeStatusService
    ) {
        this.chatModelProvider = chatModelProvider;
        this.embeddingModelProvider = embeddingModelProvider;
        this.aiRuntimeStatusService = aiRuntimeStatusService;
    }

    public AiProbeResponse probe() {
        return new AiProbeResponse(probeChat(), probeEmbedding());
    }

    private AiProbeResult probeChat() {
        String provider = "openai-compatible-chat";
        if (!aiRuntimeStatusService.chatAvailable()) {
            return AiProbeResult.skipped(provider, "聊天模型未配置真实 API Key，跳过在线探针");
        }
        try {
            String text = chatModelProvider.getObject()
                    .call(new Prompt(List.of(new UserMessage("请只回复：pong"))))
                    .getResult()
                    .getOutput()
                    .getText();
            return AiProbeResult.success(provider, Map.of("reply", text));
        } catch (Exception exception) {
            return AiProbeResult.failed(provider, exception);
        }
    }

    private AiProbeResult probeEmbedding() {
        String provider = "openai-compatible-embedding";
        if (!aiRuntimeStatusService.embeddingAvailable()) {
            return AiProbeResult.skipped(provider, "向量模型未配置真实 API Key，跳过在线探针");
        }
        try {
            float[] vector = embeddingModelProvider.getObject().embed("Rain AI 向量探针");
            return AiProbeResult.success(provider, Map.of("dimension", vector.length));
        } catch (Exception exception) {
            return AiProbeResult.failed(provider, exception);
        }
    }
}
