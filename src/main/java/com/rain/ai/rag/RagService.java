package com.rain.ai.rag;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import com.rain.ai.knowledge.DocumentChunk;
import com.rain.ai.knowledge.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ChunkRetrievalService chunkRetrievalService;
    private final PromptEngine promptEngine;
    private final AiAnswerClient aiAnswerClient;

    public RagService(
            KnowledgeBaseRepository knowledgeBaseRepository,
            ChunkRetrievalService chunkRetrievalService,
            PromptEngine promptEngine,
            AiAnswerClient aiAnswerClient
    ) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.chunkRetrievalService = chunkRetrievalService;
        this.promptEngine = promptEngine;
        this.aiAnswerClient = aiAnswerClient;
    }

    public RagAnswerResponse ask(RagAskRequest request) {
        knowledgeBaseRepository.findById(request.knowledgeBaseId())
                .orElseThrow(() -> new BizException(ErrorCode.资源不存在, "知识库不存在"));

        ChunkRetrievalResult retrievalResult = chunkRetrievalService.retrieve(
                request.knowledgeBaseId(),
                request.question()
        );
        List<DocumentChunk> chunks = retrievalResult.chunks();
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
                retrievalResult.strategy() + "，召回分片数：" + citations.size()
        );
    }
}
