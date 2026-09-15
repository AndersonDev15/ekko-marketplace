# etapa 1: build
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copia solo el pom primero para cachear las dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia el codigo y compila
COPY src ./src
RUN mvn package -DskipTests -B

# etapa 2: runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario no root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8087
HEALTHCHECK --interval=10s --timeout=5s --retries=5 --start-period=45s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8087/actuator/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]

