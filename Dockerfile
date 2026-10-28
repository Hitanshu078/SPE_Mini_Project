# ---------- Stage 1: build the JAR ----------
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only pom.xml first so the dependency layer is cached
# and re-downloaded only when pom.xml changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: slim runtime ----------
FROM eclipse-temurin:17-jre-alpine

# Run as an unprivileged user, not root
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=build --chown=app:app /app/target/calculator.jar app.jar
USER app

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# exec so java is PID 1 and receives SIGTERM from docker stop
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
