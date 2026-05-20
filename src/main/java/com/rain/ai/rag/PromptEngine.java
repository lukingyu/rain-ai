package com.rain.ai.rag;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptEngine {

    public RagPrompt build(String question, List<Document> documents) {
        String systemPrompt = """
                你是企业知识库问答助手。
                你必须只依据用户给出的参考资料回答问题。
                如果参考资料不足以回答问题，必须明确说明“资料不足”，不要编造。
                回答要结构清晰，优先给出结论，再给出依据。
                """;

        StringBuilder contextBuilder = new StringBuilder();
        for (int index = 0; index < documents.size(); index++) {
            Document document = documents.get(index);
            contextBuilder.append("【资料")
                    .append(index + 1)
                    .append("，文档ID=")
                    .append(document.getMetadata().get("document_id"))
                    .append("，分片=")
                    .append(document.getMetadata().get("chunk_index"))
                    .append("】\n")
                    .append(document.getText())
                    .append("\n\n");
        }

        String userPrompt = """
                用户问题：
                %s

                参考资料：
                %s
                请基于参考资料回答。
                """.formatted(question, contextBuilder);

        return new RagPrompt(systemPrompt, userPrompt);
    }
}
