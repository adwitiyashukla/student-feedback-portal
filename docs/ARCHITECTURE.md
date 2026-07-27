# Architecture

Design decisions and the reasoning behind them. For the endpoint reference,
see [`API.md`](API.md).

---

## 1. Shape of the system

Two deployable services and three backing stores.

```
                          ┌───────────────────────────────────────┐
  Browser ───────────────▶│  Spring Boot 3.3 · Java 21            │
  session cookie          │                                       │
                          │  ┌─────────────────────────────────┐  │
  API client ────────────▶│  │ @Order(1)  /api/**              │  │  stateless
  Bearer <JWT>            │  │   JWT filter, rate limit, CORS  │  │  problem+json
                          │  │   no CSRF (no ambient creds)    │  │
                          │  ├─────────────────────────────────┤  │
                          │  │ @Order(2)  /**                  │  │  session cookie
                          │  │   form login, CSRF, Thymeleaf   │  │  redirects
                          │  └─────────────────────────────────┘  │
                          │                                       │
                          │  web → service → repository → JPA     │
                          └──┬──────────────┬─────────────────┬───┘
                             │              │                 │
                  ┌──────────▼───┐  ┌───────▼────────┐  ┌─────▼──────────────┐
                  │  MySQL 8.4   │  │  Redis 7       │  │  FastAPI · Python  │
                  │  Flyway      │  │  cache         │  │  scikit-learn      │
                  │  HikariCP    │  │  rate limits   │  │  X-API-Key         │
                  └──────────────┘  └────────────────┘  └────────────────────┘
```

The analytics service is a separate process rather than a library because it is Python and the rest
is Java, and because its failure must not be the backend's failure. It is reached over HTTP with an
explicit timeout, called after commit, and every failure path degrades to "store the feedback
unenriched".

---

## 2. Why two security filter chains

The API and the UI are different clients with different threat models, and trying to serve both
from one chain means a chain full of conditionals.

| | `/api/**` | everything else |
|---|---|---|
| Credential | `Authorization: Bearer` | `JSESSIONID` cookie |
| Session | `STATELESS` | `IF_REQUIRED`, fixation migration, max 3 |
| CSRF | disabled | enabled |
| Unauthenticated | `401` + `problem+json` | redirect to `/login` |
| Forbidden | `403` + `problem+json` | `/error/403` page |
| Rate limited | yes | no |

CSRF matters exactly when the browser attaches credentials automatically. A cookie does; an
`Authorization` header does not. Disabling CSRF on a cookie-authenticated chain is a vulnerability;
enabling it on a bearer-authenticated one is friction with no benefit. Two ordered
`SecurityFilterChain` beans make that a structural property rather than a runtime branch.

```java
@Bean @Order(1)
SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) {
    http.securityMatcher("/api/**")
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        ...
}
```

---

## 3. Authorisation

The single most important rule in this codebase: **no endpoint accepts an identifier that
determines whose data it returns.**

Scope is derived from the authenticated principal:

```java
private Specification<Feedback> visibilityScope(AppUserDetails principal) {
    if (principal.isSuperAdmin()) return FeedbackSpecifications.all();
    if (principal.isStaff())      return FeedbackSpecifications.inDepartment(principal.departmentId());
    return FeedbackSpecifications.submittedBy(principal.id());
}
```

Caller-supplied filters are `and`-ed onto that, so a filter can only ever narrow. A
super-administrator may pass `departmentId`; for anyone else it is ignored, and
`FeedbackAuthorizationIT` asserts that a department admin passing another department's id gets
their own rows back.

Three layers, deliberately overlapping:

1. **URL rules** in `SecurityConfig` - coarse, role-based (`/api/v1/admin/**` → `SUPER_ADMIN`).
2. **Method security** - `@PreAuthorize("@feedbackAccess.canManage(#id, principal)")` for
   record-level ownership.
3. **Query scoping** - the specification above, so a list can never return a row the caller should
   not see, regardless of what got past layers 1 and 2.

**404 rather than 403 for records you do not own.** A 403 confirms the id exists, which is itself a
disclosure. `loadVisible` throws `ResourceNotFoundException` for both "no such ticket" and "not
your ticket".

---

## 4. Persistence

### Schema ownership

Flyway owns the schema. Hibernate runs with `ddl-auto: validate`, so a mismatch between an entity
and its table is a startup failure rather than a silent column drift.

Migrations live in two locations:

- `classpath:db/migration` - structural, applied in every environment
- `classpath:db/demo` - the demo dataset, on the Flyway path **only** under `dev` and `docker`

Production therefore cannot load demo data even by accident; the location is not on its path.

### Identity modelling

One `users` table for authentication, with `students` and `admins` sharing its primary key through
`@MapsId`:

