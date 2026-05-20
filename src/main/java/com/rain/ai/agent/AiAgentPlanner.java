package com.rain.ai.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rain.ai.runtime.AiRuntimeStatusService;
import com.rain.ai.skill.SkillRegistry;
import com.rain.ai.tool.ToolRegistry;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class AiAgentPlanner {

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ToolRegistry toolRegistry;
    private final SkillRegistry skillRegistry;
    private final AgentPlanJsonParser planJsonParser;
    private final ObjectMapper objectMapper;
    private final AiRuntimeStatusService aiRuntimeStatusService;

    public AiAgentPlanner(
            ObjectProvider<ChatModel> chatModelProvider,
            ToolRegistry toolRegistry,
            SkillRegistry skillRegistry,
            AgentPlanJsonParser planJsonParser,
            ObjectMapper objectMapper,
            AiRuntimeStatusService aiRuntimeStatusService
    ) {
        this.chatModelProvider = chatModelProvider;
        this.toolRegistry = toolRegistry;
        this.skillRegistry = skillRegistry;
        this.planJsonParser = planJsonParser;
        this.objectMapper = objectMapper;
        this.aiRuntimeStatusService = aiRuntimeStatusService;
    }

    public boolean available() {
        return aiRuntimeStatusService.chatAvailable();
    }

    public Optional<AgentPlan> plan(AgentChatRequest request) {
        if (!available()) {
            return Optional.empty();
        }
        try {
            ChatResponse response = chatModelProvider.getObject().call(new Prompt(buildMessages(request)));
            AgentPlan plan = planJsonParser.parse(response.getResult().getOutput().getText());
            validateCapability(plan);
            return Optional.of(plan);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private List<Message> buildMessages(AgentChatRequest request) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage("""
                你是 Rain AI 的 Agent Planner，只负责选择下一步要执行的能力，不直接回答用户问题。
                你会看到系统当前可用的 tools 和 skills：
                - tool 是原子能力，适合单步查询或单步操作。
                - skill 是多步业务流程，适合诊断、运营、检查、分析这类复合任务。
                你必须只返回一个 JSON 对象，不要返回 Markdown，不要解释。
                JSON 格式固定为：
                {"type":"TOOL 或 SKILL","name":"能力名称","arguments":{}}
                如果用户要做知识库运营诊断、健康检查、风险检查或建议生成，优先选择 skill。
                """));
        messages.add(new UserMessage(buildPlanningInput(request)));
        return messages;
    }

    private String buildPlanningInput(AgentChatRequest request) {
        return """
                当前 knowledgeBaseId：%s
                可用 tools：
                %s
                可用 skills：
                %s
                当前用户消息：
                %s
                """.formatted(
                request.knowledgeBaseId() == null ? "未指定" : request.knowledgeBaseId(),
                toJson(toolRegistry.listDefinitions()),
                toJson(skillRegistry.listDefinitions()),
                request.message()
        );
    }

    private void validateCapability(AgentPlan plan) {
        if (plan.type() == AgentPlanType.TOOL) {
            toolRegistry.getRequired(plan.name());
            return;
        }
        skillRegistry.getRequired(plan.name());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return String.valueOf(value);
        }
    }

}
