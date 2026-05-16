package com.rain.ai.rag;

import com.rain.ai.knowledge.DocumentChunk;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptEngine {

    public RagPrompt build(String question, List<DocumentChunk> chunks) {
        String systemPrompt = """
                你是企业知识库问答助手。
                你必须只依据用户给出的参考资料回答问题。
                如果参考资料不足以回答问题，必须明确说明“资料不足”，不要编造。
                回答要结构清晰，优先给出结论，再给出依据。
                """;

        StringBuilder contextBuilder = new StringBuilder();
        for (int index = 0; index < chunks.size(); index++) {
            DocumentChunk chunk = chunks.get(index);
            contextBuilder.append("【资料")
                    .append(index + 1)
                    .append("，文档ID=")
                    .append(chunk.documentId())
                    .append("，分片=")
                    .append(chunk.chunkIndex())
                    .append("】\n")
                    .append(chunk.content())
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
