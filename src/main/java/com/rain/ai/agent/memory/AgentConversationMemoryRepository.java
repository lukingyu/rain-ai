package com.rain.ai.agent.memory;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AgentConversationMemoryRepository {

    private final JdbcClient jdbcClient;

    public AgentConversationMemoryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<ConversationSummary> findSummary(String conversationId) {
        return jdbcClient.sql("""
                        SELECT conversation_id, summary, summarized_message_id
                        FROM agent_conversation_summary
                        WHERE conversation_id = :conversationId
                        """)
                .param("conversationId", conversationId)
                .query((rs, rowNum) -> new ConversationSummary(
                        rs.getString("conversation_id"),
                        rs.getString("summary"),
                        rs.getLong("summarized_message_id")
                ))
                .optional();
    }

    public List<ConversationMessageSnapshot> findMessagesAfter(String conversationId, long messageId, int limit) {
        return jdbcClient.sql("""
                        SELECT id, message_type, content
                        FROM agent_conversation_message
                        WHERE conversation_id = :conversationId
                          AND id > :messageId
                        ORDER BY id ASC
                        LIMIT :limit
                        """)
                .param("conversationId", conversationId)
                .param("messageId", messageId)
                .param("limit", limit)
                .query((rs, rowNum) -> new ConversationMessageSnapshot(
                        rs.getLong("id"),
                        rs.getString("message_type"),
                        rs.getString("content")
                ))
                .list();
    }

    public void saveSummary(ConversationSummary summary) {
        jdbcClient.sql("""
                        INSERT INTO agent_conversation_summary(conversation_id, summary, summarized_message_id)
                        VALUES (:conversationId, :summary, :summarizedMessageId)
                        ON CONFLICT (conversation_id)
                        DO UPDATE SET
                            summary = EXCLUDED.summary,
                            summarized_message_id = EXCLUDED.summarized_message_id
                        """)
                .param("conversationId", summary.conversationId())
                .param("summary", summary.summary())
                .param("summarizedMessageId", summary.summarizedMessageId())
                .update();
    }
}
