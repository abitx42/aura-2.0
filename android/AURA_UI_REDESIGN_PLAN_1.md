# Aura Notes — UI Redesign + Fix Master Plan

**Stack:** Kotlin, Jetpack Compose, Material3, Room, Firebase (Auth/Firestore/Storage)
**Goal:** paste this whole file into your AI assistant's chat and dispatch phases in order. Don't dispatch the whole thing at once — each phase should be a separate instruction, reviewed before the next.

---

## 0. What the reference screenshots actually tell us

Two reference apps, both fintech, both dark-themed. Neither should be cloned — extract the *composition patterns*, not the hex codes, because Aura Notes already has its own 5-palette theming engine (`Theme.kt`) that needs to keep working.

**From Axio (onboarding/dashboard):**
- One accent color used everywhere, never diluted with a second competing accent
- Progress dots where the active step is an elongated pill and inactive steps are plain circles
- A circular progress ring with a big centered number and a light stroke track for "spent this period"
- Dismissible insight cards with a soft gradient-glow edge
- Transaction rows: circular category icon, name, colored amount, date underneath

**From the second app (Guest/Personal/orange):**
- A numbered stat row — `01 · SPENT`, `02 · TXN`, `03 · PEOPLE` — small tracked-caps label under a large number
- Primary action = filled pill button; secondary action = dashed-outline pill button, placed side by side. This filled-vs-dashed pairing is used consistently for every primary/secondary CTA pair in the app.
- Horizontal scrollable month-pill selector (JUN-26 / JUL-26 / **AUG-26** / SEP-26 / OCT-26)
- Icon-in-circle empty states with a bold heading and a muted description line
- A 2-column grid of tool cards (icon + title + one-line stat) as a hub screen — this is directly relevant to Aura since you have 6+ modules that could use exactly this as a home/hub
- **Do not copy**: this app has a sponsored ad injected into the transaction list and a banner ad on the expense form. Aura Notes is not an ad-supported app — exclude this entirely, it's not part of the "theme," it's monetization cruft.

---

## 1. What's actually in the codebase already (don't rebuild these)

- `Theme.kt` already drives **5 palettes** (CYAN_GLOW, EMERALD_GARDEN, RADIANT_SUNSET, ROYAL_AMETHYST, OCEAN_BREEZE) × **3 modes** (DARK, LIGHT, AMOLED) — RADIANT_SUNSET is already close to the orange reference's palette, CYAN_GLOW is close to Axio's. **Do not add a new one-off accent color outside this system.**
- `AuraTokens.kt` already defines a corner-radius scale (Hero/Section/Card/Flow/Row/Input/Chip) and animation timings — reuse these, don't invent new ones.
- `AuraDismissible.kt`, `AuraSpringPress.kt`, `AuraTabIndicator.kt`, `AuraShimmer.kt`, `AuraScreenTransitions.kt` already exist — the dismissible-glow-card pattern from Axio is `AuraDismissible`, it just needs a visual pass, not a rebuild.
- **What's actually thin:** `Type.kt` defines exactly one text style (`bodyLarge`) and leaves the rest as commented-out placeholders using `FontFamily.Default` (the generic system font). This is the single biggest reason the app doesn't yet read as "premium" like the references — it has almost no typographic hierarchy.

**Architectural note worth flagging to your assistant (not blocking):** `Theme.kt` reassigns the `Aura*` colors in `Color.kt` as mutable global `var`s at composition time. This works but isn't idiomatic Compose — components should read `MaterialTheme.colorScheme.primary` (etc.), not import the raw `Aura*` vars directly, otherwise they can silently miss recomposition when the palette or mode changes. **Every new component built in this plan must use `MaterialTheme.colorScheme.*`, not the raw vars** — this one rule is what makes the redesign automatically work across all 15 palette/mode combinations for free.

---

## 2. Non-negotiables

