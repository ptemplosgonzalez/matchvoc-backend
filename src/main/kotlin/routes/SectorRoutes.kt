package com.routes

import com.Carreras
import com.Sectores
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

@Serializable
data class SectorRequest(val nombre: String)

@Serializable
data class SectorResponse(val id: Int, val nombre: String)

fun Route.sectorRoutes() {

    // GET /api/sectores — público
    get("/api/sectores") {
        val sectores = transaction {
            Sectores.selectAll().map {
                SectorResponse(it[Sectores.id], it[Sectores.nombre])
            }
        }
        call.respond(sectores)
    }

    // Rutas protegidas (solo admin)
    authenticate("auth-jwt") {

        post("/api/sectores") {
            try {
                val req = call.receive<SectorRequest>()
                if (req.nombre.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "El nombre es requerido"))
                    return@post
                }
                val newId = transaction {
                    Sectores.insert { it[nombre] = req.nombre }[Sectores.id]
                }
                call.respond(HttpStatusCode.Created, SectorResponse(newId, req.nombre))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al crear sector"))
            }
        }

        put("/api/sectores/{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val req = call.receive<SectorRequest>()
                val updated = transaction {
                    Sectores.update({ Sectores.id eq id }) { it[nombre] = req.nombre }
                }
                if (updated == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Sector no encontrado"))
                else call.respond(mapOf("mensaje" to "Sector actualizado"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al actualizar"))
            }
        }

        delete("/api/sectores/{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val deleted = transaction { Sectores.deleteWhere { Sectores.id eq id } }
                if (deleted == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Sector no encontrado"))
                else call.respond(mapOf("mensaje" to "Sector eliminado"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al eliminar"))
            }
        }
    }
}