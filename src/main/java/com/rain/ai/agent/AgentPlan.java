package com.rain.ai.agent;

import java.util.Map;

public record AgentPlan(
        AgentPlanType type,
        String name,
        Map<String, Object> arguments
) {
}
