package com.rain.ai.agent;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import com.rain.ai.knowledge.KnowledgeBaseRepository;
import com.rain.ai.rag.RagAdvisorFactory;
import com.rain.ai.rag.RagCitation;
import com.rain.ai.rag.RagCitationMapper;
import com.rain.ai.runtime.AiRuntimeStatusService;
import com.rain.ai.tool.SpringAiToolCatalog;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AgentChatService {

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final AiRuntimeStatusService aiRuntimeStatusService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final RagAdvisorFactory ragAdvisorFactory;
    private final RagCitationMapper citationMapper;
    private final SpringAiToolCatalog toolCatalog;
    private final MessageChatMemoryAdvisor memoryAdvisor;

    public AgentChatService(
            ObjectProvider<ChatModel> chatModelProvider,
            AiRuntimeStatusService aiRuntimeStatusService,
            KnowledgeBaseRepository knowledgeBaseRepository,
            RagAdvisorFactory ragAdvisorFactory,
            RagCitationMapper citationMapper,
            SpringAiToolCatalog toolCatalog,
            MessageChatMemoryAdvisor memoryAdvisor
    ) {
        this.chatModelProvider = chatModelProvider;
        this.aiRuntimeStatusService = aiRuntimeStatusService;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.ragAdvisorFactory = ragAdvisorFactory;
        this.citationMapper = citationMapper;
        this.toolCatalog = toolCatalog;
        this.memoryAdvisor = memoryAdvisor;
    }

    public AgentChatResponse chat(AgentChatRequest request) {
        String sessionId = resolveSessionId(request.sessionId());
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null || !aiRuntimeStatusService.chatAvailable()) {
            throw new BizException(ErrorCode.系统错误, "聊天模型未配置，无法执行 Agent 对话");
        }

        ChatClient.ChatClientRequestSpec prompt = ChatClient.builder(chatModel)
                .build()
                .prompt()
                .system(systemPrompt(request.knowledgeBaseId()))
                .user(request.message())
                .advisors(advisor -> advisor
                        .advisors(memoryAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, sessionId))
                .toolCallbacks(toolCatalog.callbacks());

        if (request.knowledgeBaseId() != null) {
            knowledgeBaseRepository.findById(request.knowledgeBaseId())
                    .orElseThrow(() -> new BizException(ErrorCode.资源不存在, "知识库不存在"));
            prompt = prompt.advisors(ragAdvisorFactory.forKnowledgeBase(request.knowledgeBaseId()));
        }

        ChatClientResponse response = prompt.call().chatClientResponse();
        String answer = response.chatResponse().getResult().getOutput().getText();
        List<RagCitation> citations = citationMapper.from(response);
        return new AgentChatResponse(
                sessionId,
                request.message(),
                "SPRING_AI_TOOL_CALLING",
                request.knowledgeBaseId(),
                answer,
                citations
        );
    }

    private String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return sessionId;
    }

    private String systemPrompt(UUID knowledgeBaseId) {
        String currentKnowledgeBase = knowledgeBaseId == null ? "未指定" : knowledgeBaseId.toString();
        return """
                你是 Rain AI 企业知识库 Agent。
                当前 knowledgeBaseId：%s

                你可以自主调用系统提供的 Spring AI 工具。
                系统会通过 Spring AI MessageChatMemoryAdvisor 注入同一 sessionId 下的历史对话。
                如果用户要查看知识库、失败文档或检索知识库内容，应优先调用工具。
                如果当前请求已经指定 knowledgeBaseId，系统会同时通过 RAG Advisor 注入知识库召回资料。
                回答必须基于工具结果或召回资料，不要编造系统中不存在的信息。
                """.formatted(currentKnowledgeBase);
    }
}
