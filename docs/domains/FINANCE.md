# Aura 2.0 — Money & Personal Finance Intelligence

**Status:** Capability Specification (Roadmap Phase 6)  
**Philosophy:** Contextual spending awareness and intentional budgeting without banking risk.

---

## 1. The Three-Layer Finance System

```text
┌────────────────────────────────────────────────────────┐
│             LAYER 3: FINANCIAL INTELLIGENCE            │
│  "You spent 64% of your discretionary budget on        │
│   food delivery this week. Want to adjust targets?"    │
└───────────────────────────▲────────────────────────────┘
                            │ Factual Context
┌───────────────────────────┴────────────────────────────┐
│            LAYER 2: DETERMINISTIC UNDERSTANDING        │
│  SQL calculations: Total Spent, Category Breakdown,    │
│  Remaining Budget, Daily Average, Recurring Bills      │
└───────────────────────────▲────────────────────────────┘
                            │ Normalized Records
┌───────────────────────────┴────────────────────────────┐
│              LAYER 1: TRANSACTION TRACKING             │
│  Manual Quick Capture, Receipt Scanning, Permitted     │
│  Notification / SMS Event Detection                    │
└────────────────────────────────────────────────────────┘
```

---

## 2. Signature Feature: "Can I Afford This?" 💳

When a user considers a discretionary purchase, they ask Aura: *"Can I afford ₹2,500 for sneakers?"*

Aura evaluates:
1. Available flexible cash
2. Upcoming fixed bills (rent, subscriptions)
3. Monthly savings goal target
4. Days remaining in current billing period

### Structured Verdicts:
- 🟢 **Comfortable**: *"You can buy this without impacting your monthly savings goal."*
- 🟡 **Possible, with Trade-off**: *"You have the cash, but it will reduce your planned savings buffer by 40%."*
- 🔴 **Caution**: *"This purchase exceeds your remaining discretionary budget for this week."*

---

## 3. Strict Security & Finance Boundaries

Aura's financial intelligence is strictly advisory:
- ✅ **Allowed**: Log transactions, compute category totals, analyze trends, model budgets.
- ❌ **Strictly Forbidden**:
  - Direct bank account integration or open-banking login tokens
  - Moving money or automating UPI / wire transfers
  - Storing payment credentials or credit card CVVs
