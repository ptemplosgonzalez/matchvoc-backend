package org.example.project // Revisa que coincida con tu carpeta de src

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

// Esta es tu tabla exacta para usuarios (administradores y estudiantes)
object UserTable : Table("usuarios") {
    val id = integer("id").autoIncrement()
    val nombre = varchar("nombre", 100)
    val correo = varchar("correo", 100)
    val password = varchar("password", 100)
    val role = varchar("role", 20) // Aquí guardaremos "admin" o "estudiante"
    override val primaryKey = PrimaryKey(id)
}

// Clase de datos para manejar la información en el código
data class User(
    val id: Int,
    val nombre: String,
    val correo: String,
    val role: String
)

class UserService(private val database: Database) {

    // Función para obtener todos los usuarios y verificar que la base responde
    fun getAllUsers(): List<User> = transaction(database) {
        UserTable.selectAll().map {
            User(
                id = it[UserTable.id],
                nombre = it[UserTable.nombre],
                correo = it[UserTable.correo],
                role = it[UserTable.role]
            )
        }
    }
}