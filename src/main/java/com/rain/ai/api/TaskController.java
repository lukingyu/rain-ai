package com.rain.ai.api;

import com.rain.ai.common.api.ApiResponse;
import com.rain.ai.task.AgentTask;
import com.rain.ai.task.AgentTaskService;
import com.rain.ai.task.TaskStatus;
import com.rain.ai.task.TaskType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final AgentTaskService taskService;

    public TaskController(AgentTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/{taskId}")
    public ApiResponse<AgentTask> get(@PathVariable UUID taskId) {
        return ApiResponse.success(taskService.getRequired(taskId));
    }

    @GetMapping
    public ApiResponse<List<AgentTask>> list(
            @RequestParam(required = false) TaskType taskType,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.success(taskService.list(taskType, status, limit));
    }
}
