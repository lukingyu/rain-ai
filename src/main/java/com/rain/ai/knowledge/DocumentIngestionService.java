package com.rain.ai.knowledge;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentIngestionService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentIngestionOutboxRepository outboxRepository;
    private final Path uploadDir;

    public DocumentIngestionService(
            KnowledgeBaseService knowledgeBaseService,
            KnowledgeDocumentRepository documentRepository,
            DocumentIngestionOutboxRepository outboxRepository,
            @Value("${rain.ai.storage.upload-dir}") String uploadDir
    ) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentRepository = documentRepository;
        this.outboxRepository = outboxRepository;
        this.uploadDir = Path.of(uploadDir);
    }

    @Transactional
    public DocumentUploadResult upload(UUID knowledgeBaseId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.参数错误, "上传文件不能为空");
        }

        KnowledgeBase knowledgeBase = knowledgeBaseService.getRequired(knowledgeBaseId);
        UUID documentId = UUID.randomUUID();
        Path storagePath = saveFile(documentId, file);

        KnowledgeDocument document = new KnowledgeDocument(
                documentId,
                knowledgeBase.id(),
                normalizeFilename(file.getOriginalFilename()),
                storagePath.toString(),
                DocumentStatus.PENDING,
                null
        );
        documentRepository.save(document);

        saveOutbox(document);

        return new DocumentUploadResult(document);
    }

    @Transactional
    public DocumentUploadResult reingest(UUID knowledgeBaseId, UUID documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .filter(value -> value.knowledgeBaseId().equals(knowledgeBaseId))
                .orElseThrow(() -> new BizException(ErrorCode.资源不存在, "文档不存在"));

        return new DocumentUploadResult(requeue(document));
    }

    @Transactional
    public DocumentReingestBatchResult reingestFailed(UUID knowledgeBaseId) {
        knowledgeBaseService.getRequired(knowledgeBaseId);
        List<KnowledgeDocument> failedDocuments = documentRepository.findByKnowledgeBaseIdAndStatus(
                knowledgeBaseId,
                DocumentStatus.FAILED
        );
        List<KnowledgeDocument> pendingDocuments = new ArrayList<>(failedDocuments.size());
        for (KnowledgeDocument document : failedDocuments) {
            pendingDocuments.add(requeue(document));
        }
        return new DocumentReingestBatchResult(knowledgeBaseId, pendingDocuments.size(), pendingDocuments);
    }

    private KnowledgeDocument requeue(KnowledgeDocument document) {
        KnowledgeDocument pendingDocument = new KnowledgeDocument(
                document.id(),
                document.knowledgeBaseId(),
                document.originalFilename(),
                document.storagePath(),
                DocumentStatus.PENDING,
                null
        );
        documentRepository.updateStatus(document.id(), DocumentStatus.PENDING, null);

        saveOutbox(pendingDocument);
        return pendingDocument;
    }

    private void saveOutbox(KnowledgeDocument document) {
        outboxRepository.save(
                document.id(),
                document.knowledgeBaseId(),
                document.storagePath()
        );
    }

    private Path saveFile(UUID documentId, MultipartFile file) {
        try {
            Files.createDirectories(uploadDir);
            Path targetPath = uploadDir.resolve(documentId + "-" + normalizeFilename(file.getOriginalFilename()));
            file.transferTo(targetPath);
            return targetPath;
        } catch (IOException exception) {
            throw new BizException(ErrorCode.系统错误, "保存上传文件失败");
        }
    }

    private String normalizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "unknown-document";
        }
        return Path.of(originalFilename).getFileName().toString();
    }
}
