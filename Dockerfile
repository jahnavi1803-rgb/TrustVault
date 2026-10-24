# ----------------------------------------------------------------------------
# TrustVault - Production Multi-Stage Dockerfile for Render Deployment
# ----------------------------------------------------------------------------

# Stage 1: Build and Package
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Copy Maven Wrapper and POM first to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw
# Download dependencies offline
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and build production executable JAR
COPY src/ src/
RUN ./mvnw clean package -DskipTests=true -B

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:21-jre-jammy AS runner

WORKDIR /app

# Create non-privileged system user for zero-trust security
RUN addgroup --system --gid 1001 trustvault && \
    adduser --system --uid 1001 --ingroup trustvault --no-create-home trustvault

# Copy executable jar from builder stage
COPY --from=builder /build/target/trustvault-1.0.0.jar app.jar

# Change file ownership to non-root user
RUN chown -R trustvault:trustvault /app
USER trustvault

# Dynamic port binding for Render ($PORT)
ENV PORT=8080
EXPOSE 8080

# Production JVM options (optimized memory, container-aware)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT}"]
