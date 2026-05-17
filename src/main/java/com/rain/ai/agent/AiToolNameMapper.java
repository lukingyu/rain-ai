package com.rain.ai.agent;

import com.rain.ai.tool.ToolDefinition;
import org.springframework.stereotype.Component;

@Component
public class AiToolNameMapper {

    public String toAiToolName(ToolDefinition definition) {
        return definition.name().replace('.', '_');
    }
}
