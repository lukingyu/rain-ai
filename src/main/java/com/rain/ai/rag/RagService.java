package com.rain.ai.rag;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import com.rain.ai.knowledge.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ChunkRetrievalService chunkRetrievalService;
    private final PromptContextAssembler promptContextAssembler;
    private final PromptEngine promptEngine;
    private final AiAnswerClient aiAnswerClient;

    public RagService(
            KnowledgeBaseRepository knowledgeBaseRepository,
            ChunkRetrievalService chunkRetrievalService,
            PromptContextAssembler promptContextAssembler,
            PromptEngine promptEngine,
            AiAnswerClient aiAnswerClient
    ) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.chunkRetrievalService = chunkRetrievalService;
        this.promptContextAssembler = promptContextAssembler;
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
        PromptContext promptContext = promptContextAssembler.assemble(retrievalResult.chunks());
        List<RagCitation> citations = promptContext.segments().stream()
                .map(segment -> new RagCitation(
                        segment.documentId(),
                        segment.chunkIndex(),
                        segment.content(),
                        segment.truncated()
                ))
                .toList();
        RagPrompt prompt = promptEngine.build(request.question(), promptContext);
        AiAnswer aiAnswer = aiAnswerClient.answer(prompt, citations);

        return new RagAnswerResponse(
                request.knowledgeBaseId(),
                request.question(),
                aiAnswer.text(),
                citations,
                aiAnswer.usedModel(),
                "%s，进入上下文=%d，预算=%d/%d，截断=%d，去重=%d，丢弃=%d".formatted(
                        retrievalResult.strategy(),
                        citations.size(),
                        promptContext.usedTokenCount(),
                        promptContext.tokenBudget(),
                        promptContext.truncatedChunkCount(),
                        promptContext.deduplicatedChunkCount(),
                        promptContext.omittedChunkCount()
                )
        );
    }
}
