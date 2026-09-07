# Aura 2.0 — Backend Service

**Stack:** Node.js, Fastify, TypeScript, PostgreSQL, Zod.

## Architecture Guidelines
- **Modular Monolith**: Auth, Tasks, Plans, Sync, Context Engine, Insight Engine.
- **Deterministic Math**: Use SQL queries for all calculations and metrics.
- **Action Safety**: Serialize AI suggestions into `proposed_actions` table; execute only after explicit user approval.
