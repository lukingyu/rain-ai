package com.rain.ai.knowledge;

import com.rain.ai.task.AgentTaskRepository;
import com.rain.ai.task.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentIngestionProcessor {

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final AgentTaskRepository taskRepository;
    private final TextChunker textChunker;

    public DocumentIngestionProcessor(
            KnowledgeDocumentRepository documentRepository,
            DocumentChunkRepository chunkRepository,
            AgentTaskRepository taskRepository,
            TextChunker textChunker
    ) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.taskRepository = taskRepository;
        this.textChunker = textChunker;
    }

    @Transactional
    public void process(DocumentIngestionMessage message) {
        try {
            taskRepository.updateStatus(message.taskId(), TaskStatus.RUNNING, null, null);
            documentRepository.updateStatus(message.documentId(), DocumentStatus.PARSING, null);

            String text = Files.readString(Path.of(message.storagePath()));
            documentRepository.updateStatus(message.documentId(), DocumentStatus.CHUNKING, null);

            List<String> texts = textChunker.split(text);
            chunkRepository.deleteByDocumentId(message.documentId());
            chunkRepository.saveAll(toChunks(message, texts));

            documentRepository.updateStatus(message.documentId(), DocumentStatus.COMPLETED, null);
            taskRepository.updateStatus(
                    message.taskId(),
                    TaskStatus.COMPLETED,
                    "{\"chunkCount\":%d}".formatted(texts.size()),
                    null
            );
        } catch (Exception exception) {
            documentRepository.updateStatus(message.documentId(), DocumentStatus.FAILED, exception.getMessage());
            taskRepository.updateStatus(message.taskId(), TaskStatus.FAILED, null, exception.getMessage());
            throw new IllegalStateException("文档摄取处理失败", exception);
        }
    }

    private List<DocumentChunk> toChunks(DocumentIngestionMessage message, List<String> texts) {
        List<DocumentChunk> chunks = new ArrayList<>();
        Instant now = Instant.now();
        for (int index = 0; index < texts.size(); index++) {
            String content = texts.get(index);
            chunks.add(new DocumentChunk(
                    UUID.randomUUID(),
                    message.workspaceId(),
                    message.knowledgeBaseId(),
                    message.documentId(),
                    index,
                    content,
                    estimateTokenCount(content),
                    "{\"source\":\"document_ingestion\"}",
                    now
            ));
        }
        return chunks;
    }

    private int estimateTokenCount(String content) {
        return Math.max(1, content.length() / 2);
    }
}
