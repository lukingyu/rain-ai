package com.rain.ai.api;

import com.rain.ai.agent.AgentChatRequest;
import com.rain.ai.agent.AgentChatResponse;
import com.rain.ai.agent.AgentChatService;
import com.rain.ai.agent.AgentMemoryMessage;
import com.rain.ai.agent.AgentSessionMemoryService;
import com.rain.ai.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentChatService agentChatService;
    private final AgentSessionMemoryService memoryService;

    public AgentController(AgentChatService agentChatService, AgentSessionMemoryService memoryService) {
        this.agentChatService = agentChatService;
        this.memoryService = memoryService;
    }

    @PostMapping("/chat")
    public ApiResponse<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request) {
        return ApiResponse.success(agentChatService.chat(request));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<AgentMemoryMessage>> messages(@PathVariable String sessionId) {
        return ApiResponse.success(memoryService.recentMessages(sessionId));
    }
}
