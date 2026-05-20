package com.rain.ai.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rain.ai.knowledge.DocumentChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class PromptContextAssembler {

    private static final int CONTEXT_TOKEN_BUDGET = 1_600;
    private static final int MIN_TRUNCATED_CHUNK_TOKEN_COUNT = 120;
    private static final int TRUNCATION_SUFFIX_TOKEN_COUNT = 2;

    private final ObjectMapper objectMapper;

    public PromptContextAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PromptContext assemble(List<DocumentChunk> chunks) {
        List<DocumentChunk> uniqueChunks = deduplicate(chunks);
        List<PromptContextSegment> segments = new ArrayList<>();
        int usedTokenCount = 0;
        int truncatedChunkCount = 0;

        for (DocumentChunk chunk : uniqueChunks) {
            int originalTokenCount = normalizedTokenCount(chunk);
            int remainingTokenCount = CONTEXT_TOKEN_BUDGET - usedTokenCount;
            if (remainingTokenCount <= 0) {
                break;
            }

            if (originalTokenCount <= remainingTokenCount) {
                segments.add(toSegment(segments.size() + 1, chunk, chunk.content(), originalTokenCount, false));
                usedTokenCount += originalTokenCount;
                continue;
            }

            if (remainingTokenCount < MIN_TRUNCATED_CHUNK_TOKEN_COUNT) {
                break;
            }

            String truncatedContent = truncateByTokenBudget(
                    chunk.content(),
                    remainingTokenCount - TRUNCATION_SUFFIX_TOKEN_COUNT
            );
            int usedTokens = estimateTokenCount(truncatedContent);
            segments.add(new PromptContextSegment(
                    segments.size() + 1,
                    chunk.documentId(),
                    chunk.chunkIndex(),
                    truncatedContent,
                    originalTokenCount,
                    usedTokens,
                    true
            ));
            usedTokenCount += usedTokens;
            truncatedChunkCount++;
        }

        int deduplicatedChunkCount = chunks.size() - uniqueChunks.size();
        int omittedChunkCount = uniqueChunks.size() - segments.size();
        return new PromptContext(
                segments,
                CONTEXT_TOKEN_BUDGET,
                usedTokenCount,
                truncatedChunkCount,
                deduplicatedChunkCount,
                omittedChunkCount
        );
    }

    private List<DocumentChunk> deduplicate(List<DocumentChunk> chunks) {
        Map<String, DocumentChunk> uniqueChunks = new LinkedHashMap<>();
        for (DocumentChunk chunk : chunks) {
            uniqueChunks.putIfAbsent(resolveDeduplicationKey(chunk), chunk);
        }
        return new ArrayList<>(uniqueChunks.values());
    }

    private String resolveDeduplicationKey(DocumentChunk chunk) {
        String contentHash = readContentHash(chunk.metadata());
        if (contentHash != null && !contentHash.isBlank()) {
            return "hash:" + contentHash;
        }
        return "chunk:" + chunk.id();
    }

    private String readContentHash(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> values = objectMapper.readValue(
                    metadata,
                    new TypeReference<>() {
                    }
            );
            Object contentHash = values.get("contentHash");
            return contentHash instanceof String value ? value : null;
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private PromptContextSegment toSegment(
            int citationIndex,
            DocumentChunk chunk,
            String content,
            int usedTokenCount,
            boolean truncated
    ) {
        return new PromptContextSegment(
                citationIndex,
                chunk.documentId(),
                chunk.chunkIndex(),
                content,
                normalizedTokenCount(chunk),
                usedTokenCount,
                truncated
        );
    }

    private int normalizedTokenCount(DocumentChunk chunk) {
        if (chunk.tokenCount() > 0) {
            return chunk.tokenCount();
        }
        return estimateTokenCount(chunk.content());
    }

    private String truncateByTokenBudget(String content, int tokenBudget) {
        if (tokenBudget <= 0 || content.isBlank()) {
            return "";
        }

        int cjkCount = 0;
        int asciiCount = 0;
        int endIndex = 0;
        for (int index = 0; index < content.length(); index++) {
            char value = content.charAt(index);
            if (isCjk(value)) {
                cjkCount++;
            } else if (!Character.isWhitespace(value)) {
                asciiCount++;
            }
            int tokenCount = Math.max(1, cjkCount + Math.ceilDiv(asciiCount, 4));
            if (tokenCount > tokenBudget) {
                break;
            }
            endIndex = index + 1;
        }
        return content.substring(0, endIndex).stripTrailing() + "...";
    }

    private int estimateTokenCount(String content) {
        int cjkCount = 0;
        int asciiCount = 0;
        for (int index = 0; index < content.length(); index++) {
            char value = content.charAt(index);
            if (isCjk(value)) {
                cjkCount++;
            } else if (!Character.isWhitespace(value)) {
                asciiCount++;
            }
        }
        return Math.max(1, cjkCount + Math.ceilDiv(asciiCount, 4));
    }

    private boolean isCjk(char value) {
        Character.UnicodeScript script = Character.UnicodeScript.of(value);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || script == Character.UnicodeScript.HANGUL;
    }
}
