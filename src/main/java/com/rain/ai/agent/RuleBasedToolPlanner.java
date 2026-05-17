package com.rain.ai.agent;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import com.rain.ai.knowledge.KnowledgeBase;
import com.rain.ai.knowledge.KnowledgeBaseService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class RuleBasedToolPlanner {

    private final KnowledgeBaseService knowledgeBaseService;

    public RuleBasedToolPlanner(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    public AgentPlan plan(AgentChatRequest request) {
        String message = request.message().toLowerCase(Locale.ROOT);
        if (containsAny(message, "知识库", "knowledge base", "kb")
                && containsAny(message, "列表", "有哪些", "列出", "查询")) {
            return new AgentPlan(AgentPlanType.TOOL, "knowledge_base.list", Map.of());
        }

        UUID knowledgeBaseId = resolveKnowledgeBaseId(request.knowledgeBaseId());
        if (containsAny(message, "诊断", "运营", "健康", "检查一下", "有没有问题", "风险", "建议")) {
            return new AgentPlan(AgentPlanType.SKILL, "knowledge_base_operator", Map.of(
                    "knowledgeBaseId", knowledgeBaseId.toString(),
                    "question", request.message()
            ));
        }
        if (containsAny(message, "失败", "失败文档", "处理失败")) {
            return new AgentPlan(AgentPlanType.TOOL, "document.failed.list", Map.of(
                    "knowledgeBaseId", knowledgeBaseId.toString()
            ));
        }
        return new AgentPlan(AgentPlanType.TOOL, "rag.ask", Map.of(
                "knowledgeBaseId", knowledgeBaseId.toString(),
                "question", request.message()
        ));
    }

    private boolean containsAny(String message, String... keywords) {
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private UUID resolveKnowledgeBaseId(UUID knowledgeBaseId) {
        if (knowledgeBaseId != null) {
            knowledgeBaseService.getRequired(knowledgeBaseId);
            return knowledgeBaseId;
        }
        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.list();
        if (knowledgeBases.isEmpty()) {
            throw new BizException(ErrorCode.资源不存在, "当前没有可用知识库");
        }
        return knowledgeBases.getFirst().id();
    }
}
