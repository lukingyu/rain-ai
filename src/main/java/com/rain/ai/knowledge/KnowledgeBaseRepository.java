package com.rain.ai.knowledge;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

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
                        INSERT INTO knowledge_base(id, name)
                        VALUES (:id, :name)
                        """)
                .param("id", knowledgeBase.id())
                .param("name", knowledgeBase.name())
                .update();
        return knowledgeBase;
    }

    public Optional<KnowledgeBase> findById(UUID id) {
        return jdbcClient.sql("""
                        SELECT id, name
                        FROM knowledge_base
                        WHERE id = :id
                        """)
                .param("id", id)
                .query(this::mapKnowledgeBase)
                .optional();
    }

    public List<KnowledgeBase> findAll() {
        return jdbcClient.sql("""
                        SELECT id, name
                        FROM knowledge_base
                        ORDER BY name ASC
                        """)
                .query(this::mapKnowledgeBase)
                .list();
    }

    private KnowledgeBase mapKnowledgeBase(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new KnowledgeBase(
                rs.getObject("id", UUID.class),
                rs.getString("name")
        );
    }
}
