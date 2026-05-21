package com.rain.ai.tool;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import com.rain.ai.knowledge.DocumentStatus;
import com.rain.ai.knowledge.DocumentIngestionService;
import com.rain.ai.knowledge.DocumentReingestBatchResult;
import com.rain.ai.knowledge.KnowledgeBase;
import com.rain.ai.knowledge.KnowledgeBaseService;
import com.rain.ai.knowledge.KnowledgeDocument;
import com.rain.ai.knowledge.KnowledgeDocumentRepository;
import com.rain.ai.rag.RagCitation;
import com.rain.ai.rag.RagRetrievalService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class KnowledgeBaseAiTools {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentIngestionService documentIngestionService;
    private final RagRetrievalService ragRetrievalService;

    public KnowledgeBaseAiTools(
            KnowledgeBaseService knowledgeBaseService,
            KnowledgeDocumentRepository documentRepository,
            DocumentIngestionService documentIngestionService,
            RagRetrievalService ragRetrievalService
    ) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentRepository = documentRepository;
        this.documentIngestionService = documentIngestionService;
        this.ragRetrievalService = ragRetrievalService;
    }

    @Tool(name = "listKnowledgeBases", description = "查询当前系统中的知识库列表")
    public List<KnowledgeBase> listKnowledgeBases() {
        return knowledgeBaseService.list();
    }

    @Tool(name = "listFailedDocuments", description = "查询指定知识库下处理失败的文档")
    public List<KnowledgeDocument> listFailedDocuments(
            @ToolParam(description = "知识库 ID") String knowledgeBaseId
    ) {
        UUID id = parseUuid(knowledgeBaseId, "knowledgeBaseId");
        knowledgeBaseService.getRequired(id);
        return documentRepository.findByKnowledgeBaseIdAndStatus(id, DocumentStatus.FAILED);
    }

    @Tool(name = "reingestFailedDocuments", description = "重新投递指定知识库下所有处理失败的文档摄取任务")
    public DocumentReingestBatchResult reingestFailedDocuments(
            @ToolParam(description = "知识库 ID") String knowledgeBaseId
    ) {
        UUID id = parseUuid(knowledgeBaseId, "knowledgeBaseId");
        return documentIngestionService.reingestFailed(id);
    }

    @Tool(name = "reingestAllDocuments", description = "重新投递指定知识库下全部文档的摄取任务")
    public DocumentReingestBatchResult reingestAllDocuments(
            @ToolParam(description = "知识库 ID") String knowledgeBaseId
    ) {
        UUID id = parseUuid(knowledgeBaseId, "knowledgeBaseId");
        return documentIngestionService.reingestAll(id);
    }

    @Tool(name = "searchKnowledgeBase", description = "从指定知识库的向量库中召回与问题最相关的原文片段")
    public List<RagCitation> searchKnowledgeBase(
            @ToolParam(description = "知识库 ID") String knowledgeBaseId,
            @ToolParam(description = "用户问题或检索关键词") String question
    ) {
        UUID id = parseUuid(knowledgeBaseId, "knowledgeBaseId");
        knowledgeBaseService.getRequired(id);
        return ragRetrievalService.retrieveCitations(id, question);
    }

    private UUID parseUuid(String value, String name) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw new BizException(ErrorCode.参数错误, name + " 必须是合法 UUID");
        }
    }
}
