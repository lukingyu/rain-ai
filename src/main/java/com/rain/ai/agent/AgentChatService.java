package com.rain.ai.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rain.ai.tool.ToolExecutionRequest;
import com.rain.ai.tool.ToolExecutionResponse;
import com.rain.ai.tool.ToolExecutionService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AgentChatService {

    private final RuleBasedToolPlanner toolPlanner;
    private final AiFunctionCallingAgent aiFunctionCallingAgent;
    private final ToolExecutionService toolExecutionService;
    private final AgentSessionMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public AgentChatService(
            RuleBasedToolPlanner toolPlanner,
            AiFunctionCallingAgent aiFunctionCallingAgent,
            ToolExecutionService toolExecutionService,
            AgentSessionMemoryService memoryService,
            ObjectMapper objectMapper
    ) {
        this.toolPlanner = toolPlanner;
        this.aiFunctionCallingAgent = aiFunctionCallingAgent;
        this.toolExecutionService = toolExecutionService;
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    public AgentChatResponse chat(AgentChatRequest request) {
        String sessionId = memoryService.resolveSessionId(request.sessionId());
        var history = memoryService.recentMessages(sessionId);
        memoryService.appendUserMessage(sessionId, request.message());

        if (aiFunctionCallingAgent.available()) {
            AiFunctionCallingResult aiResult = aiFunctionCallingAgent.chat(request, history);
            ToolExecutionResponse firstExecution = aiResult.toolExecutions().isEmpty()
                    ? null
                    : aiResult.toolExecutions().getFirst();
            memoryService.appendAssistantMessage(sessionId, aiResult.finalAnswer());
            return new AgentChatResponse(
                    sessionId,
                    request.message(),
                    "SPRING_AI_FUNCTION_CALLING",
                    firstExecution == null ? null : firstExecution.toolName(),
                    Map.of(),
                    firstExecution,
                    aiResult.finalAnswer()
            );
        }

        ToolPlan plan = toolPlanner.plan(request);
        ToolExecutionResponse toolExecution = toolExecutionService.execute(
                new ToolExecutionRequest(plan.toolName(), plan.arguments())
        );
        String finalAnswer = buildFinalAnswer(plan.toolName(), toolExecution);
        memoryService.appendAssistantMessage(sessionId, finalAnswer);
        return new AgentChatResponse(
                sessionId,
                request.message(),
                "RULE_BASED_FALLBACK",
                plan.toolName(),
                plan.arguments(),
                toolExecution,
                finalAnswer
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
