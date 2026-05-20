package com.rain.ai.tool;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SpringAiToolCatalog {

    private final ToolCallback[] toolCallbacks;

    public SpringAiToolCatalog(KnowledgeBaseAiTools knowledgeBaseAiTools) {
        this.toolCallbacks = ToolCallbacks.from(knowledgeBaseAiTools);
    }

    public ToolCallback[] callbacks() {
        return toolCallbacks;
    }

    public List<AiToolDefinition> definitions() {
        return Arrays.stream(toolCallbacks)
                .map(ToolCallback::getToolDefinition)
                .map(definition -> new AiToolDefinition(
                        definition.name(),
                        definition.description(),
                        definition.inputSchema()
                ))
                .toList();
    }
}
