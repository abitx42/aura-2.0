# Aura 2.0 — Founder Feedback & 14-Day Testing Log

**Status:** Active Founder Testing Log  
**Phase:** Phase 2 (Founder Testing & Stabilization)  
**Tester:** Aadi (Founder)  
**Target Duration:** 7–14 Consecutive Days  
**Objective:** Live with Aura's daily loop on a physical Android device, catch real-world lifecycle issues, identify UX friction, and verify zero data loss across offline and online transitions.

---

## 📅 14-Day Daily Loop Verification Checklist

| Day | Date | Plan Locked? 🔒 | Morning Kickoff? ☀️ | Focus Executed? ⚡ | Night Review? 🌙 | Sync Verified? 🔄 | Notes / Rating |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **Day 1** | 2026-09-08 | [ ] | [ ] | [ ] | [ ] | [ ] | Initial install & baseline test |
| **Day 2** | 2026-09-09 | [ ] | [ ] | [ ] | [ ] | [ ] | |
| **Day 3** | 2026-09-10 | [ ] | [ ] | [ ] | [ ] | [ ] | |
| **Day 4** | 2026-09-11 | [ ] | [ ] | [ ] | [ ] | [ ] | |
| **Day 5** | 2026-09-12 | [ ] | [ ] | [ ] | [ ] | [ ] | |
| **Day 6** | 2026-09-13 | [ ] | [ ] | [ ] | [ ] | [ ] | |
| **Day 7** | 2026-09-14 | [ ] | [ ] | [ ] | [ ] | [ ] | **Week 1 Gate** (7-day evaluation) |
| **Day 8** | 2026-09-15 | [ ] | [ ] | [ ] | [ ] | [ ] | |
| **Day 9** | 2026-09-16 | [ ] | [ ] | [ ] | [ ] | [ ] | |
| **Day 10**| 2026-09-17 | [ ] | [ ] | [ ] | [ ] | [ ] | |
| **Day 11**| 2026-09-18 | [ ] | [ ] | [ ] | [ ] | [ ] | |
| **Day 12**| 2026-09-19 | [ ] | [ ] | [ ] | [ ] | [ ] | |
| **Day 13**| 2026-09-20 | [ ] | [ ] | [ ] | [ ] | [ ] | |
| **Day 14**| 2026-09-21 | [ ] | [ ] | [ ] | [ ] | [ ] | **Phase 2 Completion Gate** |

---

## 🚨 Founder Friction & Bug Triage Log

*Record any friction, awkward animations, unexpected behavior, sync delays, or crashes below. You can also submit these directly in the in-app Debug Screen.*

| ID | Timestamp | Severity (P0/P1/P2) | Area / Screen | Observed Friction / Bug | Expected Behavior | Status |
| :---: | :---: | :---: | :---: | :--- | :--- | :---: |
| *e.g.* `FB-01` | *2026-09-08* | *P1* | *Focus Timer* | *Screen locked, audio chime was quiet* | *Clear vibration/chime on completion* | *Open* |

---

## 📝 Daily Testing Journal & UX Observations

### Day 1 (2026-09-08)
- **App Version Tested:** 2.0.0 (Debug APK)
- **Device:** Physical Android phone
- **Observations:**
  - Installed via `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`.
  - Debug screen accessible via Security Settings.
- **Friction Points:**
- **Improvements Needed:**

---

## 🛠️ Diagnostics & Forensic Instructions

If the app ever crashes or encounters a visual glitch:
1. **In-App Logs**: Open `Settings` $\longrightarrow$ `Founder Debug Tools 🛠️` $\longrightarrow$ inspect `Recent Life Events & Logs`.
2. **Crash Log File**: Stored locally on phone at:
   `/data/data/com.example/files/aura_crash_log.txt`
3. **Capture Friction**: In `Founder Debug Tools 🛠️`, enter text under `Founder Friction Note` and tap `Save Note` to log immediately.
