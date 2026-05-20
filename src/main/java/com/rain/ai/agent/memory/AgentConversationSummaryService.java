package com.rain.ai.agent.memory;

import com.rain.ai.runtime.AiRuntimeStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AgentConversationSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AgentConversationSummaryService.class);

    private final AgentConversationMemoryRepository memoryRepository;
    private final ObjectProvider<ChatModel> chatModelProvider;
    private final AiRuntimeStatusService aiRuntimeStatusService;
    private final int triggerMessageCount;
    private final int batchSize;

    public AgentConversationSummaryService(
            AgentConversationMemoryRepository memoryRepository,
            ObjectProvider<ChatModel> chatModelProvider,
            AiRuntimeStatusService aiRuntimeStatusService,
            @Value("${rain.ai.agent.memory.summary-trigger-message-count:12}") int triggerMessageCount,
            @Value("${rain.ai.agent.memory.summary-batch-size:40}") int batchSize
    ) {
        this.memoryRepository = memoryRepository;
        this.chatModelProvider = chatModelProvider;
        this.aiRuntimeStatusService = aiRuntimeStatusService;
        this.triggerMessageCount = Math.max(triggerMessageCount, 2);
        this.batchSize = Math.max(batchSize, this.triggerMessageCount);
    }

    public Optional<ConversationSummary> findSummary(String conversationId) {
        return memoryRepository.findSummary(conversationId);
    }

    public void refreshSummaryIfNecessary(String conversationId) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null || !aiRuntimeStatusService.chatAvailable()) {
            return;
        }

        try {
            ConversationSummary currentSummary = memoryRepository.findSummary(conversationId)
                    .orElse(new ConversationSummary(conversationId, "暂无长期记忆。", 0));
            List<ConversationMessageSnapshot> messages = memoryRepository.findMessagesAfter(
                    conversationId,
                    currentSummary.summarizedMessageId(),
                    batchSize
            );
            if (messages.size() < triggerMessageCount) {
                return;
            }

            ConversationSummaryDraft draft = ChatClient.builder(chatModel)
                    .build()
                    .prompt()
                    .system("""
                            你是 Rain AI 的长期记忆压缩器。
                            你的任务是把已有摘要和新增对话压缩成新的长期记忆。
                            只保留对后续对话有价值的信息：用户目标、偏好、已确认事实、待办问题、技术决策。
                            删除寒暄、重复内容、一次性表达和无稳定价值的信息。
                            必须输出结构化 JSON，不要输出 Markdown。
                            """)
                    .user("""
                            已有长期记忆：
                            %s

                            新增对话：
                            %s

                            请生成新的长期记忆。
                            """.formatted(currentSummary.summary(), formatMessages(messages)))
                    .call()
                    .entity(ConversationSummaryDraft.class);

            long lastMessageId = messages.getLast().id();
            memoryRepository.saveSummary(new ConversationSummary(
                    conversationId,
                    formatSummary(draft),
                    lastMessageId
            ));
        } catch (Exception exception) {
            log.warn("刷新 Agent 长期摘要记忆失败，conversationId={}", conversationId, exception);
        }
    }

    private String formatMessages(List<ConversationMessageSnapshot> messages) {
        StringBuilder builder = new StringBuilder();
        for (ConversationMessageSnapshot message : messages) {
            builder.append("[")
                    .append(message.id())
                    .append(" ")
                    .append(message.messageType())
                    .append("] ")
                    .append(message.content() == null ? "" : message.content())
                    .append("\n");
        }
        return builder.toString();
    }

    private String formatSummary(ConversationSummaryDraft draft) {
        if (draft == null) {
            return "暂无";
        }
        return """
                摘要：%s
                稳定事实：%s
                用户偏好：%s
                待确认问题：%s
                """.formatted(
                safeText(draft.summary()),
                safeList(draft.facts()),
                safeList(draft.preferences()),
                safeList(draft.openQuestions())
        ).trim();
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "暂无";
        }
        return value.trim();
    }

    private String safeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "无";
        }
        return String.join("；", values);
    }
}
