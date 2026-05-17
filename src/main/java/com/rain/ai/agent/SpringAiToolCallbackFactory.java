package com.rain.ai.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rain.ai.tool.ToolDefinition;
import com.rain.ai.tool.ToolExecutionRequest;
import com.rain.ai.tool.ToolExecutionResponse;
import com.rain.ai.tool.ToolExecutionService;
import com.rain.ai.tool.ToolHandler;
import com.rain.ai.tool.ToolParameter;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SpringAiToolCallbackFactory {

    private final ToolExecutionService toolExecutionService;
    private final ObjectMapper objectMapper;
    private final AiToolNameMapper toolNameMapper;

    public SpringAiToolCallbackFactory(
            ToolExecutionService toolExecutionService,
            ObjectMapper objectMapper,
            AiToolNameMapper toolNameMapper
    ) {
        this.toolExecutionService = toolExecutionService;
        this.objectMapper = objectMapper;
        this.toolNameMapper = toolNameMapper;
    }

    public ToolCallback create(ToolHandler handler, Consumer<ToolExecutionResponse> executionRecorder) {
        ToolDefinition internalDefinition = handler.definition();
        String aiToolName = toolNameMapper.toAiToolName(internalDefinition);
        org.springframework.ai.tool.definition.ToolDefinition springDefinition =
                org.springframework.ai.tool.definition.ToolDefinition.builder()
                        .name(aiToolName)
                        .description(internalDefinition.description())
                        .inputSchema(inputSchema(internalDefinition.parameters()))
                        .build();

        return new ToolCallback() {
            @Override
            public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                return springDefinition;
            }

            @Override
            public ToolMetadata getToolMetadata() {
                return ToolMetadata.builder()
                        .returnDirect(false)
                        .build();
            }

            @Override
            public String call(String input) {
                return call(input, new ToolContext(Map.of()));
            }

            @Override
            public String call(String input, ToolContext toolContext) {
                Map<String, Object> arguments = parseArguments(input);
                ToolExecutionResponse response = toolExecutionService.execute(
                        new ToolExecutionRequest(internalDefinition.name(), arguments)
                );
                executionRecorder.accept(response);
                return toJson(response);
            }
        };
    }

    private String inputSchema(List<ToolParameter> parameters) {
        Map<String, Object> properties = new HashMap<>();
        List<String> required = parameters.stream()
                .filter(ToolParameter::required)
                .map(ToolParameter::name)
                .toList();
        for (ToolParameter parameter : parameters) {
            properties.put(parameter.name(), Map.of(
                    "type", schemaType(parameter.type()),
                    "description", parameter.description()
            ));
        }
        return toJson(Map.of(
                "type", "object",
                "properties", properties,
                "required", required
        ));
    }

    private String schemaType(String type) {
        if ("uuid".equals(type)) {
            return "string";
        }
        return type;
    }

    private Map<String, Object> parseArguments(String input) {
        try {
            if (input == null || input.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(input, objectMapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, Object.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("AI 工具参数不是合法 JSON：" + input, exception);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("工具结果序列化失败", exception);
        }
    }
}
