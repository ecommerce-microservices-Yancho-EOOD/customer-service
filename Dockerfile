# ============================================================
# STAGE 1: BUILD — Compile the application with Maven
# ============================================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ============================================================
# STAGE 2: RUNTIME — Minimal JRE image for production
# ============================================================
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Yancho-EOOD" \
      org.opencontainers.image.title="customer-service" \
      org.opencontainers.image.description="Customer Management Service"

RUN apk add --no-cache tzdata wget && \
    cp /usr/share/zoneinfo/Europe/Sofia /etc/localtime && \
    echo "Europe/Sofia" > /etc/timezone && \
    apk del tzdata

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

COPY --from=builder --chown=spring:spring /build/target/*.jar app.jar

USER spring:spring

# Customer Service port
EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.output.ansi.enabled=ALWAYS", \
    "-jar", \
    "app.jar"]