package com.bankmind.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantConfig {

    @Value("${bankmind.qdrant.host:localhost}")
    private String host;

    @Value("${bankmind.qdrant.port:6333}")
    private int port;

    @Value("${bankmind.qdrant.api-key:}")
    private String apiKey;

    @Bean
    public QdrantClient qdrantClient() {
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(host, port, false);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            builder.withApiKey(apiKey);
        }
        return new QdrantClient(builder.build());
    }
}
