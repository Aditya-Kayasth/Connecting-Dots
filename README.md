# Connecting Dots V2 — AI-Powered Civic Tech Platform

![Next.js](https://img.shields.io/badge/Next.js_16.3-000000?style=for-the-badge&logo=nextdotjs&logoColor=white)
![React](https://img.shields.io/badge/React_19-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript_5.7-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java_25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis_7-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker_Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Google Gemini](https://img.shields.io/badge/Gemini_3.5_Flash-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white)

---

> [!IMPORTANT]
> ### ☁️ Live Cloud Hosting & Cold-Start Disclaimer
> 
> * **Frontend Deployment**: Hosted on **Vercel**.
> * **Backend Microservices Deployment**: Hosted on **Render** (Free Tier).
> 
> **Cold-Start Notice**: Free-tier cloud microservices on Render automatically spin down after 15 minutes of inactivity. When visiting the live application for the first time or after a period of inactivity, initial API requests may take **4 to 5 minutes** while backend containers complete their cold-start boot sequence.
> 
> The application includes a live status banner (`BackendStatusBanner`) that monitors service connection states and shows an elapsed timer. **Please wait 4 to 5 minutes for all microservices to wake up before logging in or submitting forms.** Once awake, the platform works smoothly and fast!

---

## 🚀 Instant Access: Pre-Seeded Demo Accounts & Real Sign-Up Templates

> [!TIP]
> ### 🤝 Community Courtesy & Code of Conduct
> These pre-seeded accounts are provided openly so anyone can explore and test Connecting Dots V2 immediately.  
> **Please be a good community member**: do not delete existing project records, alter passwords, or create chaos for other testers. Treat the shared sandbox with care and respect!

### 🔑 Instant Copy-Paste Demo Credentials
You can log in directly using the **"Quick Demo Login"** buttons in the sign-in modal, or copy-paste these credentials:

| Role | Email | Password | Pre-Configured Profile Details |
| :--- | :--- | :--- | :--- |
| **🏢 NGO Organization** | `ngo_demo@connectingdots.org` | `password123` | **Hope Foundation** (Education Technology • Global Community) |
| **💻 Technical Contributor** | `contributor_demo@connectingdots.org` | `password123` | **Alex Morgan** (Senior Full-Stack Engineer • React, Java, AI) |
| **🛡️ Platform Admin** | `admin@connectingdots.org` | `Admin@1234` | Full Platform Governance, Verification & Auditing |

---

### 📝 Want to Test Real Registration? (Sample Profile Data to Copy-Paste)
If you prefer creating a fresh, personal account instead of using the shared demo credentials, use the **"Create account"** tab and copy-paste these realistic sample values:

#### Option A: Sample NGO Organization Registration
* **Organization Name**: `Marathwada Jal Vikas Foundation` *(or any realistic NGO name)*
* **Primary Focus Domain**: `Environment & Sustainability` *(select from dropdown)*
* **Contact Number**: `9823456789` *(10 digits)*
* **Location / Region**: `Dharashiv, Maharashtra, India`
* **Email**: `your.ngo@example.com`
* **Password**: `YourPassword123!`

#### Option B: Sample Technical Contributor Registration
* **Full Name**: `Aarav Deshmukh`
* **Role / Title**: `Senior Full-Stack & Android Engineer`
* **Skills Summary**: `React, TypeScript, Java, Spring Boot, PostgreSQL, AI`
* **Portfolio or GitHub URL**: `https://github.com/your-username`
* **Contact Number**: `9822112233` *(10 digits)*
* **Location / Region**: `Pune, Maharashtra, India`
* **Email**: `your.email@example.com`
* **Password**: `YourPassword123!`

---

## 🌐 1. Platform Overview

**Connecting Dots V2** is an enterprise-grade, microservices-driven civic technology platform engineered to match Non-Governmental Organizations (NGOs) with technical volunteer contributors (software engineers, data scientists, UX designers).

### The Core Problem & AI Solution

Grassroots NGOs frequently struggle to translate real-world community needs into structured technical project briefs. Connecting Dots solves this by enabling NGOs to upload raw field notes, paper photographs, PDFs, or audio recordings in **any language**.

An asynchronous AI processing pipeline powered by **Google Gemini 3.5 Flash** transcribes, translates, and structures raw uploads into actionable engineering problem statements exposed on a public marketplace for technical contributors.

---

## 🏛️ 2. Microservices System Architecture

The platform is architected around a **decoupled 4-microservice backend** paired with a **Next.js 16 App Router frontend**:

```mermaid
flowchart TD
    Client["🌐 Next.js 16 Frontend UI<br/>(Vercel • Port 3000)"]
    
    Client -->|HTTPS REST / Bearer JWT| GW["🚪 Spring Cloud Gateway<br/>(Port 8080)"]
    GW --- Redis[("⚡ Redis Rate Limiter")]

    Eureka["📡 Eureka Service Registry<br/>(Port 8761)"]
    Core["⚙️ Core Business Service<br/>(Port 8081)<br/>• Auth & RBAC (JWT)<br/>• NGO & Contributor Logic<br/>• Project Applications"]
    AI["🤖 Gemini AI Worker<br/>(Port 8082)<br/>• Multimodal OCR & Parsing<br/>• Regional Translation<br/>• Gemini 3.5 Flash LLM"]

    GW -->|/api/v1/core/**| Core
    GW -->|/api/v1/ai/**| AI
    GW -.->|Lookup| Eureka
    Core -.->|Register| Eureka
    AI -.->|Register| Eureka

    DB[("🗄️ Neon PostgreSQL<br/>(ACID Persistence)")]
    QStash["📨 Upstash QStash<br/>(Serverless Queue)"]
    Gemini["✨ Google Gemini API<br/>(Multimodal Vision)"]

    Core -->|JPA Transactions| DB
    Core -->|1. Publish Task| QStash
    QStash -->|2. Webhook Event| AI
    AI -->|3. Multimodal Analysis| Gemini
    AI -.->|4. Save Result /ai-update| Core
```

---

## 📊 3. Microservice Architecture Matrix

Detailed technical documentation for each component is available in its respective folder:

| Component / Service | Port | Primary Responsibilities | Detailed Documentation |
| :--- | :--- | :--- | :--- |
| **`connecting-dots-frontend`** | `3000` | Next.js 16 App Router, React 19 UI, Tailwind CSS v4, dynamic status banner. | 👉 [Frontend README](connecting-dots-frontend/README.md) |
| **`gateway-service`** | `8080` | Edge entry point, route predicates (`lb://`), Redis rate limiting, CORS policy. | 👉 [Gateway README](connecting-dots-backend/gateway-service/GATEWAY_SERVICE_README.md) |
| **`core-service`** | `8081` | Auth (JWT/BCrypt), NGO & Contributor profiles, application state machine, messaging threads. | 👉 [Core Service README](connecting-dots-backend/core-service/CORE_SERVICE_README.md) |
| **`ai-service`** | `8082` | Multimodal document extraction, Gemini 3.5 Flash LLM, async QStash webhook processing. | 👉 [AI Service README](connecting-dots-backend/ai-service/AI_SERVICE_README.md) |
| **`eureka-server`** | `8761` | Netflix Eureka dynamic service discovery registry & instance health directory. | 👉 [Eureka README](connecting-dots-backend/eureka-server/EUREKA_SERVER_README.md) |

---

## 🚀 4. Local Quickstart & Development Guide

### Prerequisites

* **Docker** & **Docker Compose** installed on your system.
* **Node.js** v20+ & **Java 25 JDK** (for standalone local development).

---

### Option A: Full-Stack Local Execution (Docker Compose)

To build and run all services (Frontend, Gateway, Core, AI, Eureka, Redis) in unified Docker containers:

```bash
docker compose up --build
```

#### Access Points

* **Next.js Frontend**: `http://localhost:3000`
* **API Gateway**: `http://localhost:8080`
* **Eureka Registry Dashboard**: `http://localhost:8761`

---

### Option B: Local PowerShell Management Scripts (Windows)

For rapid local testing on Windows systems using PowerShell:

#### Start All Local Services
```powershell
.\start-system.ps1
```

#### Stop All Local Services
```powershell
.\stop-system.ps1
```

---

## 🔑 5. Complete Seeded Demo & Sample Accounts Matrix

In addition to the quick-access accounts listed at the [top of this README](#-instant-access-pre-seeded-demo-accounts--real-sign-up-templates), the platform database includes full sample Maharashtra NGOs and technical contributors pre-seeded for realistic marketplace testing:

### Core Platform Roles
| Role | Email | Password | Pre-Configured Profile Details |
| :--- | :--- | :--- | :--- |
| **System Admin** | `admin@connectingdots.org` | `Admin@1234` | Platform Governance, NGO Verification & Auditing |
| **Demo NGO** | `ngo_demo@connectingdots.org` | `password123` | Hope Foundation (Education Technology) |
| **Demo Contributor** | `contributor_demo@connectingdots.org` | `password123` | Alex Morgan (Senior Full-Stack Engineer) |

### Sample Maharashtra Grassroots NGOs
| Organization Name | Email | Password | Domain & Region |
| :--- | :--- | :--- | :--- |
| **Marathwada Jal Vikas Foundation** | `contact@jalvikas.org` | `JalVikas@2026` | Environment & Sustainability • Dharashiv |
| **Netra Vikas Disability Welfare** | `info@netravikas.org` | `NetraVikas@2026` | Education Technology • Nashik & Sambhajinagar |
| **Vidarbha Kisaan Empowerment Trust** | `contact@vidarbhakisaan.org` | `Kisaan@2026` | Community Development • Nagpur & Yavatmal |
| **Baramati Gramin Shikshan Sanstha** | `info@baramatiedu.org` | `Baramati@2026` | Education Technology • Pune & Baramati |
| **Solapur Mahila Bachat Gat Samiti** | `contact@solapurmahila.org` | `Solapur@2026` | Financial Inclusion • Solapur |

### Sample Technical Contributors
| Contributor Name | Email | Password | Role & Location |
| :--- | :--- | :--- | :--- |
| **Aarav Deshmukh** | `aarav.deshmukh@gmail.com` | `password123` | Senior Full-Stack & Android Engineer • Pune |

---

## 📁 6. Streamlined Repository Structure

```
Connecting-Dots-V2/
├── connecting-dots-frontend/            # Next.js 16 Frontend App Router
│   ├── app/                            # App Routes (admin, ngo, contributor, profile)
│   ├── components/                     # Civic-Tech UI Components & Status Banner
│   ├── lib/                            # Bearer Token API Client & Upload Helpers
│   └── README.md                       # Detailed Frontend Documentation
├── connecting-dots-backend/
│   ├── eureka-server/                  # Service Discovery Server (:8761)
│   ├── gateway-service/                # Edge API Gateway & Rate Limiter (:8080)
│   ├── core-service/                   # Core Business API & Database Persistence (:8081)
│   ├── ai-service/                     # Gemini 3.5 Flash Ingestion Worker (:8082)
│   └── BACKEND_ARCHITECTURE.md         # Master Backend Microservices Blueprint
├── Sample Problems/                    # Sample Ingestion Files (PDFs, notes, images)
├── docker-compose.yml                  # Full-Stack Orchestration Manifest
├── start-system.ps1                    # Local System Launch Script
├── stop-system.ps1                     # Local System Termination Script
├── ARCHITECTURE_DECISIONS_AND_SOLUTIONS.md # Architectural Design & Tradeoffs Doc
└── README.md                           # Master Project Readme
```

---

## 📄 7. License

Distributed under the MIT License. Built for social impact.
