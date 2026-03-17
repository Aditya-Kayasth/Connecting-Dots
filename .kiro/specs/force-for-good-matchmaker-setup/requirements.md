# Requirements Document

## Introduction

Connecting-Dots is a Spring Boot 3 backend microservice built with Java 25 and Maven. This spec covers the full initial setup: scaffolding the Maven project structure, configuring the build descriptor (pom.xml) with required dependencies, creating the Spring Boot application entry point, configuring a 100% serverless infrastructure stack (Neon DB, Upstash Redis, Groq LLM API) via environment variables loaded from a `.env` file, defining the domain layer (JPA entity, enum, repository), implementing an AI integration service backed by the Groq LLM API, and exposing a REST API for NGO problem submission and retrieval.

## Glossary

- **Matchmaker_Service**: The Spring Boot 3 microservice named Connecting-Dots.
- **Maven_Wrapper**: The `mvnw` / `mvnw.cmd` scripts and `.mvn/wrapper/` directory that allow the project to be built without a pre-installed Maven installation.
- **POM**: The `pom.xml` Project Object Model file that declares the project's build configuration, dependencies, and plugins.
- **Application_Class**: The Java class annotated with `@SpringBootApplication` that serves as the entry point for the Matchmaker_Service.
- **Application_Properties**: The `src/main/resources/application.properties` file that holds runtime configuration for the Matchmaker_Service.
- **DataSource**: The Neon DB (serverless PostgreSQL) database connection configured via environment variables.
- **Redis_Connection**: The Upstash Redis (serverless Redis with TLS) connection configured via environment variables.
- **Hibernate**: The JPA provider used by Spring Data JPA for ORM and schema management.
- **Env_Loader**: The `me.paulschwarz:spring-dotenv` library that causes Spring Boot to load a `.env` file and expose its entries as Spring environment properties.
- **Groq_API**: The Groq LLM REST API at `https://api.groq.com/openai/v1/chat/completions`, used to process raw NGO problem descriptions.
- **GroqAiService**: The Spring service component in `com.connectingdots.service` that calls the Groq_API and persists results.
- **TechCategory**: The enum in `com.connectingdots.domain` that classifies an NGO problem by technology domain.
- **NgoProblemStatement**: The JPA entity in `com.connectingdots.domain` that persists a structured NGO problem.
- **NgoProblemRepository**: The Spring Data JPA repository in `com.connectingdots.domain` that provides CRUD access to NgoProblemStatement records.
- **ProblemController**: The Spring REST controller in `com.connectingdots.controller` that exposes the `/api/v1/problems` endpoints.

---

## Requirements

### Requirement 1: Maven Project Structure

**User Story:** As a developer, I want a standard Maven directory layout, so that the project follows conventions and integrates with standard tooling.

#### Acceptance Criteria

1. THE Matchmaker_Service SHALL use the standard Maven directory layout (`src/main/java`, `src/main/resources`, `src/test/java`, `src/test/resources`).
2. THE Matchmaker_Service SHALL include a Maven_Wrapper so that the project can be built without a system-level Maven installation.
3. THE Maven_Wrapper SHALL provide both a Unix shell script (`mvnw`) and a Windows batch script (`mvnw.cmd`).

---

### Requirement 2: POM Configuration

**User Story:** As a developer, I want a correctly configured pom.xml, so that all required dependencies and build plugins are available at compile and runtime.

#### Acceptance Criteria

1. THE POM SHALL declare `org.springframework.boot:spring-boot-starter-parent` version `3.x` as the parent to inherit default dependency management.
2. THE POM SHALL set the Java source and target compatibility to Java 25.
3. THE POM SHALL declare a dependency on `spring-boot-starter-web` to enable the embedded web server and REST support.
4. THE POM SHALL declare a dependency on `spring-boot-starter-data-jpa` to enable Spring Data JPA and Hibernate.
5. THE POM SHALL declare a dependency on `org.postgresql:postgresql` as a runtime-scoped driver dependency.
6. THE POM SHALL declare a dependency on `spring-boot-starter-data-redis` to enable Spring Data Redis support.
7. THE POM SHALL declare a dependency on `org.projectlombok:lombok` with `optional` scope to enable compile-time code generation.
8. THE POM SHALL include the `spring-boot-maven-plugin` so that the Matchmaker_Service can be packaged as an executable JAR.
9. THE POM SHALL declare a dependency on `me.paulschwarz:spring-dotenv` so that Spring Boot automatically loads the `.env` file as environment properties at startup.

