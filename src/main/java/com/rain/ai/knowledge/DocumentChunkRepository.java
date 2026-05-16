package com.rain.ai.knowledge;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
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

    public List<DocumentChunk> searchByKnowledgeBaseId(UUID knowledgeBaseId, String keyword, int limit) {
        String pattern = "%" + keyword + "%";
        return jdbcClient.sql("""
                        SELECT id, workspace_id, knowledge_base_id, document_id,
                               chunk_index, content, token_count, metadata, created_at
                        FROM document_chunk
                        WHERE knowledge_base_id = :knowledgeBaseId
                          AND content ILIKE :pattern
                        ORDER BY created_at DESC, chunk_index ASC
                        LIMIT :limit
                        """)
                .param("knowledgeBaseId", knowledgeBaseId)
                .param("pattern", pattern)
                .param("limit", limit)
                .query((rs, rowNum) -> new DocumentChunk(
                        rs.getObject("id", UUID.class),
                        rs.getString("workspace_id"),
                        rs.getObject("knowledge_base_id", UUID.class),
                        rs.getObject("document_id", UUID.class),
                        rs.getInt("chunk_index"),
                        rs.getString("content"),
                        rs.getInt("token_count"),
                        rs.getString("metadata"),
                        toInstant(rs.getTimestamp("created_at"))
                ))
                .list();
    }

    public List<DocumentChunk> findLatestByKnowledgeBaseId(UUID knowledgeBaseId, int limit) {
        return jdbcClient.sql("""
                        SELECT id, workspace_id, knowledge_base_id, document_id,
                               chunk_index, content, token_count, metadata, created_at
                        FROM document_chunk
                        WHERE knowledge_base_id = :knowledgeBaseId
                        ORDER BY created_at DESC, chunk_index ASC
                        LIMIT :limit
                        """)
                .param("knowledgeBaseId", knowledgeBaseId)
                .param("limit", limit)
                .query((rs, rowNum) -> new DocumentChunk(
                        rs.getObject("id", UUID.class),
                        rs.getString("workspace_id"),
                        rs.getObject("knowledge_base_id", UUID.class),
                        rs.getObject("document_id", UUID.class),
                        rs.getInt("chunk_index"),
                        rs.getString("content"),
                        rs.getInt("token_count"),
                        rs.getString("metadata"),
                        toInstant(rs.getTimestamp("created_at"))
                ))
                .list();
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp.toInstant();
    }
}