```java
@Entity @Table(name = "students")
public class Student {
    @Id @Column(name = "user_id") private Long userId;

    @MapsId
    @OneToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
```

One identity table means one hashing policy, one lockout policy and one audit trail. Separate
tables per role would mean parallel login paths that have to be kept in sync by hand.

`@MapsId` rather than JPA inheritance: joined-table inheritance with a role discriminator does not
map cleanly onto three roles and two profile types, and composition avoids the proxy and
polymorphic-query surprises that come with it.

### Query strategy

| Need | Mechanism |
|---|---|
| Dynamic filtering | Criteria API `Specification`s, composed with `and` |
| Aggregation | `GROUP BY` in SQL, into projection interfaces |
| Avoiding N+1 | `@EntityGraph` on every finder that feeds a list view |
| Full-text search | MySQL `MATCH ... AGAINST` on `ft_feedback_text` |
| Concurrency | `@Version` optimistic locking on `User` and `Feedback` |

`open-in-view` is `false`. Lazy loading outside a transaction throws instead of quietly issuing
queries from the render loop.

Dashboard numbers are computed by the database, not by loading rows and counting in Java:

```java
@Query("""
    SELECT f.status AS status, COUNT(f) AS total
      FROM Feedback f
     WHERE (:departmentId IS NULL OR f.department.id = :departmentId)
     GROUP BY f.status
    """)
List<StatusCount> countByStatusGrouped(@Param("departmentId") Long departmentId);
```

---

## 5. The workflow state machine

Ticket status is not a string that anyone can set. Each state declares its successors:

```java
public Set<FeedbackStatus> allowedTransitions() {
    return switch (this) {
        case OPEN             -> EnumSet.of(IN_PROGRESS, AWAITING_STUDENT, RESOLVED, REJECTED);
        case IN_PROGRESS      -> EnumSet.of(AWAITING_STUDENT, RESOLVED, REJECTED);
        case AWAITING_STUDENT -> EnumSet.of(IN_PROGRESS, RESOLVED, REJECTED);
        case RESOLVED         -> EnumSet.of(CLOSED, IN_PROGRESS);
        case CLOSED, REJECTED -> NO_TRANSITIONS;
    };
}
```

`Feedback.transitionTo` consults it, refuses anything illegal, stamps the lifecycle timestamps, and
appends to the append-only `feedback_status_history` table. The rule lives on the entity, so it
holds regardless of which controller, scheduler or test invokes it.

`FeedbackDetailResponse` carries `allowedTransitions`, so the UI renders only the buttons the server
will accept - the API stays the single source of truth for what is legal.

---

## 6. Asynchrony and scheduling

Java 21 virtual threads back the `@Async` executor. Both async paths - the analytics call and
notification email - are I/O bound, which makes thread-per-task the right executor rather than a
tuned pool.

Analytics enrichment is registered as an **after-commit** callback:

```java
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override public void afterCommit() { applyAnalysis(feedbackId); }
});
```

Two properties fall out of that: a slow model never lengthens the submission request, and the model
never sees a row that was subsequently rolled back.

Scheduled jobs:

| Job | Cadence | Purpose |
|---|---|---|
| SLA breach warnings | hourly at :05 | notify assignees of overdue tickets |
| Auto-close | 02:00 daily | close resolved tickets the student never answered |
| Refresh-token purge | 03:00 daily | delete tokens expired over a day ago |
| Lockout release | every 10 min | clear lockouts whose window has passed |

---

## 7. Error handling

One `@RestControllerAdvice` maps every exception to RFC 7807 `application/problem+json`.

```json
{
  "type": "https://feedback-portal/errors/validation",
  "title": "Validation Failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "instance": "/api/v1/feedback",
  "timestamp": "2026-07-25T09:14:22Z",
  "errors": { "title": "Title must be between 10 and 150 characters" }
}
```

| Exception | Status |
|---|---|
| `ResourceNotFoundException` | 404 |
| `DuplicateResourceException` | 409 |
| `BusinessRuleException` | 422 |
| `MethodArgumentNotValidException` | 400 + field map |
| `BadCredentialsException` | 401 |
| `LockedException` | 423 |
| `OptimisticLockingFailureException` | 409 |
| anything else | 500 + correlation id |

Unexpected exceptions are logged with a generated UUID; the caller receives that UUID and nothing
else. `server.error.include-stacktrace: never`.

---

## 8. Caching

Redis, with per-cache TTLs chosen from how fast the data actually changes:

| Cache | TTL | Rationale |
|---|---|---|
| `dashboardStats` | 60 s | expensive aggregates, staleness is invisible |
| `feedbackTrends` | 10 min | monthly buckets barely move |
| `departments` | 1 h | reference data, evicted on write |

