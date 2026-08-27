# Gemini AI Microservice (`ai-service`)

The `ai-service` microservice is a dedicated worker node responsible for heavyweight artificial intelligence operations. It receives asynchronous webhooks, processes unstructured problem documents (PDFs, text files, audio transcripts) using Google Gemini 3.5 Flash LLM via Spring AI 2.0.0, and calls back `core-service` with structured problem data.

---

## 1. Core Service Overview

- **Port:** `8082`
- **Container Name:** `ai-service`
- **Primary Role:** Multimodal document parsing, AI problem structuring, domain categorization, language translation, and async callback execution.

---

## 2. Tech Stack & Dependencies

- **Language & JDK:** Java 25
- **Framework:** Spring Boot 4.0.7 & Spring AI 2.0.0
- **AI SDK:** Google GenAI Java SDK (`com.google.genai`) & Spring AI Google GenAI Starter
- **Async Execution:** Spring `@Async` & Thread Pool Execution
- **REST Client:** Spring 6 `RestClient`
- **Queue Integration:** Upstash QStash Webhooks

### Key Dependencies
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-google-genai-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.google.genai</groupId>
    <artifactId>google-genai</artifactId>
    <version>0.1.1</version>
</dependency>
```

---

## 3. Inputs, Outputs & Sequence Flow

```text
[Upstash QStash Queue]
        │
        │ 1. POST /api/v1/ai/webhook (Carries IngestionMessage payload)
        ▼
[AiController] ──> [AiProcessingService (@Async)]
                        │
                        ├─ 2. Downloads raw document from sourceFileUrl
                        ├─ 3. Generates structured output via Gemini 3.5 Flash
                        │
                        └─ 4. PUT /api/v1/core/problem-statements/{id}/ai-update
                                │
                                ▼
                        [Core Service updates record status to OPEN]
```

### Ingestion Message (Input)
```json
{
  "problemId": "c4a3b8d1-1234-5678-9abc-def123456789",
  "sourceFileUrl": "https://res.cloudinary.com/demo/image/upload/v1/problem.pdf",
  "sourceType": "PDF"
}
```

### AI Update Payload (Output Callback to `core-service`)
```json
{
  "title": "Clean Water Ingestion System for Rural Communities",
  "description": "Comprehensive document outlining low-cost filtration setup...",
  "domain": "Sustainability",
  "tags": ["water", "filtration", "community"]
}
```

---

## 4. Working Logic Explained Simply

1. **Webhook Receipt**: `AiController` listens on `POST /api/v1/ai/webhook`. When QStash delivers a task, the controller acknowledges immediately with HTTP `200 OK`.
2. **Asynchronous Handoff**: The controller delegates document parsing to `AiProcessingService.processFileAndExtractProblem()` annotated with `@Async`. This releases the HTTP request thread immediately.
3. **Gemini LLM Extraction**: `AiProcessingService` initializes the `com.google.genai.Client` using `${GEMINI_API_KEY}` and invokes `gemini-3.5-flash` to extract structured JSON data.
4. **Callback Update**: Once parsing completes, `AiProcessingService` sends an HTTP `PUT` request to `core-service` (`/api/v1/core/problem-statements/{id}/ai-update`) updating the title, description, domain, and status to `OPEN`.

---

## 5. Key Annotations & Concepts

- **`@EnableAsync` & `@Async`**: Configures Spring background execution pool, allowing `processFileAndExtractProblem` to run asynchronously in a separate worker thread.
- **`@Value("${gemini.api.key}")`**: Injects Google Gemini API key from environment configuration (`Connecting-Dots-v2/.env`).
- **`RestClient`**: Modern non-blocking Spring HTTP client used to execute callback updates back to `core-service`.
- **`ChatClient`**: Spring AI fluid API providing model-agnostic LLM prompting, structured output mapping, and memory management.

---

## 6. Key Engineering Challenges & Architectural Decisions

AI service decoupling, non-blocking webhook processing, Spring AI 2.0.0 LLM abstractions, and async retry handling are documented in the master guide:
👉 **[ARCHITECTURE_DECISIONS_AND_SOLUTIONS.md](../../ARCHITECTURE_DECISIONS_AND_SOLUTIONS.md)**
