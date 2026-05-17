package com.rain.ai.skill;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SkillRegistry {

    private final Map<String, SkillHandler> handlers;

    public SkillRegistry(List<SkillHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        handler -> handler.definition().name(),
                        Function.identity()
                ));
    }

    public List<SkillDefinition> listDefinitions() {
        return handlers.values()
                .stream()
                .map(SkillHandler::definition)
                .toList();
    }

    public SkillHandler getRequired(String skillName) {
        SkillHandler handler = handlers.get(skillName);
        if (handler == null) {
            throw new BizException(ErrorCode.资源不存在, "技能不存在：" + skillName);
        }
        return handler;
    }
}
