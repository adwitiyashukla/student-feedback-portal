FROM maven:3-eclipse-temurin-26 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN groupadd --system --gid 1001 spring \
 && useradd --system --uid 1001 --gid spring --create-home spring

WORKDIR /app

COPY --from=build --chown=spring:spring /build/target/feedback-portal.jar app.jar

RUN mkdir -p /app/uploads && chown -R spring:spring /app

USER spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 \
-XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD ["sh", "-c", "curl -fsS http://localhost:8080/api/v1/health || exit 1"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
