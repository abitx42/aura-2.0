# Aura 2.0 — Aura Brain Memory System

**Status:** Canonical Memory Specification  
**Guiding Principle:** Explicit transparency and user ownership over personal knowledge.

---

## 1. Memory Taxonomy

Aura structures personal knowledge into four distinct memory categories:

```text
┌────────────────────────────────────────────────────────┐
│                       AURA BRAIN                       │
│                                                        │
│  ┌───────────────────────┐   ┌──────────────────────┐  │
│  │         FACT          │   │      PREFERENCE      │  │
│  │ "Computer Science     │   │ "Prefers evening     │  │
│  │  student; vegetarian" │   │  workout sessions"   │  │
│  └───────────────────────┘   └──────────────────────┘  │
│                                                        │
│  ┌───────────────────────┐   ┌──────────────────────┐  │
│  │        ROUTINE        │   │       PATTERN        │  │
│  │ "Sleeps at 01:00 AM;  │   │ "Higher task focus   │  │
│  │  wakes at 08:30 AM"   │   │  between 18:00-21:00"│  │
│  └───────────────────────┘   └──────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

---

## 2. Inferred Knowledge vs. User-Confirmed Facts

Aura maintains a strict trust boundary between what the user said and what the AI inferred:

- **User-Provided Memories**: Confidence = 1.00, `confirmed_by_user = true`.
- **AI-Inferred Patterns**: Start as tentative observations (`confidence < 0.80`, `confirmed_by_user = false`).

### The "Aura Noticed..." User Confirmation Loop

```text
Aura notices repeated trend in life_events
       │
       ▼
Surfaces Card during Review:
"🧠 Aura noticed something:
 You seem to complete study tasks significantly faster in the morning.
 [ That's True ]  [ Not Always ]  [ Don't Remember ]"
       │
       ▼
User taps "That's True"
       │
       ▼
Context Memory updated:
memory_type = 'PATTERN', confirmed_by_user = true
```

---

## 3. Aura Brain Transparency Screen

Users retain complete visibility and sovereignty over everything Aura knows:

```text
YOU ──▶ AURA BRAIN ──▶ WHAT AURA KNOWS ABOUT YOU

Facts (3)
  • Student at University           [Edit] [Delete]
  • Vegetarian                      [Edit] [Delete]

Routines (2)
  • Sleep baseline: 1:00 AM - 8:30  [Edit] [Delete]

Observed Patterns (2)
  • Productive peak: 6 PM - 9 PM    [Confirm] [Forget]
  • High food spend on weekends     [Confirm] [Forget]
```
