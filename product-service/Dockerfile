#  Etapa 1: build
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copia solo el pom primero para cachear las dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Ahora copia el código y compila
COPY src ./src
RUN mvn clean package -DskipTests -B

#  Etapa 2: runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuario no root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]