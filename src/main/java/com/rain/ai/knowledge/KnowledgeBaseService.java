package com.rain.ai.knowledge;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public KnowledgeBaseService(KnowledgeBaseRepository knowledgeBaseRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    public KnowledgeBase create(CreateKnowledgeBaseRequest request) {
        KnowledgeBase knowledgeBase = new KnowledgeBase(
                UUID.randomUUID(),
                request.name().trim()
        );
        return knowledgeBaseRepository.save(knowledgeBase);
    }

    public List<KnowledgeBase> list() {
        return knowledgeBaseRepository.findAll();
    }

    public KnowledgeBase getRequired(UUID id) {
        return knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.资源不存在, "知识库不存在"));
    }
}
