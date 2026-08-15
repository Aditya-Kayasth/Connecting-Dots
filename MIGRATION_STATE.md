# Migration State: Connecting-Dots

## 1. Architecture Overview
**Target State:** Highly scalable, decoupled microservices architecture utilizing Strictly Independent Standalone Applications (No coupled multi-module `pom.xml`).
- **API Gateway:** Entry point for all client requests, routing them to the appropriate services.
- **Eureka Server:** Service registry for dynamic discovery.
- **Auth Service:** Centralized authentication and authorization.
- **Core Service:** Main business logic previously housed in the monolith.
- **AI/Python Service:** Specialized service for AI-related operations.
- **Chat Service:** Real-time messaging and chat functionalities.

## 2. Technical Stack
**Required Technologies for New Microservices:**
- **Build System:** Maven (Strictly independent standalone projects)
- **Language:** Java 25 (utilizing Records, Pattern Matching, Virtual Threads)
- **Framework:** Spring Boot 3.3.4
- **Boilerplate Reduction:** Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Slf4j`, etc.)
- **Architecture Principles:** Strict decoupled layered architecture (Repositories, Services, Controllers, Domain Models)

## 3. Completed Steps
- Initialized `MIGRATION_STATE.md` to track migration progress and maintain state.
- **Phase 1 Complete:** Verified `discovery-service` and `api-gateway` were correctly created in a previous step and they successfully compiled. We are explicitly using **Strictly Independent Standalone Applications** with zero compile-time knowledge of each other.
- **Phase 2 Complete:** Created `auth-service` as an isolated microservice. Migrated `User`, `Role`, `UserRepository`, `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, `UserDetailsServiceConfig`, `AuthenticationService`, and `AuthController`. Updated `User` model with detailed NGO and Contributor fields. Mapped `auth-service` route in `api-gateway`. Tests pass locally on H2.

- **Phase 3 Complete:** Extracted the core business logic from the monolith into a brand new `core-service` application. Migrated the `NgoProblemStatement` entity mapping geographic fields (`latitude`, `longitude`, `proximityZone`) and structured lifecycle statuses. Integrated Resilience4j for circuit breaking external logic handling with a graceful fallback. Successfully passed isolated integration tests with H2 database.
- **Phase 4 (AI-Service) Complete:** Created `ai-service` as an isolated Python microservice. Uses FastAPI for endpoints and Celery backed by Redis for asynchronous processing. Migrated legacy AI extraction logic to use xAI's Grok LLM via the `openai` python library. Integrated `pdfplumber` for robust PDF extraction. Verified routing through `api-gateway` on port 8000 and tested all logic locally with mocked endpoints.

## 4. Pending Steps
- [x] Phase 1: Setup Eureka Server and API Gateway microservices.
- [x] Phase 2: Create Auth Service and extract security/authentication logic.
- [x] Phase 3: Decouple and extract Core Service from the monolith.
- [x] Phase 4: Establish AI/Python Service with FastAPI, Celery, and xAI Grok. Establish Chat Service (Pending).
- [ ] Phase 5: Containerize new services, manage inter-service communication, and update Docker/deployment configs.
- [ ] Phase 6: Update `frontend` to route API calls through the API Gateway.

## 5. Environment Variables & Secrets Map
*To be populated as services are spun up and connected to databases, Redis, or API keys.*

| Service | Environment Variables / Secrets Needed |
|---------|----------------------------------------|
| API Gateway | |
| Eureka Server | |
| Auth Service | |
| Core Service | |
| AI/Python Service | `XAI_API_KEY`, `REDIS_URL` |
| Chat Service | |

## 6. Known Issues / Technical Debt
- *Currently none explicitly logged.*
- *Note:* Lombok annotation processing with early-access Java 25 generates `TypeTag :: UNKNOWN` internal compiler errors. Bypassed by providing standard getters and builder patterns directly in `User.java` while compiling `auth-service`.

## 7. Component Validation & Testing Logs

**auth-service (mvn clean test output):**
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.connectingdots.auth.AuthServiceApplicationTests
Hibernate: select u1_0.id from app_user u1_0 where u1_0.email=? fetch first ? rows only
Hibernate: insert into app_user (contribution_count,email,name,ngo_profile_description,password,preferences,role,tenant_id,id) values (?,?,?,?,?,?,?,?,?)
Hibernate: select u1_0.id,u1_0.contribution_count,u1_0.email,u1_0.name,u1_0.ngo_profile_description,u1_0.password,u1_0.preferences,u1_0.role,u1_0.tenant_id from app_user u1_0 where u1_0.email=?
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.060 s -- in com.connectingdots.auth.AuthServiceApplicationTests
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  8.400 s
```

**core-service (mvn clean test output):**
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.connectingdots.core.CoreServiceApplicationTests
Hibernate: insert into ngo_problem_statement (author_id,claimed_by_id,latitude,longitude,ngo_name,proximity_zone,raw_description,status,structured_problem,tech_category,id) values (?,?,?,?,?,?,?,?,?,?,?)
Hibernate: select nps1_0.id,nps1_0.author_id,nps1_0.claimed_by_id,nps1_0.latitude,nps1_0.longitude,nps1_0.ngo_name,nps1_0.proximity_zone,nps1_0.raw_description,nps1_0.status,nps1_0.structured_problem,nps1_0.tech_category from ngo_problem_statement nps1_0 where nps1_0.status=?
Hibernate: select nps1_0.id,nps1_0.author_id,nps1_0.claimed_by_id,nps1_0.latitude,nps1_0.longitude,nps1_0.ngo_name,nps1_0.proximity_zone,nps1_0.raw_description,nps1_0.status,nps1_0.structured_problem,nps1_0.tech_category from ngo_problem_statement nps1_0 where nps1_0.id=?
Hibernate: update ngo_problem_statement set author_id=?,claimed_by_id=?,latitude=?,longitude=?,ngo_name=?,proximity_zone=?,raw_description=?,status=?,structured_problem=?,tech_category=? where id=?
Hibernate: select nps1_0.id,nps1_0.author_id,nps1_0.claimed_by_id,nps1_0.latitude,nps1_0.longitude,nps1_0.ngo_name,nps1_0.proximity_zone,nps1_0.raw_description,nps1_0.status,nps1_0.structured_problem,nps1_0.tech_category from ngo_problem_statement nps1_0 where nps1_0.id=?
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 9.865 s -- in com.connectingdots.core.CoreServiceApplicationTests
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  14.554 s
```

**ai-service (pytest output):**
```text
============================= test session starts =============================
platform win32 -- Python 3.13.13, pytest-9.1.1, pluggy-1.6.0
rootdir: E:\GIT_HUB\Connecting-Dots\ai-service
plugins: anyio-4.14.1
collected 4 items

tests\test_main.py ....                                                  [100%]

======================== 4 passed, 2 warnings in 7.18s ========================
```
