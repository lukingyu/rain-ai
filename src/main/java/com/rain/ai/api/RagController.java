package com.rain.ai.api;

import com.rain.ai.common.api.ApiResponse;
import com.rain.ai.rag.RagAnswerResponse;
import com.rain.ai.rag.RagAskRequest;
import com.rain.ai.rag.RagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public ApiResponse<RagAnswerResponse> ask(@Valid @RequestBody RagAskRequest request) {
        return ApiResponse.success(ragService.ask(request));
    }
}
