package com.rain.ai.task;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AgentTaskService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final AgentTaskRepository taskRepository;
    private final String defaultWorkspaceId;

    public AgentTaskService(
            AgentTaskRepository taskRepository,
            @Value("${rain.ai.workspace.default-id}") String defaultWorkspaceId
    ) {
        this.taskRepository = taskRepository;
        this.defaultWorkspaceId = defaultWorkspaceId;
    }

    public AgentTask getRequired(UUID taskId) {
        return taskRepository.findById(taskId)
                .filter(task -> defaultWorkspaceId.equals(task.workspaceId()))
                .orElseThrow(() -> new BizException(ErrorCode.资源不存在, "任务不存在"));
    }

    public List<AgentTask> list(TaskType taskType, TaskStatus status, Integer limit) {
        int safeLimit = normalizeLimit(limit);
        return taskRepository.findLatest(defaultWorkspaceId, taskType, status, safeLimit);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
