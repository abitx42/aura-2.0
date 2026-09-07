# Aura 2.0 — Phone Awareness & Permission Architecture

**Status:** Capability Specification (Roadmap Phase 7)  
**Boundary:** Progressive, permission-based awareness — not unrestricted spyware.

---

## 1. Phone Awareness as an Input Layer

Phone context feeds into the system as an **input adapter**, never as the core brain:

```text
              USER'S PHONE ENVIRONMENT
                         │
     ┌───────────────────┼───────────────────┐
     ▼                   ▼                   ▼
 Calendar (Read)    Health Connect     Notifications (Permitted)
     │                   │                   │
     └───────────────────┼───────────────────┘
                         ▼
               EVENT DETECTION LAYER
                         │ Filters noise & sanitizes PII
                         ▼
                    LIFE EVENTS
                         │
                         ▼
                  CONTEXT ENGINE
```

---

## 2. Four Awareness Levels

| Level | Name | Permissions & Scope |
| :--- | :--- | :--- |
| **Level 0** | **Private / Manual** | Aura knows only what is explicitly entered by hand. Ideal for privacy-first users. |
| **Level 1** | **Connected** | Connects Android Calendar (read events) and Android Health Connect (read steps/sleep). |
| **Level 2** | **Smart Detection** | Parses permitted transaction notifications (e.g. UPI/bank SMS) into clean JSON records. Raw messages are immediately discarded. |
| **Level 3** | **Screen Assist** | On-demand, user-triggered assistant. Only active when explicitly invoked for whitelisted apps. |

---

## 3. The Event Sanitization Pipeline

```text
Raw Signal: "₹450 paid to Swiggy on 07-Sep"
       │
       ▼
Event Detector matches transaction signature
       │
       ▼
Privacy Filter strips phone numbers, balance, and sender details
       │
       ▼
Life Event Created:
{
  "type": "TRANSACTION_DETECTED",
  "amount": 450,
  "merchant": "Swiggy",
  "category": "FOOD"
}
       │
       ▼
Raw SMS / Notification text is purged from memory
```
