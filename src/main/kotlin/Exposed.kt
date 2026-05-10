package com

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureExposed() {
    val host     = System.getenv("MYSQLHOST")           ?: "localhost"
    val port     = System.getenv("MYSQLPORT")           ?: "3306"
    val database = System.getenv("MYSQL_DATABASE")      ?: "matchvoc"
    val user     = System.getenv("MYSQLUSER")           ?: "root"
    val password = System.getenv("MYSQL_ROOT_PASSWORD") ?: "root"

    val db = Database.connect(
        url      = "jdbc:mysql://$host:$port/$database?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
        driver   = "com.mysql.cj.jdbc.Driver",
        user     = user,
        password = password
    )

    transaction(db) {
        SchemaUtils.create(UserTable)
    }
}