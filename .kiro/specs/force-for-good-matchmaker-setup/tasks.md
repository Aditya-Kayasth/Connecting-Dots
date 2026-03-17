# Implementation Plan: Connecting-Dots Matchmaker Service Setup

## Overview

Scaffold the complete Maven project structure for the Connecting-Dots Spring Boot 3 microservice, including build configuration, application entry point, runtime properties, and a context load test.

## Tasks

- [x] 1. Create Maven directory structure
  - Create `src/main/java/com/connectingdots/`, `src/main/resources/`, `src/test/java/com/connectingdots/`, and `src/test/resources/` directories (add `.gitkeep` where needed to preserve empty dirs)
  - _Requirements: 1.1_

- [x] 2. Add Maven Wrapper files
  - [x] 2.1 Create `.mvn/wrapper/maven-wrapper.properties` pinning Maven 3.9.x distribution URL
    - _Requirements: 1.2_
  - [x] 2.2 Create `mvnw` Unix shell script (standard Spring Initializr wrapper script, executable)
    - _Requirements: 1.2, 1.3_
  - [x] 2.3 Create `mvnw.cmd` Windows batch script
    - _Requirements: 1.2, 1.3_

- [x] 3. Create pom.xml
  - Declare `org.springframework.boot:spring-boot-starter-parent:3.3.4` as parent
  - Set `<java.version>21</java.version>` (Java 21 LTS — downgraded from 25 for Lombok compatibility)
  - Add `maven-compiler-plugin` with `<release>21</release>` and `annotationProcessorPaths` for `lombok:1.18.34`
  - Add dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `postgresql` (runtime), `spring-boot-starter-data-redis`, `lombok` (optional), `spring-boot-starter-test` (test)
  - Add `spring-boot-maven-plugin` with Lombok exclusion
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8_

- [x] 4. Create application entry point
  - [x] 4.1 Create `src/main/java/com/connectingdots/ConnectingDotsApplication.java`
    - Annotate with `@SpringBootApplication`
    - Implement `main` method calling `SpringApplication.run(ConnectingDotsApplication.class, args)`
    - _Requirements: 3.1, 3.2, 3.3_

- [x] 5. Create application.properties
  - Create `src/main/resources/application.properties` with all required keys:
    - `spring.datasource.url=jdbc:postgresql://localhost:5432/connecting_dots`
    - `spring.datasource.username=postgres`
    - `spring.datasource.password=password`
    - `spring.datasource.driver-class-name=org.postgresql.Driver`
    - `spring.jpa.hibernate.ddl-auto=update`
    - `spring.jpa.show-sql=true`
    - `spring.jpa.properties.hibernate.format_sql=true`
    - `spring.data.redis.host=localhost`
    - `spring.data.redis.port=6379`
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3, 6.1, 6.2_

- [x] 6. Create Spring Boot context load test
  - [x] 6.1 Create `src/test/java/com/connectingdots/ConnectingDotsApplicationTests.java`
    - Annotate with `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)`
    - Use `@TestPropertySource` to override datasource and Redis with in-memory/mock values, or use `spring.autoconfigure.exclude` to skip DataSource and Redis auto-configuration
    - Add a single `contextLoads()` test method that asserts the context starts without exception
    - Add comment: `// Feature: force-for-good-matchmaker-setup, Example 4: Spring application context loads successfully`
    - _Requirements: 3.3_

- [x] 7. Add spring-dotenv dependency to pom.xml and configure Lombok annotation processor
  - Added `me.paulschwarz:spring-dotenv:3.0.0` as a compile-scoped dependency in `pom.xml`
  - Added `maven-compiler-plugin` with `annotationProcessorPaths` for `lombok:1.18.34` — required for Java 25 to process `@Builder`, `@Data`, etc.
  - _Requirements: 2.9_

- [x] 8. Rewrite application.properties to use env-var placeholders
  - Replace `src/main/resources/application.properties` with the following content:
    - `spring.datasource.url=${NEON_DB_URL}`
    - `spring.datasource.username=${NEON_DB_USERNAME}`
    - `spring.datasource.password=${NEON_DB_PASSWORD}`
    - `spring.jpa.hibernate.ddl-auto=update`
    - `spring.jpa.show-sql=true`
    - `spring.jpa.properties.hibernate.format_sql=true`
    - `spring.data.redis.host=${UPSTASH_REDIS_HOST}`
    - `spring.data.redis.port=${UPSTASH_REDIS_PORT}`
    - `spring.data.redis.password=${UPSTASH_REDIS_PASSWORD}`
    - `spring.data.redis.ssl.enabled=true`
    - `groq.api.key=${GROQ_API_KEY}`
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 5.1, 5.2, 5.3, 6.1, 6.2, 6.3, 6.4_

- [x] 9. Create domain layer
  - [x] 9.1 Create `TechCategory` enum in `src/main/java/com/connectingdots/domain/TechCategory.java`
  - [x] 9.2 Create `NgoProblemStatement` entity in `src/main/java/com/connectingdots/domain/NgoProblemStatement.java`
    - Refactored to plain Java POJO (no Lombok) — manual getters, setters, no-args constructor, full-args constructor
  - [x] 9.3 Create `NgoProblemRepository` in `src/main/java/com/connectingdots/domain/NgoProblemRepository.java`
    - `public interface NgoProblemRepository extends JpaRepository<NgoProblemStatement, UUID>`
    - `findByStatus(String status)` derived query method
    - `save()` inherited from `JpaRepository` — confirmed correct

- [x] 10. Create GroqAiService in `src/main/java/com/connectingdots/service/GroqAiService.java`
  - Uses `RestClient` with Groq base URL and Bearer auth
  - Model: `llama-3.1-8b-instant` (updated from llama3-8b-8192 to fix 400 Bad Request)
  - Entity instantiated via full-args constructor (no builder — Lombok removed)
  - `repository.save()` call confirmed correct via `JpaRepository` inheritance

- [x] 11. Create ProblemController
  - [x] 11.1 Create `ProblemSubmitRequest` record with `@NotBlank String ngoName` and `@NotBlank String rawDescription`
  - [x] 11.2 Create `ProblemController` in `src/main/java/com/connectingdots/controller/ProblemController.java`

- [x] 12. Update ConnectingDotsApplicationTests to work with new auto-configuration exclusions

- [ ] 13. Final checkpoint — Ensure all tests pass
  - Run `./mvnw test` and confirm all tests pass. Ask the user if any questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Secrets are loaded from `.env` at startup via `spring-dotenv`; never hard-code credentials
- The context load test must exclude DataSource/Redis/JPA auto-configuration so it passes without live infrastructure
- Property tests validate universal behavioral properties (see design document Properties 1–6)
