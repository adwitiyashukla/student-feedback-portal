# =====================================================================
#  Feedback analytics service - Python image
#
#  The model is trained during the build, so the artifact ships inside the
#  image and a cold container is ready to serve immediately instead of
#  spending its first few seconds fitting a classifier.
# =====================================================================

FROM python:3.12-slim AS base

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /app

# curl is needed by the healthcheck; nothing else is installed.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# ---------- dependencies ----------
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# ---------- application ----------
COPY app ./app
COPY train.py .

# Fit and persist the classifier at build time. The build fails if held-out
# accuracy drops below the acceptance threshold, so a bad model never ships.
RUN mkdir -p /app/data && python train.py

# ---------- runtime user ----------
RUN groupadd --system --gid 1001 analytics \
 && useradd --system --uid 1001 --gid analytics --create-home analytics \
 && chown -R analytics:analytics /app

USER analytics

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD ["sh", "-c", "curl -fsS http://localhost:8000/api/v1/health || exit 1"]

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "2"]