Redis also backs rate limiting, as a fixed window keyed on client IP and minute. That filter **fails
open**: if Redis is unreachable the request proceeds. A cache outage should degrade protection, not
take the portal down.

---

## 9. The analytics service

```
POST /api/v1/analyze          X-API-Key: <shared secret>
{ "title": "...", "text": "..." }
   ↓
sentiment  → lexicon with negation, intensifiers, clause boundaries
category   → TF-IDF (word 1-2 ∪ char 3-5) → LogisticRegression, 10 labels
priority   → explicit rules over sentiment + category + urgency vocabulary
keywords   → IDF-weighted terms from the fitted vectoriser
```

**Why a lexicon and not a transformer.** The target is a small Fargate task that must cold-start in
seconds; the input domain is narrow and predictable; and an administrator seeing "this complaint is
NEGATIVE" is entitled to ask why. The lexicon answers that by naming the terms. It handles the
cases that actually break bag-of-words scoring - negation windows, intensifiers, and clause breaks
so that *"not ideal, but the staff were excellent"* scores positive.

**Why rules and not a model for priority.** There is no labelled priority data. Manufacturing some
would produce a classifier whose mistakes nobody could explain, in a system that escalates student
grievances. Every `PriorityDecision` carries its reason string.

**On the accuracy figure.** The training corpus is template-generated, so cross-validated accuracy
on it is 1.000 and means nothing - the folds share vocabulary by construction. The reported metric
is accuracy on 28 hand-written held-out tickets: **0.893**. `train.py` exits non-zero below 0.80,
and CI runs it on every push.

**Contract.** Field names are snake_case to match the Java `AnalysisResult` record's
`@JsonProperty` mappings, and that record is annotated `@JsonIgnoreProperties(ignoreUnknown = true)`
so the Python side can add fields without breaking the Java deserialiser.

---

## 10. Testing strategy

| Layer | Tools | What it covers |
|---|---|---|
| Domain | JUnit 5, AssertJ | state machine, SLA arithmetic, lockout |
| Service | Mockito | ticket numbering, collision handling |
| Security | JUnit 5 | JWT signing, issuer and tamper rejection |
| Web + data | Testcontainers, MockMvc | real MySQL 8.4, real migrations, real HTTP |
| Analytics | pytest | 72 cases incl. a held-out accuracy gate |

**Testcontainers over H2.** The schema uses `FULLTEXT` indexes, `CHECK` constraints, `utf8mb4`
collation and `TIMESTAMPDIFF`. H2 in MySQL-compatibility mode accepts things MySQL rejects, so a
green H2 suite would be evidence of very little. The container is `static` and reused across
classes.

The integration suite tests the security properties as behaviour, not as configuration:

```java
@Test
@DisplayName("a student cannot read another student's ticket")
void studentsCannotReadEachOthersFeedback() throws Exception {
    long feedbackId = submitFeedbackAs(tokenFor(STUDENT_A));
    mockMvc.perform(get("/api/v1/feedback/{id}", feedbackId)
                    .header("Authorization", tokenFor(STUDENT_B)))
           .andExpect(status().isNotFound());
}
```

That test is the IDOR guarantee, written down as an assertion.

---

## 11. Deployment topology

**Local** - `docker compose up` brings up seven containers with healthchecks and a dependency graph
that waits on them, so `up` either reaches a working system or fails visibly.

**AWS** - Terraform in [`infra/terraform`](../infra/terraform):

```
Internet ──▶ ALB (public subnets, :443)
                │
                ├──▶ ECS Fargate · backend    (private subnets)
                └──▶ ECS Fargate · analytics  (private, no ingress from the internet)
                            │
                            ├──▶ RDS MySQL 8  (private, encrypted, automated backups)
                            ├──▶ S3           (attachments, SSE, versioned, public access blocked)
                            └──▶ Secrets Manager (DB credentials, JWT signing key)
```

Both task definitions run as non-root. Neither service holds an access key: credentials resolve
through the ECS task role via the default provider chain.

---

## 12. Trade-offs worth stating

**Server-rendered UI rather than a SPA.** One deployable, no CORS surface, no tokens in browser
storage, and the authorisation model stays in one place. The REST API is complete and documented,
so a React front end could be added later without reworking the server - it is simply not required
for the product to work.

**Hand-written mappers rather than MapStruct.** The anonymity rule - whether a submitter's name is
disclosed - is a business decision. It belongs in reviewable code, not in a generated method.

**Accounts are disabled, never deleted.** Feedback, comments and audit rows all reference users.
A hard delete either cascades away real history or leaves dangling references, so deactivation is
the only safe operation.
