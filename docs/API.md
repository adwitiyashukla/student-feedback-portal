# API reference

Base URL `http://localhost:8080` - all paths under `/api/v1`

Interactive documentation is served at [`/swagger-ui.html`](http://localhost:8080/swagger-ui.html);
the machine-readable schema is at `/v3/api-docs`. This page is the narrative version.

---

## Authentication

Sign in, then send the access token as a bearer header on everything else.

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"priya.menon@university.edu","password":"Password#123"}'
```

```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "kZ8Qx1_pV3nA...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": 3, "email": "priya.menon@university.edu", "fullName": "Dr. Priya Menon",
    "role": "ADMIN", "departmentCode": "CSE", "departmentName": "Computer Science & Engineering"
  }
}
```

Access tokens last 15 minutes. Refresh tokens last 7 days, are **single use**, and are stored
server-side only as a SHA-256 hash. Presenting an already-used refresh token revokes every session
for that user - that pattern indicates theft, not a retry.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/auth/login` | - | Exchange credentials for a token pair |
| `POST` | `/auth/refresh` | - | Rotate a refresh token |
| `POST` | `/auth/logout` | - | Revoke one refresh token |
| `POST` | `/auth/logout-all` | any | Revoke every session for the caller |
| `GET` | `/auth/me` | any | The caller's own profile |
| `POST` | `/auth/change-password` | any | Change own password; revokes all sessions |

**Failure modes.** `401` for wrong credentials *and* for an unknown account - the responses are
byte-identical, and the encoder runs even when no account exists so timing does not distinguish
them either. `423` after five consecutive failures (15-minute lockout). `429` past the rate limit.

---

## Feedback

### Submit

```bash
curl -s -X POST http://localhost:8080/api/v1/feedback \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{
        "title": "Lab computers extremely slow in Block C",
        "description": "The machines take ten minutes to boot and the IDE freezes constantly.",
        "category": "IT_SUPPORT",
        "departmentId": 10,
        "anonymous": false
      }'
```

`201 Created`, with a `Location` header.

Note what the request body does **not** contain: no id, no status, no assignee, no reply, no
resolution date. The server owns all of them. The ticket number is minted inside the transaction
against a unique-constrained column; the assignee is the least-loaded administrator in the
department; the SLA deadline derives from priority; and the analytics service enriches the record
asynchronously after commit.

### List

```
GET /api/v1/feedback?status=OPEN&category=HOSTEL&overdue=true&page=0&size=20&sort=createdAt,desc
```

| Parameter | Type | Notes |
|---|---|---|
| `status` | enum | `OPEN` - `IN_PROGRESS` - `AWAITING_STUDENT` - `RESOLVED` - `CLOSED` - `REJECTED` |
| `category` | enum | 10 values, see `/departments` and the OpenAPI schema |
| `priority` | enum | `LOW` - `MEDIUM` - `HIGH` - `URGENT` |
| `departmentId` | long | **Honoured for `SUPER_ADMIN` only** |
| `assignedToId` | long | Filter by assignee |
| `search` | string | Contains-match on ticket number, title, description |
| `overdue` | boolean | Active tickets past their SLA deadline |
| `from` / `to` | ISO-8601 | Creation-date window |

**Visibility is not a parameter.** Students receive their own submissions, department staff receive
their department's, super-administrators receive everything. Filters can only narrow that scope,
never widen it - a department admin passing another department's `departmentId` still gets their
own rows.

```json
{
  "content": [ { "id": 42, "ticketNumber": "FB-2026-000042", "title": "...",
                 "status": "IN_PROGRESS", "priority": "HIGH", "sentimentLabel": "NEGATIVE",
                 "overdue": false, "dueAt": "2026-07-28T09:00:00Z" } ],
  "page": 0, "size": 20, "totalElements": 46, "totalPages": 3,
  "first": true, "last": false
}
```

### Read one

```
GET /api/v1/feedback/{id}
```

Returns the full record: description, threaded comments, status history, attachments, the
analytics enrichment, and `allowedTransitions` - the set of states the server will actually accept
next, so a client renders only legal actions.

Internal staff notes are filtered out at the query level for students, not hidden in the template.

`404` if the record does not exist **or** the caller is not entitled to it. Returning `403` would
confirm the id exists.

### Other operations

| Method | Path | Who | Notes |
|---|---|---|---|
| `GET` | `/feedback/search?q=` | any | MySQL full-text over title and description |
| `PATCH` | `/feedback/{id}/status` | staff | `422` if the state machine forbids the transition |
| `PATCH` | `/feedback/{id}/assignee` | staff | Assignee must be in the same department |
| `POST` | `/feedback/{id}/comments` | any | `internalNote: true` honoured for staff only |
| `POST` | `/feedback/{id}/rating` | submitter | 1-5, only once resolved |
| `POST` | `/feedback/{id}/attachments` | any | multipart, images and PDF, 5 MB cap |
| `GET` | `/feedback/{id}/attachments/{aid}` | any | `Content-Disposition: attachment`, `nosniff` |

