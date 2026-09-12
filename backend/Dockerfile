# ==========================================
# Etapa 1: Builder (Compilación con OpenJDK 17)
# ==========================================
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace/app

# Copiar archivos de Maven Wrapper para aprovechar el caché de capas
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copiar código fuente y compilar el JAR excluyendo tests
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ==========================================
# Etapa 2: Runtime (Imagen final de producción)
# ==========================================
FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app

# Crear usuario y grupo de sistema sin privilegios (Seguridad AWS/Docker)
RUN addgroup -S erpgroup && adduser -S erpuser -G erpgroup
USER erpuser

# Copiar el artefacto generado desde la etapa de compilación
COPY --from=builder /workspace/app/target/*.jar app.jar

# Exponer el puerto del backend
EXPOSE 8080

# Optimización de memoria JVM en contenedores
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
