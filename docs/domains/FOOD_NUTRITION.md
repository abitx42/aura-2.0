# Aura 2.0 — Food, Nutrition & Health Intelligence

**Status:** Capability Specification (Roadmap Phase 4/5)  
**Philosophy:** Frictionless logging, non-obsessive tracking, and realistic nutrition context.

---

## 1. Frictionless Logging Architecture

Standard nutrition apps fail because searching brands and weighing grams creates fatigue. Aura offers four flexible capture methods:

```text
               USER EATS
                   │
    ┌──────────────┼──────────────┬──────────────┐
    ▼              ▼              ▼              ▼
1. Take Photo   2. Tell Aura   3. Repeat Meal  4. Manual
 (Camera Scan)   ("2 rotis")   ("Same lunch")   (Precise)
    │              │              │              │
    └──────────────┼──────────────┴──────────────┘
                   ▼
         COMPONENT EXTRACTION (AI Vision / NLP)
                   ▼
     CONFIRMATION PROMPT ("Does this look right?")
                   ▼
     STRUCTURED NUTRITION LOOKUP (SQL DB)
                   ▼
          COMMITTED FOOD LOG
```

---

## 2. Indian Cuisine Intelligence 🇮🇳

Indian food varies widely based on preparation, oil, and ghee. Aura's vision system extracts ingredients and asks for quick confirmation before calculating estimates:

```text
Aura Vision detects:
  🍚 Rice — 1 bowl
  🫘 Dal Tadka — 1 bowl
  🫓 Roti — 2 pieces
  🧀 Paneer Sabzi — 1 bowl

[ Edit Items ]    [ Looks Correct ]
```
- **Natural Language Corrections**: Users can adjust with casual speech (*"Actually 3 rotis and very little ghee"*), avoiding multi-field form editing.
- **Nutritional Safety**: The AI identifies foods; structured database tables calculate calorie and macronutrient totals.

---

## 3. Meal Memory (Frequent & Saved Meals)

Aura observes eating patterns and automatically prompts to save frequent meals:
- *"Hostel Dinner"* (2 Roti, Dal, Rice, Sabzi)
- *"Quick Morning Breakfast"* (2 Eggs, 2 Toast, Milk)
Once saved, re-logging takes one tap: `[ Repeat Usual Breakfast ]`.

---

## 4. Daily Health Balance (No Toxic Scoring)

Aura rejects single punitive health scores (e.g. *"Health Score: 42/100"*). Instead, it presents **Daily Balance**:
- 😴 **Sleep**: 🟡 (6h 15m vs. 7h 30m target)
- 🍛 **Nutrition**: 🟢 (72g / 90g protein logged)
- 💪 **Activity**: 🟡 (Workout completed, lower steps)
- 💧 **Hydration**: 🟢 (2.2L logged)
- 😊 **Mood**: 🟢 (Recorded as calm)
