package com.rain.ai.knowledge;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class DocumentChunkRepository {

    private final JdbcClient jdbcClient;

    public DocumentChunkRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void deleteByDocumentId(UUID documentId) {
        jdbcClient.sql("DELETE FROM document_chunk WHERE document_id = :documentId")
                .param("documentId", documentId)
                .update();
    }

    public void saveAll(List<DocumentChunk> chunks) {
        for (DocumentChunk chunk : chunks) {
            jdbcClient.sql("""
                            INSERT INTO document_chunk(
                                id, workspace_id, knowledge_base_id, document_id,
                                chunk_index, content, token_count, metadata, created_at
                            )
                            VALUES (
                                :id, :workspaceId, :knowledgeBaseId, :documentId,
                                :chunkIndex, :content, :tokenCount, CAST(:metadata AS jsonb), :createdAt
                            )
                            """)
                    .param("id", chunk.id())
                    .param("workspaceId", chunk.workspaceId())
                    .param("knowledgeBaseId", chunk.knowledgeBaseId())
                    .param("documentId", chunk.documentId())
                    .param("chunkIndex", chunk.chunkIndex())
                    .param("content", chunk.content())
                    .param("tokenCount", chunk.tokenCount())
                    .param("metadata", chunk.metadata())
                    .param("createdAt", Timestamp.from(chunk.createdAt()))
                    .update();
        }
    }

    public int countByDocumentId(UUID documentId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM document_chunk WHERE document_id = :documentId")
                .param("documentId", documentId)
                .query(Integer.class)
                .single();
    }
}
