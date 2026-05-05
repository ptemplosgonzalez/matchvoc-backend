plugins {
    // 1. Usamos Kotlin directamente
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    // Quitamos el plugin de ktor que da error y usamos la alternativa estable
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com"
version = "1.0.0-SNAPSHOT"

// Configuramos la aplicación manualmente
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}


tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    manifest {
        // Cambiamos "io.ktor.server.netty.EngineMain" por tu ruta real
        attributes["Main-Class"] = "com.MainKt"
    }
}

dependencies {
    // Ktor Server 2.3.12 (La más estable para evitar errores de 'convention')
    val ktor_version = "2.3.12"
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-resources:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")

    // Logs
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Base de Datos para Railway
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.41.1")
    implementation("mysql:mysql-connector-java:8.0.33")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
}