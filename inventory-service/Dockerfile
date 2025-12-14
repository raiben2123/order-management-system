# Etapa 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar pom.xml y descargar dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copiar JAR desde etapa de build
COPY --from=build /app/target/*.jar app.jar

# Exponer puerto
EXPOSE 8082

# Variables de entorno
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/inventory_service_db
ENV SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Comando para ejecutar
ENTRYPOINT ["java", "-jar", "app.jar"]