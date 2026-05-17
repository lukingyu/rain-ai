package com.rain.ai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rain.ai.tool.ToolExecutionResponse;
import com.rain.ai.tool.ToolExecutionService;
import com.rain.ai.tool.ToolRegistry;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AiFunctionCallingAgent {

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ToolRegistry toolRegistry;
    private final SpringAiToolCallbackFactory callbackFactory;
    private final String chatApiKey;

    public AiFunctionCallingAgent(
            ObjectProvider<ChatModel> chatModelProvider,
            ToolRegistry toolRegistry,
            ToolExecutionService toolExecutionService,
            ObjectMapper objectMapper,
            AiToolNameMapper toolNameMapper,
            @Value("${spring.ai.openai.chat.api-key:}") String chatApiKey
    ) {
        this.chatModelProvider = chatModelProvider;
        this.toolRegistry = toolRegistry;
        this.callbackFactory = new SpringAiToolCallbackFactory(toolExecutionService, objectMapper, toolNameMapper);
        this.chatApiKey = chatApiKey;
    }

    public boolean available() {
        return chatModelProvider.getIfAvailable() != null && hasRealApiKey();
    }

    public AiFunctionCallingResult chat(AgentChatRequest request, List<AgentMemoryMessage> history) {
        if (!available()) {
            return null;
        }
        List<ToolExecutionResponse> executions = new ArrayList<>();
        List<ToolCallback> callbacks = toolRegistry.listHandlers()
                .stream()
                .map(handler -> callbackFactory.create(handler, executions::add))
                .toList();

        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(callbacks)
                .internalToolExecutionEnabled(true)
                .temperature(0.1)
                .build();

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage("""
                        你是 Rain AI 的企业知识库智能助手。
                        你必须优先使用可用工具获得真实结果，不要凭空编造。
                        如果用户要查询知识库、失败文档或基于知识库问答，必须调用最合适的工具。
                        工具执行完成后，用简洁中文总结结果。
                        """));
        for (AgentMemoryMessage memoryMessage : history) {
            if ("user".equals(memoryMessage.role())) {
                messages.add(new UserMessage(memoryMessage.content()));
            }
        }
        messages.add(new UserMessage(request.message()));

        ChatResponse response = chatModelProvider.getObject().call(new Prompt(messages, options));

        String answer = response.getResult().getOutput().getText();
        return new AiFunctionCallingResult(answer, executions);
    }

    private boolean hasRealApiKey() {
        return chatApiKey != null
                && !chatApiKey.isBlank()
                && !chatApiKey.equals("replace-with-your-api-key")
                && !chatApiKey.equals("test-key");
    }
}
