package com.bankmind.module.knowledge.service.impl;

import com.bankmind.module.ai.dto.SearchResult;
import com.bankmind.module.ai.service.QdrantService;
import com.bankmind.module.knowledge.repository.DocumentChunkRepository;
import com.bankmind.module.knowledge.service.EmbeddingService;
import com.bankmind.module.knowledge.service.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SemanticSearchServiceImpl implements SemanticSearchService {

    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    private final DocumentChunkRepository documentChunkRepository;

    @Override
    public List<SearchResult> search(String query, int limit) {

        float[] queryVector =
                embeddingService.embed(query);

        List<Long> chunkIds =
                qdrantService.searchSimilar(
                        "bankmind_documents",
                        queryVector,
                        limit
                );

        if (chunkIds.isEmpty()) {
            return List.of();
        }

        return documentChunkRepository
                .findByIdIn(chunkIds)
                .stream()
                .map(chunk ->
                        new SearchResult(
                                chunk.getId(),
                                0.0,
                                chunk.getChunkText(),
                                chunk.getPageNumber()
                        )
                )
                .toList();
    }   // method closes here

}       // class closes here