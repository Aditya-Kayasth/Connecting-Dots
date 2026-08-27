# Connecting Dots - Key Engineering Challenges, Technical Decisions & System Solutions

This document highlights the major technical decisions, architectural trade-offs, edge-case solutions, and engineering challenges solved during the development of the **Connecting Dots** microservices backend platform.

---

## 1. System Architecture & High-Level Design

### 1.1 Architectural Pattern: Microservices Decoupling
- **Challenge:** Unstructured AI document processing and Gemini LLM calls take several seconds and are memory/CPU intensive. In a monolithic backend, heavy document ingestion spikes would starve database thread pools and degrade core HTTP response times for user authentication, messaging, and profile discovery.
- **Solution & Technical Decision:** Decoupled the platform into 4 dedicated microservices: `eureka-server` (Registry), `gateway-service` (Edge Routing), `core-service` (Business & DB Transactions), and `ai-service` (Async AI Worker). Heavy AI operations run in isolation, guaranteeing high availability and low latency for core transactional services.

### 1.2 Asynchronous Ingestion & Retries via Serverless Queues
- **Challenge:** Executing multi-second LLM processing synchronously during HTTP requests risks gateway timeouts (HTTP 504) and poor user experience.
- **Solution & Technical Decision:** Integrated Upstash QStash serverless HTTP queues. When an NGO submits a problem statement, `core-service` persists the initial record (`PROCESSING`), publishes an asynchronous task to QStash, and immediately returns HTTP `201 Created`. QStash triggers `ai-service` via an HTTP webhook asynchronously and manages exponential backoff retries if `ai-service` is temporarily unavailable.

### 1.3 Guest Exploration vs. Authenticated Mutation Enforcement
- **Challenge:** The platform must allow guest visitors to freely explore problem statements, NGO directories, and contributor profiles without logging in, while strictly securing write/mutation operations (`POST`, `PUT`, `DELETE`).
- **Solution & Technical Decision:** Implemented granular HTTP method matching in `core-service`'s `SecurityConfig`. Public discovery `GET` routes are marked `permitAll()`. Mutation endpoints enforce JWT authentication and use `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)` to ensure unauthenticated write attempts return an explicit HTTP 401 Unauthorized response instead of 403 Forbidden.

### 1.4 Single-Source-of-Truth Redis Caching & Rate Limiting
- **Challenge:** Preventing split-brain rate-limiting states and inconsistent user session caching across multiple microservices.
- **Solution & Technical Decision:** Standardized all caching and rate-limiting infrastructure on a single local Docker Redis container (`redis-cache:6379`). Both `gateway-service` and `core-service` connect to this centralized Redis instance, ensuring consistent token-bucket rate limiting without regional cache fragmentation.

### 1.5 Database Schema Lifecycle & Evolution
- **Challenge:** Maintaining relational database schema consistency across development, staging, and production without manual SQL executions.
- **Solution & Technical Decision:** Integrated Flyway database migrations (`V1` through `V8`) inside `core-service`. On service boot, Flyway automatically checks and applies pending SQL scripts against Neon PostgreSQL, guaranteeing version-controlled, reproducible schema migrations.

---

## 2. Service Discovery & Registry (`eureka-server`)

### 2.1 Dynamic Container IP Resolution
- **Challenge:** In containerized environments (Docker Compose / Kubernetes), container IP addresses change dynamically on restarts or scaling events. Hardcoding IPs in service configuration leads to brittle deployments.
- **Solution & Technical Decision:** Implemented Spring Cloud Netflix Eureka Server (`eureka-server` on port `8761`). Microservices dynamically register their IP and port upon startup and send periodic heartbeats every 30 seconds. Downstream services look up network locations by service name (e.g., `lb://core-service`).

### 2.2 Client-Side Load Balancing
- **Challenge:** Distributing incoming API Gateway traffic efficiently across multiple healthy instances of `core-service` or `ai-service`.
- **Solution & Technical Decision:** Configured `gateway-service` with Spring Cloud `DiscoveryClient` and `lb://` URI schemes. The gateway queries Eureka's live registry and load-balances requests across registered instances automatically.

### 2.3 Standalone Eureka Registry Configuration
- **Challenge:** Preventing a single-node Eureka registry from attempting to register with itself or fetch external peer registry data.
- **Solution & Technical Decision:** Configured `eureka.client.register-with-eureka: false` and `eureka.client.fetch-registry: false` in `eureka-server`'s `application.yml`, optimizing configuration for standalone deployment environments.

---

## 3. Edge Routing, CORS & Rate Limiting (`gateway-service`)

