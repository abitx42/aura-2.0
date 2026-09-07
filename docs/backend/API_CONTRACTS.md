# Aura 2.0 — Backend API Contracts

**Status:** Canonical API Contract  
**Protocol:** REST over HTTPS  
**Base URL:** `/api/v1`  
**Data Format:** `application/json` (UTF-8, ISO-8601 UTC timestamps, UUID primary keys)

---

## 1. Global Standards & Envelope

### 1.1. Common Request Headers
```http
Authorization: Bearer <jwt_access_token>
Content-Type: application/json
X-Client-Version: 2.0.0
X-Device-Id: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d
Idempotency-Key: <optional_uuid_for_mutations>
```

### 1.2. Success Response Envelope
```json
{
  "success": true,
  "data": { ... },
  "meta": {
    "serverTime": "2026-09-07T18:30:00Z"
  }
}
```

### 1.3. Error Response Envelope
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The planned end time must be after the start time.",
    "details": [
      {
        "field": "plannedEndTime",
        "issue": "must_be_after_start_time"
      }
    ]
  }
}
```

Standard Error Codes:
- `UNAUTHORIZED` (401)
- `FORBIDDEN` (403)
- `NOT_FOUND` (404)
- `VALIDATION_ERROR` (422)
- `CONFLICT_ERROR` (409)
- `RATE_LIMITED` (429)
- `INTERNAL_SERVER_ERROR` (500)
- `AI_UNAVAILABLE` (503)

---

## 2. Authentication & Profile Endpoints

### 2.1. Sign Up
- **Endpoint**: `POST /api/v1/auth/signup`
- **Request**:
  ```json
  {
    "email": "user@example.com",
    "password": "SecurePassword123!",
    "preferredName": "Aadi"
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "success": true,
    "data": {
      "user": {
        "id": "c1f7b0f6-9c4b-4f9e-a0e1-6d7c8d9e0f1a",
        "email": "user@example.com",
        "preferredName": "Aadi",
        "timezone": "Asia/Kolkata"
      },
      "accessToken": "eyJhbGciOi...",
      "refreshToken": "d8e9f0..."
    }
  }
  ```

### 2.2. Log In
- **Endpoint**: `POST /api/v1/auth/login`
- **Request**:
  ```json
  {
    "email": "user@example.com",
    "password": "SecurePassword123!"
  }
  ```
- **Response (200 OK)**: Same payload as Sign Up.

### 2.3. Update Profile Baseline (Onboarding)
- **Endpoint**: `PUT /api/v1/profile`
- **Request**:
  ```json
  {
    "lifestyleType": "STUDENT",
    "typicalWakeTime": "08:00:00",
    "typicalSleepTime": "01:00:00",
    "planningStyle": "BALANCED",
    "goals": ["PRODUCTIVITY", "STUDIES", "FITNESS"],
    "challenges": ["PROCRASTINATION", "POOR_PLANNING"]
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": {
      "id": "c1f7b0f6-9c4b-4f9e-a0e1-6d7c8d9e0f1a",
      "lifestyleType": "STUDENT",
      "onboardingCompleted": true
    }
  }
  ```

---

## 3. Tasks & Daily Plans

### 3.1. Create Task
- **Endpoint**: `POST /api/v1/tasks`
- **Request**:
  ```json
  {
    "title": "Complete DSA Assignment",
    "description": "Graphs and dynamic programming problems",
    "priority": "CRITICAL",
    "estimatedDurationMinutes": 45,
    "plannedDate": "2026-09-08",
    "plannedStartTime": "16:00:00",
    "plannedEndTime": "16:45:00"
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "success": true,
    "data": {
      "id": "task_550e8400-e29b-41d4-a716-446655440000",
      "title": "Complete DSA Assignment",
      "status": "PLANNED",
      "priority": "CRITICAL",
      "version": 1,
      "createdAt": "2026-09-07T18:30:00Z"
    }
  }
  ```

### 3.2. Complete Task
- **Endpoint**: `POST /api/v1/tasks/:id/complete`
- **Request**: `{}`
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": {
      "id": "task_550e8400-e29b-41d4-a716-446655440000",
      "status": "COMPLETED",
      "completedAt": "2026-09-07T18:45:00Z",
      "version": 2
    }
  }
  ```

### 3.3. Lock Tomorrow's Plan 🔒
- **Endpoint**: `POST /api/v1/daily-plans/:id/lock`
- **Request**:
  ```json
  {
    "planDate": "2026-09-08",
    "taskIdsInOrder": [
      "task_550e8400-e29b-41d4-a716-446655440000",
      "task_661f9511-f30c-52e5-b827-557766551111"
    ]
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": {
      "id": "plan_772a0622-a41d-63f6-c938-668877662222",
      "planDate": "2026-09-08",
      "status": "LOCKED",
      "lockedAt": "2026-09-07T21:45:00Z",
      "itemCount": 2,
      "version": 1
    }
  }
  ```

---

## 4. Proposed Actions Endpoints

### 4.1. List Pending Actions
- **Endpoint**: `GET /api/v1/actions?status=PROPOSED`
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": [
      {
        "id": "action_883b1733-b52e-74a7-da49-779988773333",
        "actionType": "RESCHEDULE_TASK",
        "status": "PROPOSED",
        "reasoningText": "Moving DSA Practice to 10:00 AM matches your peak morning focus window.",
        "payload": {
          "taskId": "task_550e8400-e29b-41d4-a716-446655440000",
          "newStartTime": "10:00:00",
          "newDate": "2026-09-08"
        },
        "createdAt": "2026-09-07T21:50:00Z"
      }
    ]
  }
  ```

### 4.2. Approve Proposed Action
- **Endpoint**: `POST /api/v1/actions/:id/approve`
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": {
      "id": "action_883b1733-b52e-74a7-da49-779988773333",
      "status": "EXECUTED",
      "executedAt": "2026-09-07T21:51:00Z"
    }
  }
  ```

### 4.3. Reject Proposed Action
- **Endpoint**: `POST /api/v1/actions/:id/reject`
- **Request**: `{ "reason": "PREFER_EVENING" }`
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": {
      "id": "action_883b1733-b52e-74a7-da49-779988773333",
      "status": "REJECTED"
    }
  }
  ```

---

## 5. Aura Context Engine Query Endpoint

### 5.1. Ask Aura
- **Endpoint**: `POST /api/v1/aura/ask`
- **Request**:
  ```json
  {
    "prompt": "What should I focus on next?",
    "clientLocalTime": "2026-09-07T16:15:00+05:30"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "data": {
      "message": "You have 45 minutes before college class ends. Your critical priority right now is **Complete DSA Assignment**.",
      "proposedAction": null,
      "suggestedQuickPrompts": [
        "What tasks are left today?",
        "Help me plan tomorrow"
      ]
    }
  }
  ```
