# Core Business Microservice (`core-service`)

The `core-service` microservice is the central transactional engine of the Connecting Dots platform. It handles user authentication, profile creation, problem statement management, project applications, messaging threads, and review ratings while maintaining relational data persistence in Neon PostgreSQL.

---

## 1. Core Service Overview

- **Port:** `8081`
- **Container Name:** `core-service`
- **Primary Role:** User authentication, domain entity persistence, transactional workflow enforcement, and integration with Upstash QStash and Cloudinary.

---

## 2. Tech Stack & Dependencies

- **Language & JDK:** Java 25
- **Framework:** Spring Boot 4.0.7 & Spring Security
- **Data Access:** Spring Data JPA / Hibernate
- **Database:** Neon Serverless PostgreSQL
- **Migrations:** Flyway (`db/migration/V1__...` to `V10__...`)
- **Security & Tokens:** JJWT (`io.jsonwebtoken` 0.12.x) & BCrypt Password Encoder
- **File Uploads:** Cloudinary Java SDK
- **Testing:** Testcontainers (PostgreSQL Docker container), MockMvc, Spring Security Test

---

## 3. Endpoints & API Matrix

| HTTP Method | Endpoint Path | Access Level | Description / Payload |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/core/auth/register` | `permitAll()` | Registers user (`NGO` / `CONTRIBUTOR`) & issues JWT token. |
| `POST` | `/api/v1/core/auth/login` | `permitAll()` | Validates credentials & issues JWT token. Default Admin: `admin@connectingdots.org` / `Admin@1234`. |
| `GET` | `/api/v1/core/problem-statements` | `permitAll()` | Public guest discovery of open problem statements (paged/filtered). |
| `POST` | `/api/v1/core/problem-statements` | `NGO` Role | Creates problem statement & triggers QStash AI ingestion. |
| `GET` | `/api/v1/core/profiles/ngos` | `permitAll()` | Public directory of NGO profiles. |
| `GET` | `/api/v1/core/profiles/contributors` | `permitAll()` | Public directory of Contributor profiles. |
| `POST` | `/api/v1/core/profiles/ngo` | `NGO` Role | Creates NGO profile for authenticated user. |
| `POST` | `/api/v1/core/profiles/contributor` | `CONTRIBUTOR` Role | Creates Contributor profile for authenticated user. |
| `POST` | `/api/v1/core/applications` | `CONTRIBUTOR` Role | Submits project application for a problem. |
| `POST` | `/api/v1/core/applications/{id}/messages` | Application Participant | Sends thread-isolated chat message. |
| `POST` | `/api/v1/core/reviews` | Authenticated | Submits project completion review. |
| `GET` | `/api/v1/core/admin/stats` | `ADMIN` Role | Platform-wide counters (total users, NGOs, problems, applications). |
| `GET` | `/api/v1/core/admin/users` | `ADMIN` Role | System user directory for administrative auditing. |
| `DELETE` | `/api/v1/core/admin/users/{id}` | `ADMIN` Role | Deletes/bans a user account. |
| `GET` | `/api/v1/core/admin/ngos` | `ADMIN` Role | NGO directory with `isVerified` trust status. |
| `PUT` | `/api/v1/core/admin/ngos/{id}/verify` | `ADMIN` Role | Toggles or updates NGO verification status (`isVerified`). |
| `DELETE` | `/api/v1/core/admin/problems/{id}` | `ADMIN` Role | Removes inappropriate problem statements. |

---

## 4. Key Working Logic & Features

### A. Security, Role Hierarchy & Seeded Admin
- **Public Guest Access**: All `GET` endpoints for discovery (`/problem-statements/**`, `/profiles/**`, `/reviews/**`) are marked `permitAll()` in `SecurityConfig`. Anonymous guests can explore records freely.
- **Protected Mutations**: Write actions (`POST`, `PUT`, `DELETE`) require authentication. Unauthenticated requests trigger `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)`, returning clean `401 Unauthorized` responses.
- **Roles (`NGO`, `CONTRIBUTOR`, `ADMIN`)**: `JwtAuthenticationFilter` extracts `"role"` claims from incoming tokens and injects `SimpleGrantedAuthority("ROLE_" + role)` into `SecurityContextHolder`. `/api/v1/core/admin/**` endpoints enforce `hasRole('ADMIN')`.
- **Seeded Admin Account**: Automatically created on startup via Flyway (`admin@connectingdots.org` / `Admin@1234`).

### B. NGO Verification & Trust Management
NGO profiles contain an `isVerified` boolean field (`is_verified` column in `ngo_profiles`). Direct uploads remain enabled for frictionless NGO onboarding, while Admins can toggle verification status via `PUT /api/v1/core/admin/ngos/{id}/verify` to display trust badges on the frontend.

### C. Application-Isolated Messaging
Messages are tied to a specific `application_id` (`application_messages` table). When an NGO assigns 2 different problems to 2 separate contributors:
- **Application 1** (`Problem A` + `Contributor 1`) has isolated thread `app-uuid-1`.
- **Application 2** (`Problem B` + `Contributor 2`) has isolated thread `app-uuid-2`.
- `MessageService` verifies that the sender is either the Contributor or NGO tied to that specific application ID, enforcing privacy between threads.

### D. Database Versioning (Flyway)
Flyway automatically executes migration scripts on startup:
- `V1__init_schema.sql` (Users, Profiles, Problems)
- `V4` - `V8` (Applications, Messages, Language, Projects, Reviews)
- `V9__add_is_verified_to_ngo_profiles.sql` (Adds `is_verified` column to `ngo_profiles`)
- `V10__seed_default_admin_user.sql` (Seeds default `admin@connectingdots.org` user with `ON CONFLICT DO NOTHING`)

---

## 5. Key Annotations & Concepts

- **`@RestController` & `@RequestMapping`**: Declares Spring MVC REST controller with base URI path.
- **`@Valid` & `@NotBlank`**: Enforces DTO validation on incoming JSON payloads.
- **`@Transactional`**: Wraps database modifications in ACID database transactions.
- **`@Entity` & `@Table`**: Maps Java classes (`User`, `ProblemStatement`, `Message`) to PostgreSQL tables.
- **`@CreationTimestamp`**: Hibernate annotation auto-populating `created_at` timestamp fields.
- **`@Value("${jwt.secret:${JWT_SECRET}}")`**: Injects configuration variables with fallback defaults.

---

## 6. Key Engineering Challenges & Architectural Decisions

Core Service security architecture, guest vs mutation controls, application chat thread isolation, and Flyway database migration decisions are documented in the master guide:
👉 **[ARCHITECTURE_DECISIONS_AND_SOLUTIONS.md](../../ARCHITECTURE_DECISIONS_AND_SOLUTIONS.md)**