Status transition example:

```bash
curl -s -X PATCH http://localhost:8080/api/v1/feedback/42/status \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"status":"RESOLVED","note":"Replaced the switch; verified with the lab staff."}'
```

An illegal transition - `OPEN -> CLOSED`, say - returns:

```json
{ "type": "https://feedback-portal/errors/business-rule",
  "title": "Unprocessable", "status": 422,
  "detail": "Cannot move feedback FB-2026-000042 from OPEN to CLOSED" }
```

---

## Users, departments, dashboards, notifications

| Method | Path | Role |
|---|---|---|
| `GET` | `/users/students` | staff |
| `POST` | `/users/students` | staff |
| `GET` / `POST` | `/users/admins` | `SUPER_ADMIN` |
| `GET` | `/users/admins/by-department/{id}` | staff |
| `PATCH` | `/users/{id}/enabled` - `/unlock` | `SUPER_ADMIN` |
| `GET` | `/departments` | any |
| `POST` | `/departments`, `PATCH /{id}/active` | `SUPER_ADMIN` |
| `GET` | `/dashboard/admin`, `/dashboard/admin/trend` | staff |
| `GET` | `/dashboard/student` | student |
| `GET` | `/notifications`, `/notifications/unread-count` | any |
| `POST` | `/notifications/mark-all-read` | any |
| `GET` | `/health`, `/health/dependencies` | - |

There is no public registration endpoint. Accounts are created by staff, which is how a university
portal actually works and removes a whole class of self-signup abuse.

`/dashboard/admin` returns totals, open and overdue counts, average resolution hours, average
satisfaction, breakdowns by status and category, sentiment per category, a 12-month trend and a
per-department league table - every figure a SQL `GROUP BY`, cached in Redis for 60 seconds.

---

## Errors

Every failure is RFC 7807 `application/problem+json`.

```json
{
  "type": "https://feedback-portal/errors/validation",
  "title": "Validation Failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "instance": "/api/v1/feedback",
  "timestamp": "2026-07-25T09:14:22Z",
  "errors": {
    "title": "Title must be between 10 and 150 characters",
    "description": "Description must be between 20 and 5000 characters"
  }
}
```

| Status | Meaning |
|---|---|
| `400` | Malformed body, or bean-validation failure (`errors` map included) |
| `401` | Missing, expired or invalid token |
| `403` | Authenticated but not permitted |
| `404` | No such record, or not visible to the caller |
| `409` | Uniqueness conflict, or a stale optimistic-lock version |
| `422` | Valid request, violates a domain rule |
| `423` | Account locked out |
| `429` | Rate limit exceeded - see `Retry-After` |
| `500` | Unexpected; includes a `correlationId` to quote, and nothing else |

---

## Rate limits

Fixed window, per client IP, honouring `X-Forwarded-For` behind a load balancer.

| Scope | Limit |
|---|---|
| `/api/v1/auth/**` | 10 requests/minute |
| everything else under `/api/**` | 120 requests/minute |

Responses carry `X-RateLimit-Limit` and `X-RateLimit-Remaining`; a `429` carries `Retry-After`. The
limiter fails open - if Redis is unreachable, requests proceed rather than the portal going down.

---

## Analytics service

Internal, service-to-service, on port 8000. Not exposed to browsers; authenticated with a shared
`X-API-Key` compared using `hmac.compare_digest`.

```bash
curl -s -X POST http://localhost:8000/api/v1/analyze \
  -H "X-API-Key: $ANALYTICS_API_KEY" -H 'Content-Type: application/json' \
  -d '{"title":"Mess food","text":"The dinner is undercooked and several students fell ill."}'
```

```json
{
  "sentiment_label": "NEGATIVE",
  "sentiment_score": -0.7421,
  "category": "HOSTEL",
  "category_confidence": 0.9134,
  "priority": "URGENT",
  "keywords": ["undercooked", "dinner", "students", "mess"],
  "model_version": "tfidf-logreg-v2.0.0"
}
```

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/health` | Liveness and model readiness (public) |
| `POST` | `/api/v1/analyze` | Classify one submission |
| `POST` | `/api/v1/analyze/batch` | Up to 200 at once, for backfill |
| `GET` | `/api/v1/model/info` | Categories, vocabulary size, accuracy |
| `POST` | `/api/v1/model/retrain` | Refit and persist |

The backend treats all of this as advisory: it is called asynchronously after commit, with an
explicit timeout, and every failure path stores the feedback unenriched rather than rejecting it.
The model may raise a ticket's priority but never lower one a human has set.
