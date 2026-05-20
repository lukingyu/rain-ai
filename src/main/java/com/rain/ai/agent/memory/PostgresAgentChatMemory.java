package com.rain.ai.agent.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
public class PostgresAgentChatMemory implements ChatMemory {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<AssistantMessage.ToolCall>> TOOL_CALL_LIST_TYPE =
            new TypeReference<>() {
            };
    private static final TypeReference<List<ToolResponseMessage.ToolResponse>> TOOL_RESPONSE_LIST_TYPE =
            new TypeReference<>() {
            };

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;
    private final int windowSize;

    public PostgresAgentChatMemory(
            JdbcClient jdbcClient,
            ObjectMapper objectMapper,
            @Value("${rain.ai.agent.memory.window-size:24}") int windowSize
    ) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
        this.windowSize = Math.max(windowSize, 2);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        validateConversationId(conversationId);
        for (Message message : messages) {
            saveMessage(conversationId, message);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        validateConversationId(conversationId);
        return jdbcClient.sql("""
                        SELECT message_type, content, metadata::text, tool_calls::text, tool_responses::text
                        FROM (
                            SELECT id, message_type, content, metadata, tool_calls, tool_responses
                            FROM agent_conversation_message
                            WHERE conversation_id = :conversationId
                            ORDER BY id DESC
                            LIMIT :limit
                        ) value
                        ORDER BY id ASC
                        """)
                .param("conversationId", conversationId)
                .param("limit", windowSize)
                .query((rs, rowNum) -> toMessage(
                        rs.getString("message_type"),
                        rs.getString("content"),
                        rs.getString("metadata"),
                        rs.getString("tool_calls"),
                        rs.getString("tool_responses")
                ))
                .list();
    }

    @Override
    public void clear(String conversationId) {
        validateConversationId(conversationId);
        jdbcClient.sql("""
                        DELETE FROM agent_conversation_message
                        WHERE conversation_id = :conversationId
                        """)
                .param("conversationId", conversationId)
                .update();
    }

    private void saveMessage(String conversationId, Message message) {
        jdbcClient.sql("""
                        INSERT INTO agent_conversation_message(
                            conversation_id, message_type, content, metadata, tool_calls, tool_responses
                        )
                        VALUES (
                            :conversationId, :messageType, :content, CAST(:metadata AS jsonb),
                            CAST(:toolCalls AS jsonb), CAST(:toolResponses AS jsonb)
                        )
                        """)
                .param("conversationId", conversationId)
                .param("messageType", message.getMessageType().name())
                .param("content", normalizeContent(message))
                .param("metadata", toJson(metadata(message)))
                .param("toolCalls", toJson(toolCalls(message)))
                .param("toolResponses", toJson(toolResponses(message)))
                .update();
    }

    private Message toMessage(
            String messageType,
            String content,
            String metadataJson,
            String toolCallsJson,
            String toolResponsesJson
    ) {
        MessageType type = MessageType.valueOf(messageType);
        String safeContent = content == null ? "" : content;
        return switch (type) {
            case USER -> new UserMessage(safeContent);
            case SYSTEM -> new SystemMessage(safeContent);
            case ASSISTANT -> AssistantMessage.builder()
                    .content(safeContent)
                    .properties(readJson(metadataJson, MAP_TYPE))
                    .toolCalls(readJson(toolCallsJson, TOOL_CALL_LIST_TYPE))
                    .build();
            case TOOL -> ToolResponseMessage.builder()
                    .metadata(readJson(metadataJson, MAP_TYPE))
                    .responses(readJson(toolResponsesJson, TOOL_RESPONSE_LIST_TYPE))
                    .build();
        };
    }

    private String normalizeContent(Message message) {
        String content = message.getText();
        return content == null ? "" : content;
    }

    private Map<String, Object> metadata(Message message) {
        if (message instanceof AbstractMessage abstractMessage) {
            return abstractMessage.getMetadata();
        }
        return Map.of();
    }

    private List<AssistantMessage.ToolCall> toolCalls(Message message) {
        if (message instanceof AssistantMessage assistantMessage) {
            return assistantMessage.getToolCalls();
        }
        return List.of();
    }

    private List<ToolResponseMessage.ToolResponse> toolResponses(Message message) {
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            return toolResponseMessage.getResponses();
        }
        return List.of();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.系统错误, "会话记忆序列化失败");
        }
    }

    private <T> T readJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.系统错误, "会话记忆反序列化失败");
        }
    }

    private void validateConversationId(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            throw new BizException(ErrorCode.参数错误, "conversationId 不能为空");
        }
    }
}
