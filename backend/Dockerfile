# ==========================================
# Etapa 1: Builder (Compilación con Gradle y OpenJDK 21)
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace/app

# Copiar scripts y configuración de Gradle Wrapper para aprovechar el caché de capas
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ ./gradle/
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon

# Copiar código fuente y compilar el JAR excluyendo tests
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ==========================================
# Etapa 2: Runtime (Imagen final de producción)
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Crear usuario y grupo de sistema sin privilegios (Seguridad AWS/Docker)
RUN addgroup -S erpgroup && adduser -S erpuser -G erpgroup
USER erpuser

# Copiar el artefacto generado desde la etapa de compilación
COPY --from=builder /workspace/app/build/libs/app.jar app.jar

# Exponer el puerto del backend
EXPOSE 8080

# Optimización de memoria JVM en contenedores
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
