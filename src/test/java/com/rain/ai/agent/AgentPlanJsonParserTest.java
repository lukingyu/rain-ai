package com.rain.ai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPlanJsonParserTest {

    private final AgentPlanJsonParser parser = new AgentPlanJsonParser(new ObjectMapper());

    @Test
    void 从模型返回的Json代码块中解析技能计划() {
        String content = """
                ```json
                {
                  "type": "SKILL",
                  "name": "knowledge_base_operator",
                  "arguments": {
                    "knowledgeBaseId": "dbdfca2f-c4cd-4075-82b5-2865bcae1633",
                    "question": "帮我诊断这个知识库"
                  }
                }
                ```
                """;

        AgentPlan plan = parser.parse(content);

        assertThat(plan.type()).isEqualTo(AgentPlanType.SKILL);
        assertThat(plan.name()).isEqualTo("knowledge_base_operator");
        assertThat(plan.arguments())
                .containsEntry("knowledgeBaseId", "dbdfca2f-c4cd-4075-82b5-2865bcae1633")
                .containsEntry("question", "帮我诊断这个知识库");
    }
}
