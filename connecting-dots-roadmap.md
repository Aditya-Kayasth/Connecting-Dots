# Connecting-Dots — Backend Implementation Roadmap

**Scope:** ERD → fully working, deployed backend (no frontend). Two real services (`core-service`, `ai-service`) plus the infrastructure that makes them a genuine microservice system: `eureka-server` for discovery and `gateway-service` as the single entry point.

**Stack locked in:** Neon (Postgres), Cloudinary (file storage), Google Gemini API (via Spring AI), Upstash (Redis + QStash). Java 21, Spring Boot 4.1.x.

---

## Architecture at a glance

```
                     ┌─────────────────┐
                     │  eureka-server   │  (service registry)
                     └────────▲─────────┘
                              │ registers
        ┌─────────────────────┼─────────────────────┐
        │                     │                      │
┌───────┴───────┐     ┌───────┴───────┐      ┌───────┴───────┐
│ gateway-service│────▶│ core-service  │      │  ai-service    │
│ (entry point)  │     │ (auth, users, │◀────▶│ (Gemini + Tika │
└───────┬────────┘     │ problems, etc)│ async │  ingestion)    │
        │              └───────┬───────┘      └───────┬────────┘
        │                      │                       │
     clients               Neon Postgres          Cloudinary (files)
                            Upstash Redis          Upstash QStash (queue)
                                                    Google Gemini API
```

`core-service` and `ai-service` are the only two services with real, independent scaling reasons to exist separately — that's the story you tell in an interview. `eureka-server` and `gateway-service` are the supporting cast that make the split real instead of cosmetic.

