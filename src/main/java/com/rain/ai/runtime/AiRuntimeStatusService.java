package com.rain.ai.runtime;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiRuntimeStatusService {

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final String chatApiKey;
    private final String chatBaseUrl;
    private final String chatModelName;
    private final String embeddingApiKey;
    private final String embeddingBaseUrl;
    private final String embeddingModelName;
    private final Integer embeddingDimensions;

    public AiRuntimeStatusService(
            ObjectProvider<ChatModel> chatModelProvider,
            ObjectProvider<EmbeddingModel> embeddingModelProvider,
            @Value("${spring.ai.openai.chat.api-key:}") String chatApiKey,
            @Value("${spring.ai.openai.chat.base-url:${spring.ai.openai.base-url:}}") String chatBaseUrl,
            @Value("${spring.ai.openai.chat.options.model:}") String chatModelName,
            @Value("${spring.ai.openai.embedding.api-key:}") String embeddingApiKey,
            @Value("${spring.ai.openai.embedding.base-url:${spring.ai.openai.base-url:}}") String embeddingBaseUrl,
            @Value("${spring.ai.openai.embedding.options.model:text-embedding-v4}") String embeddingModelName,
            @Value("${spring.ai.openai.embedding.options.dimensions:1536}") Integer embeddingDimensions
    ) {
        this.chatModelProvider = chatModelProvider;
        this.embeddingModelProvider = embeddingModelProvider;
        this.chatApiKey = chatApiKey;
        this.chatBaseUrl = chatBaseUrl;
        this.chatModelName = chatModelName;
        this.embeddingApiKey = embeddingApiKey;
        this.embeddingBaseUrl = embeddingBaseUrl;
        this.embeddingModelName = embeddingModelName;
        this.embeddingDimensions = embeddingDimensions;
    }

    public boolean chatAvailable() {
        return chatModelProvider.getIfAvailable() != null && hasRealApiKey(chatApiKey);
    }

    public boolean embeddingAvailable() {
        return embeddingModelProvider.getIfAvailable() != null && hasRealApiKey(embeddingApiKey);
    }

    public AiRuntimeStatus status() {
        return new AiRuntimeStatus(
                new AiProviderStatus(
                        "openai-compatible-chat",
                        hasRealApiKey(chatApiKey),
                        chatModelProvider.getIfAvailable() != null,
                        chatBaseUrl,
                        chatModelName,
                        null
                ),
                new AiProviderStatus(
                        "openai-compatible-embedding",
                        hasRealApiKey(embeddingApiKey),
                        embeddingModelProvider.getIfAvailable() != null,
                        embeddingBaseUrl,
                        embeddingModelName,
                        embeddingDimensions
                )
        );
    }

    private boolean hasRealApiKey(String apiKey) {
        return apiKey != null
                && !apiKey.isBlank()
                && !apiKey.equals("replace-with-your-api-key")
                && !apiKey.equals("test-key");
    }
}
