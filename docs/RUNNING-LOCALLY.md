# Running the stack on a laptop

A short, no-jargon guide to starting this project without cooking your machine.

---

## Why the old command was a problem

The obvious command is:

```powershell
docker compose up -d --build backend
```

It looks like it builds one thing. It doesn't. `backend` declares
`depends_on` with `condition: service_healthy` for **mysql**, **redis** and
**analytics**, so that single command:

1. starts MySQL 8.4 (which initialises a fresh data directory),
2. starts Redis,
3. builds *and* starts a Python service that trains a model,
4. and simultaneously runs a Maven build that compiles 119 Java files.

Four containers and a cold compile, fighting over the same cores. That is why
the fans screamed and why `docker stats` stopped answering — the Docker daemon
was starved, not broken.

---

## One-time setup

### 1. Give Docker enough memory

Docker Desktop → **Settings** → **Resources** → **Memory**.

Set it to **6 GB** if your laptop has 16 GB of RAM, or **4 GB** if it has 8 GB.
Click **Apply & Restart**.

Below about 3.5 GB, the Java build will swap to disk and can appear to hang
forever. This single setting fixes most "stuck build" problems.

### 2. Make sure `.env` exists

```powershell
Copy-Item .env.example .env
```

Then open `.env` and fill in the required values. The startup script checks
for this file and stops early with a clear message if it's missing.

---

## Starting the stack

From the project folder:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-stack.ps1
```

That's the whole thing. The script runs five stages in order and waits for
each one to actually be healthy before starting the next:

| Stage | What happens | Roughly |
|-------|--------------|---------|
| 1 | Pre-flight checks (Docker alive, memory, `.env`) | seconds |
| 2 | Build the backend image — **nothing else is running** | 3–6 min cold |
| 3 | Build the analytics image | 2–4 min cold |
| 4 | Start MySQL + Redis, wait for healthy | 1–2 min |
| 5 | Start analytics, then the backend | 1–2 min |

First run is the slow one. After that the images are cached and a restart
takes well under a minute.

### Useful flags

```powershell
# Reuse existing images, just start the containers
.\scripts\start-stack.ps1 -SkipBuild

# Also start Prometheus and Grafana (heavier - only if you need dashboards)
.\scripts\start-stack.ps1 -Monitoring

# Stop everything (keeps your database)
.\scripts\start-stack.ps1 -Down
```

---

## When it's up

| What | Where |
|------|-------|
| Application | http://localhost:8080 |
| Health check | http://localhost:8080/api/v1/health |
| API docs | http://localhost:8080/swagger-ui.html |
| Analytics health | http://localhost:8000/api/v1/health |
| Captured email | http://localhost:8025 |

---

## Things worth knowing

**Silence during the build is normal.** `Compiling 119 source files with javac`
prints nothing at all until it finishes. It is not frozen. Don't press Ctrl+C.

**If dependencies re-download every time**, the `COPY pom.xml` layer cache was
lost — usually from `--no-cache` or a Docker prune. It'll re-cache after one
successful build.

**If the laptop gets hot anyway**, stop the stack when you're done:

```powershell
.\scripts\start-stack.ps1 -Down
```

Containers keep running in the background until you stop them, including
after you close the terminal.

**To check whether a build is really working**, don't use `docker stats` — it
needs the same daemon that's under load. Use Task Manager → Details, and look
at `Vmmem` / `Vmmem WSL` CPU. High CPU means it's compiling.

---

## If something fails

The script prints the failing container's last 40 log lines automatically.
Read the **first** line containing `ERROR` — everything after it is usually
knock-on noise.

To follow a service manually:

```powershell
docker compose logs -f backend
docker compose logs -f mysql
```
