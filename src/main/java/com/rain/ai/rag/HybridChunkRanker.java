package com.rain.ai.rag;

import com.rain.ai.knowledge.DocumentChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class HybridChunkRanker {

    private static final int RRF_K = 60;
    private static final double VECTOR_WEIGHT = 1.0;
    private static final double KEYWORD_WEIGHT = 0.85;

    public List<DocumentChunk> fuse(
            List<DocumentChunk> vectorCandidates,
            List<DocumentChunk> keywordCandidates,
            int limit
    ) {
        Map<UUID, RankedChunk> rankedChunks = new LinkedHashMap<>();
        accumulate(rankedChunks, vectorCandidates, VECTOR_WEIGHT);
        accumulate(rankedChunks, keywordCandidates, KEYWORD_WEIGHT);

        return rankedChunks.values().stream()
                .sorted(Comparator.comparingDouble(RankedChunk::score).reversed()
                        .thenComparingInt(RankedChunk::bestRank)
                        .thenComparing(value -> value.chunk().documentId())
                        .thenComparingInt(value -> value.chunk().chunkIndex()))
                .limit(limit)
                .map(RankedChunk::chunk)
                .toList();
    }

    private void accumulate(
            Map<UUID, RankedChunk> rankedChunks,
            List<DocumentChunk> candidates,
            double weight
    ) {
        for (int index = 0; index < candidates.size(); index++) {
            DocumentChunk chunk = candidates.get(index);
            int rank = index + 1;
            double score = weight / (RRF_K + rank);
            rankedChunks.compute(
                    chunk.id(),
                    (id, current) -> current == null
                            ? new RankedChunk(chunk, score, rank)
                            : current.add(score, rank)
            );
        }
    }

    private record RankedChunk(
            DocumentChunk chunk,
            double score,
            int bestRank
    ) {

        private RankedChunk add(double addedScore, int rank) {
            return new RankedChunk(
                    chunk,
                    score + addedScore,
                    Math.min(bestRank, rank)
            );
        }
    }
}
