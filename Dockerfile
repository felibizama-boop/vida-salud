# =============================================================================
#  VidaSalud - Microservicio de negocio (Spring Boot + RabbitMQ + Kafka)
#  EP5 / DSY1107 - Desarrollo Cloud Native I
# -----------------------------------------------------------------------------
#  Build multi-etapa:
#     1) maven   -> compila y empaqueta el jar ejecutable.
#     2) temurin -> ejecuta el jar con la JVM 17.
#
#  Uso (mediante el compose de la capa de aplicación):
#     docker compose -f infra/apps/compose.yml up -d --build ms-vidasalud-core
# =============================================================================

# ---- Etapa 1: compilación Maven ----
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Etapa 2: runtime ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
