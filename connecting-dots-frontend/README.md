# Connecting Dots — Next.js 15 Frontend Application

![Next.js](https://img.shields.io/badge/Next.js_15-000000?style=for-the-badge&logo=nextdotjs&logoColor=white)
![React](https://img.shields.io/badge/React_19-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS_v4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)

The frontend application for **Connecting Dots V2**, a platform matching Non-Governmental Organizations (NGOs) with technical volunteer contributors.

---

## AI Prompt Engineering & Rapid Prototyping

This entire user interface was architected, prototyped, and generated in **5 structured prompts using v0 Mini by Vercel** and wired to the Spring Boot microservices backend using Google Antigravity agentic pairing.

### The 5-Prompt Build Sequence
1. **Foundation & Auth**: Configured root layout, civic-tech color system (Deep Teal `#0F766E`, Warm Amber `#D97706`), `next-themes` dark/light mode toggle, and contextual registration/login modals.
2. **Guest Discovery**: Implemented public problem statement explorer with tag filtering, NGO directory grid, and contributor directory grid with first-visit onboarding modal.
3. **NGO Workspace**: Implemented problem submission form with PDF/audio/image upload support, AI review queue (`PROCESSING` → `DRAFT` → `OPEN`), and applicant decision controls.
4. **Contributor Workspace & Messaging**: Implemented application tracking (`PENDING`, `ACCEPTED`, `REJECTED`), gated apply interactions, and isolated 1-on-1 application messaging threads with 5-second polling.
5. **Reviews & Admin Governance**: Implemented 1–5 star rating review engine, contributor profile reviews list, and the executive Admin Operations Center.

---

## Key Technical Implementation Highlights

### 1. Centralized Bearer Token API Client (`lib/api-client.ts`)
* Configured to connect to Spring Cloud Gateway (`http://localhost:8080`).
* Automatically attaches `Authorization: Bearer <token>` to mutation requests.
* Safely parses JWT claims (`sub` for email, `role` for `ROLE_ADMIN`, `ROLE_NGO`, `ROLE_CONTRIBUTOR`).
* Stores session state across `localStorage` and `sessionStorage`.

### 2. Signed Cloudinary Upload Integration (`lib/upload.ts`)
* Requests pre-signed upload parameters from `GET /api/v1/core/files/signature`.
* Performs direct client-side multipart uploads to Cloudinary (`/auto/upload`).
* Automatically triggers QStash AI ingestion (`POST /api/v1/core/problem-statements/{id}/ingest`) after problem creation.

### 3. Backend Cold-Start Resilience (`components/backend-status-banner.tsx`)
* Detects free-tier cloud backend cold starts (Render ~30–60s boot time).
* Displays a non-intrusive status banner (*"⚡ Connecting to backend server... (free-tier cold start in progress, please wait)"*).

### 4. Inline Field Editing
* Profile views support inline editing (pencil icons) for rapid field updates (`PUT /api/v1/core/profiles/...`).

---

## App Routes & Directory Structure

```
app/
├── (public)
│   ├── page.tsx                       # Landing Hero & Public Problem Feed
│   ├── profile/                       # Contributor & NGO Public Profiles with Inline Editing
│   ├── review/                        # Post-project Star Rating Form
│   └── reset-password/                # Password Recovery Page
├── ngo/
│   └── page.tsx                       # Authenticated NGO Workspace & Problem Submission
├── contributor/
│   └── page.tsx                       # Authenticated Contributor Workspace & Applications List
├── applications/
│   └── [applicationId]/
│       └── page.tsx                   # 1-on-1 Application Messaging Thread (5s polling)
└── admin/
    └── page.tsx                       # Admin Operations Center & Governance Tables

components/
├── backend-status-banner.tsx          # Cold-start health detector
├── ngo-workspace.tsx                  # NGO problem submission & AI draft review
├── contributor-workspace.tsx          # Contributor application management
├── public-explorer.tsx                # Problem & directory filters
└── reviews-list.tsx                   # Star ratings & community feedback list
```

---

## Local Development Commands

### 1. Installation
```bash
npm install
```

### 2. Development Server
```bash
npm run dev
```
Open `http://localhost:3000` in your browser.

### 3. Production Build Validation
```bash
npm run build
```

---

## License

Distributed under the MIT License.
