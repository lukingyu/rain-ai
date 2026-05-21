package com.rain.ai.knowledge;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentIngestionProcessor {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeDocumentReader documentReader;
    private final TokenTextSplitter tokenTextSplitter;
    private final VectorStore vectorStore;

    public DocumentIngestionProcessor(
            KnowledgeDocumentRepository documentRepository,
            KnowledgeDocumentReader documentReader,
            TokenTextSplitter tokenTextSplitter,
            VectorStore vectorStore
    ) {
        this.documentRepository = documentRepository;
        this.documentReader = documentReader;
        this.tokenTextSplitter = tokenTextSplitter;
        this.vectorStore = vectorStore;
    }

    @Transactional
    public void process(DocumentIngestionMessage message) {
        try {
            documentRepository.updateStatus(message.documentId(), DocumentStatus.PARSING, null);
            List<Document> documents = documentReader.read(message);

            documentRepository.updateStatus(message.documentId(), DocumentStatus.CHUNKING, null);
            List<Document> chunks = splitBySpringAi(documents);
            deleteDocumentVectors(message);

            documentRepository.updateStatus(message.documentId(), DocumentStatus.EMBEDDING, null);
            vectorStore.add(chunks);

            documentRepository.updateStatus(message.documentId(), DocumentStatus.COMPLETED, null);
        } catch (Exception exception) {
            documentRepository.updateStatus(message.documentId(), DocumentStatus.FAILED, exception.getMessage());
            throw new IllegalStateException("文档摄取处理失败", exception);
        }
    }

    private List<Document> splitBySpringAi(List<Document> documents) {
        List<Document> splitDocuments = tokenTextSplitter.split(documents);
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
