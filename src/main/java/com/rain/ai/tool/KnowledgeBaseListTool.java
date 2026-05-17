package com.rain.ai.tool;

import com.rain.ai.knowledge.KnowledgeBaseService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeBaseListTool implements ToolHandler {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseListTool(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "knowledge_base.list",
                "查询当前工作区下的知识库列表",
                List.of()
        );
    }

    @Override
    public Object execute(ToolExecutionContext context) {
        return knowledgeBaseService.list();
    }
}
