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
        // RAG 召回保持直线流程：向量先找语义相似分片，关键词只负责补足结果数量。
        List<DocumentChunk> vectorChunks = retrieveByVector(knowledgeBaseId, question);
        List<DocumentChunk> chunks = new ArrayList<>(vectorChunks);
        fillByKeyword(knowledgeBaseId, question, chunks);
        if (chunks.isEmpty()) {
            chunks.addAll(documentChunkRepository.findLatestByKnowledgeBaseId(knowledgeBaseId, MAX_CHUNKS));
        }

        return new ChunkRetrievalResult(
                chunks.stream().limit(MAX_CHUNKS).toList(),
                "向量优先召回，向量=%d，关键词补充=%d".formatted(
                        vectorChunks.size(),
                        Math.max(0, chunks.size() - vectorChunks.size())
                )
        );
    }

    private List<DocumentChunk> retrieveByVector(UUID knowledgeBaseId, String question) {
        String queryEmbedding = embeddingService.embedQuery(question);
        if (queryEmbedding == null) {
            return List.of();
        }
        return embeddingRecordRepository.searchSimilarChunks(
                knowledgeBaseId,
                queryEmbedding,
                MAX_CHUNKS
        );
    }

    private void fillByKeyword(UUID knowledgeBaseId, String question, List<DocumentChunk> chunks) {
        List<String> keywords = extractKeywords(question);
        Map<UUID, DocumentChunk> selectedChunks = new LinkedHashMap<>();
        for (DocumentChunk chunk : chunks) {
            selectedChunks.put(chunk.id(), chunk);
        }

        for (String keyword : keywords) {
            List<DocumentChunk> matchedChunks = documentChunkRepository.searchByKnowledgeBaseId(
                    knowledgeBaseId,
                    keyword,
                    MAX_CHUNKS
            );
            for (DocumentChunk chunk : matchedChunks) {
                selectedChunks.putIfAbsent(chunk.id(), chunk);
            }
            if (selectedChunks.size() >= MAX_CHUNKS) {
                break;
            }
        }

        chunks.clear();
        chunks.addAll(selectedChunks.values().stream().limit(MAX_CHUNKS).toList());
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
