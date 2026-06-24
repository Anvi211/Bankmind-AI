package com.bankmind.module.knowledge.service.embedding;

import java.util.List;

/**
 * Abstraction representing a specific embedding provider (e.g., Mock, OpenAI, Gemini, Local).
 */
public interface EmbeddingProvider {

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
     * Get the name of this provider (e.g., "MOCK", "OPENAI").
     *
     * @return The provider name.
     */
    String getProviderName();

    /**
     * Get the vector dimension of this provider's model.
     *
     * @return The dimension size.
     */
    int getDimension();
}
