package com.rain.ai.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rain.ai.rag.RagAnswerResponse;
import com.rain.ai.skill.SkillExecutionResponse;
import com.rain.ai.skill.SkillStepResult;
import com.rain.ai.tool.ToolExecutionResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class AiAgentResponder {

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ObjectMapper objectMapper;
    private final String chatApiKey;

    public AiAgentResponder(
            ObjectProvider<ChatModel> chatModelProvider,
            ObjectMapper objectMapper,
            @Value("${spring.ai.openai.chat.api-key:}") String chatApiKey
    ) {
        this.chatModelProvider = chatModelProvider;
        this.objectMapper = objectMapper;
        this.chatApiKey = chatApiKey;
    }

    public String answer(AgentChatRequest request, AgentPlan plan, Object executionResult) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel != null && hasRealApiKey()) {
            ChatResponse response = chatModel.call(new Prompt(List.of(
                    new SystemMessage("""
                            你是 Rain AI 的企业知识库助手。
                            你不能编造工具没有返回的信息，只能基于执行结果回答。
                            回答要直接面向用户，优先给结论，再给关键依据。
                            如果执行结果里有 citations、steps 或 result，需要把它们整理成用户能读懂的中文。
                            """),
                    new UserMessage("""
                            用户问题：
                            %s

                            Agent 执行计划：
                            %s

                            执行结果：
                            %s
                            """.formatted(request.message(), toJson(plan), toJson(executionResult)))
            )));
            return response.getResult().getOutput().getText();
        }
        return localAnswer(plan, executionResult);
    }

    private String localAnswer(AgentPlan plan, Object executionResult) {
        return switch (plan.type()) {
            case TOOL -> localToolAnswer(executionResult);
            case SKILL -> localSkillAnswer(executionResult);
        };
    }

    private String localToolAnswer(Object executionResult) {
        if (executionResult instanceof ToolExecutionResponse response) {
            Object result = response.result();
            if (result instanceof RagAnswerResponse ragAnswer) {
                return ragAnswer.answer();
            }
            if (result instanceof Collection<?> collection) {
                return "工具执行完成，返回记录数：" + collection.size() + "。";
            }
            return "工具执行完成，结果：" + toJson(result);
        }
        return "工具执行完成，结果：" + toJson(executionResult);
    }

    private String localSkillAnswer(Object executionResult) {
        if (!(executionResult instanceof SkillExecutionResponse response)) {
            return "技能执行完成，结果：" + toJson(executionResult);
        }
        StringBuilder answer = new StringBuilder(response.summary()).append("\n");
        for (SkillStepResult step : response.steps()) {
            answer.append("- ")
                    .append(step.stepName())
                    .append("：")
                    .append(step.status());
            if (step.result() instanceof Collection<?> collection) {
                answer.append("，返回记录数 ").append(collection.size());
            }
            if (step.result() instanceof RagAnswerResponse ragAnswer) {
                answer.append("\n").append(ragAnswer.answer());
            }
            answer.append("\n");
        }
        return answer.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return String.valueOf(value);
        }
    }

    private boolean hasRealApiKey() {
        return chatApiKey != null
                && !chatApiKey.isBlank()
                && !chatApiKey.equals("replace-with-your-api-key")
                && !chatApiKey.equals("test-key");
    }
}
