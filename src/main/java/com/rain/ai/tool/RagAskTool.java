package com.rain.ai.tool;

import com.rain.ai.rag.RagAskRequest;
import com.rain.ai.rag.RagService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RagAskTool implements ToolHandler {

    private final RagService ragService;

    public RagAskTool(RagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "rag.ask",
                "基于指定知识库执行 RAG 问答",
                List.of(
                        new ToolParameter("knowledgeBaseId", "uuid", true, "知识库 ID"),
                        new ToolParameter("question", "string", true, "用户问题")
                )
        );
    }

    @Override
    public Object execute(ToolExecutionContext context) {
        UUID knowledgeBaseId = ToolArguments.requiredUuid(context.arguments(), "knowledgeBaseId");
        String question = ToolArguments.requiredString(context.arguments(), "question");
        return ragService.ask(new RagAskRequest(knowledgeBaseId, question));
    }
}
