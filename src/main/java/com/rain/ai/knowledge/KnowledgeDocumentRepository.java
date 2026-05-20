package com.rain.ai.knowledge;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

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
                            id, knowledge_base_id, original_filename, storage_path, status, error_message
                        )
                        VALUES (
                            :id, :knowledgeBaseId, :originalFilename, :storagePath, :status, :errorMessage
                        )
                        """)
                .param("id", document.id())
                .param("knowledgeBaseId", document.knowledgeBaseId())
                .param("originalFilename", document.originalFilename())
                .param("storagePath", document.storagePath())
                .param("status", document.status().name())
                .param("errorMessage", document.errorMessage())
                .update();
        return document;
    }

    public Optional<KnowledgeDocument> findById(UUID id) {
        return jdbcClient.sql("""
                        SELECT id, knowledge_base_id, original_filename, storage_path, status, error_message
                        FROM knowledge_document
                        WHERE id = :id
                        """)
                .param("id", id)
                .query(this::mapDocument)
                .optional();
    }

    public List<KnowledgeDocument> findByKnowledgeBaseId(UUID knowledgeBaseId) {
        return jdbcClient.sql("""
                        SELECT id, knowledge_base_id, original_filename, storage_path, status, error_message
                        FROM knowledge_document
                        WHERE knowledge_base_id = :knowledgeBaseId
                        ORDER BY original_filename ASC
                        """)
                .param("knowledgeBaseId", knowledgeBaseId)
                .query(this::mapDocument)
                .list();
    }

    public List<KnowledgeDocument> findByKnowledgeBaseIdAndStatus(UUID knowledgeBaseId, DocumentStatus status) {
        return jdbcClient.sql("""
                        SELECT id, knowledge_base_id, original_filename, storage_path, status, error_message
                        FROM knowledge_document
                        WHERE knowledge_base_id = :knowledgeBaseId
                          AND status = :status
                        ORDER BY original_filename ASC
                        """)
                .param("knowledgeBaseId", knowledgeBaseId)
                .param("status", status.name())
                .query(this::mapDocument)
                .list();
    }

    public void updateStatus(UUID id, DocumentStatus status, String errorMessage) {
        jdbcClient.sql("""
                        UPDATE knowledge_document
                        SET status = :status, error_message = :errorMessage
                        WHERE id = :id
                        """)
                .param("id", id)
                .param("status", status.name())
                .param("errorMessage", errorMessage)
                .update();
    }

    private KnowledgeDocument mapDocument(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new KnowledgeDocument(
                rs.getObject("id", UUID.class),
                rs.getObject("knowledge_base_id", UUID.class),
                rs.getString("original_filename"),
                rs.getString("storage_path"),
                DocumentStatus.valueOf(rs.getString("status")),
                rs.getString("error_message")
        );
    }
}
