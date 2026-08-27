# Role & Technical Objective
You are an expert Frontend UI/UX Developer specializing in Next.js, Tailwind CSS, and Framer Motion. Your task is to design a highly optimized, modern, lightweight, and interactive frontend architecture based on the following user journey and technical constraints.

---

## 🛠️ Tech Stack Specification
- **Framework:** Next.js 15 (App Router)
- **Styling:** Tailwind CSS (leveraging native grid, flexbox, and backdrop-blur utilities)
- **Animations:** Framer Motion (for smooth layout transitions, split-screen scaling, and switch-toggles)

---

## 🎨 Design Philosophy & Aesthetic Guidelines
- **Style:** Minimalist, sleek, ultra-modern, and lightweight.
- **Component Density:** Low. Prioritize generous whitespace, premium typography, and purposeful micro-interactions.
- **Usability:** High visual hierarchy with zero unnecessary cognitive load.
- **Color scheme:** Open — do not lock the design to a single fixed palette (earlier mockups used orange/purple; treat that as a placeholder, not a requirement). Choose a palette that works cleanly across both light and dark mode.

## 🌗 Theming: Dark Mode / Light Mode
- Implement a real, persisted **dark/light mode toggle** (not just `prefers-color-scheme` detection) — use Tailwind's `dark:` class strategy with the toggle state stored client-side (e.g. `localStorage`) so the choice survives a reload.
- Toggle control lives in the same sticky top area as the Global Switch (Contributor/NGO), so it's reachable from every screen without hunting for a settings page.
- Every component spec below (landing split-screen, registration forms, guest feed, modals) must define both a light and a dark variant — don't treat dark mode as an afterthought pass at the end.

## 📱 Responsive Requirements
- Two concrete target ranges, not just "responsive in general": **Android phone viewports** (~360–430px wide) and **laptop screens** (~1280–1600px wide, plus a check at 1920px).
- The split-screen landing layout in particular needs an explicit mobile behavior — a 50/50 horizontal split that works at 1440px will not read as two distinct halves at 375px. Define whether it stacks vertically, becomes a single toggle-driven view, or some other mobile-specific treatment; don't just let it compress.
- Test breakpoints at minimum: `sm` (mobile), `md` (tablet), `lg`/`xl` (laptop/desktop) using Tailwind's default scale unless you have a reason to customize it.

## 🚫 Explicitly Out of Scope
- **No live-update / real-time notification toasts** (the "Surat Textile Workers project is trending," "New NGO registered from Kerala" style popups seen in early mockups). There's no backend push mechanism behind these yet — build static, non-live versions of any social-proof UI, or omit it.

---

## 🗺️ Extended Guest Mode & Core Requirements

### 1. Split-Screen Landing Page & Universal Global Switch
- **Initial View:** A clean, full-screen split layout dividing the screen into **Contributor (Donor)** and **NGO** modes, featuring a smooth, responsive scaling animation when hovering over either side (desktop only — hover states don't apply on touch, so define a tap-based equivalent for mobile).
- **The Navigation Switch:** Once a side is selected (or when exploring in Guest Mode), a sleek, sticky **Global Switch Button** must remain accessible at the top of the interface, alongside the dark/light toggle. Toggling this button seamlessly slides the entire app context and layout from NGO view to Contributor view without requiring a page reload.

### 2. Comprehensive Guest Experience (Public Read-Only Access)
Guests must have unrestricted, beautifully formatted read-only access to explore the core platform data. The UI must cleanly display:
- **Profiles:** Public views for both individual Contributors (impact areas, history) and NGOs (mission, region).
- **Problems & Status:** Active problem statements, live project statuses, and structural milestone progress.
- **Assignments:** Clear maps or relational chains showing exactly which contributor is matched to which problem (`Applications` in the backend).
- **NGO-Contributor Connections:** A discovery wall or feed showing historical pairings and successful collaborations — static content pulled on load, not a live/pushed feed (see Out of Scope above).

### 3. First-Time User Guided Onboarding (The Blur Pop-ups)
- **Trigger:** Triggers automatically when a new user enters Guest Mode or interacts with a profile/problem card for the first time.
- **Aesthetic:** A compact, elegant tutorial modal pops up. The background instantly shifts to a premium blurred state (`backdrop-blur-md bg-white/30` in light mode, an appropriately dark-mode-aware equivalent — not the same opacity/color values reused verbatim — in dark mode) to isolate the onboarding window.
- **Functionality:** A sleek multi-step progression explaining how the relational connection between problems, NGOs, and contributors works on this platform.

### 4. Contextual Auth Modals (Login/Register Gate)
- **Behavior:** When a guest attempts a restricted action (e.g., "Apply", or "Submit a Problem Statement"), a smooth entry modal appears.
- **Dynamic State:** The modal must dynamically adapt its entry fields, context, and copy depending on whether the user is registering as a Contributor or an NGO — matching the backend's separate `POST /api/v1/core/profiles/ngo` and `POST /api/v1/core/profiles/contributor` flows.

### 5. Private Authenticated Interfaces
- **Profile Management Pages:** Private edit states for profiles matching the layout of the public guest profiles but adding interactive form controls.
- **Note:** hold off on an admin/verification dashboard until it's confirmed there's a backend role and endpoints to back it — see the flag above.

---

## ⚙️ Technical Blueprint Request
Please generate the comprehensive Next.js frontend implementation for this architecture.

Provide:
1. An optimized **App Router Folder Structure** (`app/(public)`, `app/(dashboard)`, etc.) keeping public guest views cleanly isolated from private authenticated routes.
2. The component code for the **Global Switch Toggle** and the **Dark/Light Mode Toggle** using Framer Motion for fluid transitions, including the responsive mobile behavior for the split-screen landing.
3. The component code for the **Public Profile/Problem Feed View** with the responsive `backdrop-blur` onboarding popup modal integrated natively, in both light and dark variants.
