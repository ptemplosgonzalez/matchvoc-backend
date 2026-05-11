package com.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.Usuarios
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class UsuarioResponse(
    val id: Int,
    val nombre: String,
    val correo: String,
    val role: String,
    val fotoUrl: String?,
    val createdAt: String
)

@Serializable
data class UpdatePerfilRequest(
    val nombre: String? = null,
    val correo: String? = null,
    val fotoUrl: String? = null
)

@Serializable
data class UpdatePasswordRequest(
    val passwordActual: String,
    val passwordNuevo: String
)

fun Route.usuarioRoutes() {

    authenticate("auth-jwt") {

        // GET todos los usuarios
        get("/api/usuarios") {
            val usuarios = transaction {
                Usuarios.selectAll().map {
                    UsuarioResponse(
                        id        = it[Usuarios.id],
                        nombre    = it[Usuarios.nombre],
                        correo    = it[Usuarios.correo],
                        role      = it[Usuarios.role],
                        fotoUrl   = it[Usuarios.fotoUrl],
                        createdAt = it[Usuarios.createdAt].toString()
                    )
                }
            }
            call.respond(usuarios)
        }

        // GET perfil propio (desde el token)
        get("/api/usuarios/me") {
            val principal = call.principal<JWTPrincipal>()
            val userId    = principal?.payload?.getClaim("userId")?.asInt()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No autorizado"))

            val usuario = transaction {
                Usuarios.select { Usuarios.id eq userId }.singleOrNull()?.let {
                    UsuarioResponse(
                        id        = it[Usuarios.id],
                        nombre    = it[Usuarios.nombre],
                        correo    = it[Usuarios.correo],
                        role      = it[Usuarios.role],
                        fotoUrl   = it[Usuarios.fotoUrl],
                        createdAt = it[Usuarios.createdAt].toString()
                    )
                }
            } ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Usuario no encontrado"))

            call.respond(usuario)
        }

        // PUT actualizar perfil (nombre, correo, foto)
        put("/api/usuarios/me") {
            val principal = call.principal<JWTPrincipal>()
            val userId    = principal?.payload?.getClaim("userId")?.asInt()
                ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No autorizado"))

            try {
                val req = call.receive<UpdatePerfilRequest>()

                // Verificar que el correo no esté en uso por otro usuario
                if (req.correo != null) {
                    val correoEnUso = transaction {
                        Usuarios.select { (Usuarios.correo eq req.correo) and (Usuarios.id neq userId) }.count() > 0
                    }
                    if (correoEnUso) {
                        call.respond(HttpStatusCode.Conflict, mapOf("error" to "Ese correo ya está en uso"))
                        return@put
                    }
                }

                transaction {
                    Usuarios.update({ Usuarios.id eq userId }) { row ->
                        req.nombre?.let  { row[nombre]  = it }
                        req.correo?.let  { row[correo]  = it }
                        req.fotoUrl?.let { row[fotoUrl] = it }
                    }
                }

                val updated = transaction {
                    Usuarios.select { Usuarios.id eq userId }.single().let {
                        UsuarioResponse(
                            id        = it[Usuarios.id],
                            nombre    = it[Usuarios.nombre],
                            correo    = it[Usuarios.correo],
                            role      = it[Usuarios.role],
                            fotoUrl   = it[Usuarios.fotoUrl],
                            createdAt = it[Usuarios.createdAt].toString()
                        )
                    }
                }

                call.respond(updated)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al actualizar perfil"))
            }
        }

        // PUT cambiar contraseña
        put("/api/usuarios/me/password") {
            val principal = call.principal<JWTPrincipal>()
            val userId    = principal?.payload?.getClaim("userId")?.asInt()
                ?: return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No autorizado"))

            try {
                val req = call.receive<UpdatePasswordRequest>()

                if (req.passwordNuevo.length < 6) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "La nueva contraseña debe tener al menos 6 caracteres"))
                    return@put
                }

                val hashActual = transaction {
                    Usuarios.select { Usuarios.id eq userId }.single()[Usuarios.passwordHash]
                }

                val valida = BCrypt.verifyer().verify(req.passwordActual.toCharArray(), hashActual).verified
                if (!valida) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "La contraseña actual es incorrecta"))
                    return@put
                }

                val nuevoHash = BCrypt.withDefaults().hashToString(12, req.passwordNuevo.toCharArray())
                transaction {
                    Usuarios.update({ Usuarios.id eq userId }) { it[passwordHash] = nuevoHash }
                }

                call.respond(mapOf("mensaje" to "Contraseña actualizada correctamente"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al cambiar contraseña"))
            }
        }

        // GET usuario por ID
        get("/api/usuarios/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            val usuario = transaction {
                Usuarios.select { Usuarios.id eq id }.singleOrNull()?.let {
                    UsuarioResponse(
                        id        = it[Usuarios.id],
                        nombre    = it[Usuarios.nombre],
                        correo    = it[Usuarios.correo],
                        role      = it[Usuarios.role],
                        fotoUrl   = it[Usuarios.fotoUrl],
                        createdAt = it[Usuarios.createdAt].toString()
                    )
                }
            }
            if (usuario == null)
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Usuario no encontrado"))
            else
                call.respond(usuario)
        }

        // DELETE eliminar usuario
        delete("/api/usuarios/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            val deleted = transaction { Usuarios.deleteWhere { Usuarios.id eq id } }
            if (deleted == 0)
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Usuario no encontrado"))
            else
                call.respond(mapOf("mensaje" to "Usuario eliminado"))
        }
    }
}