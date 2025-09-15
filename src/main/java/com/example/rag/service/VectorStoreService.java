package com.example.rag.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class VectorStoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VectorStoreService.class);
    
    @Value("${qdrant.url}")
    private String qdrantUrl;
    
    @Value("${qdrant.collection.name}")
    private String collectionName;
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public VectorStoreService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @jakarta.annotation.PostConstruct
    public void initializeCollection() {
        try {
            createQdrantCollection();
            LOGGER.info("Qdrant collection '{}' initialized successfully at {}", collectionName, qdrantUrl);
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize Qdrant collection: {}", e.getMessage());
        }
    }

    private void createQdrantCollection() throws Exception {
        // Create collection structure
        Map<String, Object> collectionConfig = new HashMap<>();
        collectionConfig.put("vectors", Map.of(
            "size", 384,
            "distance", "Cosine"
        ));

        String jsonPayload = objectMapper.writeValueAsString(collectionConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(qdrantUrl + "/collections/" + collectionName))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200 && response.statusCode() != 409) { // 409 = already exists
            throw new RuntimeException("Failed to create collection: " + response.body());
        }
    }

    public void saveEmbedding(String id, List<Float> embedding) {
        saveEmbeddingWithContent(id, embedding, "Document content for ID: " + id);
    }

    public void saveEmbeddingWithContent(String id, List<Float> embedding, String content) {
        try {
            LOGGER.info("Saving embedding to Qdrant - ID: {} (size: {}, content length: {})", 
                       id, embedding.size(), content.length());
            
            // Create point structure for Qdrant
            Map<String, Object> point = new HashMap<>();
            point.put("id", UUID.randomUUID().toString());
            point.put("vector", embedding);
            
            // Add metadata payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("document_id", id);
            payload.put("content", content);
            payload.put("timestamp", System.currentTimeMillis());
            point.put("payload", payload);
            
            // Wrap in points array
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("points", List.of(point));
            
            String jsonPayload = objectMapper.writeValueAsString(requestBody);
            
            // Send upsert request to Qdrant
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(qdrantUrl + "/collections/" + collectionName + "/points"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                LOGGER.info("Successfully saved embedding to Qdrant: {}", id);
            } else {
                throw new RuntimeException("Qdrant upsert failed: " + response.body());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to save embedding to Qdrant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save embedding to Qdrant", e);
        }
    }

    public List<Float> queryEmbedding(String id) {
        // Implementation for querying specific embedding by ID
        // This would require implementing a get by ID operation
        return new ArrayList<>();
    }

    public void storeEmbeddings(float[] embeddings) {
        // Convert float array to List<Float> and store with generated ID
        List<Float> embeddingList = new ArrayList<>();
        for (float f : embeddings) {
            embeddingList.add(f);
        }
        String id = "embedding_" + System.currentTimeMillis();
        saveEmbedding(id, embeddingList);
    }

    public List<String> querySimilarEmbeddings(float[] queryEmbeddings) {
        try {
            LOGGER.info("Querying similar embeddings from Qdrant");
            
            // Convert float[] to List<Float>
            List<Float> queryVector = new ArrayList<>();
            for (float f : queryEmbeddings) {
                queryVector.add(f);
            }
            
            // Create search request for Qdrant
            Map<String, Object> searchRequest = new HashMap<>();
            searchRequest.put("vector", queryVector);
            searchRequest.put("limit", 5);
            searchRequest.put("with_payload", true);
            
            String jsonPayload = objectMapper.writeValueAsString(searchRequest);
            
            // Send search request to Qdrant
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(qdrantUrl + "/collections/" + collectionName + "/points/search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // Parse Qdrant response
                JsonNode responseJson = objectMapper.readTree(response.body());
                JsonNode results = responseJson.path("result");
                
                List<String> similarDocuments = new ArrayList<>();
                for (JsonNode result : results) {
                    String content = result.path("payload").path("content").asText();
                    double score = result.path("score").asDouble();
                    similarDocuments.add(content + " (similarity: " + String.format("%.3f", score) + ")");
                }
                
                LOGGER.info("Found {} similar documents from Qdrant", similarDocuments.size());
                return similarDocuments;
            } else {
                throw new RuntimeException("Qdrant search failed: " + response.body());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to query Qdrant: {}", e.getMessage(), e);
            // Return fallback results in case of error
            return List.of(
                "Error retrieving from Qdrant. Using fallback content.",
                "Cloud computing provides scalability and cost efficiency.",
                "Modern applications benefit from microservices architecture."
            );
        }
    }
}