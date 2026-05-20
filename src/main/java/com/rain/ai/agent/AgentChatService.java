package com.rain.ai.agent;

import com.rain.ai.skill.SkillExecutionRequest;
import com.rain.ai.skill.SkillExecutionResponse;
import com.rain.ai.skill.SkillExecutionService;
import com.rain.ai.tool.ToolExecutionRequest;
import com.rain.ai.tool.ToolExecutionResponse;
import com.rain.ai.tool.ToolExecutionService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AgentChatService {

    private final RuleBasedToolPlanner toolPlanner;
    private final AiAgentPlanner aiAgentPlanner;
    private final AiAgentResponder aiAgentResponder;
    private final ToolExecutionService toolExecutionService;
    private final SkillExecutionService skillExecutionService;

    public AgentChatService(
            RuleBasedToolPlanner toolPlanner,
            AiAgentPlanner aiAgentPlanner,
            AiAgentResponder aiAgentResponder,
            ToolExecutionService toolExecutionService,
            SkillExecutionService skillExecutionService
    ) {
        this.toolPlanner = toolPlanner;
        this.aiAgentPlanner = aiAgentPlanner;
        this.aiAgentResponder = aiAgentResponder;
        this.toolExecutionService = toolExecutionService;
        this.skillExecutionService = skillExecutionService;
    }

    public AgentChatResponse chat(AgentChatRequest request) {
        String sessionId = resolveSessionId(request.sessionId());
        Optional<AgentPlan> aiPlan = aiAgentPlanner.plan(request);
        String plannerType = aiPlan.isPresent() ? "SPRING_AI_LLM_PLANNER" : "RULE_BASED_FALLBACK";
        AgentPlan plan = aiPlan.orElseGet(() -> toolPlanner.plan(request));

        return switch (plan.type()) {
            case TOOL -> executeToolPlan(sessionId, request, plannerType, plan);
            case SKILL -> executeSkillPlan(sessionId, request, plannerType, plan);
        };
    }

    private String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return sessionId;
    }

    private AgentChatResponse executeToolPlan(
            String sessionId,
            AgentChatRequest request,
            String plannerType,
            AgentPlan plan
    ) {
        ToolExecutionResponse toolExecution = toolExecutionService.execute(
                new ToolExecutionRequest(plan.name(), plan.arguments())
        );
        String finalAnswer = aiAgentResponder.answer(request, plan, toolExecution);
        return new AgentChatResponse(
                sessionId,
                request.message(),
                plannerType,
                plan.type().name(),
                plan.name(),
                null,
                plan.arguments(),
                toolExecution,
                null,
                finalAnswer
        );
    }

    private AgentChatResponse executeSkillPlan(
            String sessionId,
            AgentChatRequest request,
            String plannerType,
            AgentPlan plan
    ) {
        SkillExecutionResponse skillExecution = skillExecutionService.execute(
                new SkillExecutionRequest(plan.name(), plan.arguments())
        );
        String finalAnswer = aiAgentResponder.answer(request, plan, skillExecution);
        return new AgentChatResponse(
                sessionId,
                request.message(),
                plannerType,
                plan.type().name(),
                null,
                plan.name(),
                plan.arguments(),
                null,
                skillExecution,
                finalAnswer
        );
    }
}
