package com

import io.ktor.server.application.*
import org.example.project.UserTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
// Asegúrate de importar tu objeto UserTable si está en otro archivo
// import org.example.project.UserTable

fun Application.configureExposed() {
    val driverClass = environment.config.property("storage.driverClassName").getString()
    val jdbcUrl = environment.config.property("storage.jdbcURL").getString()
    val user = environment.config.property("storage.user").getString()
    val password = environment.config.property("storage.password").getString()

    // Guardamos la conexión en una variable local
    val db = Database.connect(
        url = jdbcUrl,
        driver = driverClass,
        user = user,
        password = password
    )

    // Usamos esa variable 'db' para la transacción
    transaction(db) {
        SchemaUtils.create(UserTable)
    }
}