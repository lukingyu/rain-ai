package com.rain.ai.knowledge;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class DocumentIngestionOutboxRepository {

    private final JdbcClient jdbcClient;

    public DocumentIngestionOutboxRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void save(UUID documentId, UUID knowledgeBaseId, String storagePath) {
        jdbcClient.sql("""
                        INSERT INTO document_ingestion_outbox(
                            document_id, knowledge_base_id, storage_path, status
                        )
                        VALUES (:documentId, :knowledgeBaseId, :storagePath, :status)
                        """)
                .param("documentId", documentId)
                .param("knowledgeBaseId", knowledgeBaseId)
                .param("storagePath", storagePath)
                .param("status", DocumentIngestionOutboxStatus.PENDING.name())
                .update();
    }

    public List<DocumentIngestionOutbox> claimPending(int batchSize, int maxRetryCount) {
        return jdbcClient.sql("""
                        UPDATE document_ingestion_outbox
                        SET status = :sending
                        WHERE id IN (
                            SELECT id
                            FROM document_ingestion_outbox
                            WHERE status IN (:pending, :sendingCandidate, :failed)
                              AND retry_count < :maxRetryCount
                            ORDER BY id ASC
                            LIMIT :batchSize
                            FOR UPDATE SKIP LOCKED
                        )
                        RETURNING id, document_id, knowledge_base_id, storage_path,
                                  status, retry_count, error_message
                        """)
                .param("sending", DocumentIngestionOutboxStatus.SENDING.name())
                .param("pending", DocumentIngestionOutboxStatus.PENDING.name())
                .param("sendingCandidate", DocumentIngestionOutboxStatus.SENDING.name())
                .param("failed", DocumentIngestionOutboxStatus.FAILED.name())
                .param("maxRetryCount", maxRetryCount)
                .param("batchSize", batchSize)
                .query(this::mapOutbox)
                .list();
    }

    public void markSent(long id) {
        jdbcClient.sql("""
                        UPDATE document_ingestion_outbox
                        SET status = :status,
                            error_message = NULL
                        WHERE id = :id
                        """)
                .param("id", id)
                .param("status", DocumentIngestionOutboxStatus.SENT.name())
                .update();
    }

    public void markFailed(long id, String errorMessage) {
        jdbcClient.sql("""
                        UPDATE document_ingestion_outbox
                        SET status = :status,
                            retry_count = retry_count + 1,
                            error_message = :errorMessage
                        WHERE id = :id
                        """)
                .param("id", id)
                .param("status", DocumentIngestionOutboxStatus.FAILED.name())
                .param("errorMessage", errorMessage)
                .update();
    }

    private DocumentIngestionOutbox mapOutbox(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new DocumentIngestionOutbox(
                rs.getLong("id"),
                rs.getObject("document_id", UUID.class),
                rs.getObject("knowledge_base_id", UUID.class),
                rs.getString("storage_path"),
                DocumentIngestionOutboxStatus.valueOf(rs.getString("status")),
                rs.getInt("retry_count"),
                rs.getString("error_message")
        );
    }
}
