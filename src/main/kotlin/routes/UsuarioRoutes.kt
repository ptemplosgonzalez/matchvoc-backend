package com.routes

import com.Usuarios
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
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

        // GET un usuario por ID
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