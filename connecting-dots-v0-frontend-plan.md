# Connecting Dots — Frontend Build Plan (v0 by Vercel)

**Goal:** enterprise-grade frontend, built fast, at zero cost, wired to the already-deployed 4-service Spring Boot backend.

**Reality check on the constraint set:** v0's free tier gives $5/month in credits and caps you at **7 messages/day**, no rollover, no buying more on free. That's plenty to build this whole frontend — but only if each message is a complete, well-specified screen or screen-group, not a back-and-forth conversation. Budget roughly 6-8 total prompts across 1-2 days, not one per component.

---

## Part 1 — What to prepare *before* opening v0

v0 produces much better output when it isn't inventing your data model. Have these ready:

1. **A one-paragraph product brief** (reusable across every prompt) — copy this into every v0 conversation as context:
   > "Connecting Dots is a platform matching NGOs (who submit problems via document/voice upload, structured by AI) with technical volunteer contributors. Roles: NGO, CONTRIBUTOR, ADMIN. Default seeded Admin: admin@connectingdots.org / Admin@1234. Guests can browse everything read-only without logging in; only mutations require auth."

2. **Real field names from your actual API**, so generated types match reality instead of plausible-sounding guesses:
   - **Problem statement:** `id`, `title`, `description`, `domain`, `status` (`PROCESSING`, `DRAFT`, `OPEN`, `IN_PROGRESS`, `CLOSED`, `RESOLVED`), `sourceFileUrl`, `sourceType` (`PDF`, `AUDIO`, `IMAGE`), `ngoName`, `techCategory` (`SOFTWARE_WEB`, `DATA_SCIENCE_ML`)
   - **NGO Profile DTO (`NgoProfileRequest`):** `organizationName`, `domain`, `contactNumber`, `preferredLanguage` (default: `"en"`), `isVerified` (boolean)
   - **Contributor Profile DTO (`ContributorProfileRequest`):** `firstName`, `lastName`, `skillsSummary`, `portfolioUrl`, `preferredLanguage` (default: `"en"`), `completedProjects` (default: `0`)
   - **Application:** `id`, `problemId`, `contributorProfileId`, `status` (`PENDING`, `ACCEPTED`, `REJECTED`, `COMPLETED`), `createdAt`
   - **Message:** `id`, `applicationId`, `senderId`, `content`, `createdAt`
   - **Review:** `id`, `problemId`, `raterId`, `rateeId`, `rating` (1-5), `comment`, `createdAt`
   - **User / Auth Roles:** `NGO`, `CONTRIBUTOR`, `ADMIN` (Seeded Admin login: `admin@connectingdots.org` / `Admin@1234`)
   - **Admin API Endpoints:** `GET /api/v1/core/admin/stats` (counters), `GET /api/v1/core/admin/users` (user table), `GET /api/v1/core/admin/ngos` (NGO verification table), `PUT /api/v1/core/admin/ngos/{id}/verify` (toggle verification status), `DELETE /api/v1/core/admin/users/{id}` (user moderation), `DELETE /api/v1/core/admin/problems/{id}` (content moderation)

