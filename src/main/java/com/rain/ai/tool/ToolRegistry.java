package com.rain.ai.tool;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ToolRegistry {

    private final Map<String, ToolHandler> handlers;

    public ToolRegistry(List<ToolHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        handler -> handler.definition().name(),
                        Function.identity()
                ));
    }

    public List<ToolDefinition> listDefinitions() {
        return handlers.values()
                .stream()
                .map(ToolHandler::definition)
                .sorted(Comparator.comparing(ToolDefinition::name))
                .toList();
    }

    public ToolHandler getRequired(String toolName) {
        ToolHandler handler = handlers.get(toolName);
        if (handler == null) {
            throw new BizException(ErrorCode.资源不存在, "工具不存在：" + toolName);
        }
        return handler;
    }
}
