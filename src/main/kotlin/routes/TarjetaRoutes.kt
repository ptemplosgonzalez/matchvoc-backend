package com.routes

import com.Carreras
import com.Sectores
import com.Tarjetas
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class TarjetaRequest(
    val texto: String,
    val idCarrera: Int,
    val imagenUrl: String? = null
)

@Serializable
data class TarjetaResponse(
    val id: Int,
    val texto: String,
    val idCarrera: Int,
    val nombreCarrera: String,
    val nombreSector: String,
    val imagenUrl: String?,
    val activa: Boolean
)

fun Route.tarjetaRoutes() {

    // GET /api/tarjetas — todas las activas (para el test en la app)
    get("/api/tarjetas") {
        val tarjetas = transaction {
            (Tarjetas innerJoin Carreras innerJoin Sectores)
                .select { Tarjetas.activa eq true }
                .map {
                    TarjetaResponse(
                        id            = it[Tarjetas.id],
                        texto         = it[Tarjetas.texto],
                        idCarrera     = it[Tarjetas.idCarrera],
                        nombreCarrera = it[Carreras.nombre],
                        nombreSector  = it[Sectores.nombre],
                        imagenUrl     = it[Tarjetas.imagenUrl],
                        activa        = it[Tarjetas.activa]
                    )
                }
        }
        call.respond(tarjetas)
    }

    // GET /api/carreras/{id}/tarjetas — tarjetas de una carrera
    get("/api/carreras/{id}/tarjetas") {
        val idCarrera = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
        val tarjetas = transaction {
            (Tarjetas innerJoin Carreras innerJoin Sectores)
                .select { Tarjetas.idCarrera eq idCarrera }
                .map {
                    TarjetaResponse(
                        id            = it[Tarjetas.id],
                        texto         = it[Tarjetas.texto],
                        idCarrera     = it[Tarjetas.idCarrera],
                        nombreCarrera = it[Carreras.nombre],
                        nombreSector  = it[Sectores.nombre],
                        imagenUrl     = it[Tarjetas.imagenUrl],
                        activa        = it[Tarjetas.activa]
                    )
                }
        }
        call.respond(tarjetas)
    }

    authenticate("auth-jwt") {

        post("/api/tarjetas") {
            try {
                val req = call.receive<TarjetaRequest>()
                if (req.texto.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "El texto es requerido"))
                    return@post
                }
                val newId = transaction {
                    Tarjetas.insert {
                        it[texto]     = req.texto
                        it[idCarrera] = req.idCarrera
                        it[imagenUrl] = req.imagenUrl
                        it[activa]    = true
                    }[Tarjetas.id]
                }
                val (nombreCarrera, nombreSector) = transaction {
                    (Carreras innerJoin Sectores)
                        .select { Carreras.id eq req.idCarrera }
                        .map { it[Carreras.nombre] to it[Sectores.nombre] }
                        .single()
                }
                call.respond(HttpStatusCode.Created,
                    TarjetaResponse(newId, req.texto, req.idCarrera, nombreCarrera, nombreSector, req.imagenUrl, true))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al crear tarjeta"))
            }
        }

        put("/api/tarjetas/{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val req = call.receive<TarjetaRequest>()
                val updated = transaction {
                    Tarjetas.update({ Tarjetas.id eq id }) {
                        it[texto]     = req.texto
                        it[idCarrera] = req.idCarrera
                        it[imagenUrl] = req.imagenUrl
                    }
                }
                if (updated == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Tarjeta no encontrada"))
                else call.respond(mapOf("mensaje" to "Tarjeta actualizada"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al actualizar"))
            }
        }

        delete("/api/tarjetas/{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                // Soft delete: marcar como inactiva en lugar de borrar
                val updated = transaction {
                    Tarjetas.update({ Tarjetas.id eq id }) { it[activa] = false }
                }
                if (updated == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Tarjeta no encontrada"))
                else call.respond(mapOf("mensaje" to "Tarjeta desactivada"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al eliminar"))
            }
        }
    }
}