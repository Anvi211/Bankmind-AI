package com.bankmind.module.knowledge.service.embedding;

import com.bankmind.module.knowledge.service.EmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of EmbeddingService that delegates calls to the active EmbeddingProvider
 * chosen via the application properties configuration.
 */
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingProvider activeProvider;

    public EmbeddingServiceImpl(
            List<EmbeddingProvider> providers,
            @Value("${bankmind.embedding.provider:MOCK}") String configuredProvider) {

        this.activeProvider = providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(configuredProvider))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported embedding provider: '" + configuredProvider + 
                        "'. Supported providers: MOCK, OPENAI. Check your configuration."));
    }

    @Override
    public float[] embed(String text) {
        return activeProvider.embed(text);
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        return activeProvider.embedAll(texts);
    }

    @Override
    public int getDimension() {
        return activeProvider.getDimension();
    }

    @Override
    public String getProviderName() {
        return activeProvider.getProviderName();
    }
}
