# Aura 2.0 — AI Model Strategy & Multi-Tier Provider Gateway

**Status:** Canonical Model Strategy  
**Guiding Principle:** High reliability and low latency through task-specific model routing and multi-tier fallbacks.

---

## 1. Task-to-Model Routing Matrix

Rather than using a single expensive model for every query, Aura routes requests to the optimal model class:

```text
User Request / Background Job
              │
              ▼
    AI PROVIDER GATEWAY
              │
    ┌─────────┼─────────┬─────────┐
    ▼         ▼         ▼         ▼
  Intent    Vision    Reasoning  Batch
  Router    Router     Router    Insights
```

| Task | Requirements | Primary Model Tier | Fallback Tier |
| :--- | :--- | :--- | :--- |
| **Intent Classification** | Ultra-low latency (<300ms), lightweight token cost | Fast Open-Source / Small (e.g. Llama-3-8B via Groq / Gemini Flash) | Rule-based regex parser |
| **Universal Capture Parsing** | Accurate entity extraction into typed JSON | Small Fast Instruction Model | Gemini Flash / OpenAI Mini |
| **Context Reasoning & Actions** | Strict JSON schema following, no hallucinations | High-Instruction Reasoning Model (e.g. Llama-3-70B / Gemini Flash / Claude Sonnet) | Secondary hosted provider |
| **Food & Plate Vision** | Multimodal image understanding, ingredient breakdown | Multimodal Vision Model (e.g. Llama-3.2-Vision / Gemini Flash Vision) | Manual selection sheet |
| **Weekly / Monthly Review** | Deep synthesis, empathetic tone, high context window | Strong Reasoning Model (Executed via scheduled background job) | Cached template summary |

---

## 2. Multi-Tier Provider Hierarchy

To prevent platform lock-in and manage operating costs:

```text
Tier 1: Free Hosted / Open-Source (Groq, OpenRouter, Self-Hosted Ollama)
        │ Timeout (3500ms) or Rate Limited
        ▼
Tier 2: Managed Cloud Standard (Gemini Flash / OpenAI Mini)
        │ Timeout (5000ms) or 5xx Outage
        ▼
Tier 3: Graceful Deterministic Fallback (Offline Heuristics)
        • The user is notified that AI is momentarily offline
        • The core daily planning loop is NEVER blocked
```

---

## 3. Strict Schema Enforcement (Zod)

All model calls must utilize **Structured Outputs (JSON Schema Mode)**. The gateway enforces:
- Output must conform to the Zod schema for that specific intent.
- Zero markdown code blocks (e.g. ```` ```json ````) in structured action endpoints.
- If schema validation fails, the gateway executes **one automated repair pass**. If the repair fails, it logs an alert and drops the action payload, returning clean text to avoid crashing the client.
