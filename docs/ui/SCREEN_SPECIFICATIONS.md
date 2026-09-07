# Aura 2.0 — Screen Specifications

**Status:** Implementation Blueprint  
**Coverage:** 21 MVP Screens with states, user interactions, and data contracts.

---

## Screen 01 — Splash
- **Purpose**: Initialize local Room DB, restore session token, and route to destination.
- **Routing Logic**:
  - No token $\longrightarrow$ Screen 02 (Authentication)
  - Token present + onboarding incomplete $\longrightarrow$ Screen 03 (Onboarding Welcome)
  - Token present + onboarding complete $\longrightarrow$ Screen 11 (Today)

---

## Screen 02 — Authentication
- **Purpose**: Sign up or log in with Email & Password.
- **Components**: Branded header, email input, password input, primary CTA `[ Continue with Email ]`, toggle `[ Already have an account? Log In ]`.
- **Validation**: Offline format validation before network request.
- **Errors**: User-friendly messages (e.g. *"Incorrect email or password"*, *"Saved locally; device is offline"*).

---

## Screens 03 to 10 — Conversational Onboarding Flow
- **Screen 03 (Welcome)**: Statement: *"Let's build a system around your real life."* $\longrightarrow$ `[ Let's Begin ]`.
- **Screen 04 (Basic Profile)**: Single focused question: *"What should Aura call you?"* + Date of birth + Timezone.
- **Screen 05 (Life Context)**: Lifestyle selector: `Student` | `Working Professional` | `Freelancer` | `Other`.
- **Screen 06 (Routine & Sleep Baseline)**: Usual wake and sleep time pickers. Question: *"Do you want to maintain or improve this schedule?"* (No judgment).
- **Screen 07 (Goals)**: Multi-select: `Productivity`, `Studies`, `Fitness`, `Sleep`, `Finances`, `Habits`.
- **Screen 08 (Challenges)**: Multi-select: `Procrastination`, `Poor planning`, `Phone distractions`, `Low energy`.
- **Screen 09 (Aura Understanding)**: Transparent card summarizing learned context.
  - User can tap `[ Edit ]` or confirm with `[ Looks Right ]`.
- **Screen 10 (Onboarding Complete)**: *"Your Aura is ready."* Primary CTA navigates directly to `[ Plan Tomorrow ]` (Screen 13).

---

## Screen 11 — Today (The Main Screen)
- **Header**: Time-adaptive greeting (e.g. *"Good morning, Aadi ☀️"* + Date).
- **Plan Status Banner**: `Planned` | `Not Planned Yet` (with `[ Organize Today ]` button) | `Plan Adapted`.
- **Current Focus Card**: Deterministically displays the single most urgent task:
  - Active task $\to$ Scheduled time task $\to$ Next Critical task $\to$ Next Planned task.
  - Buttons: `[ Start Focus ]` (or `[ Complete ]` / `[ Reschedule ]`).
- **Today's Timeline**: Chronological items displaying time, completion checkbox, title, and priority pill.
- **Daily Progress**: Visual progress bar (e.g. `4 / 6 Completed`).
- **Quick Actions**: Add task, Ask Aura, Review plan.

---

## Screens 12 to 16 — Planning & Day Locking
- **Screen 12 (Plan Overview)**: Tabs for `Today`, `Tomorrow`, `Upcoming`.
- **Screen 13 (Plan Tomorrow)**:
  - Step 1: Fixed commitments (calendar blocks).
  - Step 2: Unfinished tasks triage (Move to tomorrow, skip, or schedule later).
  - Step 3: Add new priorities with estimated durations.
  - Step 4: Time budget analysis (warns if planned hours exceed available hours).
- **Screen 14 (Create / Edit Task)**: Simple title input, priority pills (Normal, Important, Critical), duration picker, time slot.
- **Screen 15 (Task Details)**: Full task history, notes, reschedule options, and delete with confirmation.
- **Screen 16 (Lock Tomorrow 🔒)**:
  - Summary: Total planned tasks, top priorities, wake/sleep target.
  - Primary CTA: `[ Lock Tomorrow 🔒 ]`.
  - Modification rule: Editing a locked plan remains allowed, prompting: *"You are updating a locked plan. (Optional reason: unexpected event, underestimation, emergency)"*.

---

## Screens 17 & 18 — Reflection & Progress
- **Screen 17 (Night Review 🌙)**:
  - Evening prompt: *"Let's review today."*
  - Summarizes completed vs. remaining tasks.
  - Reconciles remaining tasks: `[ Reschedule ]` | `[ Skip ]` | `[ Mark Incomplete ]`.
  - Seamlessly bridges to `[ Plan Tomorrow ]`.
- **Screen 18 (Progress & Consistency)**:
  - Personal streak: Days planned, Days locked, Completion rate.
  - GitHub-style Consistency Map.

---

## Screens 19 to 21 — AI, States & Aura Brain
- **Screen 19 (Aura AI)**: Conversational assistant displaying interactive **Action Cards** (`AuraPrimaryAction` vs. `AuraSecondaryAction`), never plain text command dumps.
- **Screen 20 (States)**: Standardized empty states (icon in circle + bold title + guidance copy) and shimmer loading skeletons (`AuraShimmer`).
- **Screen 21 (You & Aura Brain)**: Profile settings, sync diagnostics, and the **What Aura Knows About You** memory manager (`FACT`, `PREFERENCE`, `ROUTINE`, `PATTERN` with Edit/Delete controls).