3. **Realistic sample records** per entity to use directly in prompts:
   - **Sample Problem Statements:**
     ```json
     [
       {
         "id": "7dd308c2-237a-4099-bf2d-5fc99c55e5c4",
         "ngoName": "Aditya's Tech Rescue",
         "title": "Digitize Student Records & Dropout Early Warning AI",
         "description": "We tutor underprivileged students in Nagpur. We need a web dashboard to digitize attendance and quiz scores, with machine learning to predict students at risk of dropping out before exams.",
         "domain": "Education",
         "status": "OPEN",
         "techCategory": "DATA_SCIENCE_ML"
       },
       {
         "id": "7e3eab66-8db2-479f-adfc-6638f037e9ca",
         "ngoName": "Shiksha Foundation",
         "title": "Rural School Attendance Tracking Web Portal",
         "description": "Our rural schools lose paper attendance rosters. We require a simple mobile-friendly web application for teachers to mark daily attendance offline and sync when connected.",
         "domain": "Education",
         "status": "OPEN",
         "techCategory": "SOFTWARE_WEB"
       },
       {
         "id": "eb3e6159-f856-48db-bb52-993f414a99ba",
         "ngoName": "Payaar Foundation",
         "title": "Animal Rescue Staff & Medicine Reimbursement Management",
         "description": "We manage field volunteers treating injured animals. We need a portal to track field staff tasks and automate medicine purchase reimbursement requests.",
         "domain": "Animal Welfare",
         "status": "OPEN",
         "techCategory": "SOFTWARE_WEB"
       }
     ]
     ```
   - **Sample NGO Profiles:**
     ```json
     [
       {
         "id": "n1111111-1111-1111-1111-111111111111",
         "organizationName": "Aditya's Tech Rescue",
         "domain": "Education & Digital Literacy",
         "contactNumber": "+91 98765 43210",
         "preferredLanguage": "en"
       },
       {
         "id": "n2222222-2222-2222-2222-222222222222",
         "organizationName": "Shiksha Foundation",
         "domain": "Rural Education Infrastructure",
         "contactNumber": "+91 98123 45678",
         "preferredLanguage": "hi"
       },
       {
         "id": "n3333333-3333-3333-3333-333333333333",
         "organizationName": "Payaar Foundation",
         "domain": "Animal Welfare & Veterinary Care",
         "contactNumber": "+91 97654 32109",
         "preferredLanguage": "mr"
       }
     ]
     ```
   - **Sample Contributor Profiles:**
     ```json
     [
       {
         "id": "c1111111-1111-1111-1111-111111111111",
         "firstName": "Aarav",
         "lastName": "Sharma",
         "skillsSummary": "Next.js, React, Tailwind CSS, TypeScript, Node.js",
         "portfolioUrl": "https://github.com/aaravsharma",
         "preferredLanguage": "en",
         "completedProjects": 4
       },
       {
         "id": "c2222222-2222-2222-2222-222222222222",
         "firstName": "Priya",
         "lastName": "Patel",
         "skillsSummary": "Python, FastAPI, Scikit-Learn, PyTorch, PostgreSQL",
         "portfolioUrl": "https://priyapatel.dev",
         "preferredLanguage": "en",
         "completedProjects": 6
       },
       {
         "id": "c3333333-3333-3333-3333-333333333333",
         "firstName": "Rohan",
         "lastName": "Deshmukh",
         "skillsSummary": "Spring Boot, Java, Docker, PostgreSQL, React",
         "portfolioUrl": "https://github.com/rohandeshmukh",
         "preferredLanguage": "mr",
         "completedProjects": 2
       }
     ]
     ```

4. **A starting color palette:**
   - Primary Accent: Deep Teal (`#0F766E` / `teal-700`)
   - Secondary Accent: Warm Amber (`#D97706` / `amber-600`)
   - Dark Mode Background & Surface: Dark Slate (`#0F172A` / `#1E293B`)
   - Light Mode Background & Surface: Slate Tint (`#F8FAFC` / `#FFFFFF`)

5. **A Vercel account** (free, needed to use v0 at all) and a **GitHub account** with a new empty repo for the frontend.

---

## Part 2 — The prompts, consolidated to 5

Paste the product brief (Part 1.1) at the start of each of these. Each numbered item is **one v0 message** — 5 total, leaving 2 of your 7 daily messages in reserve for same-day fixes rather than needing tomorrow's reset.

