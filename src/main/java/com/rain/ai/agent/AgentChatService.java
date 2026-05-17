package com.rain.ai.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rain.ai.tool.ToolExecutionRequest;
import com.rain.ai.tool.ToolExecutionResponse;
import com.rain.ai.tool.ToolExecutionService;
import org.springframework.stereotype.Service;

@Service
public class AgentChatService {

    private final RuleBasedToolPlanner toolPlanner;
    private final ToolExecutionService toolExecutionService;
    private final ObjectMapper objectMapper;

    public AgentChatService(
            RuleBasedToolPlanner toolPlanner,
            ToolExecutionService toolExecutionService,
            ObjectMapper objectMapper
    ) {
        this.toolPlanner = toolPlanner;
        this.toolExecutionService = toolExecutionService;
        this.objectMapper = objectMapper;
    }

    public AgentChatResponse chat(AgentChatRequest request) {
        ToolPlan plan = toolPlanner.plan(request);
        ToolExecutionResponse toolExecution = toolExecutionService.execute(
                new ToolExecutionRequest(plan.toolName(), plan.arguments())
        );
        return new AgentChatResponse(
                request.message(),
                plan.toolName(),
                plan.arguments(),
                toolExecution,
                buildFinalAnswer(plan.toolName(), toolExecution)
        );
    }

    private String buildFinalAnswer(String toolName, ToolExecutionResponse toolExecution) {
        return switch (toolName) {
            case "knowledge_base.list" -> "已查询知识库列表，结果已由工具返回。";
            case "document.failed.list" -> "已查询处理失败的文档，结果已由工具返回。";
            case "rag.ask" -> "已基于知识库完成问答，结果已由工具返回。";
            default -> "工具执行完成：" + toJson(toolExecution.result());
        };
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return String.valueOf(value);
        }
    }
}
