package com.bankmind.module.knowledge.service.embedding;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock embedding provider for local development. Generates a deterministic unit-normalized
 * vector of size 1536 based on the hash of the text, bypassing external API calls.
 */
@Component
public class MockEmbeddingProvider implements EmbeddingProvider {

    private static final int DIMENSION = 1536;

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSION];
        // Hash code serves as a seed so that the same text generates the exact same mock vector.
        long seed = text != null ? text.hashCode() : 0L;
        Random random = new Random(seed);

        for (int i = 0; i < DIMENSION; i++) {
            // Generate in range [-1.0, 1.0]
            vector[i] = random.nextFloat() * 2 - 1;
        }

        normalize(vector);
        return vector;
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        List<float[]> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    @Override
    public String getProviderName() {
        return "MOCK";
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }

    /**
     * Normalize the vector to unit length (L2 norm = 1.0).
     */
    private void normalize(float[] vector) {
        double sumSquare = 0;
        for (float val : vector) {
            sumSquare += val * val;
        }
        float norm = (float) Math.sqrt(sumSquare);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }
}
