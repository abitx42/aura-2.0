# Aura 2.0 — Context Engine V1 Specification

**Status:** Canonical Intelligence Specification  
**Primary Role:** Answers *"What information does Aura need right now?"*

---

## 1. Architectural Pipeline

The Context Engine operates as a strict deterministic pipeline before any token is transmitted to an LLM provider:

```text
User Input / Event
       │
       ▼
1. Intent Analyzer (Rule & Embedding Classifier)
       │ Determines intent domain: PLANNING | TASK_QUERY | REFLECTION | GENERAL
       ▼
2. Context Planner
       │ Decides required database entities and historical time window (e.g. 7 days)
       ▼
3. Deterministic SQL Retriever
       │ Executes exact SQL queries for counts, completion rates, and timestamps
       ▼
4. Context Builder & Compressor
       │ Sanitizes sensitive fields and formats compact JSON payload
       ▼
5. LLM Gateway (Structured Outputs)
       │ Emits markdown narrative + structured ProposedAction JSON (if applicable)
       ▼
6. Response Parser & Action Serializer
       │ Commits ProposedAction to DB (if any) and sends UI state to client
```

---

## 2. Intent Domains & Retrieval Rules

| Intent Domain | Trigger Examples | Deterministic Data Retrieved |
| :--- | :--- | :--- |
| `PLANNING_ASSIST` | *"Help me plan tomorrow"*, *"I'm overloaded"* | Tomorrow's calendar events, today's unfinished tasks, available time blocks, user sleep baseline. |
| `TASK_QUERY` | *"What do I have left today?"*, *"Did I study?"* | Today's tasks, statuses, duration elapsed, current focus. |
| `REFLECTION` | *"How was my week?"*, *"Why am I behind?"* | 7-day task completion rate, days locked, postponed task frequency, sleep durations. |
| `FINANCE_QUERY` | *"Can I afford this?"*, *"How much spent on food?"* | Deterministic category sum from transactions, remaining monthly discretionary budget. |

---

## 3. Privacy & Context Isolation Boundary

To protect user privacy and maximize prompt efficiency:
- **No Global Dumping**: The Context Engine **never** sends the user's entire life history or unrelated domain logs.
- If a user asks a study planning question, health metrics, financial transactions, and mood entries are strictly excluded from the LLM prompt.
- Factual numbers (sums, averages, percentages) are calculated in SQL and provided as immutable facts. The prompt explicitly instructs the LLM:
  > *"You are provided with verified database calculations. You must reason using these exact numbers. Never invent or adjust figures."*
