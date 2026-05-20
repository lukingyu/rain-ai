package com.rain.ai.rag;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import com.rain.ai.knowledge.KnowledgeBaseRepository;
import com.rain.ai.runtime.AiRuntimeStatusService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ObjectProvider<ChatModel> chatModelProvider;
    private final AiRuntimeStatusService aiRuntimeStatusService;
    private final RagAdvisorFactory ragAdvisorFactory;
    private final RagCitationMapper citationMapper;

    public RagService(
            KnowledgeBaseRepository knowledgeBaseRepository,
            ObjectProvider<ChatModel> chatModelProvider,
            AiRuntimeStatusService aiRuntimeStatusService,
            RagAdvisorFactory ragAdvisorFactory,
            RagCitationMapper citationMapper
    ) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.chatModelProvider = chatModelProvider;
        this.aiRuntimeStatusService = aiRuntimeStatusService;
        this.ragAdvisorFactory = ragAdvisorFactory;
        this.citationMapper = citationMapper;
    }

    public RagAnswerResponse ask(RagAskRequest request) {
        knowledgeBaseRepository.findById(request.knowledgeBaseId())
                .orElseThrow(() -> new BizException(ErrorCode.资源不存在, "知识库不存在"));
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null || !aiRuntimeStatusService.chatAvailable()) {
            throw new BizException(ErrorCode.系统错误, "聊天模型未配置，无法执行 RAG 问答");
        }

        ChatClientResponse response = ChatClient.builder(chatModel)
                .build()
                .prompt()
                .system("""
                        你是企业知识库问答助手。
                        你必须基于知识库召回资料回答，不允许编造资料外的信息。
                        """)
                .user(request.question())
                .advisors(ragAdvisorFactory.forKnowledgeBase(request.knowledgeBaseId()))
                .call()
                .chatClientResponse();

        List<RagCitation> citations = citationMapper.from(response);
        String answer = response.chatResponse().getResult().getOutput().getText();

        return new RagAnswerResponse(
                request.knowledgeBaseId(),
                request.question(),
                answer,
                citations,
                true,
                "Spring AI QuestionAnswerAdvisor，进入上下文分片数：" + citations.size()
        );
    }
}
