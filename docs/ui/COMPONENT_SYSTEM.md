# Aura 2.0 — Compose Component System & Library

**Status:** Canonical Component Specification  
**Tech Stack:** Kotlin, Jetpack Compose, Material 3, AuraTokens

---

## 1. Core Component Catalog

All components read strictly from `MaterialTheme.colorScheme.*` and reuse token scales from `AuraTokens.kt`.

### 1.1. `AuraNumberedStat`
Displays tracked-caps eyebrow label, bold hero metric, and supporting caption:
```text
01 · SPENT
₹ 420
+12% vs yesterday
```
- Eyebrow: `labelSmall` with `letterSpacing = 1.sp`
- Metric: `displayMedium` with `FontWeight.ExtraBold`
- Caption: `bodySmall` with `color = MaterialTheme.colorScheme.onSurfaceVariant`

### 1.2. `AuraPrimaryAction` & `AuraSecondaryAction`
Consistent CTA pairing used across every action sheet and card:
- **`AuraPrimaryAction`**: Filled pill button (`containerColor = MaterialTheme.colorScheme.primary`, `contentColor = MaterialTheme.colorScheme.onPrimary`).
- **`AuraSecondaryAction`**: Dashed-outline pill button (`border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant, PathEffect.dashPathEffect(...))`).
- Both automatically wrap touch targets in `AuraSpringPress` and fire `AuraHaptics.lightTick()` on tap.

### 1.3. `AuraProgressRing`
Circular stroke ring for time elapsed and daily progress:
- Animated sweep angle using `AuraAnimTiming.Standard` (never snaps abruptly).
- Large centered metric with optional trailing unit icon.
- Unfilled background track with subtle opacity (`0.12f`).

### 1.4. `AuraPeriodSelector`
Horizontal scrollable pill selector for dates/months (e.g. `AUG-26`, `SEP-26`):
- Active pill indicator slides between selections using `AuraTabIndicator` rather than abruptly swapping.
- Tap triggers `AuraHaptics.selectionClick()`.

### 1.5. `AuraEmptyState`
Standardized placeholder when lists have zero items:
- Centered circular container with icon.
- Bold heading (e.g. *"Your day is clear"*).
- Muted guidance copy with clear next action button.

### 1.6. `AuraHubCard`
2-column grid item for feature navigation:
- Icon in circular tint + module title + one-line dynamic stat (e.g. `Tasks · 3 pending`).
- Bounded by `AuraCornerRadius.Card` (16dp) with subtle border.

### 1.7. `AuraDismissible`
Swipe-to-dismiss container enhanced with:
- Soft edge gradient glow revealing action colors.
- Smooth spring physics when returning from incomplete swipe.

### 1.8. `AuraLoadingState` & `AuraShimmer`
Used everywhere data is fetching (Room read, sync catch-up):
- Zero bare, unbranded spinners.
- Fluid shimmering card skeletons matching the exact geometry of destination content.
