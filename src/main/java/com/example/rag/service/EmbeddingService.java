package com.example.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingService.class);
    private final EmbeddingModel embeddingModel;

    @Autowired
    public EmbeddingService(@Qualifier("titanEmbeddingModel") EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] generateEmbeddings(String inputText) {
        if (inputText == null || inputText.isBlank()) {
            throw new IllegalArgumentException("inputText must not be null/blank");
        }
        try {
            float[] embedding = embeddingModel.embed(inputText);
            LOGGER.debug("Generated embedding with {} dimensions", embedding.length);
            return embedding;
        } catch (Exception e) {
            LOGGER.error("Failed to generate embedding: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }
}
