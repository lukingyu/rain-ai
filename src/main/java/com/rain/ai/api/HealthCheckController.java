package com.rain.ai.api;

import com.rain.ai.common.api.ApiResponse;
import com.rain.ai.runtime.AiProbeResponse;
import com.rain.ai.runtime.AiRuntimeProbeService;
import com.rain.ai.runtime.AiRuntimeStatus;
import com.rain.ai.runtime.AiRuntimeStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    private final AiRuntimeStatusService aiRuntimeStatusService;
    private final AiRuntimeProbeService aiRuntimeProbeService;

    public HealthCheckController(
            AiRuntimeStatusService aiRuntimeStatusService,
            AiRuntimeProbeService aiRuntimeProbeService
    ) {
        this.aiRuntimeStatusService = aiRuntimeStatusService;
        this.aiRuntimeProbeService = aiRuntimeProbeService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "application", "rain-ai",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/ai")
    public ApiResponse<AiRuntimeStatus> ai() {
        return ApiResponse.success(aiRuntimeStatusService.status());
    }

    @PostMapping("/ai/probe")
    public ApiResponse<AiProbeResponse> probeAi() {
        return ApiResponse.success(aiRuntimeProbeService.probe());
    }
}
