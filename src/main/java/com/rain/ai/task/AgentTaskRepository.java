package com.rain.ai.task;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AgentTaskRepository {

    private final JdbcClient jdbcClient;

    public AgentTaskRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public AgentTask save(AgentTask task) {
        jdbcClient.sql("""
                        INSERT INTO agent_task(
                            id, workspace_id, task_type, aggregate_id, status,
                            payload, result, error_message, created_at, updated_at
                        )
                        VALUES (
                            :id, :workspaceId, :taskType, :aggregateId, :status,
                            CAST(:payload AS jsonb), CAST(:result AS jsonb), :errorMessage, :createdAt, :updatedAt
                        )
                        """)
                .param("id", task.id())
                .param("workspaceId", task.workspaceId())
                .param("taskType", task.taskType().name())
                .param("aggregateId", task.aggregateId())
                .param("status", task.status().name())
                .param("payload", task.payload())
                .param("result", task.result())
                .param("errorMessage", task.errorMessage())
                .param("createdAt", Timestamp.from(task.createdAt()))
                .param("updatedAt", Timestamp.from(task.updatedAt()))
                .update();
        return task;
    }

    public Optional<AgentTask> findById(UUID id) {
        return jdbcClient.sql("""
                        SELECT id, workspace_id, task_type, aggregate_id, status,
                               payload::text AS payload, result::text AS result,
                               error_message, created_at, updated_at
                        FROM agent_task
                        WHERE id = :id
                        """)
                .param("id", id)
                .query((rs, rowNum) -> new AgentTask(
                        rs.getObject("id", UUID.class),
                        rs.getString("workspace_id"),
                        TaskType.valueOf(rs.getString("task_type")),
                        rs.getObject("aggregate_id", UUID.class),
                        TaskStatus.valueOf(rs.getString("status")),
                        rs.getString("payload"),
                        rs.getString("result"),
                        rs.getString("error_message"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()
                ))
                .optional();
    }

    public void updateStatus(UUID id, TaskStatus status, String result, String errorMessage) {
        jdbcClient.sql("""
                        UPDATE agent_task
                        SET status = :status,
                            result = CAST(:result AS jsonb),
                            error_message = :errorMessage,
                            updated_at = :updatedAt
                        WHERE id = :id
                        """)
                .param("id", id)
                .param("status", status.name())
                .param("result", result)
                .param("errorMessage", errorMessage)
                .param("updatedAt", Timestamp.from(Instant.now()))
                .update();
    }
}
