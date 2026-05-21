package com.rain.ai.knowledge;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeDocumentQueryService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocumentRepository documentRepository;

    public KnowledgeDocumentQueryService(
            KnowledgeBaseService knowledgeBaseService,
            KnowledgeDocumentRepository documentRepository
    ) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentRepository = documentRepository;
    }

    public List<KnowledgeDocumentListItem> list(UUID knowledgeBaseId) {
        knowledgeBaseService.getRequired(knowledgeBaseId);
        return documentRepository.findByKnowledgeBaseId(knowledgeBaseId).stream()
                .map(KnowledgeDocumentListItem::from)
                .toList();
    }
}
