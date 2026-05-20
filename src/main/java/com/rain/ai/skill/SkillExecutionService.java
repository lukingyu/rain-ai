package com.rain.ai.skill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import com.rain.ai.task.AgentTask;
import com.rain.ai.task.AgentTaskRepository;
import com.rain.ai.task.TaskStatus;
import com.rain.ai.task.TaskType;
import com.rain.ai.tool.ToolArguments;
import com.rain.ai.tool.ToolParameter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SkillExecutionService {

    private final SkillRegistry skillRegistry;
    private final AgentTaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    public SkillExecutionService(
            SkillRegistry skillRegistry,
            AgentTaskRepository taskRepository,
            ObjectMapper objectMapper
    ) {
        this.skillRegistry = skillRegistry;
        this.taskRepository = taskRepository;
        this.objectMapper = objectMapper;
    }

    public SkillExecutionResponse execute(SkillExecutionRequest request) {
        SkillHandler handler = skillRegistry.getRequired(request.skillName());
        Map<String, Object> arguments = request.safeArguments();
        validateRequiredArguments(handler.definition(), arguments);

        UUID taskId = UUID.randomUUID();
        AgentTask task = new AgentTask(
                taskId,
                TaskType.SKILL_EXECUTION,
                extractAggregateId(arguments).orElse(taskId),
                TaskStatus.PENDING,
                null,
                null
        );
        taskRepository.save(task);

        try {
            taskRepository.updateStatus(taskId, TaskStatus.RUNNING, null, null);
            SkillExecutionOutcome outcome = handler.execute(new SkillExecutionContext(arguments));
            SkillExecutionResponse response = new SkillExecutionResponse(
                    taskId,
                    request.skillName(),
                    TaskStatus.COMPLETED.name(),
                    outcome.summary(),
                    outcome.steps()
            );
            taskRepository.updateStatus(taskId, TaskStatus.COMPLETED, toJson(response), null);
            return response;
        } catch (Exception exception) {
            taskRepository.updateStatus(taskId, TaskStatus.FAILED, null, exception.getMessage());
            throw exception;
        }
    }

    private void validateRequiredArguments(SkillDefinition definition, Map<String, Object> arguments) {
        for (ToolParameter parameter : definition.parameters()) {
            if (parameter.required() && !arguments.containsKey(parameter.name())) {
                throw new BizException(ErrorCode.参数错误, "缺少技能参数：" + parameter.name());
            }
        }
    }

    private Optional<UUID> extractAggregateId(Map<String, Object> arguments) {
        Object value = arguments.get("knowledgeBaseId");
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(ToolArguments.requiredUuid(arguments, "knowledgeBaseId"));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.系统错误, "技能执行结果序列化失败");
        }
    }
}
