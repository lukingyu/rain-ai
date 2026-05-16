package com.rain.ai.knowledge;

import com.rain.ai.common.exception.BizException;
import com.rain.ai.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final String defaultWorkspaceId;

    public KnowledgeBaseService(
            KnowledgeBaseRepository knowledgeBaseRepository,
            @Value("${rain.ai.workspace.default-id}") String defaultWorkspaceId
    ) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.defaultWorkspaceId = defaultWorkspaceId;
    }

    public KnowledgeBase create(CreateKnowledgeBaseRequest request) {
        Instant now = Instant.now();
        KnowledgeBase knowledgeBase = new KnowledgeBase(
                UUID.randomUUID(),
                defaultWorkspaceId,
                request.name().trim(),
                normalizeDescription(request.description()),
                now,
                now
        );
        return knowledgeBaseRepository.save(knowledgeBase);
    }

    public List<KnowledgeBase> list() {
        return knowledgeBaseRepository.findByWorkspaceId(defaultWorkspaceId);
    }

    public KnowledgeBase getRequired(UUID id) {
        return knowledgeBaseRepository.findById(id)
                .filter(base -> defaultWorkspaceId.equals(base.workspaceId()))
                .orElseThrow(() -> new BizException(ErrorCode.资源不存在, "知识库不存在"));
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
