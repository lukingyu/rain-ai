package com.rain.ai.knowledge;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

@Service
public class DocumentIngestionService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentIngestionOutboxRepository outboxRepository;
    private final TransactionTemplate transactionTemplate;
    private final ExecutorService documentReingestExecutor;
    private final Semaphore reingestSemaphore;
    private final Path uploadDir;

    public DocumentIngestionService(
            KnowledgeBaseService knowledgeBaseService,
            KnowledgeDocumentRepository documentRepository,
            DocumentIngestionOutboxRepository outboxRepository,
            TransactionTemplate transactionTemplate,
            @Qualifier("documentReingestExecutor") ExecutorService documentReingestExecutor,
            @Value("${rain.ai.storage.upload-dir}") String uploadDir,
            @Value("${rain.ai.knowledge.reingest-concurrency:8}") int reingestConcurrency
    ) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentRepository = documentRepository;
        this.outboxRepository = outboxRepository;
        this.transactionTemplate = transactionTemplate;
        this.documentReingestExecutor = documentReingestExecutor;
        this.reingestSemaphore = new Semaphore(Math.max(reingestConcurrency, 1));
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

    public DocumentReingestBatchResult reingestFailed(UUID knowledgeBaseId) {
        knowledgeBaseService.getRequired(knowledgeBaseId);
        List<KnowledgeDocument> failedDocuments = documentRepository.findByKnowledgeBaseIdAndStatus(
                knowledgeBaseId,
                DocumentStatus.FAILED
        );
        return reingestBatch(knowledgeBaseId, failedDocuments);
    }

    public DocumentReingestBatchResult reingestAll(UUID knowledgeBaseId) {
        knowledgeBaseService.getRequired(knowledgeBaseId);
        return reingestBatch(knowledgeBaseId, documentRepository.findByKnowledgeBaseId(knowledgeBaseId));
    }

    private DocumentReingestBatchResult reingestBatch(UUID knowledgeBaseId, List<KnowledgeDocument> documents) {
        List<CompletableFuture<ReingestOutcome>> futures = documents.stream()
                .map(this::requeueAsync)
                .toList();

        List<ReingestOutcome> outcomes = futures.stream()
                .map(CompletableFuture::join)
                .sorted(Comparator.comparing(outcome -> String.valueOf(outcome.documentId())))
                .toList();
        List<KnowledgeDocument> pendingDocuments = outcomes.stream()
                .filter(ReingestOutcome::success)
                .map(ReingestOutcome::document)
                .toList();
        List<DocumentReingestFailure> failures = outcomes.stream()
                .filter(outcome -> !outcome.success())
                .map(outcome -> new DocumentReingestFailure(outcome.documentId(), outcome.errorMessage()))
                .toList();
        return new DocumentReingestBatchResult(
                knowledgeBaseId,
                documents.size(),
                pendingDocuments.size(),
                failures.size(),
                pendingDocuments,
                failures
        );
    }

    private CompletableFuture<ReingestOutcome> requeueAsync(KnowledgeDocument document) {
        return CompletableFuture.supplyAsync(
                        () -> requeueWithBoundary(document),
                        documentReingestExecutor
                )
                .exceptionally(exception -> ReingestOutcome.failed(document.id(), exception.getMessage()));
    }

    private ReingestOutcome requeueWithBoundary(KnowledgeDocument document) {
        try {
            reingestSemaphore.acquire();
            try {
                KnowledgeDocument pendingDocument = transactionTemplate.execute(status -> requeue(document));
                if (pendingDocument == null) {
                    return ReingestOutcome.failed(document.id(), "重新投递事务未返回文档结果");
                }
                return ReingestOutcome.succeeded(pendingDocument);
            } finally {
                reingestSemaphore.release();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ReingestOutcome.failed(document.id(), "批量重新投递被中断");
        } catch (RuntimeException exception) {
            return ReingestOutcome.failed(document.id(), exception.getMessage());
        }
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

    private record ReingestOutcome(
            UUID documentId,
            boolean success,
            KnowledgeDocument document,
            String errorMessage
    ) {

        private static ReingestOutcome succeeded(KnowledgeDocument document) {
            return new ReingestOutcome(document.id(), true, document, null);
        }

        private static ReingestOutcome failed(UUID documentId, String errorMessage) {
            return new ReingestOutcome(documentId, false, null, errorMessage);
        }
    }
}
