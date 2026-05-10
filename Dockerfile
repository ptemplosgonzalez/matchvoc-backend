# Etapa 1: Compilacion
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /build

COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./

RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon || true

COPY src/ src/

RUN ./gradlew shadowJar --no-daemon

# Etapa 2: Imagen final
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /build/build/libs/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
