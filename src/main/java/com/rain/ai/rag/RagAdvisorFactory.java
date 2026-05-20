package com.rain.ai.rag;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RagAdvisorFactory {

    private static final int RETRIEVAL_TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.35;
    private static final PromptTemplate RAG_PROMPT_TEMPLATE = new PromptTemplate("""
            {query}

            下面是从知识库召回的参考资料：
            ---------------------
            {question_answer_context}
            ---------------------

            请只依据参考资料回答问题。
            如果参考资料不足以回答，必须明确说明“资料不足”，不要编造。
            回答时先给结论，再给关键依据。
            """);

    private final VectorStore vectorStore;

    public RagAdvisorFactory(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public QuestionAnswerAdvisor forKnowledgeBase(UUID knowledgeBaseId) {
        FilterExpressionBuilder filter = new FilterExpressionBuilder();
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(RETRIEVAL_TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .filterExpression(filter.eq("knowledge_base_id", knowledgeBaseId.toString()).build())
                .build();

        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .promptTemplate(RAG_PROMPT_TEMPLATE)
                .build();
    }
}