**On "Celery" — there isn't a direct JVM equivalent, so here's the substitute:** Celery is Python-specific. On Spring Boot, cross-service async work is done with either `@Async` (in-process only, doesn't help you here since `ai-service` is a separate deployable) or a real queue. Since you're already on Upstash, use **Upstash QStash** — it's an HTTP-based queue/scheduler with a permanent free tier (roughly 500–1,000 messages/day depending on current plan, 3 automatic retries, dead-letter queue), and it fits Spring Boot naturally: you `POST` a message with a destination URL, QStash delivers it as an HTTP call to your endpoint. No broker to host, no extra service to deploy. One constraint to design around: QStash enforces a 60-second response timeout on the endpoint it calls — so `ai-service`'s webhook must **acknowledge immediately and do the actual Gemini work in a background thread**, not block the QStash call itself. That's covered in Phase 7.

---

## Phase 0 — Environment & Tooling Setup

- Create accounts: Neon, Cloudinary, Upstash (enable both Redis and QStash on the same account), Google AI Studio (Gemini API key — use the Developer API key path, not Vertex AI), GitHub, Render.
- Local tools: JDK 21, Maven or Gradle, Docker Desktop, Postman or Insomnia, DBeaver (or Neon's own SQL editor).
- Repo strategy: **one monorepo**, four subfolders (`eureka-server/`, `gateway-service/`, `core-service/`, `ai-service/`) each an independent Spring Boot Maven/Gradle module. For a solo dev, one repo is far easier to keep in sync than four — you avoid version-drift between services and CI is simpler.
- Set up `.env`/`application-local.yml` conventions now so secrets never get committed — you'll be juggling five providers' credentials shortly.

## Phase 1 — ERD & Database Design

- Finalize the schema (incorporating the corrections from earlier): `Users`, `NgoProfiles`, `ContributorProfiles`, `Problems` (with `source_file_url`, `source_type`, `raw_transcript`, `status` including the new `DRAFT` state, `version` for optimistic locking), `Milestones`, `Applications`, `Reviews`. All tables get `created_at`/`updated_at` via a shared `BaseEntity`. Note the rename: what was called `Conversations` is really an application/match record (`problem_id`, `contributor_id`, `status`) — no chat content lives here, so `Applications` is the honest name for it. Actual messaging is a separate concern to design later.
- Write it as Flyway migrations from day one (`V1__init_schema.sql`), not JPA `ddl-auto: update` — this is what lets you demonstrate real migration discipline and gives you a clean history to show in the repo.
- Provision the Neon project. Use Neon's **branching** feature — a `dev` branch for local/CI work, `main` for the deployed instance — it's a genuinely distinctive Neon feature worth mentioning on your resume.
- Verify connectivity from a local client before writing any service code.

## Phase 2 — Service Registry & Gateway Skeleton

Do this before any business logic — it's cheap to prove now and expensive to retrofit later.

- Build `eureka-server` (`spring-cloud-starter-netflix-eureka-server`), minimal config, confirm it runs and shows its dashboard.
- Build `gateway-service` (`spring-cloud-starter-gateway`) with Eureka-based dynamic routing (`lb://core-service`, `lb://ai-service`) instead of hardcoded URLs.
- Stand up placeholder `core-service` and `ai-service` — just a `/health` endpoint each — and confirm both register with Eureka and are reachable **through the gateway**, not directly.
- Milestone check: hitting the gateway's single URL should transparently route to whichever service owns that path, resolved via Eureka. Once this works, the rest is filling in real logic behind an already-proven skeleton.

## Phase 3 — Core-Service: Identity & Auth

- `User` entity + Flyway migration.
- Spring Security + JWT: short-lived access token, refresh token stored in Upstash Redis (keyed by user id, with TTL) so logout/revocation is real instead of "wait for expiry."
- `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`.
- Spring Boot Actuator `/health` wired in from here on for every service — Render's deploy health checks need it.

## Phase 4 — Core-Service: Domain Layer

- `NgoProfiles` / `ContributorProfiles` CRUD, tied to `User` via the one-to-one relationship.
- `Problems` entity with the full status state machine: `UPLOADED → PROCESSING → DRAFT → OPEN → IN_PROGRESS → CLOSED`.
- `Milestones`, `Applications` entities and their CRUD.
- `GET /problems` filtering via JPA **Specifications** (needed since city lives on `NgoProfiles`, not `Problems` — this is a cross-table filter, not a flat `WHERE`).
- Write one deliberate concurrency test that proves the `@Version` optimistic lock actually throws on conflicting concurrent updates — good to have as a demo, not just declared in the schema.

## Phase 5 — File Upload & Cloudinary Integration

- Backend endpoint that generates a **signed Cloudinary upload payload** (signature + timestamp), so the raw voice memo/PDF/image is uploaded **directly from the client to Cloudinary**, bypassing your 512MB Render instance entirely. Only the resulting URL comes back to `core-service`.
- On upload completion, `core-service` creates the `Problem` row with `status = UPLOADED`, `source_file_url`, `source_type`.
- `POST /problems/{id}/ingest` — the trigger that kicks off Phase 7's async flow.

## Phase 6 — AI-Service: Ingestion Engine

- Spring AI with the **Google GenAI starter** (Gemini Developer API key, not Vertex AI — no GCP service account needed).
- Apache Tika for PDF/DOCX text extraction; pass audio/image files to Gemini directly as multimodal input (no separate speech-to-text step needed).
- Use Spring AI's structured-output conversion (`chatClient.entity(ProblemDraftDto.class)`) instead of hand-rolled "please return JSON" prompting — more reliable, and a stronger implementation detail to discuss.
- `ProblemDraftDto` → title, description, deliverable_type, tags, list of milestone drafts.
- Unit tests that **mock the Gemini client** so your parsing/mapping logic is tested deterministically, independent of the live API.

## Phase 7 — Async Orchestration (the queue glue)

- `core-service`, on `POST /problems/{id}/ingest`, publishes a QStash message targeting `ai-service`'s webhook (via the gateway's public URL — QStash needs a reachable HTTPS endpoint).
- `ai-service`'s webhook **immediately returns 200** to QStash, then runs the Cloudinary fetch → Tika/Gemini pipeline on a background thread (`@Async` or a virtual-thread executor) — this is what keeps you inside QStash's 60-second delivery timeout regardless of how long Gemini takes.
- When processing finishes, `ai-service` calls an **internal** `core-service` endpoint (`PUT /internal/problems/{id}/draft`) to write the structured result and flip status to `DRAFT`.
- Protect internal endpoints with a shared-secret header or a service-to-service JWT — they should never be reachable from the public gateway route table.
- On Gemini/parsing failure, flip status to a `PROCESSING_FAILED` state with the raw file URL intact, so nothing is silently lost and the NGO can retry.

## Phase 8 — Matchmaking & Trust

- `POST /problems/{id}/apply`, `PUT /applications/{id}/status` (accept/reject).
- Chat/messaging is intentionally left out of this schema for now — you mentioned different plans for it, so it's a separate design decision to make later rather than something baked into `Applications`.
- `Reviews` table (rater, ratee, problem, rating, comment) + a scheduled or trigger-based recompute of `ContributorProfiles.completed_projects`.
- Email on status change — use a free-tier transactional email provider (check current limits when you get here, they shift) triggered from `core-service` on application acceptance/rejection.

## Phase 9 — Testing

- JUnit 5 + Mockito for unit tests in both services.
- **Testcontainers** spinning up real Postgres for `core-service` integration tests — far more convincing than an in-memory H2 substitute.
- **WireMock** to stub `ai-service` when testing `core-service`'s orchestration logic in isolation, and vice versa — this is your answer when asked "how did you test services that depend on each other."

## Phase 10 — Containerization & CI/CD

- One multi-stage `Dockerfile` per service, JVM heap tuned for Render's 512MB free-tier ceiling (e.g. `-Xmx400m -XX:+UseSerialGC` — the serial collector has a smaller footprint than the default, which matters at this memory size).
- `docker-compose.yml` for local dev — all four services plus a local Postgres/Redis pair so you're not burning Neon/Upstash quota while iterating.
- GitHub Actions: build + test on every push, build and push Docker images on merge to `main`.

## Phase 11 — Deployment (Render free tier)

- Four free Render web services: `eureka-server`, `gateway-service`, `core-service`, `ai-service`. Render's free plan needs no credit card and stays deployed indefinitely (with cold starts after idle) — the most reliable genuinely-free option for a Java stack right now among the mainstream PaaS options.
- Environment variables per service: Neon connection string, Cloudinary key/secret, Upstash Redis + QStash tokens, Gemini API key, the internal service-to-service secret, and each service's Eureka registration URL (pointing at the deployed `eureka-server`, not `localhost`).
- Configure Render's health check path to your Actuator `/health` endpoint with a generous initial delay — cold JVM starts on 512MB can take 10–30 seconds, and a too-aggressive health check will kill the instance before it's actually up.
- Document the cold-start behavior in your README as a known limitation rather than letting someone discover it and wonder if it's a bug.

## Phase 12 — Observability & Documentation Polish

- OpenAPI/Swagger UI on `core-service` and `ai-service` — lets anyone (including an interviewer) explore the API without your Postman collection.
- Export and commit a Postman collection anyway, for the ingestion flow specifically (it's the part that isn't self-evident from REST semantics alone).
- README with: the architecture diagram, a short sequence diagram of the ingestion flow (upload → QStash → Gemini → draft → NGO review → publish), and one paragraph explicitly justifying the two-service split (the answer you already have: `ai-service` has a different scaling/resource profile than the CRUD-heavy `core-service`).

---

## If you're time-constrained: critical path vs. polish

If you need something deployed and demoable before you have time for everything: **Phases 0–7 are the critical path** (skeleton, auth, domain, upload, AI ingestion, async glue) — that's the part that makes this project distinctive. Phases 10–11 (containerize and deploy) should happen as soon as 0–7 are stable, even before Phase 8's trust/reviews layer, so you have a live link early. Phases 8, 9, and 12 are what take it from "working" to "resume-ready" — do them once you already have something running end-to-end, not before.
