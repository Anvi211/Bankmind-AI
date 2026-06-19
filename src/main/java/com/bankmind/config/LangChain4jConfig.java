package com.bankmind.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChain4jConfig {

    @Value("${bankmind.langchain4j.openai.api-key:mock-openai-key}")
    private String apiKey;

    @Value("${bankmind.langchain4j.openai.chat-model-name:gpt-4o}")
    private String chatModelName;

    @Value("${bankmind.langchain4j.openai.embedding-model-name:text-embedding-3-small-v1}")
    private String embeddingModelName;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(chatModelName)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
