package com.rain.ai.rag;

import com.rain.ai.embedding.EmbeddingRecordRepository;
import com.rain.ai.embedding.EmbeddingService;
import com.rain.ai.knowledge.DocumentChunk;
import com.rain.ai.knowledge.DocumentChunkRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ChunkRetrievalService {

    private static final int MAX_KEYWORDS = 6;
    private static final int MAX_CHUNKS = 5;

    private final EmbeddingService embeddingService;
    private final EmbeddingRecordRepository embeddingRecordRepository;
    private final DocumentChunkRepository documentChunkRepository;

    public ChunkRetrievalService(
            EmbeddingService embeddingService,
            EmbeddingRecordRepository embeddingRecordRepository,
            DocumentChunkRepository documentChunkRepository
    ) {
        this.embeddingService = embeddingService;
        this.embeddingRecordRepository = embeddingRecordRepository;
        this.documentChunkRepository = documentChunkRepository;
    }

    public ChunkRetrievalResult retrieve(UUID knowledgeBaseId, String question) {
        ChunkRetrievalResult vectorResult = retrieveByVector(knowledgeBaseId, question);
        if (!vectorResult.chunks().isEmpty()) {
            return vectorResult;
        }
        return retrieveByKeyword(knowledgeBaseId, question);
    }

    private ChunkRetrievalResult retrieveByVector(UUID knowledgeBaseId, String question) {
        String queryEmbedding = embeddingService.embedQuery(question);
        if (queryEmbedding == null) {
            return new ChunkRetrievalResult(List.of(), "向量召回不可用，未配置真实模型 Key");
        }
        List<DocumentChunk> chunks = embeddingRecordRepository.searchSimilarChunks(
                knowledgeBaseId,
                queryEmbedding,
                MAX_CHUNKS
        );
        return new ChunkRetrievalResult(chunks, "pgvector 向量召回");
    }

    private ChunkRetrievalResult retrieveByKeyword(UUID knowledgeBaseId, String question) {
        List<String> keywords = extractKeywords(question);
        Set<DocumentChunk> result = new LinkedHashSet<>();
        for (String keyword : keywords) {
            result.addAll(documentChunkRepository.searchByKnowledgeBaseId(knowledgeBaseId, keyword, MAX_CHUNKS));
            if (result.size() >= MAX_CHUNKS) {
                break;
            }
        }
        if (result.isEmpty()) {
            result.addAll(documentChunkRepository.findLatestByKnowledgeBaseId(knowledgeBaseId, MAX_CHUNKS));
        }
        return new ChunkRetrievalResult(
                result.stream().limit(MAX_CHUNKS).toList(),
                "关键词召回降级"
        );
    }

    private List<String> extractKeywords(String question) {
        String normalized = question.toLowerCase(Locale.ROOT)
                .replaceAll("[，。！？、；：,.!?;:\\\\n\\\\r\\\\t]", " ");
        String[] parts = normalized.split("\\s+");
        List<String> keywords = new ArrayList<>();
        for (String part : parts) {
            if (part.length() >= 2) {
                keywords.add(part);
            }
            if (keywords.size() >= MAX_KEYWORDS) {
                break;
            }
        }
        if (keywords.isEmpty()) {
            keywords.add(question.strip());
        }
        return keywords;
    }
}
