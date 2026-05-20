package com.rain.ai.mq;

import com.rain.ai.knowledge.DocumentIngestionMessagePublisher;
import com.rain.ai.knowledge.DocumentIngestionOutbox;
import com.rain.ai.knowledge.DocumentIngestionOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
public class DocumentIngestionOutboxRelay {

    private final DocumentIngestionOutboxRepository outboxRepository;
    private final DocumentIngestionMessagePublisher messagePublisher;
    private final ExecutorService executorService;
    private final int batchSize;
    private final int maxRetryCount;

    public DocumentIngestionOutboxRelay(
            DocumentIngestionOutboxRepository outboxRepository,
            DocumentIngestionMessagePublisher messagePublisher,
            ExecutorService documentIngestionOutboxExecutor,
            @Value("${rain.ai.rocketmq.outbox.batch-size:16}") int batchSize,
            @Value("${rain.ai.rocketmq.outbox.max-retry-count:10}") int maxRetryCount
    ) {
        this.outboxRepository = outboxRepository;
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
            outboxRepository.markFailed(outbox.id(), exception.getMessage());
        }
    }
}
