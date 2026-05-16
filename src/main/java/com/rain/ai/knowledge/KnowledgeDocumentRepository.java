package com.rain.ai.knowledge;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class KnowledgeDocumentRepository {

    private final JdbcClient jdbcClient;

    public KnowledgeDocumentRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public KnowledgeDocument save(KnowledgeDocument document) {
        jdbcClient.sql("""
                        INSERT INTO knowledge_document(
                            id, workspace_id, knowledge_base_id, original_filename, content_type,
                            size_bytes, storage_path, status, error_message, created_at, updated_at
                        )
                        VALUES (
                            :id, :workspaceId, :knowledgeBaseId, :originalFilename, :contentType,
                            :sizeBytes, :storagePath, :status, :errorMessage, :createdAt, :updatedAt
                        )
                        """)
                .param("id", document.id())
                .param("workspaceId", document.workspaceId())
                .param("knowledgeBaseId", document.knowledgeBaseId())
                .param("originalFilename", document.originalFilename())
                .param("contentType", document.contentType())
                .param("sizeBytes", document.sizeBytes())
                .param("storagePath", document.storagePath())
                .param("status", document.status().name())
                .param("errorMessage", document.errorMessage())
                .param("createdAt", Timestamp.from(document.createdAt()))
                .param("updatedAt", Timestamp.from(document.updatedAt()))
                .update();
        return document;
    }

    public Optional<KnowledgeDocument> findById(UUID id) {
        return jdbcClient.sql("""
                        SELECT id, workspace_id, knowledge_base_id, original_filename, content_type,
                               size_bytes, storage_path, status, error_message, created_at, updated_at
                        FROM knowledge_document
                        WHERE id = :id
                        """)
                .param("id", id)
                .query(this::mapDocument)
                .optional();
    }

    public List<KnowledgeDocument> findByKnowledgeBaseId(UUID knowledgeBaseId) {
        return jdbcClient.sql("""
                        SELECT id, workspace_id, knowledge_base_id, original_filename, content_type,
                               size_bytes, storage_path, status, error_message, created_at, updated_at
                        FROM knowledge_document
                        WHERE knowledge_base_id = :knowledgeBaseId
                        ORDER BY created_at DESC
                        """)
                .param("knowledgeBaseId", knowledgeBaseId)
                .query(this::mapDocument)
                .list();
    }

    private KnowledgeDocument mapDocument(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new KnowledgeDocument(
                rs.getObject("id", UUID.class),
                rs.getString("workspace_id"),
                rs.getObject("knowledge_base_id", UUID.class),
                rs.getString("original_filename"),
                rs.getString("content_type"),
                rs.getLong("size_bytes"),
                rs.getString("storage_path"),
                DocumentStatus.valueOf(rs.getString("status")),
                rs.getString("error_message"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }
}
