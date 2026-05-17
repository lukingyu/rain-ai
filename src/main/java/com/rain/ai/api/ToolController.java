package com.rain.ai.api;

import com.rain.ai.common.api.ApiResponse;
import com.rain.ai.skill.SkillDefinition;
import com.rain.ai.skill.SkillExecutionRequest;
import com.rain.ai.skill.SkillExecutionResponse;
import com.rain.ai.skill.SkillExecutionService;
import com.rain.ai.skill.SkillRegistry;
import com.rain.ai.tool.ToolDefinition;
import com.rain.ai.tool.ToolExecutionRequest;
import com.rain.ai.tool.ToolExecutionResponse;
import com.rain.ai.tool.ToolExecutionService;
import com.rain.ai.tool.ToolRegistry;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ToolController {

    private final ToolRegistry toolRegistry;
    private final ToolExecutionService toolExecutionService;
    private final SkillRegistry skillRegistry;
    private final SkillExecutionService skillExecutionService;

    public ToolController(
            ToolRegistry toolRegistry,
            ToolExecutionService toolExecutionService,
            SkillRegistry skillRegistry,
            SkillExecutionService skillExecutionService
    ) {
        this.toolRegistry = toolRegistry;
        this.toolExecutionService = toolExecutionService;
        this.skillRegistry = skillRegistry;
        this.skillExecutionService = skillExecutionService;
    }

    @GetMapping("/tools")
    public ApiResponse<List<ToolDefinition>> listTools() {
        return ApiResponse.success(toolRegistry.listDefinitions());
    }

    @PostMapping("/tools/execute")
    public ApiResponse<ToolExecutionResponse> execute(@Valid @RequestBody ToolExecutionRequest request) {
        return ApiResponse.success(toolExecutionService.execute(request));
    }

    @GetMapping("/skills")
    public ApiResponse<List<SkillDefinition>> listSkills() {
        return ApiResponse.success(skillRegistry.listDefinitions());
    }

    @PostMapping("/skills/execute")
    public ApiResponse<SkillExecutionResponse> executeSkill(@Valid @RequestBody SkillExecutionRequest request) {
        return ApiResponse.success(skillExecutionService.execute(request));
    }
}
