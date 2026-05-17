package com.rain.ai.skill;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SkillRegistry {

    public List<SkillDefinition> listDefinitions() {
        return List.of(new SkillDefinition(
                "knowledge_base_operator",
                "企业知识库运营技能：可以列出知识库、检查失败文档，并基于知识库回答问题",
                List.of("knowledge_base.list", "document.failed.list", "rag.ask")
        ));
    }
}
