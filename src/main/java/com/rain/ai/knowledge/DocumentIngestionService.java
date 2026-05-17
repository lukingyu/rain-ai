package com.rain.ai.knowledge;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import com.rain.ai.task.AgentTask;
import com.rain.ai.task.AgentTaskRepository;
import com.rain.ai.task.TaskStatus;
import com.rain.ai.task.TaskType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

@Service
public class DocumentIngestionService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocumentRepository documentRepository;
    private final AgentTaskRepository taskRepository;
    private final DocumentIngestionMessagePublisher messagePublisher;
    private final Path uploadDir;

    public DocumentIngestionService(
            KnowledgeBaseService knowledgeBaseService,
            KnowledgeDocumentRepository documentRepository,
            AgentTaskRepository taskRepository,
            DocumentIngestionMessagePublisher messagePublisher,
            @Value("${rain.ai.storage.upload-dir}") String uploadDir
    ) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
        this.messagePublisher = messagePublisher;
        this.uploadDir = Path.of(uploadDir);
    }

    @Transactional
    public DocumentUploadResult upload(UUID knowledgeBaseId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.参数错误, "上传文件不能为空");
        }

        KnowledgeBase knowledgeBase = knowledgeBaseService.getRequired(knowledgeBaseId);
        UUID documentId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Instant now = Instant.now();
        Path storagePath = saveFile(documentId, file);

        KnowledgeDocument document = new KnowledgeDocument(
                documentId,
                knowledgeBase.workspaceId(),
                knowledgeBase.id(),
                normalizeFilename(file.getOriginalFilename()),
                file.getContentType(),
                file.getSize(),
                storagePath.toString(),
                DocumentStatus.PENDING,
                null,
                now,
                now
        );
        documentRepository.save(document);

        AgentTask task = new AgentTask(
                taskId,
                knowledgeBase.workspaceId(),
                TaskType.DOCUMENT_INGESTION,
                document.id(),
                TaskStatus.PENDING,
                """
                        {"documentId":"%s","knowledgeBaseId":"%s"}
                        """.formatted(document.id(), knowledgeBase.id()).trim(),
                null,
                null,
                now,
                now
        );
        taskRepository.save(task);
        publishAfterCommit(document, task);

        return new DocumentUploadResult(document, task);
    }

    @Transactional
    public DocumentUploadResult reingest(UUID knowledgeBaseId, UUID documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .filter(value -> value.knowledgeBaseId().equals(knowledgeBaseId))
                .orElseThrow(() -> new BizException(ErrorCode.资源不存在, "文档不存在"));

        UUID taskId = UUID.randomUUID();
        Instant now = Instant.now();
        KnowledgeDocument pendingDocument = new KnowledgeDocument(
                document.id(),
                document.workspaceId(),
                document.knowledgeBaseId(),
                document.originalFilename(),
                document.contentType(),
                document.sizeBytes(),
                document.storagePath(),
                DocumentStatus.PENDING,
                null,
                document.createdAt(),
                now
        );
        documentRepository.updateStatus(document.id(), DocumentStatus.PENDING, null);

        AgentTask task = new AgentTask(
                taskId,
                document.workspaceId(),
                TaskType.DOCUMENT_INGESTION,
                document.id(),
                TaskStatus.PENDING,
                """
                        {"documentId":"%s","knowledgeBaseId":"%s","reingest":true}
                        """.formatted(document.id(), document.knowledgeBaseId()).trim(),
                null,
                null,
                now,
                now
        );
        taskRepository.save(task);
        publishAfterCommit(pendingDocument, task);
        return new DocumentUploadResult(pendingDocument, task);
    }

    private void publishAfterCommit(KnowledgeDocument document, AgentTask task) {
        DocumentIngestionMessage message = new DocumentIngestionMessage(
                task.id(),
                document.id(),
                document.knowledgeBaseId(),
                document.workspaceId(),
                document.storagePath()
        );
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        messagePublisher.publish(message);
                    }
                }
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