- **Every existing module and feature stays.** Notes (with version history), Tasks (Kanban + Calendar), Journal, Habits, Money (accounts, transactions, debts, savings goals, reminders), Drawing Canvas, Clock Widget, onboarding — this plan changes how they look and move, never what they do or what they contain. If a phase would require cutting a feature to hit a visual pattern, the feature wins and the pattern gets adapted instead.
- No ads, no sponsored content, no third-party banners — ever
- Don't touch `Repository.kt`, `PendingOperation.kt`, `SyncWorker.kt`, `NetworkMonitor.kt` — offline sync logic is out of scope for this plan
- New components read `MaterialTheme.colorScheme.*`, never `AuraCyanNeon` etc. directly
- Reuse `AuraCornerRadius` / `AuraAnimTiming` from `AuraTokens.kt` — don't hardcode new dp/ms values
- Every new composable must render correctly in at least DARK+CYAN_GLOW and LIGHT mode before moving to the next phase — don't discover a light-mode contrast bug on phase 7

---

## 3. Phased task breakdown (dispatch one phase at a time)

### Phase 0 — Fix before you restyle (functional bugs, from the earlier code review)
1. No `google-services.json` exists and the `google-services` Gradle plugin isn't applied anywhere — `AuraApplication.kt` silently falls back to a placeholder Firebase project, so Auth/Firestore/Storage don't actually connect. Needs a real Firebase project wired in.
2. `AuthManager.kt` calls `.requestIdToken("YOUR_WEB_CLIENT_ID_FROM_FIREBASE_CONSOLE")` — literal placeholder, sign-in fails until replaced with a real OAuth web client ID.
3. `FirestoreSyncManager.userRoot()` falls back to `"anonymous"` when signed out — guard this so signed-out installs can't collide in one shared document.
4. `Repository.kt` uses `.fallbackToDestructiveMigration()` — fine for now, swap for real Room migrations before anyone but you has real data in the app.
5. `applicationId = "com.sahil.auranotes"` and `rootProject.name = "My Application"` — leftover template naming, rename to match Aura branding.

*Acceptance: app builds, sign-in actually completes against a real Firebase project, no naming placeholders remain.*

### Phase 1 — Typography (highest-leverage single change)
Rebuild `Type.kt` with a full Material3 type scale. Use one rounded geometric sans-serif across the whole app (e.g. Manrope or Plus Jakarta Sans, loaded as a bundled font resource) — weight does the hierarchy work, matching how both references use one family at different weights rather than mixing fonts.
- `displayLarge/Medium` — 34–40sp, ExtraBold — big hero numbers, greeting names
- `headlineLarge/Medium` — 24–28sp, Bold — section titles
- `titleLarge/Medium` — 18–20sp, SemiBold — card titles
- `bodyLarge/Medium` — 16/14sp, Regular
- `labelLarge/Medium/Small` — 12–13sp, Medium, letter-spacing ~1sp for tracked-caps eyebrow labels (`TOTAL BALANCE`, `01 · SPENT` style)

*Acceptance: every screen still compiles and renders with the new type scale; nothing reads as "default Android font" anymore.*

### Phase 2 — Shared component library
Build these once, reuse everywhere. All read `MaterialTheme.colorScheme.*` and `AuraTokens`. This is also where "smoother" gets built in from the start, not bolted on later — every component below wraps its tap target in `AuraSpringPress` and fires `AuraHaptics` on press by default, so smoothness is the baseline behavior, not a thing to remember per-screen:
1. `AuraNumberedStat` — the `01 · LABEL` / big number / caption pattern
2. `AuraPrimaryAction` + `AuraSecondaryAction` — filled pill vs. dashed-outline pill, used as a pair. Spring-press feedback + a light haptic tick on every tap.
3. `AuraProgressRing` — circular ring, light track, centered big number, optional trailing icon. Ring fill animates in with `AuraAnimTiming` rather than snapping to value.
4. `AuraPeriodSelector` — horizontal scrollable pill row for months/dates/periods, using `AuraTabIndicator` so the active pill slides between selections instead of jumping.
5. `AuraEmptyState` — icon-in-circle + bold heading + muted description, one composable reused everywhere
6. Restyle pass on existing `AuraDismissible` — add the soft gradient-edge glow treatment, keep its existing swipe-dismiss physics
7. `AuraHubCard` — icon-in-circle + title + one-line stat, for the toolbox grid
8. `AuraLoadingState` — wraps `AuraShimmer` as the one loading treatment used everywhere data is fetching (first Room read, waiting on Firestore sync) instead of a bare spinner

