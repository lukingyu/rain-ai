package com.rain.ai.agent.memory;

import java.util.List;

public record ConversationSummaryDraft(
        String summary,
        List<String> facts,
        List<String> preferences,
        List<String> openQuestions
) {
}
