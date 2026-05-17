package com.rain.ai.tool;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public final class ToolArguments {

    private ToolArguments() {
    }

    public static UUID requiredUuid(Map<String, Object> arguments, String name) {
        String value = requiredString(arguments, name);
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.参数错误, "工具参数格式错误：" + name + " 必须是 UUID");
        }
    }

    public static String requiredString(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new BizException(ErrorCode.参数错误, "缺少工具参数：" + name);
        }
        return String.valueOf(value);
    }
}
