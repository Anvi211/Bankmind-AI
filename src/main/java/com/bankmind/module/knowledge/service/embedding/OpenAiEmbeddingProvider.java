package com.bankmind.module.knowledge.service.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAI embedding provider implementing EmbeddingProvider using the LangChain4j EmbeddingModel.
 */
@Component
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingModel embeddingModel;

    public OpenAiEmbeddingProvider(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text).content().vector();
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        List<TextSegment> segments = texts.stream()
                .map(TextSegment::from)
                .collect(Collectors.toList());

        return embeddingModel.embedAll(segments).content().stream()
                .map(Embedding::vector)
                .collect(Collectors.toList());
    }

    @Override
    public String getProviderName() {
        return "OPENAI";
    }

    @Override
    public int getDimension() {
        // Default dimension for text-embedding-3-small-v1 is 1536
        return 1536;
    }
}
