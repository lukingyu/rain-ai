package com.rain.ai.rag;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import com.rain.ai.knowledge.KnowledgeBaseRepository;
import org.springframework.ai.document.Document;
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
        List<Document> documents = retrievalResult.documents();
        List<RagCitation> citations = documents.stream()
                .map(document -> new RagCitation(
                        String.valueOf(document.getMetadata().get("document_id")),
                        toInt(document.getMetadata().get("chunk_index")),
                        document.getText()
                ))
                .toList();
        RagPrompt prompt = promptEngine.build(request.question(), documents);
        AiAnswer aiAnswer = aiAnswerClient.answer(prompt, citations);

        return new RagAnswerResponse(
                request.knowledgeBaseId(),
                request.question(),
                aiAnswer.text(),
                citations,
                aiAnswer.usedModel(),
                retrievalResult.strategy() + "，进入上下文分片数：" + citations.size()
        );
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
