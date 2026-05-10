package com.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.Usuarios
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class RegisterRequest(
    val nombre: String,
    val correo: String,
    val password: String,
    val role: String = "student"
)

@Serializable
data class LoginRequest(
    val correo: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: Int,
    val nombre: String,
    val role: String
)

object AuthService {

    fun register(req: RegisterRequest): AuthResponse {
        val existe = transaction {
            Usuarios.select { Usuarios.correo eq req.correo }.count() > 0
        }
        if (existe) error("El correo ya está registrado")

        val hash = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())

        var newId = 0
        transaction {
            val stmt = Usuarios.insert {
                it[nombre]       = req.nombre
                it[correo]       = req.correo
                it[passwordHash] = hash
                it[role]         = req.role
            }
            newId = stmt[Usuarios.id]
        }

        return AuthResponse(
            token  = JwtConfig.generateToken(newId, req.role),
            userId = newId,
            nombre = req.nombre,
            role   = req.role
        )
    }

    fun login(req: LoginRequest): AuthResponse {
        val user = transaction {
            Usuarios.select { Usuarios.correo eq req.correo }.singleOrNull()
        } ?: error("Correo o contraseña incorrectos")

        val passwordValida = BCrypt.verifyer()
            .verify(req.password.toCharArray(), user[Usuarios.passwordHash])
            .verified

        if (!passwordValida) error("Correo o contraseña incorrectos")

        return AuthResponse(
            token  = JwtConfig.generateToken(user[Usuarios.id], user[Usuarios.role]),
            userId = user[Usuarios.id],
            nombre = user[Usuarios.nombre],
            role   = user[Usuarios.role]
        )
    }
}