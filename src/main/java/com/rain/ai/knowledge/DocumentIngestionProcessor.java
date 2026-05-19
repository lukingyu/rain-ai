package com.rain.ai.knowledge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rain.ai.task.AgentTaskRepository;
import com.rain.ai.task.TaskStatus;
import com.rain.ai.embedding.EmbeddingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentIngestionProcessor {

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final AgentTaskRepository taskRepository;
    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public DocumentIngestionProcessor(
            KnowledgeDocumentRepository documentRepository,
            DocumentChunkRepository chunkRepository,
            AgentTaskRepository taskRepository,
            TextChunker textChunker,
            EmbeddingService embeddingService,
            ObjectMapper objectMapper
    ) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.taskRepository = taskRepository;
        this.textChunker = textChunker;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void process(DocumentIngestionMessage message) {
        try {
            taskRepository.updateStatus(message.taskId(), TaskStatus.RUNNING, null, null);
            documentRepository.updateStatus(message.documentId(), DocumentStatus.PARSING, null);

            String text = Files.readString(Path.of(message.storagePath()));
            documentRepository.updateStatus(message.documentId(), DocumentStatus.CHUNKING, null);

            List<ChunkSegment> segments = textChunker.split(text);
            embeddingService.deleteDocumentEmbeddings(message.documentId());
            chunkRepository.deleteByDocumentId(message.documentId());
            List<DocumentChunk> chunks = toChunks(message, segments);
            chunkRepository.saveAll(chunks);
            embeddingService.rebuildDocumentEmbeddings(message.documentId(), chunks);

            documentRepository.updateStatus(message.documentId(), DocumentStatus.COMPLETED, null);
            taskRepository.updateStatus(
                    message.taskId(),
                    TaskStatus.COMPLETED,
                    "{\"chunkCount\":%d,\"embeddingEnabled\":%s}".formatted(segments.size(), embeddingService.available()),
                    null
            );
        } catch (Exception exception) {
            documentRepository.updateStatus(message.documentId(), DocumentStatus.FAILED, exception.getMessage());
            taskRepository.updateStatus(message.taskId(), TaskStatus.FAILED, null, exception.getMessage());
            throw new IllegalStateException("文档摄取处理失败", exception);
        }
    }

    private List<DocumentChunk> toChunks(DocumentIngestionMessage message, List<ChunkSegment> segments) {
        List<DocumentChunk> chunks = new ArrayList<>();
        Instant now = Instant.now();
        for (int index = 0; index < segments.size(); index++) {
            ChunkSegment segment = segments.get(index);
            chunks.add(new DocumentChunk(
                    UUID.randomUUID(),
                    message.workspaceId(),
                    message.knowledgeBaseId(),
                    message.documentId(),
                    index,
                    segment.content(),
                    segment.tokenCount(),
                    toMetadata(segment),
                    now
            ));
        }
        return chunks;
    }

    private String toMetadata(ChunkSegment segment) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "source", "document_ingestion",
                    "sectionTitle", segment.sectionTitle(),
                    "charStart", segment.charStart(),
                    "charEnd", segment.charEnd(),
                    "boundary", segment.boundary(),
                    "contentHash", segment.contentHash()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("构建分片元数据失败", exception);
        }
    }
}
