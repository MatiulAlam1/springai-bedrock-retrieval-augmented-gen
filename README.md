
# RAG with Spring AI, AWS Bedrock, and Qdrant

## Overview
The **RAG Spring AI Bedrock** project is a **Spring Boot application** that demonstrates a complete **Retrieval-Augmented Generation (RAG)** pipeline. It integrates:

- **Spring AI** – orchestrates AI workflows  
- **AWS Bedrock** – hosts large language models (LLMs) like **Anthropic Claude 3.5 Sonnet**  
- **Qdrant** – vector database for semantic search and document retrieval  

The workflow is as follows:

1. Users send a chat request or upload a document.  
2. Text is converted into embeddings and matched against documents stored in **Qdrant**.  
3. Relevant documents are retrieved and added as context.  
4. The enriched prompt is sent to **AWS Bedrock** for response generation.  
5. The AI returns a **context-aware answer**.  

This makes the system capable of **knowledge-augmented conversations** and **document-based Q&A**.

## Project Structure
```
rag-spring-ai-bedrock
├── src
│   └── main
│       ├── java/com/example/rag
│       │   ├── RagApplication.java
│       │   ├── config/BedrockConfig.java
│       │   ├── controller/ChatController.java
│       │   ├── service
│       │   │   ├── EmbeddingService.java
│       │   │   ├── VectorStoreService.java
│       │   │   └── ChatService.java
│       │   └── model/ChatRequest.java
│       └── resources
│           ├── application.yml
│           └── static
├── pom.xml
└── README.md
```

## Setup Instructions

1. Clone the Repository:
\`\`\`bash
git clone https://github.com/your-repo/rag-spring-ai-bedrock.git
cd rag-spring-ai-bedrock
\`\`\`

2. Run Qdrant (Vector Database):
\`\`\`bash
docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant
\`\`\`

3. Configure Application Properties (`application.yml`):
\`\`\`yaml
spring:
  ai:
    bedrock:
      anthropic:
        chat:
          model: anthropic.claude-3-5-sonnet-20241022-v2:0
      aws:
        region: us-east-1
        access-key: YOUR_AWS_ACCESS_KEY
        secret-key: YOUR_AWS_SECRET_KEY
    vector-store:
      qdrant:
        host: localhost
        port: 6333
\`\`\`

4. Build the Project:
\`\`\`bash
mvn clean install
\`\`\`

5. Run the Application:
\`\`\`bash
mvn spring-boot:run
\`\`\`

6. Access the API at `/api/chat`.

## REST API Endpoints

### Index Document
POST `/api/chat/index` – Upload a file to add to Qdrant.

Example:
\`\`\`bash
curl -X POST http://localhost:8080/api/chat/index -F "file=@example.pdf"
\`\`\`

### Query Vector DB & Chat
POST `/api/chat/query` – Send a text query to get a context-aware response.

Example:
\`\`\`bash
curl -X POST http://localhost:8080/api/chat/query -H "Content-Type: application/json" -d '"What is Retrieval-Augmented Generation?"'
\`\`\`

## Dependencies
- Spring Boot
- Spring AI (`spring-ai-bedrock-starter`, `spring-ai-qdrant-store-starter`)
- AWS SDK for Java

## Contributing
- Open issues for bugs/features
- Submit pull requests for improvements

## License
MIT License
