package com.rain.ai.api;

import com.rain.ai.common.api.ApiResponse;
import com.rain.ai.knowledge.DocumentReingestBatchResult;
import com.rain.ai.knowledge.DocumentIngestionService;
import com.rain.ai.knowledge.DocumentUploadResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/knowledge-bases/{knowledgeBaseId}/documents")
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;

    public DocumentController(DocumentIngestionService documentIngestionService) {
        this.documentIngestionService = documentIngestionService;
    }

    @PostMapping
    public ApiResponse<DocumentUploadResult> upload(
            @PathVariable UUID knowledgeBaseId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(documentIngestionService.upload(knowledgeBaseId, file));
    }

    @PostMapping("/{documentId}/reingest")
    public ApiResponse<DocumentUploadResult> reingest(
            @PathVariable UUID knowledgeBaseId,
            @PathVariable UUID documentId
    ) {
        return ApiResponse.success(documentIngestionService.reingest(knowledgeBaseId, documentId));
    }

    @PostMapping("/failed/reingest")
    public ApiResponse<DocumentReingestBatchResult> reingestFailed(@PathVariable UUID knowledgeBaseId) {
        return ApiResponse.success(documentIngestionService.reingestFailed(knowledgeBaseId));
    }
}
