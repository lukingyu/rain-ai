package com.rain.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import com.rain.ai.task.AgentTask;
import com.rain.ai.task.AgentTaskRepository;
import com.rain.ai.task.TaskStatus;
import com.rain.ai.task.TaskType;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class ToolExecutionService {

    private final ToolRegistry toolRegistry;
    private final AgentTaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    public ToolExecutionService(
            ToolRegistry toolRegistry,
            AgentTaskRepository taskRepository,
            ObjectMapper objectMapper
    ) {
        this.toolRegistry = toolRegistry;
        this.taskRepository = taskRepository;
        this.objectMapper = objectMapper;
    }

    public ToolExecutionResponse execute(ToolExecutionRequest request) {
        ToolHandler handler = toolRegistry.getRequired(request.toolName());
        Map<String, Object> arguments = request.safeArguments();
        validateRequiredArguments(handler.definition(), arguments);

        UUID taskId = UUID.randomUUID();
        AgentTask task = new AgentTask(
                taskId,
                TaskType.TOOL_EXECUTION,
                extractAggregateId(arguments).orElse(taskId),
                TaskStatus.PENDING,
                null,
                null
        );
        taskRepository.save(task);

        try {
            taskRepository.updateStatus(taskId, TaskStatus.RUNNING, null, null);
            Object result = handler.execute(new ToolExecutionContext(arguments));
            taskRepository.updateStatus(taskId, TaskStatus.COMPLETED, toJson(result), null);
            return new ToolExecutionResponse(taskId, request.toolName(), TaskStatus.COMPLETED.name(), result);
        } catch (Exception exception) {
            taskRepository.updateStatus(taskId, TaskStatus.FAILED, null, exception.getMessage());
            throw exception;
        }
    }

    private void validateRequiredArguments(ToolDefinition definition, Map<String, Object> arguments) {
        for (ToolParameter parameter : definition.parameters()) {
            if (parameter.required() && !arguments.containsKey(parameter.name())) {
                throw new BizException(ErrorCode.参数错误, "缺少工具参数：" + parameter.name());
            }
        }
    }

    private java.util.Optional<UUID> extractAggregateId(Map<String, Object> arguments) {
        Object value = arguments.get("knowledgeBaseId");
        if (value == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(ToolArguments.requiredUuid(arguments, "knowledgeBaseId"));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.系统错误, "工具执行结果序列化失败");
        }
    }
}
