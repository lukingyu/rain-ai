package com.rain.ai.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rain.ai.skill.SkillExecutionRequest;
import com.rain.ai.skill.SkillExecutionResponse;
import com.rain.ai.skill.SkillExecutionService;
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
    private final SkillExecutionService skillExecutionService;
    private final AgentSessionMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public AgentChatService(
            RuleBasedToolPlanner toolPlanner,
            AiFunctionCallingAgent aiFunctionCallingAgent,
            ToolExecutionService toolExecutionService,
            SkillExecutionService skillExecutionService,
            AgentSessionMemoryService memoryService,
            ObjectMapper objectMapper
    ) {
        this.toolPlanner = toolPlanner;
        this.aiFunctionCallingAgent = aiFunctionCallingAgent;
        this.toolExecutionService = toolExecutionService;
        this.skillExecutionService = skillExecutionService;
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
                    AgentPlanType.TOOL.name(),
                    firstExecution == null ? null : firstExecution.toolName(),
                    null,
                    Map.of(),
                    firstExecution,
                    null,
                    aiResult.finalAnswer()
            );
        }

        AgentPlan plan = toolPlanner.plan(request);
        if (plan.type() == AgentPlanType.SKILL) {
            SkillExecutionResponse skillExecution = skillExecutionService.execute(
                    new SkillExecutionRequest(plan.name(), plan.arguments())
            );
            String finalAnswer = buildSkillFinalAnswer(plan.name(), skillExecution);
            memoryService.appendAssistantMessage(sessionId, finalAnswer);
            return new AgentChatResponse(
                    sessionId,
                    request.message(),
                    "RULE_BASED_FALLBACK",
                    plan.type().name(),
                    null,
                    plan.name(),
                    plan.arguments(),
                    null,
                    skillExecution,
                    finalAnswer
            );
        }

        ToolExecutionResponse toolExecution = toolExecutionService.execute(
                new ToolExecutionRequest(plan.name(), plan.arguments())
        );
        String finalAnswer = buildToolFinalAnswer(plan.name(), toolExecution);
        memoryService.appendAssistantMessage(sessionId, finalAnswer);
        return new AgentChatResponse(
                sessionId,
                request.message(),
                "RULE_BASED_FALLBACK",
                plan.type().name(),
                plan.name(),
                null,
                plan.arguments(),
                toolExecution,
                null,
                finalAnswer
        );
    }

    private String buildToolFinalAnswer(String toolName, ToolExecutionResponse toolExecution) {
        return switch (toolName) {
            case "knowledge_base.list" -> "已查询知识库列表，结果已由工具返回。";
            case "document.failed.list" -> "已查询处理失败的文档，结果已由工具返回。";
            case "rag.ask" -> "已基于知识库完成问答，结果已由工具返回。";
            default -> "工具执行完成：" + toJson(toolExecution.result());
        };
    }

    private String buildSkillFinalAnswer(String skillName, SkillExecutionResponse skillExecution) {
        return switch (skillName) {
            case "knowledge_base_operator" -> "已完成知识库运营诊断，结果包含失败文档检查和 RAG 诊断建议。";
            default -> "技能执行完成：" + toJson(skillExecution);
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