---

### Requirement 3: Application Entry Point

**User Story:** As a developer, I want a Spring Boot application class, so that the Matchmaker_Service can be started with a standard Java main method.

#### Acceptance Criteria

1. THE Application_Class SHALL be annotated with `@SpringBootApplication`.
2. THE Application_Class SHALL reside in the root package `com.connectingdots`.
3. WHEN the Matchmaker_Service is started, THE Application_Class SHALL invoke `SpringApplication.run` to bootstrap the application context.

---

### Requirement 4: Environment-Based Configuration

**User Story:** As a developer, I want all infrastructure credentials loaded from a `.env` file via environment variables, so that no secrets are hard-coded in source files and the service connects to the serverless infrastructure stack.

#### Acceptance Criteria

1. THE Application_Properties SHALL set `spring.datasource.url` to `${NEON_DB_URL}` so that the Neon DB JDBC URL is resolved from the environment.
2. THE Application_Properties SHALL set `spring.datasource.username` to `${NEON_DB_USERNAME}` so that the Neon DB username is resolved from the environment.
3. THE Application_Properties SHALL set `spring.datasource.password` to `${NEON_DB_PASSWORD}` so that the Neon DB password is resolved from the environment.
4. THE Application_Properties SHALL set `spring.data.redis.host` to `${UPSTASH_REDIS_HOST}` so that the Upstash Redis hostname is resolved from the environment.
5. THE Application_Properties SHALL set `spring.data.redis.port` to `${UPSTASH_REDIS_PORT}` so that the Upstash Redis port is resolved from the environment.
6. THE Application_Properties SHALL set `spring.data.redis.password` to `${UPSTASH_REDIS_PASSWORD}` so that the Upstash Redis password is resolved from the environment.
7. THE Application_Properties SHALL set `spring.data.redis.ssl.enabled` to `true` so that the Redis_Connection uses TLS as required by Upstash.
8. THE Application_Properties SHALL set `groq.api.key` to `${GROQ_API_KEY}` so that the Groq_API key is resolved from the environment.

---

### Requirement 5: Hibernate / JPA Configuration

**User Story:** As a developer, I want Hibernate configured for automatic schema updates and SQL visibility, so that schema changes are applied on startup and queries are visible during development.

#### Acceptance Criteria

1. THE Application_Properties SHALL set `spring.jpa.hibernate.ddl-auto` to `update` so that Hibernate applies incremental schema changes on startup.
2. THE Application_Properties SHALL set `spring.jpa.show-sql` to `true` so that generated SQL statements are printed to the console.
3. THE Application_Properties SHALL set `spring.jpa.properties.hibernate.format_sql` to `true` so that printed SQL is human-readable.

---

### Requirement 6: Redis Connection Configuration

**User Story:** As a developer, I want Redis connection properties sourced from environment variables with TLS enabled, so that the Matchmaker_Service connects securely to Upstash Redis.

#### Acceptance Criteria

1. THE Application_Properties SHALL set `spring.data.redis.host` to `${UPSTASH_REDIS_HOST}`.
2. THE Application_Properties SHALL set `spring.data.redis.port` to `${UPSTASH_REDIS_PORT}`.
3. THE Application_Properties SHALL set `spring.data.redis.password` to `${UPSTASH_REDIS_PASSWORD}`.
4. THE Application_Properties SHALL set `spring.data.redis.ssl.enabled` to `true`.

---

