# Aura 2.0 — Product Metrics & Analytics Framework

**Status:** Canonical Product Framework  
**Philosophy:** Measure intentional life consistency, not addictive screen time.

---

## 1. The North Star Metric

> **Meaningful Planned Days Completed (MPDC)**
> The average number of days per active user where the user:
> 1. Planned tomorrow intentionally,
> 2. Locked their plan 🔒,
> 3. Completed or honestly reconciled $\ge 70\%$ of their commitments, and
> 4. Completed their Night Review 🌙.

A high MPDC proves Aura is successfully ingraining its core daily behavioral loop.

---

## 2. Core Operational Metrics

### 2.1. The Daily Loop Funnel
```text
Daily Active Users
       │
       ▼
Plan Tomorrow Initiated (% of DAU)
       │ Target: > 65%
       ▼
Plan Locked 🔒 (% of initiated)
       │ Target: > 80%
       ▼
Today Screen Opened Next Morning (% of locked)
       │ Target: > 85%
       ▼
Night Review Completed (% of opened)
       │ Target: > 70%
```

### 2.2. Consistency Metrics
- **Locked Day Streak**: Consecutive days with a locked plan.
- **Commitment Realization Rate**: Ratio of completed tasks to locked tasks (healthy range: 70% – 85%; <50% triggers the Unrealistic Planning intervention).
- **Excuse vs. Reality Split**: Proportion of rescheduled tasks flagged as Overloaded Schedule vs. Avoidance.

---

## 3. Deliberate Anti-Metrics (What We Do NOT Optimize For)

Aura is a tool for life, not an attention-trap:
- ❌ **Total Time Spent in App**: If a user completes their daily loop in 3 minutes total (1 min morning, 2 min evening), Aura is succeeding. We do not design for endless browsing.
- ❌ **Notification Click-Through Rate**: We do not send manipulative push notifications to artificially inflate engagement.
- ❌ **Total Task Count**: Inflating task lists with trivial items games traditional productivity apps; Aura rewards completing critical locked priorities.

---

## 4. Privacy-Preserving Telemetry Principles

- **Zero Content Leakage**: Analytics events track action types (e.g. `TASK_COMPLETED`, `PLAN_LOCKED`), never task titles, descriptions, notes, food items, or financial amounts.
- **Local Differential Aggregation**: Telemetry is batched and anonymized before transmission.
- **Opt-Out Control**: Users can toggle off usage analytics at any time via Settings $\to$ Privacy.