### Message 1 — Foundation + Auth: layout shell, theme system, global switch, modals
> Build the root layout for a Next.js 15 App Router project using Tailwind CSS and shadcn/ui. Include: (1) a persisted dark/light mode toggle using next-themes, reachable from a sticky top bar on every page; (2) a global "I represent an NGO / I am a Contributor" switch in the same top bar that changes app-wide context without a page reload; (3) a responsive split-screen landing hero (NGO side / Contributor side) that works down to 375px mobile width — define an explicit mobile behavior (stacked, not compressed) — and up through 1600px+ laptop/desktop; (4) contextual login/register modals, triggered from the top bar or from any restricted action, that dynamically switch fields and copy for NGO registration (fields: organizationName, domain, contactNumber, preferredLanguage), Contributor registration (fields: firstName, lastName, skillsSummary, portfolioUrl, preferredLanguage), and login (email, password). No live-update or real-time toast notifications anywhere. Use Deep Teal (#0F766E) and Warm Amber (#D97706) as the primary accents, with Dark Slate (#0F172A) background in dark mode and slate (#F8FAFC) in light mode.

### Message 2 — Guest discovery: problem feed + profile directories
> Build three connected views: (1) a public problem-statement feed showing these sample records: [{"id":"7dd308c2-237a-4099-bf2d-5fc99c55e5c4","ngoName":"Aditya's Tech Rescue","title":"Digitize Student Records & Dropout Early Warning AI","description":"We tutor underprivileged students in Nagpur. We need a web dashboard to digitize attendance and quiz scores, with machine learning to predict students at risk of dropping out before exams.","domain":"Education","status":"OPEN","techCategory":"DATA_SCIENCE_ML"},{"id":"7e3eab66-8db2-479f-adfc-6638f037e9ca","ngoName":"Shiksha Foundation","title":"Rural School Attendance Tracking Web Portal","description":"Our rural schools lose paper attendance rosters. We require a simple mobile-friendly web application for teachers to mark daily attendance offline and sync when connected.","domain":"Education","status":"OPEN","techCategory":"SOFTWARE_WEB"},{"id":"eb3e6159-f856-48db-bb52-993f414a99ba","ngoName":"Payaar Foundation","title":"Animal Rescue Staff & Medicine Reimbursement Management","description":"We manage field volunteers treating injured animals. We need a portal to track field staff tasks and automate medicine purchase reimbursement requests.","domain":"Animal Welfare","status":"OPEN","techCategory":"SOFTWARE_WEB"}] with domain/tag filtering and status badges (OPEN, PROCESSING, DRAFT, IN_PROGRESS, RESOLVED); (2) an NGO directory grid using sample records: [{"organizationName":"Aditya's Tech Rescue","domain":"Education & Literacy","contactNumber":"+91 98765 43210","preferredLanguage":"en"},{"organizationName":"Shiksha Foundation","domain":"Rural Education","contactNumber":"+91 98123 45678","preferredLanguage":"hi"},{"organizationName":"Payaar Foundation","domain":"Animal Welfare","contactNumber":"+91 97654 32109","preferredLanguage":"mr"}]; (3) a contributor directory grid using sample records: [{"firstName":"Aarav","lastName":"Sharma","skillsSummary":"Next.js, React, Tailwind CSS, TypeScript, Node.js","portfolioUrl":"https://github.com/aaravsharma","preferredLanguage":"en","completedProjects":4},{"firstName":"Priya","lastName":"Patel","skillsSummary":"Python, FastAPI, Scikit-Learn, PyTorch, PostgreSQL","portfolioUrl":"https://priyapatel.dev","preferredLanguage":"en","completedProjects":6},{"firstName":"Rohan","lastName":"Deshmukh","skillsSummary":"Spring Boot, Java, Docker, PostgreSQL, React","portfolioUrl":"https://github.com/rohandeshmukh","preferredLanguage":"mr","completedProjects":2}]. Include a first-visit onboarding modal that triggers once per guest, with a backdrop-blur-md background isolation effect, both light and dark variants, explaining how problems/NGOs/contributors relate. No live-update feed — static content only.

### Message 3 — NGO dashboard: submit problem + review AI draft
> Build an authenticated NGO dashboard with: (1) a problem-statement submission form (file upload for PDF/audio/image — placeholder upload component for now); (2) a "drafts pending review" view showing AI-structured output (title, description, domain, tags) with edit/approve controls before publishing, matching status states PROCESSING → DRAFT → OPEN; (3) a list of applications received per problem, with accept/reject actions.

### Message 4 — Contributor dashboard + shared applications/messaging
> Build an authenticated Contributor dashboard with: (1) the problem feed with an "Apply" action gated behind auth; (2) a "my applications" list showing status (PENDING/ACCEPTED/REJECTED) per application, linking to (3) a shared application detail view — reusable from both the Contributor's and NGO's applications lists — showing problem context, applicant/owner info, and a message thread scoped to that specific applicationId (isolated so no cross-application messages ever show), with simple polling-based send/receive, no WebSocket; (4) a completed-projects section feeding into a reputation display.

### Message 5 — Reviews + Admin Dashboard
> Build (1) a review submission form (rating 1-5 + comment) shown after an application is marked complete, and a reviews-received list on profile pages; (2) an authenticated Admin dashboard accessible by admin users (admin@connectingdots.org) featuring: (a) real-time platform statistics cards (total users, total NGOs, total problems, total applications fetched from GET /api/v1/core/admin/stats); (b) an NGO Verification & Trust Management table (fetched from GET /api/v1/core/admin/ngos) showing organizationName, domain, and an "isVerified" badge (VERIFIED / UNVERIFIED) with a toggle action calling PUT /api/v1/core/admin/ngos/{id}/verify; (c) a platform user management table listing all registered users (email, role, status fetched from GET /api/v1/core/admin/users) with a "Remove User" action calling DELETE /api/v1/core/admin/users/{id}; (d) a problem statement auditing table listing submitted problems with a "Delete Inappropriate Problem" action calling DELETE /api/v1/core/admin/problems/{id}.

If a generation comes back visibly broken or ignores part of the spec, use one of your 2 reserved messages to correct it rather than starting the whole screen over — a targeted "fix X, keep everything else" message costs the same as a fresh one but preserves what already worked.

---

## Part 3 — Wiring it to the real backend

Since you're handing this step to the AI agent in Antigravity once the v0 output lands in your `frontend/` folder, treat what follows as the brief to give that agent rather than something to do by hand — it covers what "correct" looks like so the agent's mapping can be checked against it:

1. **One API client module**, not scattered `fetch` calls — a single file wrapping calls to `process.env.NEXT_PUBLIC_API_BASE_URL` (your `gateway-service` URL, `localhost:8080` for now), with the JWT attached from wherever you decide to store it.
2. **Token storage decision:** the simplest option for a solo resume project is storing the JWT in memory (React state/context) plus `localStorage` for persistence across reloads, attached as `Authorization: Bearer <token>` on each call. It's not the most secure pattern (an httpOnly-cookie-via-Next.js-Route-Handler approach is stronger), but it matches your backend's existing stateless-JWT design without adding a proxy layer, and is a reasonable trade-off to state plainly rather than hide if it comes up in an interview.
3. **Replace every placeholder** v0 generated (upload widgets, sample data) with real calls to your documented endpoints — `/api/v1/core/auth/*`, `/api/v1/core/problem-statements`, `/api/v1/core/applications`, etc.
4. **File upload:** wire the upload form to your Cloudinary signature endpoint (`GET /api/v1/core/files/signature`) — client requests a signed payload from `core-service`, then uploads directly to Cloudinary, exactly as designed on the backend side.

---

## Part 4 — Deployment sequence (later — not the current step)

For when you're actually ready to deploy, not before:

1. Add your future Vercel domain + `http://localhost:3000` to `gateway-service`'s CORS allow-list, redeploy the backend on Render.
3. In v0, use its built-in **GitHub sync** to push the generated project to your empty frontend repo (or export and `git push` manually if you prefer more control over commit history).
4. `git clone` locally, `npm install`, `npm run dev`, set `NEXT_PUBLIC_API_BASE_URL` in `.env.local` pointing at the deployed Render gateway URL, and manually test the real auth/browse/apply flow end-to-end against the live backend.
5. Import the GitHub repo into Vercel (new project → import). Set the same `NEXT_PUBLIC_API_BASE_URL` (and any other env vars) in Vercel's project settings.
6. Push to `main` — Vercel auto-builds and deploys. Every subsequent push redeploys automatically; no manual redeploy step, ever.
7. Free tier note: Vercel's Hobby plan is free and sufficient for a personal/non-commercial project like this one — no card required beyond what you already used to sign up for v0.

No Docker involved anywhere in the frontend path — that's specific to the Render-hosted backend, not this stack.
