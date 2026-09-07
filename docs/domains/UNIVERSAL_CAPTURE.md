# Aura 2.0 — Universal Quick Capture & Inbox

**Status:** Capability Specification  
**Access Point:** Center Floating Action Button (✦) on Bottom Navigation

---

## 1. The Multi-Intent Natural Language Parser

When users are in a hurry, they should not have to decide which tab to open. They dump everything into Universal Capture:

> *"Remind me to submit assignment on Friday, spent ₹250 on lunch, and call Rahul tonight."*

The parser splits the entry into discrete typed domain operations:

```text
Input String
     │
     ▼
Multi-Intent Tokenizer
     ├── 📌 Task: "Submit assignment" | Due: Friday 17:00
     ├── 💰 Expense: ₹250 | Category: Food / Lunch
     └── 📌 Task: "Call Rahul" | Scheduled: Tonight 20:00
     │
     ▼
User Confirmation Sheet
  [ Save All 3 Items ]  [ Edit Individual ]
```

---

## 2. Capture Modalities

1. **Voice Capture (🎤)**: Real-time on-device or fast API transcription into the multi-intent parser.
2. **Camera Scan (📷)**: Captures receipts (splits line items) or food plates (identifies dishes).
3. **Quick Task (✓)**: Fast single-field task creation with progressive disclosure for priority and duration.
4. **Quick Expense (₹)**: Instant numerical keypad + category chips.
5. **Quick Reflection (📝)**: Short mood or journal check-in.
