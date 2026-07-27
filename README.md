# Student Feedback Portal

A feedback and grievance management platform for higher-education institutions — students raise
tickets, the system routes and prioritises them, department staff resolve them against an SLA, and
administrators see where the institution is actually failing its students.

[![CI](https://github.com/adwitiyashukla/student-feedback-portal/actions/workflows/ci.yml/badge.svg)](https://github.com/adwitiyashukla/student-feedback-portal/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F)
![Python](https://img.shields.io/badge/Python-3.12-3776AB)
![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)
![License](https://img.shields.io/badge/license-MIT-blue)

---

## Design notes

A few decisions worth calling out, because they are the ones a reader is most likely to question:

**Authorisation derives from the authenticated principal.** No endpoint accepts an identifier
naming *whose* data to load — a caller cannot widen their own scope by editing a URL or a filter
parameter. The integration suite asserts this directly.

**Two security filter chains, not one.** The API is stateless and bearer-authenticated, so CSRF
tokens are meaningless there; the UI carries a session cookie, so they are essential. Two ordered
`SecurityFilterChain` beans express that more safely than one chain full of conditionals.

**The workflow is a state machine.** Status transitions are validated against an explicit set of
legal moves rather than trusted from the request, and only staff roles can drive them.

**Every identifier is server-minted.** Ticket numbers are allocated server-side and
unique-constrained, never supplied by the client.

**Failures are typed.** RFC 7807 problem responses with correlation ids, rather than a blank page
or a leaked stack trace.

---

## Running it

Everything is containerised. You need Docker and nothing else.

```bash
git clone https://github.com/adwitiyashukla/student-feedback-portal.git
cd student-feedback-portal

cp .env.example .env

# Generate a real signing key - the app refuses to start without one
echo "JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')" >> .env

docker compose up --build
```

First build takes a few minutes (Maven dependencies, and the classifier is trained during the
image build). After that:

| | URL | Notes |
|---|---|---|
| **Portal** | http://localhost:8080 | Server-rendered UI |
| **API docs** | http://localhost:8080/swagger-ui.html | Interactive OpenAPI 3 |
| **Analytics API** | http://localhost:8000/docs | FastAPI service |
| **Mail** | http://localhost:8025 | MailHog catches notification email |
| **Metrics** | http://localhost:9090 | Prometheus |
| **Dashboards** | http://localhost:3000 | Grafana (`admin` / `admin`) |

### Demo accounts

The `dev` and `docker` profiles load a demo dataset: 10 departments, 8 administrators, 24 students
and 46 tickets spread across every status, with comment threads and status history.

| Role | Email | Password |
|---|---|---|
| Department admin (CSE) | `priya.menon@university.edu` | `Password#123` |
| Department admin (Hostel) | `manoj.gupta@university.edu` | `Password#123` |
| Student | `aarav.sharma1@student.university.edu` | `Password#123` |
| Super admin | whatever you set as `BOOTSTRAP_ADMIN_EMAIL` | your `BOOTSTRAP_ADMIN_PASSWORD` |

Production migrates from `classpath:db/migration` alone and never sees the demo data.

### Running the pieces on their own

```bash
# Backend - needs MySQL and Redis on localhost
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Analytics service
cd analytics-service
pip install -r requirements-dev.txt
python train.py && uvicorn app.main:app --reload
```

---

## Architecture

```
                        ┌──────────────────────┐
   Browser ────────────▶│  Spring Boot 3.3     │
   (Thymeleaf, session) │  Java 21             │
                        │                      │
   API client ─────────▶│  ┌────────────────┐  │
   (JWT bearer)         │  │ Chain 1: /api  │  │  stateless, JWT, rate-limited
                        │  │ Chain 2: /**   │  │  session cookie, CSRF, form login
                        │  └────────────────┘  │
                        └───┬───────┬───────┬──┘
                            │       │       │
              ┌─────────────┘       │       └──────────────┐
              ▼                     ▼                      ▼
      ┌───────────────┐   ┌──────────────────┐   ┌──────────────────┐
      │  MySQL 8.4    │   │  Redis 7         │   │  FastAPI         │
      │  Flyway       │   │  cache +         │   │  Python 3.12     │
      │  HikariCP     │   │  rate limits     │   │  scikit-learn    │
      └───────────────┘   └──────────────────┘   └──────────────────┘
                                                   sentiment · category
                                                   priority · keywords
```

Two security filter chains rather than one, because the API and the UI are genuinely different
clients: the API is stateless and bearer-authenticated, so CSRF tokens are meaningless there; the
UI carries a session cookie, so they are essential. Expressing that as two ordered
`SecurityFilterChain` beans is clearer and safer than one chain full of conditionals.

Full detail in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

### Repository layout

```
├── backend/              Spring Boot 3.3 · Java 21 · REST API + Thymeleaf UI
│   ├── src/main/java/    domain · repository · service · security · web
│   └── src/main/resources/db/migration/   versioned Flyway schema
├── analytics-service/    FastAPI · scikit-learn NLP microservice
├── infra/
│   ├── terraform/        AWS: VPC, ECS Fargate, RDS, ALB, S3, Secrets Manager
│   ├── prometheus/       scrape config
│   └── grafana/          provisioned datasource and dashboard
├── docs/                 architecture and API reference
└── docker-compose.yml    the whole stack, one command
```

---

## What it does

**For students** — submit feedback against a department and category, optionally anonymously;
track it by ticket number; hold a threaded conversation with staff; attach photos; rate the
resolution.

**For department staff** — a filterable queue scoped to their own department, with SLA deadlines
and overdue highlighting; a workflow state machine that rejects illegal transitions; internal
notes invisible to the student; reassignment within the department.

**For administrators** — dashboards with volume trends, category breakdown, sentiment by category,
per-department resolution times and satisfaction scores; user and department management; a full
audit trail.

**Automatically** — new feedback is routed to the least-loaded administrator in the owning
department, classified by the analytics service, given an SLA deadline from its priority, escalated
if a safety term appears, and auto-closed if the student never responds to a resolution.

---

## The analytics service

A separate FastAPI service classifies each submission. The backend calls it asynchronously after
commit, and treats it as strictly advisory — if the service is down, feedback is still accepted, it
just arrives unenriched.

**Sentiment** is a curated lexicon with negation, intensifier and clause-boundary handling rather
than a transformer. The deployment target is a small Fargate task that must cold-start in seconds,
the input domain is narrow, and — unlike a black-box model — every score can be explained by
pointing at the terms that produced it. `"not helpful"` scores negative; `"not ideal but the staff
were excellent"` scores positive.

**Category** is a TF-IDF (word 1–2 grams ∪ character 3–5 grams) feature union feeding a logistic
regression over 10 labels. Character n-grams are in there because student submissions are full of
typos, and character features degrade gracefully where word features miss entirely.

**Priority** is an explicit rule set, not a model. There is no labelled priority data to learn
from, and a university is not going to sign off on software that escalates complaints for reasons
nobody can articulate. Safety vocabulary forces `URGENT`; strong dissatisfaction in a
progress-blocking category forces `URGENT`; and so on down. Every decision carries its reason.

### On the accuracy number

Training data is generated from per-category templates, so 5-fold cross-validation on it reports
**1.000** — which is meaningless, because the folds share vocabulary by construction. The number
worth quoting is accuracy on a held-out set of 28 hand-written tickets the model never sees:

```
5-fold CV (synthetic)   : 1.0000
Held-out (hand-written) : 0.8929   <-- the real number
```

`train.py` fails the build below 0.80, and CI runs it on every push.

---

## Testing

```bash
cd backend           && mvn verify        # unit + integration + coverage gate
cd analytics-service && pytest            # 72 tests
```

**Backend.** JUnit 5 and Mockito for the domain and services; MockMvc for the web layer;
Testcontainers for integration tests, which start a real MySQL 8.4 — not H2 in compatibility mode.
That distinction matters here specifically: the schema uses `FULLTEXT` indexes, `CHECK` constraints
and `TIMESTAMPDIFF`, and every Flyway migration runs against the engine that runs in production.
JaCoCo enforces a line-coverage floor of 45% in the `verify` phase, measured over the bundle with
entities, DTOs and the application class excluded. Current coverage is 46%. The floor is set just
under the real figure deliberately: it exists to catch a regression, not to advertise a number.

The integration suite asserts the security properties directly: a student requesting another
student's ticket by id gets `404` (not `403` — the
existence of the id is itself not disclosed); a department admin passing another department's id as
a filter parameter gets their own department's rows back; a student attempting a status change gets
`403`; an `OPEN → CLOSED` transition gets `422`.

**Analytics.** 72 pytest cases across the sentiment lexicon, the classifier, the priority rules,
keyword extraction and the HTTP surface — including a held-out accuracy gate.

One of those tests found a real bug while this was being written. `"The network outage was restored
very quickly"` was being escalated to `HIGH`, because `outage` is an urgency term — but the message
is *praise*. Escalating thank-you notes buries the real queue underneath them. The fix, and the
regression test for it, are in [`app/services/priority.py`](analytics-service/app/services/priority.py)
and [`tests/test_priority.py`](analytics-service/tests/test_priority.py).

---

## Security

| Concern | Approach |
|---|---|
| Password storage | BCrypt cost 12, `DelegatingPasswordEncoder` so the algorithm can be rotated without a mass reset |
| Sessions | 15-minute JWT access tokens; opaque refresh tokens stored only as SHA-256 hashes, rotated on every use, all revoked on reuse |
| Brute force | Account lockout after 5 failures; Redis fixed-window rate limiting, tighter on `/auth/**` |
| Enumeration | Identical response and timing whether an account exists or not |
| Authorisation | Role checks plus record-level ownership guards in `@PreAuthorize`; scope is never taken from request input |
| Injection | JPA and Criteria API throughout; every native query uses bound parameters |
| XSS | Thymeleaf escapes by default; `th:utext` appears nowhere in the codebase |
| CSRF | Enabled on the session-authenticated UI chain |
| Headers | HSTS, `X-Content-Type-Options`, `frame-ancestors 'none'`, strict CSP with no `unsafe-inline` |
| Uploads | MIME allow-list, size cap, path-traversal check, `Content-Disposition: attachment` |
| Secrets | Nothing in source; startup fails if `JWT_SECRET` is absent or under 64 bytes |
| Audit | Append-only log of authentication and data-significant actions |
| Supply chain | Dependabot, Trivy, gitleaks and CodeQL in CI |

---

## Deployment

[`infra/terraform/`](infra/terraform/) provisions the AWS side: VPC with public and private
subnets, ECS Fargate services behind an ALB, RDS MySQL in private subnets, S3 for attachments,
Secrets Manager for credentials, ECR, and CloudWatch logs.

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
terraform init && terraform plan
```

No AWS credentials are needed to run the project — `docker compose up` is the supported path, and
S3 storage is only selected under the `prod` profile.

---

## Tech stack

**Backend** — Java 21 (virtual threads, records, pattern matching) · Spring Boot 3.3 · Spring
Security 6 · Spring Data JPA / Hibernate 6 · Flyway · MySQL 8.4 · Redis 7 · Thymeleaf · Bootstrap 5
· Chart.js · springdoc-openapi · Micrometer · Maven

**Analytics** — Python 3.12 · FastAPI · Pydantic v2 · scikit-learn · joblib · pytest · ruff

**Infrastructure** — Docker · Docker Compose · GitHub Actions · Terraform · AWS (ECS Fargate, RDS,
ALB, S3, Secrets Manager) · Prometheus · Grafana

---

## Documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — design decisions and the reasoning behind them
- [`docs/API.md`](docs/API.md) — endpoint reference with worked examples
- [`docs/RUNNING-LOCALLY.md`](docs/RUNNING-LOCALLY.md) — running the stack on a laptop

---

## Licence

MIT — see [LICENSE](LICENSE).

Built by **Adwitiya Shukla** · [adwitiyashukla9@gmail.com](mailto:adwitiyashukla9@gmail.com)
