package com.rain.ai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RagRetrievalService {

    private static final int RETRIEVAL_TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.35;

    private final VectorStore vectorStore;
    private final RagCitationMapper citationMapper;

    public RagRetrievalService(VectorStore vectorStore, RagCitationMapper citationMapper) {
        this.vectorStore = vectorStore;
        this.citationMapper = citationMapper;
    }

    public List<RagCitation> retrieveCitations(UUID knowledgeBaseId, String question) {
        FilterExpressionBuilder filter = new FilterExpressionBuilder();
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(RETRIEVAL_TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .filterExpression(filter.eq("knowledge_base_id", knowledgeBaseId.toString()).build())
                .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        return citationMapper.from(documents);
    }
}
