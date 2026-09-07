# Aura 2.0 — Product Requirements Document (PRD)

**Document Status:** Canonical PRD  
**Target Version:** Aura 2.0 MVP  

---

## 1. Executive Summary

Modern productivity software forces individuals to fragment their lives across 5 to 7 disconnected applications: a task manager (Todoist/TickTick), a calendar (Google Calendar), a health/sleep app (Apple Health/Whoop), a calorie tracker (MyFitnessPal), an expense tracker (Wallet/Splitwise), and an AI chat assistant (ChatGPT). 

None of these apps talk to each other. When a user sleeps poorly, their task manager still demands 10 hours of intense focus. When a user overspends on food delivery, their health app has no awareness of their dietary shift.

**Aura 2.0** is the **Personal Operating System for daily life**. It connects planning, execution, habits, nutrition, sleep, and finance into one intelligent system. 

Instead of an overwhelming dashboard of widgets or a passive chatbot, Aura guides the user through an intentional daily behavioral loop:
$$\text{Plan Tomorrow} \longrightarrow \text{Lock Tomorrow 🔒} \longrightarrow \text{Execute (Today)} \longrightarrow \text{Night Review 🌙} \longrightarrow \text{Learn \& Plan Better}$$

---

## 2. Target Personas

Aura is built for **one individual managing one life** (single-player personal OS, not enterprise team management):

1. **The Overwhelmed Student / Young Professional**:
   - Struggles with procrastination, inconsistent sleep schedules, and overloaded schedules.
   - Needs clarity on *"What should I do right now?"* rather than managing complex project boards.
2. **The Inconsistent Improver**:
   - Starts habits enthusiastically but drops them within two weeks.
   - Needs gentle, adaptive accountability that challenges avoidance while fixing unrealistic schedules.
3. **The Multi-App Consolidator**:
   - Tired of logging into 5 different tools daily to monitor tasks, workouts, food, and expenses.

---

## 3. The Four Daily Questions Aura Answers

- **Morning (07:00 – 11:00)**: *"What am I committed to doing today?"* (Morning Brief & Timeline).
- **Daytime (11:00 – 18:00)**: *"What should I focus on right now?"* (Deterministic Current Focus).
- **Evening (18:00 – 21:00)**: *"What actually happened today, and what remains?"* (Daytime reconciliation).
- **Night (21:00 – 00:00)**: *"How should tomorrow be planned better?"* (Night Review & Lock Tomorrow).

---

## 4. Core Features & System Capabilities

### 4.1. Intentional Planning & Day Lock 🔒
- **Plan Tomorrow Flow**:
  - Step 1: Review fixed calendar commitments.
  - Step 2: Reconcile unfinished tasks from today (move, schedule, skip).
  - Step 3: Add new priorities with estimated durations.
  - Step 4: Validate schedule against available hours (prevent silent overlap).
  - Step 5: **Lock Tomorrow**: Explicit commitment timestamp storing the intended plan.
- **Modification without Shame**: Locked plans can be adapted at any time. Changes are acknowledged as intentional adjustments (`Reason: unexpected event / underestimation / avoidance`).

### 4.2. Action-Oriented Today Screen
- **Deterministic Current Focus**:
  $$\text{Active Task} \longrightarrow \text{Currently Scheduled Task} \longrightarrow \text{Next Critical Task} \longrightarrow \text{Next Planned Task}$$
  No AI hallucination or latency in determining what the user needs to do next.
- **Chronological Timeline**: Clear visual states (Upcoming, Active, Completed, Rescheduled, Missed). Color is never the sole indicator of state.
- **Daily Progress**: Real progress metrics (e.g. 4/6 tasks completed) without artificial productivity gamification.

### 4.3. Night Review 🌙
- Honest close of the day.
- Resolves each remaining task: Reschedule, Skip, or Mark Incomplete.
- Direct seamless transition into the Plan Tomorrow flow.

### 4.4. Adaptive Accountability System
- Aura adapts its communication style based on evidence:
  - 🟢 **Supportive**: First failure, new user, genuine unexpected emergency.
  - 🟡 **Balanced**: Developing pattern (e.g., workout postponed 3 times in 7 days).
  - 🔴 **Strict Accountability**: Repeated avoidance pattern (e.g., moved 6 times with ample free time).
- **"Excuse vs. Reality" Engine**: Distinguishes between unrealistic scheduling (too many tasks $\to$ fix system) versus avoidance (procrastination $\to$ direct challenge).

### 4.5. Deep Life Analysis & Cross-Domain Insights
- Uncovers patterns across domains:
  - Sleep $\leftrightarrow$ Task completion rates.
  - College schedule $\leftrightarrow$ Food delivery spending.
  - Overloaded plans $\leftrightarrow$ Postponed tasks.
- **Correlation $\neq$ Causation**: Insights state observed correlations with confidence percentages; never false causal claims.
- **Insight Fatigue Prevention**: Maximum 1 to 3 prioritized insights daily.

### 4.6. Aura Brain & Transparent Memory
- Memory categories: `FACT`, `PREFERENCE`, `ROUTINE`, `PATTERN`.
- **"Aura Noticed..." Flow**: Pattern candidates require explicit user confirmation before becoming permanent knowledge.
- Full user transparency: users can inspect, edit, or delete any memory.

### 4.7. AI Safety & Proposed Actions
- AI never directly writes to user domain data.
- Suggestions emit a `ProposedAction`. The user must tap `Approve`, and the deterministic backend validates and commits the change.

---

## 5. Non-Functional Requirements

1. **Offline-First Resilience**:
   - All core user actions (task CRUD, plan locking, review) must succeed locally without an active network connection.
   - Background synchronization syncs with cloud PostgreSQL when connectivity resumes.
2. **Instant UI Responsiveness**:
   - Local Room queries must complete in under 50ms.
   - Tap interactions provide instant tactile feedback via `AuraSpringPress` and `AuraHaptics`.
3. **Data Privacy & Ownership**:
   - Zero selling or commercial sharing of user data.
   - Fine-grained permission model for phone awareness (Calendar, Health, Notifications, Screen Assist).
4. **Design Aesthetic**:
   - "Premium Personal OS": calm, minimalist, glassmorphic accents, dark (`#0D0E10`) / light (`#F7F7F5`) modes.
