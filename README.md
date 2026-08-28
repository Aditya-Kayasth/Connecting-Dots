# Connecting Dots V2 — AI-Powered Civic Tech Platform

![Next.js](https://img.shields.io/badge/Next.js_15-000000?style=for-the-badge&logo=nextdotjs&logoColor=white)
![React](https://img.shields.io/badge/React_19-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java_25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker_Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Google Gemini](https://img.shields.io/badge/Gemini_3.5_Flash-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS_v4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)

---

## Overview

**Connecting Dots V2** is an enterprise-grade, microservices-driven civic tech matching platform that connects Non-Governmental Organizations (NGOs) with technical volunteer contributors (software engineers, data scientists, UX designers).

The platform addresses a major industry bottleneck: NGOs often struggle to articulate technical specifications from raw problem descriptions. Connecting Dots solves this by allowing NGOs to upload handwritten paper notes, printed PDFs, or voice recordings in **any language**. An asynchronous AI processing pipeline powered by **Google Gemini 3.5 Flash Vision** transcribes, translates, and structures raw uploads into actionable engineering briefs.

---

## Executive Architecture

The system is architected around a **decoupled 4-microservice backend** paired with an **AI-prompt-engineered Next.js 15 frontend**:

```
                              ┌───────────────────────────────────┐
                              │  Connecting Dots Next.js 15 UI    │
                              └─────────────────┬─────────────────┘
                                                │ (HTTP / Bearer JWT)
                                                ▼
                              ┌───────────────────────────────────┐
                              │    Spring Cloud Gateway (8080)    │
                              │   Redis Token-Bucket Rate Limiter │
                              └─────────┬───────────────┬─────────┘
                                        │               │
                      ┌─────────────────┘               └─────────────────┐
                      ▼                                                   ▼
┌───────────────────────────────────────────┐       ┌───────────────────────────────────────────┐
│           core-service (8081)             │       │            ai-service (8082)              │
│  - User Auth & RBAC (JWT / BCrypt)        │       │  - Google Gemini 3.5 Flash Vision         │
│  - Neon PostgreSQL & Flyway Migrations    │       │  - Multimodal OCR & Translation           │
│  - Problem & Application State Machine    │       │  - Spring AI Structured Output            │
└─────────────────────┬─────────────────────┘       └─────────────────────▲─────────────────────┘
                      │                                                   │
                      │ 1. Publish Event                                  │ 2. Webhook Delivery
                      ▼                                                   │
          ┌───────────────────────┐                                       │
          │ Upstash QStash Queue  ├───────────────────────────────────────┘
          └───────────────────────┘
```

---

## AI-Native Front-End Engineering & Prompting Velocity

A key engineering highlight of this repository is the **rapid design and implementation of the complete frontend architecture using Advanced Generative AI Prompt Engineering**.

### The 5-Prompt v0 Mini Execution Strategy
Rather than building components manually over weeks, the entire production-ready user interface was specified, prototyped, and generated in **just 5 structured prompts using v0 Mini by Vercel**:

1. **Prompt 1 (Foundation & Auth)**: Established the root layout shell, persisted dark/light themes (`next-themes`), civic-tech design system, and dynamic role-switching authentication modals.
2. **Prompt 2 (Guest Discovery)**: Built the public problem feed, domain filtering, NGO directory grid, and contributor directory grid with guest onboarding isolation.
3. **Prompt 3 (NGO Workspace)**: Built the problem submission pipeline, file attachment placeholders, and the AI review queue (`PROCESSING` → `DRAFT` → `OPEN`).
4. **Prompt 4 (Contributor Workspace & Messaging)**: Built the application tracking engine, role-gated apply interactions, and isolated 1-on-1 application message threads with polling synchronization.
5. **Prompt 5 (Reviews & Governance)**: Built the 1–5 star rating engine, public profile review lists, and the executive Admin Operations Center.

Following v0 code generation, the frontend was extracted and integrated with the live backend using agentic AI pair programming (Google Antigravity), demonstrating state-of-the-art developer velocity and generative AI workflow orchestration.

---

## Tech Stack

### Frontend Stack
* **Framework**: Next.js 15 (App Router, Turbopack, Standalone Docker Runner)
* **Library**: React 19, TypeScript 5.7
* **Styling**: Tailwind CSS v4, custom CSS variable tokens, Lucide Icons
* **State & Auth**: Client-side JWT session context, `localStorage` persistence, `next-themes`
* **File Uploads**: Direct signed client-to-Cloudinary multipart uploads

### Backend Stack
* **Language & Runtime**: Java 25, Spring Boot 3.4
* **Service Discovery**: Spring Cloud Netflix Eureka (`eureka-server` on port 8761)
* **API Gateway**: Spring Cloud Gateway (Reactive Netty event-loop on port 8080)
* **Database**: PostgreSQL (Neon Serverless DB), Flyway Migration Versioning (`V1`–`V10`)
* **Caching & Rate Limiting**: Redis 7 Alpine (Token-Bucket algorithm via `#{@ipKeyResolver}`)
* **Asynchronous Messaging**: Upstash QStash serverless HTTP queue with exponential backoff retries
* **AI Ingestion**: Spring AI 2.0.0, Google Gemini 3.5 Flash Vision API

---

## Core System Features

### 1. Multimodal AI Problem Ingestion
NGOs submit problem statements via text, PDF documents, or handwritten notes/photos in any language. `core-service` generates a signed Cloudinary signature, uploads the source material, and publishes an asynchronous task to Upstash QStash. QStash triggers `ai-service`, where Gemini 3.5 Flash transcribes the input, translates regional languages, extracts key requirements, and updates `core-service` via a secure callback.

### 2. Guest Exploration vs. Role-Gated Mutations
Guests can explore problem statements, NGO directories, and contributor profiles read-only. Mutation operations (`POST`, `PUT`, `DELETE`) enforce stateless JWT authentication with role-based access control (`ROLE_ADMIN`, `ROLE_NGO`, `ROLE_CONTRIBUTOR`).

### 3. Isolated 1-on-1 Application Messaging
When a contributor applies to an NGO project, a private messaging thread is bound to that exact `application_id`. A 5-second polling mechanism fetches messages securely (`/api/v1/core/applications/{id}/messages`), isolating chat threads between the NGO owner and applicant.

### 4. Admin Operations & Governance Center
Accessible exclusively to authorized administrator accounts (`admin@connectingdots.org`), featuring live platform statistics, NGO verification management (`isVerified` status toggle), user moderation, and problem content auditing.

### 5. Backend Cold-Start Resilience
For cloud deployments on free-tier hosting (e.g., Render), the frontend includes a `BackendStatusBanner` component that pings backend health and alerts users if a cold-start boot sequence (~30–60 seconds) is in progress.

---

## Getting Started

### Prerequisites
* **Docker** & **Docker Compose** installed on your system.
* **Node.js** v20+ (for standalone frontend development).

### 1. Full-Stack Local Run (Single Command)
Run the entire platform (Frontend + Gateway + Core + AI + Eureka + Redis) using Docker Compose:

```bash
docker compose up --build
```

Access points:
* **Frontend**: `http://localhost:3000`
* **API Gateway**: `http://localhost:8080`
* **Eureka Registry**: `http://localhost:8761`

### 2. SeedTest Credentials

| Role | Email | Password | Access Level |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin@connectingdots.org` | `Admin@1234` | Platform Governance & Verification |
| **NGO** | `ngo_test@connectingdots.org` | `Ngo@1234` | Problem Submission & Matching |
| **Contributor** | `contributor_test@connectingdots.org` | `Contributor@1234` | Applications & Messaging |

---

## Project Structure

```
Connecting-Dots-V2/
├── connecting-dots-frontend/      # Next.js 15 App Router Frontend (v0 AI Generated)
│   ├── app/                      # Next.js Pages & Routes (admin, ngo, contributor, profile)
│   ├── components/               # Civic-tech UI Components & Status Banners
│   ├── lib/                      # Centralized API Client & Cloudinary Upload Helper
│   └── Dockerfile                # Multi-stage Standalone Node.js Runner
├── connecting-dots-backend/
│   ├── eureka-server/            # Spring Cloud Eureka Service Registry (8761)
│   ├── gateway-service/          # Spring Cloud Gateway & Redis Rate Limiter (8080)
│   ├── core-service/             # Core Business API, JWT Auth & Flyway Migrations (8081)
│   └── ai-service/               # Gemini 3.5 Flash Multimodal Ingestion Worker (8082)
├── docker-compose.yml            # Full-Stack Orchestration Manifest
├── ARCHITECTURE_DECISIONS_AND_SOLUTIONS.md # Detailed Engineering & System Design Doc
└── README.md                     # Master Repository Overview
```

---

## License

Distributed under the MIT License. Built for impact.
