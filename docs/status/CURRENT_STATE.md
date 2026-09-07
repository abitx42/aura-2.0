# Aura 2.0 — Current Project State & Decisions Log

**Last Updated:** 2026-09-07  
**Repository Branch:** `main`  
**Current Milestone:** Milestone 1 — Project Foundation & Scaffolding

---

## 1. Project Phase Status

| Phase / Area | Status | Notes |
| :--- | :--- | :--- |
| **Canonical Documentation Suite** | ✅ Complete | Full documentation and governance suite authored in `docs/`. |
| **Repository Scaffolding** | 🔄 In Progress | Root governance files, `.gitignore`, and package structures initialized. |
| **Android Client Setup** | ⏳ Queued | Kotlin + Jetpack Compose + Room + Hilt shell. |
| **Fastify Backend Setup** | ⏳ Queued | Node.js + TypeScript + Fastify + PostgreSQL schema migrations. |
| **MVP Core Daily Loop** | ⏳ Queued | Onboarding $\to$ Plan Tomorrow $\to$ Lock $\to$ Today $\to$ Night Review. |

---

## 2. Locked Architecture Decisions

1. **AI Action Boundary**: AI produces only `ProposedActions`. Direct writes to canonical tables are strictly forbidden.
2. **Deterministic Facts**: LLMs never compute sums, counts, or streaks. SQL executes all math.
3. **Offline-First Resilience**: All primary actions write to Room first, update the UI instantly, and sync via background queue.
4. **Theming Discipline**: Composable components must use `MaterialTheme.colorScheme.*` to support all 15 palette/mode combinations.
5. **Privacy Levels**: Progressively tiered phone awareness. Zero continuous background screen recording.

---

## 3. Active Next Steps
- Initialize Android module structure with Gradle version catalogs.
- Initialize Node/Fastify backend project with Prisma/Drizzle schema.
- Wire authentication endpoints.
