package com.rain.ai.tool;

public interface ToolHandler {

    ToolDefinition definition();

    Object execute(ToolExecutionContext context);
}
