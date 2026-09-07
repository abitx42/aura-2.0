# Aura 2.0 — Android Application

**Stack:** Kotlin, Jetpack Compose, Material 3, Room, WorkManager, Hilt, Navigation Compose.

## Architecture Guidelines
- **Offline-First**: Write to Room locally first, emit StateFlow immediately, enqueue to WorkManager.
- **Tokens & Theming**: Reference colors via `MaterialTheme.colorScheme.*` and radius/motion tokens via `AuraTokens.kt`.
- **Tactile Feedback**: Wrap clickable elements in `AuraSpringPress` and fire `AuraHaptics`.
