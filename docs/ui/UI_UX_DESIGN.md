# Aura 2.0 — UI/UX Design System & App Flow

**Status:** Canonical Design System  
**Design Philosophy:** *"Premium Personal OS"* — Calm, intentional, tactile, and free of clutter.

---

## 1. Visual Identity & Aesthetic Principles

Aura combines the calm simplicity of Apple Health, the structural clarity of Notion, and the futuristic polish of Arc Browser:

- **Soft Glass Minimalism**: Rounded cards with subtle borders, generous white-space, and minimal visual noise.
- **No Clutter / Progressive Disclosure**: Screens answer one question at a time. Secondary controls appear only when needed.
- **No Generic AI Chat Grids**: Aura interactions produce tactile **Action Cards** rather than endless text transcripts.

---

## 2. Color Palette & Theming Engine

Aura maintains its **5-Palette $\times$ 3-Mode (Dark / Light / AMOLED)** engine via `Theme.kt`:
- `CYAN_GLOW` (Default neon cyan)
- `EMERALD_GARDEN` (Restorative sage/emerald)
- `RADIANT_SUNSET` (Warm orange/coral)
- `ROYAL_AMETHYST` (Deep violet/indigo)
- `OCEAN_BREEZE` (Cool maritime blue)

### Base Neutral Values
- **Dark Mode Background**: `#0D0E10` | Surface Cards: `#181A1F`
- **Light Mode Background**: `#F7F7F5` | Surface Cards: `#FFFFFF`
- **Aurora Signature Gradient**: Subtle blend from Violet (`#7C4DFF`) $\to$ Deep Blue (`#2979FF`) $\to$ Cyan (`#00E5FF`), used sparingly for AI focus moments and plan locks.

> **Component Rule:** Components must **always** read colors from `MaterialTheme.colorScheme.*`, never referencing hardcoded color variables directly.

---

## 3. Typography Scale

Built with a unified geometric sans-serif (e.g. **Manrope** or **Plus Jakarta Sans**):

| Style | Size | Weight | Tracking | Usage |
| :--- | :--- | :--- | :--- | :--- |
| `displayLarge` | 36sp | ExtraBold | -0.5sp | Hero numbers, large greeting names |
| `headlineMedium` | 24sp | Bold | -0.2sp | Section headers, modal titles |
| `titleMedium` | 18sp | SemiBold | 0sp | Card headlines, task titles |
| `bodyLarge` | 16sp | Regular | 0.1sp | Primary body text, descriptions |
| `labelSmall` | 12sp | Medium | +1.0sp | Tracked uppercase eyebrows (`01 · SPENT`, `TOTAL COMPLETED`) |

---

## 4. Navigation Architecture (5 Core Destinations)

```text
┌────────────────────────────────────────────────────────┐
│   [ 🏠 Today ]  [ 📅 Plan ]  ( ✦ )  [ 📊 Insights ]  [ 🤖 Aura ] │
└────────────────────────────────────────────────────────┘
```
1. **🏠 Today**: Current day's command center (Dynamic timeline, Current Focus).
2. **📅 Plan**: Calendar, task bank, and tomorrow's preparation.
3. **✦ Universal Capture (Center FAB)**: Instant modal to capture voice, tasks, notes, or expenses.
4. **📊 Insights**: Weekly review, consistency streaks, and cross-domain reports.
5. **🤖 Aura**: Contextual assistant with structured proposed action cards.

---

## 5. Dynamic Time-Adaptive Today Screen

The Today screen dynamically adapts its hierarchy based on time of day:

```text
🌅 Morning (07:00 – 11:00)
   • Morning Brief & Sleep Check-in
   • Today's Locked Commitment Timeline
   • Immediate First Priority ("Next Up")

☀️ Afternoon (11:00 – 18:00)
   • Current Focus Task with Start/Complete buttons
   • Daytime Progress Bar (e.g. 3/6 completed)
   • Unfinished Priority Re-evaluation

🌇 Evening (18:00 – 21:00)
   • Evening progress summary
   • Remaining open commitments
   • Prompt: "Wrap up today or reschedule?"

🌙 Night (21:00 – 00:00)
   • Night Review 🌙: Reconcile today's outcomes
   • Signature Call-to-Action: [ Plan Tomorrow & Lock 🔒 ]
```

---

## 6. Aura Consistency Map

Inspired by GitHub's contribution graph, Aura visualizes commitment consistency:
- Each cell represents a day.
- Color intensity reflects **how faithfully the user followed their own locked commitments** (not toxic hours worked).
- Tapping a cell displays the Day Summary (Tasks completed, lock timestamp, review completed).
