package com.rain.ai.rag;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiAnswerClient {

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final String openAiApiKey;

    public AiAnswerClient(
            ObjectProvider<ChatModel> chatModelProvider,
            @Value("${spring.ai.openai.api-key:}") String openAiApiKey
    ) {
        this.chatModelProvider = chatModelProvider;
        this.openAiApiKey = openAiApiKey;
    }

    public AiAnswer answer(RagPrompt prompt, List<RagCitation> citations) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel != null && hasRealApiKey()) {
            ChatResponse response = chatModel.call(new Prompt(List.of(
                    new SystemMessage(prompt.systemPrompt()),
                    new UserMessage(prompt.userPrompt())
            )));
            return new AiAnswer(response.getResult().getOutput().getText(), true);
        }
        return new AiAnswer(localAnswer(citations), false);
    }

    private boolean hasRealApiKey() {
        return openAiApiKey != null
                && !openAiApiKey.isBlank()
                && !openAiApiKey.equals("replace-with-your-api-key")
                && !openAiApiKey.equals("test-key");
    }

    private String localAnswer(List<RagCitation> citations) {
        if (citations.isEmpty()) {
            return "资料不足：当前知识库没有召回到能回答该问题的内容。";
        }
        StringBuilder answer = new StringBuilder("根据当前知识库资料，可以得到以下结论：\n");
        for (int index = 0; index < citations.size(); index++) {
            RagCitation citation = citations.get(index);
            answer.append(index + 1)
                    .append(". ")
                    .append(limit(citation.content(), 220))
                    .append("\n");
        }
        answer.append("当前未配置真实 OpenAI API Key，因此这里使用本地降级回答；配置 Key 后会切换为大模型生成。");
        return answer.toString();
    }

    private String limit(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
