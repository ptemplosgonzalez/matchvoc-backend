# Usamos Java 17 como definiste en tu Gradle
FROM openjdk:17-jdk-slim
WORKDIR /app
# Copiamos el JAR que genera Shadow
COPY build/libs/app.jar app.jar
# Ejecutamos el servidor
ENTRYPOINT ["java", "-jar", "app.jar"]