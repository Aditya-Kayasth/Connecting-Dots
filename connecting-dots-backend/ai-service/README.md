# AI Service (`ai-service`)

## Role & Responsibilities
An isolated worker microservice responsible for handling multimodal AI processing, parsing unstructured documents, structuring data using Google Gemini Flash models (via Spring AI 2.0.0 `ChatClient`), and managing real-time dynamic text translation.

## Tech Stack
* Spring Boot 4.0.7 / Spring WebFlux
* Spring Cloud 2025.1.2
* Spring AI 2.0.0 (Google GenAI Starter & SDK)
* Google Gemini 3.5 Flash Model Integration
* Upstash QStash Webhook Integration (`/api/v1/ai/webhook`)
* Java 25

## Key Capabilities
* **Multimodal Ingestion:** Ingests raw PDFs, audio memos, and text payloads directly from cloud storage buckets or incoming webhook triggers.
* **Gemini LLM Integration:** Leverages generative AI models via Spring AI 2.0.0 `ChatClient` to convert messy grassroots problem descriptions into standardized structured schemas (`title`, `description`, `domain`).
* **Dynamic Localization:** Provides on-the-fly technical translation utilities for cross-lingual collaboration between grassroots NGOs and technical contributors.

## Port & Integration Points
* **Default Port:** `8082`
* **Upstash Webhook Endpoint:** `POST /api/v1/ai/webhook`
* **Callback Target:** `PUT ${CORE_SERVICE_URL}/api/v1/core/problem-statements/{id}/ai-update`
