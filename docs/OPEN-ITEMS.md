# Open items

State as of 27 July 2026. The application works end to end and CI is green on
`main`; nothing here is blocking.

---

## Verified working

The full triage loop has been exercised by hand, not just unit-tested:

- sign in as student and as admin, with role-based routing
- submit feedback (anonymous and named)
- admin queue with filters, student directory, admin dashboard with charts
- status transitions `OPEN → IN_PROGRESS → RESOLVED`, with history and notes
- reassignment between administrators
- notifications delivered to the student on each transition
- student rating of a resolved ticket
- threaded comments, with internal notes hidden from students
- the analytics service classifying sentiment, category and confidence

---

## Open

### 1. Security alerts

The Security tab shows 15 CodeQL findings. They have never been reviewed, so it
is not known how many are real. This matters more than the other items: the
project's stated design goal is a sound authorisation and security model, and
unreviewed alerts undercut that claim to anyone who looks.

### 2. Test coverage is 46%

The JaCoCo floor is set to 45% deliberately - just under the real figure, so it
catches a regression rather than failing every build. But 5 unit test files and
2 integration test files across 111 production classes is thin. The services and
the web layer are where coverage is missing.

### 3. Attachment upload is never exercised

`Attachment` gained an `AuditingEntityListener` during debugging, because it had
the same missing-listener defect that made `Notification` fail on insert. That
fix is unverified: there is no UI route for uploads, so the path is API-only and
nothing has ever run it. If it is broken, it is broken in the same way - a
`created_at cannot be null` on insert.

### 4. Timestamps display in UTC

The history and detail pages render stored UTC timestamps directly, so a viewer
in another timezone sees times that do not match their clock. Storage is correct;
it is the display layer that needs to localise.

### 5. Reassignment is absent from ticket history

Status transitions are recorded in `FeedbackStatusHistory`, but changing a
ticket's owner writes nothing. Consistent with the entity's name, but an audit
trail that records state changes and not ownership changes is an odd half-measure.

### 6. Dependabot pull requests

Two of the open grouped PRs fail CI - the backend and analytics dependency
groups. They are proposals on their own branches and cannot affect `main`. Worth
opening the failing runs to see whether a specific bump is incompatible before
merging or closing them.

---

## Running it

See [`RUNNING-LOCALLY.md`](RUNNING-LOCALLY.md). Short version:

```powershell
.\scripts\start-stack.ps1          # start
.\scripts\start-stack.ps1 -Down    # stop when finished
```

Stop the stack when you are done. Containers keep running in the background
otherwise, including after the terminal is closed.

### If Docker wedges

It happened twice during development: `docker ps` either hangs or returns a 500
from the Linux engine. Restarting Windows clears it. `wsl --shutdown` alone did
not.

### If Flyway refuses to start

Editing any file under `db/migration/` changes its checksum, and Flyway will
reject a database migrated from the old version. Recreate the volumes:

```powershell
docker compose down -v
.\scripts\start-stack.ps1
```

The demo dataset reseeds automatically.
