package com

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureExposed() {
    // 1. Leemos los datos desde el application.yaml que configuramos antes
    // Estas variables permiten que el backend se conecte a Railway automáticamente
    val driverClass = environment.config.property("storage.driverClassName").getString()
    val jdbcUrl = environment.config.property("storage.jdbcURL").getString()
    val user = environment.config.property("storage.user").getString()
    val password = environment.config.property("storage.password").getString()

    // 2. Conectamos a MySQL usando JDBC (el estándar más estable para Kotlin/Ktor)
    Database.connect(
        url = jdbcUrl,
        driver = driverClass,
        user = user,
        password = password
    )

    // 3. Bloque para la creación automática de tablas (Schema)
    // Más adelante, cuando definamos las tablas de MatchVoc (Usuarios, Tests, etc.),
    // las pondremos aquí dentro de un bloque transaction { }
    transaction {
        // Ejemplo futuro: SchemaUtils.create(UsuariosTable)
    }
}