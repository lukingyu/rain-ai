package com.rain.ai.tool;

import com.rain.ai.knowledge.DocumentStatus;
import com.rain.ai.knowledge.KnowledgeBaseService;
import com.rain.ai.knowledge.KnowledgeDocumentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class FailedDocumentListTool implements ToolHandler {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocumentRepository documentRepository;

    public FailedDocumentListTool(
            KnowledgeBaseService knowledgeBaseService,
            KnowledgeDocumentRepository documentRepository
    ) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentRepository = documentRepository;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "document.failed.list",
                "查询指定知识库下处理失败的文档",
                List.of(new ToolParameter("knowledgeBaseId", "uuid", true, "知识库 ID"))
        );
    }

    @Override
    public Object execute(ToolExecutionContext context) {
        UUID knowledgeBaseId = ToolArguments.requiredUuid(context.arguments(), "knowledgeBaseId");
        knowledgeBaseService.getRequired(knowledgeBaseId);
        return documentRepository.findByKnowledgeBaseIdAndStatus(knowledgeBaseId, DocumentStatus.FAILED);
    }
}
