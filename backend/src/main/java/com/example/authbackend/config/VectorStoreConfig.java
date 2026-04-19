package com.example.authbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VectorStoreConfig {

    @Value("${chroma.host:localhost}")
    private String chromaHost;

    @Value("${chroma.port:8000}")
    private int chromaPort;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.embedding-model:nomic-embed-text}")
    private String embeddingModel;

    @Value("${rag.similarity-threshold:1.5}")
    private double similarityThreshold;

    public String getChromaUrl() {
        return "http://" + chromaHost + ":" + chromaPort;
    }

    public String getOllamaUrl() {
        return ollamaBaseUrl;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }
}
