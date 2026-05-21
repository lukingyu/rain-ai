package com.rain.ai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagGroundingEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RagGroundingEvaluator.class);

    public RagGroundingEvaluation evaluate(
            ChatModel chatModel,
            String question,
            String answer,
            List<RagCitation> citations
    ) {
        if (citations.isEmpty()) {
            return RagGroundingEvaluation.unavailable("没有召回到参考片段，无法完成依据性自检");
        }

        try {
            RagGroundingEvaluation evaluation = ChatClient.builder(chatModel)
                    .build()
                    .prompt()
                    .system("""
                            你是 RAG 回答依据性审查器。
                            你的任务是判断“回答”是否被“参考片段”充分支撑。
                            只检查依据性，不评价语言风格。
                            如果回答包含参考片段没有支撑的事实、数字、结论或承诺，grounded 必须为 false。
                            unsupportedClaims 只列出没有依据的关键结论。
                            必须输出结构化 JSON，不要输出 Markdown。
                            """)
                    .user("""
                            用户问题：
                            %s

                            模型回答：
                            %s

                            参考片段：
                            %s

                            请完成依据性审查。
                            """.formatted(question, answer, formatCitations(citations)))
                    .call()
                    .entity(RagGroundingEvaluation.class);
            if (evaluation == null) {
                return RagGroundingEvaluation.unavailable("模型未返回依据性自检结果");
            }
            return evaluation;
        } catch (Exception exception) {
            LOGGER.warn("RAG 回答依据性自检失败", exception);
            return RagGroundingEvaluation.unavailable("依据性自检失败：" + exception.getMessage());
        }
    }

    private String formatCitations(List<RagCitation> citations) {
        StringBuilder builder = new StringBuilder();
        for (RagCitation citation : citations) {
            builder.append("[documentId=")
                    .append(citation.documentId())
                    .append(", chunkIndex=")
                    .append(citation.chunkIndex())
                    .append("]\n")
                    .append(citation.content())
                    .append("\n\n");
        }
        return builder.toString();
    }
}
