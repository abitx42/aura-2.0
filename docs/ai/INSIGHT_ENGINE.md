# Aura 2.0 — Deep Life Analysis & Insight Engine

**Status:** Canonical Intelligence Specification  
**Primary Role:** Answers *"What meaningful patterns exist across the user's life domains?"*

---

## 1. Insight Generation Pipeline

```text
CANONICAL DATABASE
        │
        ▼
1. Data Quality Gate (Verify Minimum Sample Size)
        │
        ▼
2. Deterministic Statistical Analysis (SQL / Math)
        │ Computes variances, averages, completion percentages
        ▼
3. Pattern Candidate Detection
        │ Flags candidate: e.g. SLEEP_ASSOCIATED_WITH_TASK_COMPLETION
        ▼
4. Evidence Scoring (Confidence % Calculation)
        │
        ▼
5. AI Natural Language Translation
        │ Formats observed correlation without asserting causality
        ▼
6. Insight Priority Ranking & Fatigue Filter (Max 1–3 daily)
        │
        ▼
USER INTERFACE (Insight Card + Action Button)
```

---

## 2. Data Quality Gates (Minimum Observation Thresholds)

Aura never generates patterns from insufficient data:

| Pattern Domain | Minimum Verified Data Required | Rationale |
| :--- | :--- | :--- |
| **Sleep $\leftrightarrow$ Productivity** | 14 logged days | Eliminates random weekend noise and temporary illness. |
| **Planning $\leftrightarrow$ Completion** | 7 logged days | Requires at least one full weekly cycle of planned vs. locked days. |
| **Spending Trends** | 30 logged days | Accounts for monthly billing cycles and rent/tuition variations. |
| **Habit Consistency** | 14 logged days | Validates routine formation vs. novelty effect. |

---

## 3. Evidence Scoring & Confidence Levels

Every insight is internally scored and labeled:

- 🟢 **Strong Pattern (Confidence $\ge$ 80%)**: *"Aura has consistently noticed that your task completion is 31% higher on days following 7+ hours of sleep."*
- 🟡 **Possible Pattern (Confidence 60% – 79%)**: *"Aura noticed a potential trend: study sessions scheduled after 8 PM are postponed more frequently."*
- ⚪ **Early Observation (Confidence 40% – 59%)**: *"It's early to say for sure, but your workout completion seems higher before dinner."*

---

## 4. Insight Fatigue Prevention & Ranking

To avoid nagging or overwhelming the user, the Insight Engine applies an **Insight Priority Score**:
$$\text{Priority Score} = \text{Evidence Strength} + \text{Importance} + \text{Actionability} + \text{Novelty} - \text{Repetition}$$

- **Daily Cap**: Strictly **1 to 3 meaningful insights** presented during the Evening/Night Review.
- **Insight $\to$ Action**: Every insight offers a direct resolution:
  - `[ Try This ]` (Emits a `ProposedAction` to adjust schedule or target)
  - `[ Not Now ]`
  - `[ Don't Suggest Again ]`

---

## 5. Review Cycles

1. **Daily Insights**: Micro-observations during evening/night review.
2. **Weekly Review**: Comprehensive weekly digest (Consistency %, average sleep, spending breakdown, biggest friction point, recommended plan adjustments).
3. **Monthly Life Report**: Interactive "Life Wrapped" analyzing month-over-month growth, consistency streaks, productivity shifts, and routine milestones.
