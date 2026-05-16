package com.rain.ai.knowledge;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class KnowledgeBaseRepository {

    private final JdbcClient jdbcClient;

    public KnowledgeBaseRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public KnowledgeBase save(KnowledgeBase knowledgeBase) {
        jdbcClient.sql("""
                        INSERT INTO knowledge_base(id, workspace_id, name, description, created_at, updated_at)
                        VALUES (:id, :workspaceId, :name, :description, :createdAt, :updatedAt)
                        """)
                .param("id", knowledgeBase.id())
                .param("workspaceId", knowledgeBase.workspaceId())
                .param("name", knowledgeBase.name())
                .param("description", knowledgeBase.description())
                .param("createdAt", Timestamp.from(knowledgeBase.createdAt()))
                .param("updatedAt", Timestamp.from(knowledgeBase.updatedAt()))
                .update();
        return knowledgeBase;
    }

    public Optional<KnowledgeBase> findById(UUID id) {
        return jdbcClient.sql("""
                        SELECT id, workspace_id, name, description, created_at, updated_at
                        FROM knowledge_base
                        WHERE id = :id
                        """)
                .param("id", id)
                .query((rs, rowNum) -> new KnowledgeBase(
                        rs.getObject("id", UUID.class),
                        rs.getString("workspace_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()
                ))
                .optional();
    }

    public List<KnowledgeBase> findByWorkspaceId(String workspaceId) {
        return jdbcClient.sql("""
                        SELECT id, workspace_id, name, description, created_at, updated_at
                        FROM knowledge_base
                        WHERE workspace_id = :workspaceId
                        ORDER BY created_at DESC
                        """)
                .param("workspaceId", workspaceId)
                .query((rs, rowNum) -> new KnowledgeBase(
                        rs.getObject("id", UUID.class),
                        rs.getString("workspace_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()
                ))
                .list();
    }
}
