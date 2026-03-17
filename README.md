# Connecting-Dots: AI-Driven NGO Problem Structuring Engine

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/Neon%20DB-Serverless%20PostgreSQL-4169E1?style=flat-square&logo=postgresql)
![Groq](https://img.shields.io/badge/Groq-llama--3.1--8b--instant-blueviolet?style=flat-square)
![Maven](https://img.shields.io/badge/Maven-Wrapper-C71A36?style=flat-square&logo=apachemaven)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

---

## Overview

**Connecting-Dots** is a production-ready Spring Boot 3 microservice that acts as an **automated Business Analyst** for NGOs. It accepts raw, unstructured problem descriptions submitted by non-profit organisations and uses the **Groq LLM API** (powered by `llama-3.1-8b-instant`) to parse, structure, and categorise them into a clean, queryable format — ready for matching with volunteer technologists.

The service eliminates the manual overhead of interpreting vague problem statements by applying Generative AI to produce a precise technical summary and a strict technology category classification, all persisted to a serverless PostgreSQL database.

---

## Architecture

```
HTTP Client
    │
    ▼
ProblemController  (/api/v1/problems)
    │
    ▼
GroqAiService  ──────────────────────►  Groq API (llama-3.1-8b-instant)
    │                                        │
    │          ◄────── JSON response ────────┘
    ▼
NgoProblemRepository
    │
    ▼
Neon DB (Serverless PostgreSQL)
```

Configuration is fully environment-driven — all secrets are loaded from a `.env` file at startup via `spring-dotenv`. No credentials are hard-coded.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (compiled with JDK 25) |
| Framework | Spring Boot 3.3.4 |
| REST | Spring Web (Embedded Tomcat) |
| Persistence | Spring Data JPA + Hibernate |
| Database | Neon DB (Serverless PostgreSQL) |
| Cache / Session | Upstash Redis (Serverless, TLS) |
| AI / LLM | Groq API — `llama-3.1-8b-instant` |
| HTTP Client | Spring `RestClient` |
| Config | `spring-dotenv` (.env loader) |
| Build | Maven Wrapper (mvnw) |
| Validation | Spring Boot Starter Validation |

---

## Key Features

- **AI-Powered Problem Structuring** — Raw NGO descriptions are sent to the Groq LLM with a strict Business Analyst system prompt. The model returns a clean technical summary (`structuredProblem`) ready for developer consumption.

- **Automated Technology Categorisation** — The LLM maps each problem to one of four strict enum values (`SOFTWARE_WEB`, `DATA_SCIENCE_ML`, `IOT_HARDWARE`, `PROCESS_AUTOMATION`). Invalid categories are rejected with an `IllegalArgumentException` before persistence.

- **Serverless Infrastructure** — Zero infrastructure to manage. Neon DB and Upstash Redis are fully serverless; the service connects via environment variables and is ready for cloud deployment.

- **Robust REST API** — Clean layered architecture: Controller → Service → Repository. Input validation via `@Valid` + `@NotBlank` returns HTTP 400 before the AI layer is ever invoked.

- **Self-Contained Build** — Maven Wrapper (`mvnw` / `mvnw.cmd`) means no Maven installation required. Clone and run.

---

## API Reference

### `POST /api/v1/problems/submit`

Submits a raw NGO problem description. The service calls the Groq LLM, structures the response, and persists the result.

**Request**

```http
POST /api/v1/problems/submit
Content-Type: application/json
```

```json
{
  "ngoName": "Clean Water Initiative",
  "rawDescription": "We are a small NGO working in rural Kenya. We collect water quality data on paper forms from 50 villages every week, but we have no way to analyse trends or alert communities when contamination levels are dangerous. We need some kind of digital system."
}
```

**Response** `200 OK`

```json
{
  "id": "a3f1c2d4-8b7e-4f2a-9c1d-0e5f6a7b8c9d",
  "ngoName": "Clean Water Initiative",
  "rawDescription": "We are a small NGO working in rural Kenya...",
  "structuredProblem": "The organisation requires a digital data collection and analytics platform to replace paper-based water quality monitoring. The system must aggregate readings from 50 distributed field sites, perform trend analysis, and trigger automated contamination alerts for community notification.",
  "techCategory": "DATA_SCIENCE_ML",
  "status": "OPEN"
}
```

---

### `GET /api/v1/problems/open`

Returns all problem statements with `status = "OPEN"`.

**Response** `200 OK`

```json
[
  {
    "id": "a3f1c2d4-8b7e-4f2a-9c1d-0e5f6a7b8c9d",
    "ngoName": "Clean Water Initiative",
    "structuredProblem": "...",
    "techCategory": "DATA_SCIENCE_ML",
    "status": "OPEN"
  }
]
```

---

### Tech Category Enum

| Value | Domain |
|---|---|
| `SOFTWARE_WEB` | Web apps, mobile apps, portals |
| `DATA_SCIENCE_ML` | Analytics, ML models, dashboards |
| `IOT_HARDWARE` | Sensors, embedded systems, hardware |
| `PROCESS_AUTOMATION` | Workflow automation, RPA, scripting |

---

## Local Setup

### Prerequisites

- JDK 25 installed (compiles to Java 21 target)
- A [Neon DB](https://neon.tech) project with a database created
- An [Upstash Redis](https://upstash.com) database (TLS enabled)
- A [Groq API key](https://console.groq.com)

### 1. Clone the repository

```bash
git clone https://github.com/your-username/connecting-dots.git
cd connecting-dots
```

### 2. Configure environment variables

Create a `.env` file in the project root:

```env
NEON_DB_URL=jdbc:postgresql://<your-neon-host>/neondb?sslmode=require
NEON_DB_USERNAME=your_neon_username
NEON_DB_PASSWORD=your_neon_password

UPSTASH_REDIS_HOST=your-upstash-host.upstash.io
UPSTASH_REDIS_PORT=6379
UPSTASH_REDIS_PASSWORD=your_upstash_password

GROQ_API_KEY=your_groq_api_key
```

> The `.env` file is loaded automatically at startup by `spring-dotenv`. It is listed in `.gitignore` and should never be committed.

### 3. Run the service

```bash
# Unix / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

The service starts on `http://localhost:8080`.

### 4. Test the API

```bash
curl -X POST http://localhost:8080/api/v1/problems/submit \
  -H "Content-Type: application/json" \
  -d '{
    "ngoName": "EduReach Africa",
    "rawDescription": "We need a way to track student attendance and learning outcomes across 30 rural schools with no internet connectivity."
  }'
```

---

## Project Structure

```
src/main/java/com/connectingdots/
├── ConnectingDotsApplication.java      # Spring Boot entry point
├── controller/
│   └── ProblemController.java          # REST endpoints
├── domain/
│   ├── NgoProblemStatement.java        # JPA entity
│   ├── NgoProblemRepository.java       # Spring Data repository
│   └── TechCategory.java              # Enum: problem classification
└── service/
    └── GroqAiService.java              # Groq LLM integration
```

---

## Schema

Hibernate manages the schema automatically (`ddl-auto=update`). The `ngo_problem_statement` table is created on first run:

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key, auto-generated |
| `ngo_name` | VARCHAR | Organisation name |
| `raw_description` | TEXT | Original unstructured input |
| `structured_problem` | TEXT | AI-generated technical summary |
| `tech_category` | VARCHAR | Enum value (stored as string) |
| `status` | VARCHAR | Default: `OPEN` |

---

## Roadmap

- [ ] Volunteer profile matching engine
- [ ] JWT-based authentication
- [ ] Webhook notifications on problem match
- [ ] Admin dashboard (React frontend)
- [ ] Flyway database migrations

---

## License

MIT — see [LICENSE](LICENSE) for details.
