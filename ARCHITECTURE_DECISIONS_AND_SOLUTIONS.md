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

### 4.2 Stateless Authentication, Role Authorization & Seeded Admin
- **Challenge:** Verifying user identities and authorization roles across distributed microservices without relying on server-side HTTP sessions, while maintaining safe, unhackable administrative access.
- **Solution & Technical Decision:** Implemented stateless JWT authentication using JJWT (`io.jsonwebtoken 0.12.x`) and BCrypt password hashing. Tokens embed subject email, expiration (24h), and user roles (`ROLE_CONTRIBUTOR`, `ROLE_NGO`, `ROLE_ADMIN`). Public self-registration is restricted to `NGO` and `CONTRIBUTOR` roles to prevent public privilege escalation. The default Admin account (`admin@connectingdots.org` / `Admin@1234`) is seeded securely via Flyway (`V10__seed_default_admin_user.sql`) using `ON CONFLICT (email) DO NOTHING`. Dedicated admin endpoints under `/api/v1/core/admin/**` enforce `hasRole('ADMIN')`.

### 4.3 Direct NGO Uploads with Reactive Verification & Trust Badges
- **Challenge:** Requiring pre-approval for NGOs creates friction and delays urgent problem uploads, while allowing unverified NGOs risks volunteer distrust.
- **Solution & Technical Decision:** Implemented a hybrid verification model: NGOs register and post problems immediately (`OPEN`). Admin Dashboard endpoints (`GET /api/v1/core/admin/ngos` and `PUT /api/v1/core/admin/ngos/{id}/verify`) allow Admins to inspect profiles and toggle an `isVerified` flag (persisted via Flyway `V9__add_is_verified_to_ngo_profiles.sql`). Verified NGOs display trust badges on the frontend without blocking initial uploads.

### 4.4 Production-Grade Database Integration Testing
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

---

## 6. Cloud Deployment, Microservices Networking & UX Production Engineering

### 6.1 Explicit Spring WebFlux `CorsWebFilter` Java Bean
- **Challenge:** Spring Cloud Gateway in WebFlux mode can bypass YAML `globalcors` settings when handling browser preflight `OPTIONS` requests from local dev (`http://localhost:3000`) or production (`https://*.vercel.app`), causing `TypeError: Failed to fetch`.
- **Solution & Technical Decision:** Implemented a dedicated `@Configuration` class (`CorsConfig.java`) returning a `CorsWebFilter` bean with `setAllowedOriginPatterns(Arrays.asList("*"))` and `setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"))`. This explicitly attaches `Access-Control-Allow-Origin: *` headers to all preflight `OPTIONS` and API requests.

### 6.2 Explicit Gateway Route Predicate Isolation
- **Challenge:** Writing multi-path predicates on a single YAML line (e.g. `- Path=/api/v1/core/**, /api/v1/auth/**`) caused Spring Cloud Gateway 4.x/5.x to treat the entire string as a single literal pattern including the comma, resulting in `HTTP 404 Not Found` for routes like `/api/v1/core/auth/login`.
- **Solution & Technical Decision:** Separated route paths into distinct, explicit route definitions (`core-service-route`, `auth-service-route`, `ai-service-route`) in `gateway-service`'s `application.yml`, guaranteeing clean path matching.

### 6.3 Direct HTTPS Microservice Target Routing on Render
- **Challenge:** On Render's isolated free container tier, inter-container resolution over internal Docker IPs (`lb://core-service` on port 8081) causes Eureka heartbeat losses and `Cannot execute request on any known server` errors.
- **Solution & Technical Decision:** Configured `gateway-service` route URIs to target direct public HTTPS URLs (`${CORE_SERVICE_URL:https://core-service-9mpw.onrender.com}` and `${AI_SERVICE_URL:https://ai-service-6r96.onrender.com}`), providing 100% reliable cross-container communication.

### 6.4 Java 25 JVM Fast-Boot Optimization for Small Containers
- **Challenge:** Java 25 Spring Boot cold-starts on Render free tier (0.5 CPU shares / 512MB RAM) took 60–90 seconds due to heavy C2 JIT bytecode compilation.
- **Solution & Technical Decision:** Added `-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xverify:none` to Dockerfile `JAVA_OPTS`. Forces JVM to use C1 fast compilation during boot, cutting Spring Boot cold-start time by 50% (from ~90s down to ~25-35s). Extended Gateway Netty `response-timeout` to `120s` to guarantee zero Gateway timeouts.

### 6.5 Core Service Security Chain & JWT Filter Bypass
- **Challenge:** Unauthenticated POST requests to `/api/v1/core/auth/register` returned `HTTP 401 Unauthorized` because Spring Security's default CORS filter intercepted requests before reaching `AuthController`.
- **Solution & Technical Decision:** Added `.cors(cors -> cors.disable())` in `SecurityConfig.java` and implemented `shouldNotFilter` in `JwtAuthenticationFilter.java` to bypass public auth routes (`/api/v1/core/auth/**`), allowing registration and login to proceed seamlessly.

### 6.6 Next.js Serverless Build Tracing vs Standalone Docker Output
- **Challenge:** Using `output: 'standalone'` in `next.config.mjs` caused Vercel serverless builds to fail with `ENOENT: no such file or directory, open '.../next-server.js.nft.json'`. Conversely, pnpm 10 workspace lockfiles caused Vercel package resolution failures.
- **Solution & Technical Decision:** Conditionally applied `output: 'standalone'` only when `DOCKER_BUILD=true` is set. Removed `pnpm-lock.yaml` and `pnpm-workspace.yaml` from `connecting-dots-frontend` to force Vercel to use standard `npm` (`package-lock.json`), ensuring 100% clean Next.js 16 build generation.
