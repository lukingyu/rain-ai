package com.rain.ai.knowledge;

import com.rain.ai.task.AgentTaskRepository;
import com.rain.ai.task.TaskStatus;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DocumentIngestionProcessor {

    private final KnowledgeDocumentRepository documentRepository;
    private final AgentTaskRepository taskRepository;
    private final TokenTextSplitter tokenTextSplitter;
    private final VectorStore vectorStore;

    public DocumentIngestionProcessor(
            KnowledgeDocumentRepository documentRepository,
            AgentTaskRepository taskRepository,
            TokenTextSplitter tokenTextSplitter,
            VectorStore vectorStore
    ) {
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
        this.tokenTextSplitter = tokenTextSplitter;
        this.vectorStore = vectorStore;
    }

    @Transactional
    public void process(DocumentIngestionMessage message) {
        try {
            taskRepository.updateStatus(message.taskId(), TaskStatus.RUNNING, null, null);
            documentRepository.updateStatus(message.documentId(), DocumentStatus.PARSING, null);

            String text = Files.readString(Path.of(message.storagePath()));
            documentRepository.updateStatus(message.documentId(), DocumentStatus.CHUNKING, null);

            List<Document> chunks = splitBySpringAi(message, text);
            deleteDocumentVectors(message);
            vectorStore.add(chunks);

            documentRepository.updateStatus(message.documentId(), DocumentStatus.COMPLETED, null);
            taskRepository.updateStatus(
                    message.taskId(),
                    TaskStatus.COMPLETED,
                    "{\"chunkCount\":%d,\"vectorStore\":\"spring-ai-pgvector\"}".formatted(chunks.size()),
                    null
            );
        } catch (Exception exception) {
            documentRepository.updateStatus(message.documentId(), DocumentStatus.FAILED, exception.getMessage());
            taskRepository.updateStatus(message.taskId(), TaskStatus.FAILED, null, exception.getMessage());
            throw new IllegalStateException("文档摄取处理失败", exception);
        }
    }

    private List<Document> splitBySpringAi(DocumentIngestionMessage message, String text) {
        Document sourceDocument = Document.builder()
                .text(text)
                .metadata(Map.of(
                        "source", "document_ingestion",
                        "workspace_id", message.workspaceId(),
                        "knowledge_base_id", message.knowledgeBaseId().toString(),
                        "document_id", message.documentId().toString(),
                        "storage_path", message.storagePath()
                ))
                .build();

        List<Document> splitDocuments = tokenTextSplitter.split(sourceDocument);
        List<Document> chunks = new ArrayList<>(splitDocuments.size());
        for (int index = 0; index < splitDocuments.size(); index++) {
            chunks.add(splitDocuments.get(index).mutate()
                    .metadata("chunk_index", index)
                    .build());
        }
        return chunks;
    }

    private void deleteDocumentVectors(DocumentIngestionMessage message) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        vectorStore.delete(builder.eq("document_id", message.documentId().toString()).build());
    }
}
