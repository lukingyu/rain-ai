package com.rain.ai.embedding;

import com.rain.ai.knowledge.DocumentChunk;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EmbeddingService {

    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final EmbeddingRecordRepository embeddingRecordRepository;
    private final String embeddingApiKey;
    private final String embeddingModelName;

    public EmbeddingService(
            ObjectProvider<EmbeddingModel> embeddingModelProvider,
            EmbeddingRecordRepository embeddingRecordRepository,
            @Value("${spring.ai.openai.embedding.api-key:}") String embeddingApiKey,
            @Value("${spring.ai.openai.embedding.options.model:text-embedding-v4}") String embeddingModelName
    ) {
        this.embeddingModelProvider = embeddingModelProvider;
        this.embeddingRecordRepository = embeddingRecordRepository;
        this.embeddingApiKey = embeddingApiKey;
        this.embeddingModelName = embeddingModelName;
    }

    public boolean available() {
        return embeddingModelProvider.getIfAvailable() != null && hasRealApiKey();
    }

    public void rebuildDocumentEmbeddings(UUID documentId, List<DocumentChunk> chunks) {
        if (!available() || chunks.isEmpty()) {
            return;
        }
        embeddingRecordRepository.deleteByDocumentId(documentId);
        List<String> contents = chunks.stream()
                .map(DocumentChunk::content)
                .toList();
        List<float[]> vectors = embeddingModelProvider.getObject().embed(contents);
        List<EmbeddingRecord> records = new ArrayList<>();
        Instant now = Instant.now();
        for (int index = 0; index < chunks.size(); index++) {
            DocumentChunk chunk = chunks.get(index);
            records.add(new EmbeddingRecord(
                    UUID.randomUUID(),
                    chunk.workspaceId(),
                    chunk.knowledgeBaseId(),
                    chunk.documentId(),
                    chunk.id(),
                    embeddingModelName,
                    toVectorLiteral(vectors.get(index)),
                    now
            ));
        }
        embeddingRecordRepository.saveAll(records);
    }

    public String embedQuery(String question) {
        if (!available()) {
            return null;
        }
        return toVectorLiteral(embeddingModelProvider.getObject().embed(question));
    }

    private boolean hasRealApiKey() {
        return embeddingApiKey != null
                && !embeddingApiKey.isBlank()
                && !embeddingApiKey.equals("replace-with-your-api-key")
                && !embeddingApiKey.equals("test-key");
    }

    private String toVectorLiteral(float[] vector) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < vector.length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(vector[index]);
        }
        return builder.append(']').toString();
    }
}
