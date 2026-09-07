# Aura 2.0 — Security & Privacy Architecture

**Status:** Canonical Security Specification  
**Guiding Principle:** User-owned privacy by default. Personal life data must never be commercialized, leaked, or exposed without explicit consent.

---

## 1. Data Protection Lifecycle

```text
DATA INGRESS
  • Password hashed via Argon2id / bcrypt before storage
  • HTTPS (TLS 1.3) enforced on all communication endpoints
        │
        ▼
LOCAL STORAGE (ANDROID)
  • Room SQLite database secured on device sandbox
  • JWT access tokens and refresh tokens stored in Android EncryptedSharedPreferences
        │
        ▼
DATA MINIMIZATION & SANITIZATION PIPELINE
  • Context Engine strips sensitive PII before calling AI providers
  • Zero financial account numbers or raw SMS content sent to LLM
        │
        ▼
AI PROVIDER BOUNDARY
  • AI calls set zero-retention flags where supported
  • Context limited to the minimum relevant domain facts
        │
        ▼
USER DELETION & EXPORT
  • Full data export (JSON) available on demand
  • Permanent account deletion triggers cascading purge across all tables
```

---

## 2. Phone Awareness Security Boundaries

To prevent Aura from becoming invasive or violating platform policies:

1. **No Silent Background Screen Scraping**:
   - Continuous background screen capture via Accessibility services is strictly prohibited.
   - Any screen assistance must be **user-initiated**, clearly indicated by a foreground notification, and restricted to whitelisted apps.
2. **Notification Event Sanitization**:
   - Permitted notifications (e.g. UPI/transaction alerts) are processed through an on-device regex filter.
   - Only the structured event payload (`amount`, `merchant`, `timestamp`) is retained. The raw message body is **immediately purged** from memory.
3. **Health Connect & Calendar Permissions**:
   - Uses official Android permission dialogs with explicit runtime consent.
   - Reads only permitted metrics (e.g. sleep duration, workout sessions). Never reads medical diagnostic records.

---

## 3. Aura Brain Sovereignty (Transparency Screen)

Users maintain constitutional rights over their digital brain:
- **Inspect**: Every stored memory (`FACT`, `PREFERENCE`, `ROUTINE`, `PATTERN`) is visible in the UI with its source and confidence score.
- **Edit**: Users can correct misunderstood context (e.g. change sleep routine or dietary preference).
- **Forget**: Users can tap `[ Forget This ]` to delete individual memories permanently from `context_memory`.
- **Nuclear Reset**: Users can purge all inferred patterns without deleting basic task history.
