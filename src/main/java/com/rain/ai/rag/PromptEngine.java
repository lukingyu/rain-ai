package com.rain.ai.rag;

import org.springframework.stereotype.Component;

@Component
public class PromptEngine {

    public RagPrompt build(String question, PromptContext context) {
        String systemPrompt = """
                你是企业知识库问答助手。
                你必须只依据用户给出的参考资料回答问题。
                如果参考资料不足以回答问题，必须明确说明“资料不足”，不要编造。
                回答要结构清晰，优先给出结论，再给出依据。
                涉及具体事实时，必须标注资料编号，例如【资料1】。
                """;

        StringBuilder contextBuilder = new StringBuilder();
        for (PromptContextSegment segment : context.segments()) {
            contextBuilder.append("【资料")
                    .append(segment.citationIndex())
                    .append("，文档ID=")
                    .append(segment.documentId())
                    .append("，分片=")
                    .append(segment.chunkIndex())
                    .append("，已截断=")
                    .append(segment.truncated())
                    .append("】\n")
                    .append(segment.content())
                    .append("\n\n");
        }

        String userPrompt = """
                用户问题：
                %s

                参考资料：
                %s

                上下文预算：
                已使用 %d/%d 个估算 token。

                请基于参考资料回答。
                """.formatted(question, contextBuilder, context.usedTokenCount(), context.tokenBudget());

        return new RagPrompt(systemPrompt, userPrompt);
    }
}
