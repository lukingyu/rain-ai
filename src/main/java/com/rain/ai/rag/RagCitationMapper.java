package com.rain.ai.rag;

import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagCitationMapper {

    public List<RagCitation> from(ChatClientResponse response) {
        if (response == null || response.chatResponse() == null) {
            return List.of();
        }
        List<Document> documents = response.chatResponse()
                .getMetadata()
                .getOrDefault(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS, List.of());
        return from(documents);
    }

    public List<RagCitation> from(List<Document> documents) {
        return documents.stream()
                .map(document -> new RagCitation(
                        String.valueOf(document.getMetadata().get("document_id")),
                        toInt(document.getMetadata().get("chunk_index")),
                        document.getText()
                ))
                .toList();
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
