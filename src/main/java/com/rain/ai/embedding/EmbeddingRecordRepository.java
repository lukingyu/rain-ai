package com.rain.ai.embedding;

import com.rain.ai.knowledge.DocumentChunk;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class EmbeddingRecordRepository {

    private final JdbcClient jdbcClient;

    public EmbeddingRecordRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void deleteByDocumentId(UUID documentId) {
        jdbcClient.sql("DELETE FROM embedding_record WHERE document_id = :documentId")
                .param("documentId", documentId)
                .update();
    }

    public void saveAll(List<EmbeddingRecord> records) {
        for (EmbeddingRecord record : records) {
            jdbcClient.sql("""
                            INSERT INTO embedding_record(
                                id, workspace_id, knowledge_base_id, document_id,
                                chunk_id, embedding_model, embedding, created_at
                            )
                            VALUES (
                                :id, :workspaceId, :knowledgeBaseId, :documentId,
                                :chunkId, :embeddingModel, CAST(:embedding AS vector), :createdAt
                            )
                            ON CONFLICT (chunk_id) DO UPDATE SET
                                embedding_model = EXCLUDED.embedding_model,
                                embedding = EXCLUDED.embedding,
                                created_at = EXCLUDED.created_at
                            """)
                    .param("id", record.id())
                    .param("workspaceId", record.workspaceId())
                    .param("knowledgeBaseId", record.knowledgeBaseId())
                    .param("documentId", record.documentId())
                    .param("chunkId", record.chunkId())
                    .param("embeddingModel", record.embeddingModel())
                    .param("embedding", record.embedding())
                    .param("createdAt", Timestamp.from(record.createdAt()))
                    .update();
        }
    }

    public List<DocumentChunk> searchSimilarChunks(UUID knowledgeBaseId, String queryEmbedding, int limit) {
        return jdbcClient.sql("""
                        SELECT c.id, c.workspace_id, c.knowledge_base_id, c.document_id,
                               c.chunk_index, c.content, c.token_count, c.metadata, c.created_at
                        FROM embedding_record e
                        JOIN document_chunk c ON c.id = e.chunk_id
                        WHERE e.knowledge_base_id = :knowledgeBaseId
                        ORDER BY e.embedding <=> CAST(:queryEmbedding AS vector)
                        LIMIT :limit
                        """)
                .param("knowledgeBaseId", knowledgeBaseId)
                .param("queryEmbedding", queryEmbedding)
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
