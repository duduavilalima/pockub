# Stage 1 — build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Baixa dependências separado do código (cache de camada)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Compila
COPY src/ src/
RUN mvn package -DskipTests -q

# Stage 2 — runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Usuário não-root por segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