### Requirement 7: Domain Layer

**User Story:** As a developer, I want a domain model with an enum, a JPA entity, and a repository, so that NGO problem statements can be persisted and queried.

#### Acceptance Criteria

1. THE Matchmaker_Service SHALL define a `TechCategory` enum in package `com.connectingdots.domain` with exactly the values `SOFTWARE_WEB`, `DATA_SCIENCE_ML`, `IOT_HARDWARE`, and `PROCESS_AUTOMATION`.
2. THE Matchmaker_Service SHALL define an `NgoProblemStatement` JPA entity in package `com.connectingdots.domain` annotated with `@Entity`, `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, and `@Builder`.
3. THE NgoProblemStatement entity SHALL declare an `id` field of type `UUID` annotated with `@Id` and `@GeneratedValue(strategy = GenerationType.UUID)`.
4. THE NgoProblemStatement entity SHALL declare a `ngoName` field of type `String`.
5. THE NgoProblemStatement entity SHALL declare a `rawDescription` field of type `String` annotated with `@Column(columnDefinition = "TEXT")`.
6. THE NgoProblemStatement entity SHALL declare a `structuredProblem` field of type `String` annotated with `@Column(columnDefinition = "TEXT")`.
7. THE NgoProblemStatement entity SHALL declare a `techCategory` field of type `TechCategory` annotated with `@Enumerated(EnumType.STRING)`.
8. THE NgoProblemStatement entity SHALL declare a `status` field of type `String` with a default value of `"OPEN"`.
9. THE Matchmaker_Service SHALL define an `NgoProblemRepository` interface in package `com.connectingdots.domain` extending `JpaRepository<NgoProblemStatement, UUID>`.

---

### Requirement 8: AI Integration Service

**User Story:** As a developer, I want a service that sends raw NGO problem descriptions to the Groq LLM API and persists the structured result, so that unstructured input is automatically categorised and stored.

#### Acceptance Criteria

1. THE Matchmaker_Service SHALL define a `GroqAiService` Spring-managed component in package `com.connectingdots.service`.
2. THE GroqAiService SHALL use Spring `RestClient` to POST requests to `https://api.groq.com/openai/v1/chat/completions`.
3. THE GroqAiService SHALL use the model identifier `llama3-8b-8192` in every request sent to the Groq_API.
4. THE GroqAiService SHALL include a system prompt that instructs the LLM to act as a Business Analyst, parse the raw description, and return ONLY a JSON object with keys `structuredProblem` (a technical summary string) and `techCategory` (a string that exactly matches one of the TechCategory enum values).
5. THE GroqAiService SHALL expose a `processAndSaveProblem(String ngoName, String rawDescription)` method that calls the Groq_API, maps the JSON response to an NgoProblemStatement, saves the entity via NgoProblemRepository, and returns the saved NgoProblemStatement.
6. WHEN the Groq_API returns a `techCategory` value that does not match any TechCategory enum constant, THE GroqAiService SHALL throw an `IllegalArgumentException` with a descriptive message.

---

### Requirement 9: REST API

**User Story:** As an API consumer, I want REST endpoints to submit NGO problems and retrieve open problems, so that external clients can interact with the Matchmaker_Service over HTTP.

#### Acceptance Criteria

1. THE Matchmaker_Service SHALL define a `ProblemController` Spring REST controller in package `com.connectingdots.controller` with base path `/api/v1/problems`.
2. WHEN a POST request is sent to `/api/v1/problems/submit` with a JSON body containing `ngoName` and `rawDescription`, THE ProblemController SHALL invoke GroqAiService and return the saved NgoProblemStatement with HTTP status 200.
3. WHEN a GET request is sent to `/api/v1/problems/open`, THE ProblemController SHALL return a list of all NgoProblemStatement records whose `status` field equals `"OPEN"`.
4. IF the request body for POST `/api/v1/problems/submit` is missing `ngoName` or `rawDescription`, THEN THE ProblemController SHALL return HTTP status 400.
