package com.rain.ai.mq;

import com.rain.ai.knowledge.DocumentIngestionMessagePublisher;
import com.rain.ai.knowledge.DocumentIngestionOutbox;
import com.rain.ai.knowledge.DocumentIngestionOutboxRepository;
import com.rain.ai.knowledge.KnowledgeDocumentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
public class DocumentIngestionOutboxRelay {

    private final DocumentIngestionOutboxRepository outboxRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentIngestionMessagePublisher messagePublisher;
    private final ExecutorService executorService;
    private final int batchSize;
    private final int maxRetryCount;

    public DocumentIngestionOutboxRelay(
            DocumentIngestionOutboxRepository outboxRepository,
            KnowledgeDocumentRepository documentRepository,
            DocumentIngestionMessagePublisher messagePublisher,
            @Qualifier("documentIngestionOutboxExecutor") ExecutorService documentIngestionOutboxExecutor,
            @Value("${rain.ai.rocketmq.outbox.batch-size:16}") int batchSize,
            @Value("${rain.ai.rocketmq.outbox.max-retry-count:10}") int maxRetryCount
    ) {
        this.outboxRepository = outboxRepository;
        this.documentRepository = documentRepository;
        this.messagePublisher = messagePublisher;
        this.executorService = documentIngestionOutboxExecutor;
        this.batchSize = Math.max(batchSize, 1);
        this.maxRetryCount = Math.max(maxRetryCount, 1);
    }

    @Scheduled(fixedDelayString = "${rain.ai.rocketmq.outbox.fixed-delay-ms:2000}")
    public void dispatch() {
        List<DocumentIngestionOutbox> outboxes = outboxRepository.claimPending(batchSize, maxRetryCount);
        if (outboxes.isEmpty()) {
            return;
        }

        CompletableFuture<?>[] futures = outboxes.stream()
                .map(outbox -> CompletableFuture.runAsync(() -> publish(outbox), executorService))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
    }

    private void publish(DocumentIngestionOutbox outbox) {
        try {
            messagePublisher.publish(outbox.toMessage());
            outboxRepository.markSent(outbox.id());
        } catch (Exception exception) {
            String errorMessage = exception.getMessage();
            outboxRepository.markFailed(outbox.id(), errorMessage);
            if (outbox.retryCount() + 1 >= maxRetryCount) {
                documentRepository.markFailedIfPending(
                        outbox.documentId(),
                        "文档摄取消息投递 RocketMQ 失败：" + errorMessage
                );
            }
        }
    }
}
