package com.bankmind.module.knowledge.service;

import java.util.List;

/**
 * Service interface for generating vector embeddings from text.
 * This is designed to be provider-agnostic, supporting mock, OpenAI, Gemini, or local models.
 */
public interface EmbeddingService {

    /**
     * Generate embedding vector for a single text chunk.
     *
     * @param text The text to embed.
     * @return float array representing the embedding vector.
     */
    float[] embed(String text);

    /**
     * Generate embedding vectors for a batch of text chunks.
     *
     * @param texts The list of texts to embed.
     * @return List of float arrays representing the embedding vectors.
     */
    List<float[]> embedAll(List<String> texts);

    /**
     * Get the vector dimension of the active embedding model.
     *
     * @return The dimension size.
     */
    int getDimension();

    /**
     * Get the name of the active embedding provider.
     *
     * @return The active provider name.
     */
    String getProviderName();
}