### 3.1 Reactive Non-Blocking Gateway Architecture
- **Challenge:** Traditional thread-per-request blocking gateway frameworks (like Tomcat or Zuul 1.x) saturate under high concurrent traffic.
- **Solution & Technical Decision:** Selected Spring Cloud Gateway built on Spring WebFlux and Project Reactor (Netty). The non-blocking event-loop model handles thousands of concurrent client requests with minimal CPU and memory overhead.

### 3.2 IP-Based Token-Bucket Traffic Throttling
- **Challenge:** Protecting backend services from Denial of Service (DoS) attacks and brute-force traffic spikes.
- **Solution & Technical Decision:** Implemented a reactive `RequestRateLimiter` filter backed by Redis using the Token-Bucket algorithm. Configured `#{@ipKeyResolver}` to resolve client IP addresses, enforcing a replenish rate of 10 requests/sec and a burst capacity of 20. Requests exceeding the limit immediately receive HTTP `429 Too Many Requests`.

### 3.3 Centralized CORS Security Policy
- **Challenge:** Managing Cross-Origin Resource Sharing (CORS) rules individually across multiple microservices leads to redundant configuration and security oversights.
- **Solution & Technical Decision:** Consolidated CORS handling at `gateway-service`. Global CORS rules specify allowed origins, HTTP methods (`GET`, `POST`, `PUT`, `DELETE`), and headers, insulating downstream services from CORS management.

---

## 4. Core Business Domain & Data Security (`core-service`)

### 4.1 Thread-Isolated Messaging between NGOs and Contributors
- **Challenge:** An NGO may have multiple active problem statements assigned to different contributors. Message threads must remain strictly private between the NGO and each assigned contributor.
- **Solution & Technical Decision:** Bound messages directly to a unique `application_id` (`application_messages` table). `MessageService` validates that the authenticated sender is either the specific contributor attached to that application or the NGO owning the problem statement. Contributors cannot view or post to chat threads outside their assigned application ID.

### 4.2 Stateless Authentication & Role Authorization
- **Challenge:** Verifying user identities and authorization roles across distributed microservices without relying on server-side HTTP sessions.
- **Solution & Technical Decision:** Implemented stateless JWT authentication using JJWT (`io.jsonwebtoken 0.12.x`) and BCrypt password hashing. Tokens embed subject email, expiration (24h), and user roles (`ROLE_CONTRIBUTOR`, `ROLE_NGO`, `ROLE_ADMIN`). `JwtAuthenticationFilter` intercepts requests, validates the signature, and injects `GrantedAuthority` into Spring's `SecurityContextHolder`.

### 4.3 Production-Grade Database Integration Testing
- **Challenge:** In-memory databases (like H2) often fail to catch PostgreSQL-specific syntax errors, column constraints, or transaction locking issues during automated testing.
- **Solution & Technical Decision:** Configured Testcontainers in `core-service` integration test suites (`SecurityAccessIntegrationTest`). Tests spin up a real PostgreSQL Docker container during execution, ensuring test assertions accurately mirror production Neon PostgreSQL behavior.

---

## 5. Gemini AI Multimodal Processing (`ai-service`)

### 5.1 Non-Blocking Webhook Processing with Thread Pooling
- **Challenge:** QStash webhook HTTP calls require immediate response acknowledgment to prevent queue timeouts, but LLM processing requires several seconds.
- **Solution & Technical Decision:** `AiController` acknowledges the incoming QStash webhook immediately with HTTP `200 OK` and delegates processing to `AiProcessingService` annotated with `@Async`. Spring's background worker thread pool executes file downloading, Gemini LLM parsing, and callback execution asynchronously.

### 5.2 Standardized LLM Structuring via Spring AI 2.0.0
- **Challenge:** Raw HTTP calls to LLM APIs produce unstructured text strings requiring brittle regex or manual JSON parsing.
- **Solution & Technical Decision:** Leveraged Spring AI 2.0.0 `ChatClient` with Google Gemini 3.5 Flash. Spring AI's structured output converters automatically transform Gemini LLM responses into strongly typed DTOs containing title, description, domain, and tags.

### 5.3 Fail-Safe Asynchronous Callback & Retry Mechanism
- **Challenge:** Ensuring `core-service` database records are updated reliably after AI document extraction completes.
- **Solution & Technical Decision:** Upon completion, `ai-service` uses Spring 6 `RestClient` to issue an HTTP `PUT` callback (`/api/v1/core/problem-statements/{id}/ai-update`). If `core-service` is temporarily busy, QStash retry policies manage task re-delivery, ensuring reliable data synchronization across services.
