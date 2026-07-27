# =====================================================================
#  Student Feedback Portal - backend image
#
#  Multi-stage: the JDK, Maven and the whole dependency cache stay in the
#  build stage. The shipped image is a JRE plus one jar, runs as a non-root
#  user, and carries a healthcheck the orchestrator can use.
# =====================================================================

# ---------- Stage 1: build ----------
FROM maven:3-eclipse-temurin-26 AS build

WORKDIR /build

# Copy the POM alone first: this layer is cached until a dependency actually
# changes, so ordinary source edits never re-download the world.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests -Dmaven.test.skip=true

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-jammy AS runtime

# Run as an unprivileged user. A container process that does not need root
# should not have it.
RUN groupadd --system --gid 1001 spring \
 && useradd --system --uid 1001 --gid spring --create-home spring

WORKDIR /app

COPY --from=build --chown=spring:spring /build/target/feedback-portal.jar app.jar

# Attachment directory for the LOCAL storage profile.
RUN mkdir -p /app/uploads && chown -R spring:spring /app

USER spring

EXPOSE 8080

# Container-aware heap sizing, plus a fast entropy source so SecureRandom
# does not block on a cold container.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 \
-XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD ["sh", "-c", "curl -fsS http://localhost:8080/api/v1/health || exit 1"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
