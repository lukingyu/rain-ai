package com.rain.ai.knowledge;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class KnowledgeDocumentReader {

    public List<Document> read(DocumentIngestionMessage message) {
        Path storagePath = Path.of(message.storagePath());
        List<Document> documents = new TikaDocumentReader(new FileSystemResource(storagePath)).get();
        List<Document> readableDocuments = documents.stream()
                .filter(document -> document.getText() != null && !document.getText().isBlank())
                .map(document -> withKnowledgeMetadata(document, message))
                .toList();
        if (readableDocuments.isEmpty()) {
            throw new IllegalStateException("文档未解析出可用于向量化的文本");
        }
        return readableDocuments;
    }

    private Document withKnowledgeMetadata(Document document, DocumentIngestionMessage message) {
        return document.mutate()
                .metadata("source", "document_ingestion")
                .metadata("knowledge_base_id", message.knowledgeBaseId().toString())
                .metadata("document_id", message.documentId().toString())
                .metadata("storage_path", message.storagePath())
                .build();
    }
}
