package com.rain.ai.rag;

import com.rain.ai.embedding.EmbeddingRecordRepository;
import com.rain.ai.embedding.EmbeddingService;
import com.rain.ai.knowledge.DocumentChunk;
import com.rain.ai.knowledge.DocumentChunkRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ChunkRetrievalService {

    private static final int MAX_KEYWORDS = 8;
    private static final int MAX_CHUNKS = 5;
    private static final int VECTOR_CANDIDATE_LIMIT = 20;
    private static final int KEYWORD_CANDIDATE_LIMIT = 20;

    private final EmbeddingService embeddingService;
    private final EmbeddingRecordRepository embeddingRecordRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final HybridChunkRanker hybridChunkRanker;

    public ChunkRetrievalService(
            EmbeddingService embeddingService,
            EmbeddingRecordRepository embeddingRecordRepository,
            DocumentChunkRepository documentChunkRepository,
            HybridChunkRanker hybridChunkRanker
    ) {
        this.embeddingService = embeddingService;
        this.embeddingRecordRepository = embeddingRecordRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.hybridChunkRanker = hybridChunkRanker;
    }

    public ChunkRetrievalResult retrieve(UUID knowledgeBaseId, String question) {
        List<DocumentChunk> vectorCandidates = retrieveVectorCandidates(knowledgeBaseId, question);
        List<DocumentChunk> keywordCandidates = retrieveKeywordCandidates(knowledgeBaseId, question);
        if (vectorCandidates.isEmpty() && keywordCandidates.isEmpty()) {
            List<DocumentChunk> latestChunks = documentChunkRepository.findLatestByKnowledgeBaseId(
                    knowledgeBaseId,
                    MAX_CHUNKS
            );
            return new ChunkRetrievalResult(latestChunks, "无有效候选，降级返回最新分片");
        }

        List<DocumentChunk> fusedChunks = hybridChunkRanker.fuse(
                vectorCandidates,
                keywordCandidates,
                MAX_CHUNKS
        );
        return new ChunkRetrievalResult(
                fusedChunks,
                "混合召回 RRF，向量候选=%d，关键词候选=%d，融合后=%d".formatted(
                        vectorCandidates.size(),
                        keywordCandidates.size(),
                        fusedChunks.size()
                )
        );
    }

    private List<DocumentChunk> retrieveVectorCandidates(UUID knowledgeBaseId, String question) {
        String queryEmbedding = embeddingService.embedQuery(question);
        if (queryEmbedding == null) {
            return List.of();
        }
        return embeddingRecordRepository.searchSimilarChunks(
                knowledgeBaseId,
                queryEmbedding,
                VECTOR_CANDIDATE_LIMIT
        );
    }

    private List<DocumentChunk> retrieveKeywordCandidates(UUID knowledgeBaseId, String question) {
        List<String> keywords = extractKeywords(question);
        Map<UUID, DocumentChunk> result = new LinkedHashMap<>();
        for (String keyword : keywords) {
            List<DocumentChunk> chunks = documentChunkRepository.searchByKnowledgeBaseId(
                    knowledgeBaseId,
                    keyword,
                    KEYWORD_CANDIDATE_LIMIT
            );
            for (DocumentChunk chunk : chunks) {
                result.putIfAbsent(chunk.id(), chunk);
            }
            if (result.size() >= KEYWORD_CANDIDATE_LIMIT) {
                break;
            }
        }
        return result.values().stream()
                .limit(KEYWORD_CANDIDATE_LIMIT)
                .toList();
    }

    private List<String> extractKeywords(String question) {
        Map<String, Boolean> keywords = new LinkedHashMap<>();
        String strippedQuestion = question.strip();
        if (strippedQuestion.length() >= 2 && strippedQuestion.length() <= 40) {
            keywords.put(strippedQuestion, true);
        }

        String normalized = strippedQuestion.toLowerCase(Locale.ROOT)
                .replaceAll("[，。！？、；：,.!?;:\\n\\r\\t]", " ");
        String[] parts = normalized.split("\\s+");
        for (String part : parts) {
            if (part.length() >= 2) {
                keywords.put(part, true);
                addCjkTerms(part, keywords);
            }
            if (keywords.size() >= MAX_KEYWORDS) {
                break;
            }
        }
        if (keywords.isEmpty()) {
            keywords.put(strippedQuestion, true);
        }
        return new ArrayList<>(keywords.keySet()).stream()
                .limit(MAX_KEYWORDS)
                .toList();
    }

    private void addCjkTerms(String value, Map<String, Boolean> keywords) {
        if (keywords.size() >= MAX_KEYWORDS || !containsCjk(value)) {
            return;
        }
        for (int index = 0; index + 2 <= value.length(); index += 2) {
            String term = value.substring(index, Math.min(index + 2, value.length()));
            if (term.length() >= 2) {
                keywords.put(term, true);
            }
            if (keywords.size() >= MAX_KEYWORDS) {
                return;
            }
        }
    }

    private boolean containsCjk(String value) {
        for (int index = 0; index < value.length(); index++) {
            Character.UnicodeScript script = Character.UnicodeScript.of(value.charAt(index));
            if (script == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }
}
