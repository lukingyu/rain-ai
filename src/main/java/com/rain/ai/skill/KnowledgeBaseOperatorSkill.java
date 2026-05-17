package com.rain.ai.skill;

import com.rain.ai.tool.ToolArguments;
import com.rain.ai.tool.ToolExecutionRequest;
import com.rain.ai.tool.ToolExecutionResponse;
import com.rain.ai.tool.ToolExecutionService;
import com.rain.ai.tool.ToolParameter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class KnowledgeBaseOperatorSkill implements SkillHandler {

    private static final String DEFAULT_DIAGNOSIS_QUESTION =
            "请基于当前知识库，总结核心内容、潜在风险和后续运营建议。";

    private final ToolExecutionService toolExecutionService;

    public KnowledgeBaseOperatorSkill(ToolExecutionService toolExecutionService) {
        this.toolExecutionService = toolExecutionService;
    }

    @Override
    public SkillDefinition definition() {
        return new SkillDefinition(
                "knowledge_base_operator",
                "企业知识库运营技能：按固定流程检查失败文档，并基于知识库生成运营诊断建议",
                List.of("document.failed.list", "rag.ask"),
                List.of(
                        new ToolParameter("knowledgeBaseId", "uuid", true, "知识库 ID"),
                        new ToolParameter("question", "string", false, "可选诊断问题，不传时使用默认运营诊断问题")
                )
        );
    }

    @Override
    public SkillExecutionOutcome execute(SkillExecutionContext context) {
        UUID knowledgeBaseId = ToolArguments.requiredUuid(context.arguments(), "knowledgeBaseId");
        String question = optionalString(context.arguments(), "question", DEFAULT_DIAGNOSIS_QUESTION);
        List<SkillStepResult> steps = new ArrayList<>();

        ToolExecutionResponse failedDocuments = toolExecutionService.execute(new ToolExecutionRequest(
                "document.failed.list",
                Map.of("knowledgeBaseId", knowledgeBaseId.toString())
        ));
        steps.add(toStep("检查处理失败的文档", failedDocuments));

        ToolExecutionResponse diagnosis = toolExecutionService.execute(new ToolExecutionRequest(
                "rag.ask",
                Map.of(
                        "knowledgeBaseId", knowledgeBaseId.toString(),
                        "question", question
                )
        ));
        steps.add(toStep("生成知识库运营诊断", diagnosis));

        return new SkillExecutionOutcome("知识库运营技能执行完成，已返回失败文档检查结果和 RAG 诊断结果。", steps);
    }

    private SkillStepResult toStep(String stepName, ToolExecutionResponse response) {
        return new SkillStepResult(
                stepName,
                response.toolName(),
                response.taskId(),
                response.status(),
                response.result()
        );
    }

    private String optionalString(Map<String, Object> arguments, String name, String defaultValue) {
        Object value = arguments.get(name);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return String.valueOf(value);
    }
}
