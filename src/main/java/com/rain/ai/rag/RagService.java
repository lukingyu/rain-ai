package com.rain.ai.rag;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import com.rain.ai.knowledge.DocumentChunk;
import com.rain.ai.knowledge.DocumentChunkRepository;
import com.rain.ai.knowledge.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RagService {

    private static final int MAX_KEYWORDS = 6;
    private static final int MAX_CHUNKS = 5;

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final PromptEngine promptEngine;
    private final AiAnswerClient aiAnswerClient;

    public RagService(
            KnowledgeBaseRepository knowledgeBaseRepository,
            DocumentChunkRepository documentChunkRepository,
            PromptEngine promptEngine,
            AiAnswerClient aiAnswerClient
    ) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.promptEngine = promptEngine;
        this.aiAnswerClient = aiAnswerClient;
    }

    public RagAnswerResponse ask(RagAskRequest request) {
        knowledgeBaseRepository.findById(request.knowledgeBaseId())
                .orElseThrow(() -> new BizException(ErrorCode.资源不存在, "知识库不存在"));

        List<DocumentChunk> chunks = retrieve(request);
        List<RagCitation> citations = chunks.stream()
                .map(chunk -> new RagCitation(chunk.documentId(), chunk.chunkIndex(), chunk.content()))
                .toList();
        RagPrompt prompt = promptEngine.build(request.question(), chunks);
        AiAnswer aiAnswer = aiAnswerClient.answer(prompt, citations);

        return new RagAnswerResponse(
                request.knowledgeBaseId(),
                request.question(),
                aiAnswer.text(),
                citations,
                aiAnswer.usedModel(),
                "召回分片数：" + citations.size()
        );
    }

    private List<DocumentChunk> retrieve(RagAskRequest request) {
        List<String> keywords = extractKeywords(request.question());
        Set<DocumentChunk> result = new LinkedHashSet<>();
        for (String keyword : keywords) {
            result.addAll(documentChunkRepository.searchByKnowledgeBaseId(
                    request.knowledgeBaseId(),
                    keyword,
                    MAX_CHUNKS
            ));
            if (result.size() >= MAX_CHUNKS) {
                break;
            }
        }
        if (result.isEmpty()) {
            result.addAll(documentChunkRepository.findLatestByKnowledgeBaseId(request.knowledgeBaseId(), MAX_CHUNKS));
        }
        return result.stream()
                .limit(MAX_CHUNKS)
                .toList();
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
