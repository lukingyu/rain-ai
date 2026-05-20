package com.rain.ai.task;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AgentTaskService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final AgentTaskRepository taskRepository;

    public AgentTaskService(AgentTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public AgentTask getRequired(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BizException(ErrorCode.资源不存在, "任务不存在"));
    }

    public List<AgentTask> list(TaskType taskType, TaskStatus status, Integer limit) {
        int safeLimit = normalizeLimit(limit);
        return taskRepository.findLatest(taskType, status, safeLimit);
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
