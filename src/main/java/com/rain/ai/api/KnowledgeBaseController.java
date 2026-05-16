package com.rain.ai.api;

import com.rain.ai.common.api.ApiResponse;
import com.rain.ai.knowledge.CreateKnowledgeBaseRequest;
import com.rain.ai.knowledge.KnowledgeBase;
import com.rain.ai.knowledge.KnowledgeBaseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping
    public ApiResponse<KnowledgeBase> create(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return ApiResponse.success(knowledgeBaseService.create(request));
    }

    @GetMapping
    public ApiResponse<List<KnowledgeBase>> list() {
        return ApiResponse.success(knowledgeBaseService.list());
    }
}
