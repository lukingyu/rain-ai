package com.rain.ai.task;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
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
                            id, task_type, aggregate_id, status, result, error_message
                        )
                        VALUES (
                            :id, :taskType, :aggregateId, :status, CAST(:result AS jsonb), :errorMessage
                        )
                        """)
                .param("id", task.id())
                .param("taskType", task.taskType().name())
                .param("aggregateId", task.aggregateId())
                .param("status", task.status().name())
                .param("result", task.result())
                .param("errorMessage", task.errorMessage())
                .update();
        return task;
    }

    public Optional<AgentTask> findById(UUID id) {
        return jdbcClient.sql("""
                        SELECT id, task_type, aggregate_id, status, result::text AS result, error_message
                        FROM agent_task
                        WHERE id = :id
                        """)
                .param("id", id)
                .query((rs, rowNum) -> new AgentTask(
                        rs.getObject("id", UUID.class),
                        TaskType.valueOf(rs.getString("task_type")),
                        rs.getObject("aggregate_id", UUID.class),
                        TaskStatus.valueOf(rs.getString("status")),
                        rs.getString("result"),
                        rs.getString("error_message")
                ))
                .optional();
    }

    public List<AgentTask> findLatest(TaskType taskType, TaskStatus status, int limit) {
        return jdbcClient.sql("""
                        SELECT id, task_type, aggregate_id, status, result::text AS result, error_message
                        FROM agent_task
                        WHERE (:taskType IS NULL OR task_type = :taskType)
                          AND (:status IS NULL OR status = :status)
                        ORDER BY id DESC
                        LIMIT :limit
                        """)
                .param("taskType", taskType == null ? null : taskType.name())
                .param("status", status == null ? null : status.name())
                .param("limit", limit)
                .query((rs, rowNum) -> new AgentTask(
                        rs.getObject("id", UUID.class),
                        TaskType.valueOf(rs.getString("task_type")),
                        rs.getObject("aggregate_id", UUID.class),
                        TaskStatus.valueOf(rs.getString("status")),
                        rs.getString("result"),
                        rs.getString("error_message")
                ))
                .list();
    }

    public void updateStatus(UUID id, TaskStatus status, String result, String errorMessage) {
        jdbcClient.sql("""
                        UPDATE agent_task
                        SET status = :status,
                            result = CAST(:result AS jsonb),
                            error_message = :errorMessage
                        WHERE id = :id
                        """)
                .param("id", id)
                .param("status", status.name())
                .param("result", result)
                .param("errorMessage", errorMessage)
                .update();
    }
}