*Acceptance: each component previewed in isolation (Compose Preview) across DARK/CYAN_GLOW and LIGHT before moving on, and each interactive one is confirmed to spring-press and (where appropriate) fire haptics.*

### Phase 3 — Money module (most direct reference match)
Apply Phase 2 components to `MoneyComponents.kt`: `AuraProgressRing` for spend-this-month, `AuraNumberedStat` row for spent/transactions/people-style stats, `AuraPeriodSelector` for month navigation, `AuraPrimaryAction`/`AuraSecondaryAction` for add-transaction vs. split-expense, `AuraEmptyState` for empty ledgers.

### Phase 4 — Home/Hub + navigation
Add or restyle a hub screen in `MainAppContainer.kt` using `AuraHubCard` in a 2-column grid — one card per module (Notes, Tasks, Journal, Habits, Money, Drawing). Restyle the bottom nav so the active tab gets a filled pill/rounded-square background behind the icon, matching the reference's active-state treatment.

### Phase 5 — Remaining modules
Apply the same Phase 2 components to `NotesComponents.kt`, `TasksComponents.kt`, `JournalCalendarComponents.kt` — empty states, stat rows, and action-button pairs should look identical in pattern across every module. Consistency across modules is the actual goal, not any single screen looking good in isolation.

### Phase 6 — Onboarding
Apply the new type scale and `AuraPrimaryAction` to `OnboardingScreen.kt`. If it doesn't already have a step-progress indicator, add one using the elongated-pill-for-active / circle-for-inactive pattern from Axio.

### Phase 7 — Motion & haptic consistency pass
This is the phase that actually delivers "smoother," as opposed to just "restyled." Audit every screen for:
- Every tappable element (buttons, list rows, nav tabs, chips, FABs) uses `AuraSpringPress` — not just the new Phase 2 components, but anything pre-existing that got skipped
- Every screen-to-screen navigation goes through `AuraScreenTransitions` — check for any `NavHost` destination still using the default abrupt transition
- Every tab/segmented control (bottom nav, Trends/Categories-style pills, month selector) uses `AuraTabIndicator` for a sliding active state, not an instant swap
- Every loading moment (cold app start, Room→Firestore sync catch-up, image/drawing load) shows `AuraLoadingState`/`AuraShimmer`, never a blank frame or a bare spinner
- Meaningful actions (completing a task, saving a transaction, hitting a savings goal) get a haptic tick via `AuraHaptics` — small enough to not be annoying, present enough to feel tactile

*Acceptance: navigate the entire app start to finish without hitting a screen that visibly "jumps" instead of transitioning, and without a blank/spinner loading state anywhere Room or Firestore data is being fetched.*

### Phase 8 — Cleanup
- Split `MoneyComponents.kt` (4,474 lines) and `MainAppContainer.kt` (3,482 lines) into per-screen files now that the new components have simplified a lot of the inline code
- Spot-check 3–4 palette/mode combinations beyond the default (not all 15) for contrast issues
- Confirm nothing added in Phases 1–7 reads `AuraCyanNeon` etc. directly instead of `MaterialTheme.colorScheme`

---

## 4. What not to change

- The 5-palette × 3-mode theming engine's underlying logic in `Theme.kt` — extend what reads from it, don't replace the switching mechanism itself
- `Repository.kt`, sync, and offline-queue logic
- Anything in `data/Database.kt` entity definitions (schema changes are a separate, careful task involving migrations, not part of a UI pass)
