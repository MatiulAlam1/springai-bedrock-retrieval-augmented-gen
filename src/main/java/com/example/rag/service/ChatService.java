package com.example.rag.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ChatService {

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final ChatClient chatClient;

    @Autowired
    public ChatService(EmbeddingService embeddingService,
                       VectorStoreService vectorStoreService,
                       ChatClient.Builder chatClientBuilder ) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.chatClient = chatClientBuilder
                .defaultAdvisors(/* e.g., new QuestionAnswerAdvisor(yourVectorStore) */)
                .defaultSystem("You are a helpful AI assistant for document queries.")
                .build();
    }

    public void indexDocument(String document) {
        float[] embeddings = embeddingService.generateEmbeddings(document);

        String documentId = "doc_" + System.currentTimeMillis();
        java.util.List<Float> embeddingList = new java.util.ArrayList<>();
        for (float f : embeddings) {
            embeddingList.add(f);
        }

        vectorStoreService.saveEmbeddingWithContent(documentId, embeddingList, document);
    }

    public void indexDocument(MultipartFile file) throws IOException {
        String content = extractTextFromFile(file);
        indexDocument(content);
    }

    private String extractTextFromFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File name is required");
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        switch (extension) {
            case "txt":
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            case "pdf": {
                DocumentReader pdfReader = new PagePdfDocumentReader(new InputStreamResource(file.getInputStream()));
                StringBuilder sb = new StringBuilder();
                pdfReader.read().forEach(doc -> sb.append(doc.getText()).append("\n"));
                return sb.toString().trim();
            }
            case "docx": {
                try (XWPFDocument document = new XWPFDocument(file.getInputStream());
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    return extractor.getText();
                }
            }
            case "doc":
                throw new UnsupportedOperationException("Legacy .doc format not supported. Please use .docx format.");
            default:
                throw new UnsupportedOperationException("Unsupported file type: " + extension);
        }
    }

    public String queryAndChat(String query) {
        float[] queryEmbeddings = embeddingService.generateEmbeddings(query);

        java.util.List<String> similarDocuments = vectorStoreService.querySimilarEmbeddings(queryEmbeddings);

        String context = String.join("\n", similarDocuments);
        String enhancedQuery = "Context:\n" + context + "\n\nQuery:\n" + query;

        return chatClient.prompt(enhancedQuery)
                         .call()
                         .content();
    }
}
