package com.rain.ai.api;

import com.rain.ai.common.api.ApiResponse;
import com.rain.ai.tool.AiToolDefinition;
import com.rain.ai.tool.SpringAiToolCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final SpringAiToolCatalog toolCatalog;

    public ToolController(SpringAiToolCatalog toolCatalog) {
        this.toolCatalog = toolCatalog;
    }

    @GetMapping
    public ApiResponse<List<AiToolDefinition>> listTools() {
        return ApiResponse.success(toolCatalog.definitions());
    }
}
