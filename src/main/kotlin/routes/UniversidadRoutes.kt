package com.routes

import com.Universidades
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class UniversidadRequest(
    val nombre: String,
    val localidad: String? = null,
    val sitioWeb: String? = null,
    val ofertaEducativa: String? = null,
    val logoUrl: String? = null
)

@Serializable
data class UniversidadResponse(
    val id: Int,
    val nombre: String,
    val localidad: String?,
    val sitioWeb: String?,
    val ofertaEducativa: String?,
    val logoUrl: String?
)

fun Route.universidadRoutes() {

    // GET todas — público
    get("/api/universidades") {
        val lista = transaction {
            Universidades.selectAll().map {
                UniversidadResponse(
                    id               = it[Universidades.id],
                    nombre           = it[Universidades.nombre],
                    localidad        = it[Universidades.localidad],
                    sitioWeb         = it[Universidades.sitioWeb],
                    ofertaEducativa  = it[Universidades.ofertaEducativa],
                    logoUrl          = it[Universidades.logoUrl]
                )
            }
        }
        call.respond(lista)
    }

    authenticate("auth-jwt") {

        post("/api/universidades") {
            try {
                val req = call.receive<UniversidadRequest>()
                val newId = transaction {
                    Universidades.insert {
                        it[nombre]          = req.nombre
                        it[localidad]       = req.localidad
                        it[sitioWeb]        = req.sitioWeb
                        it[ofertaEducativa] = req.ofertaEducativa
                        it[logoUrl]         = req.logoUrl
                    }[Universidades.id]
                }
                call.respond(HttpStatusCode.Created,
                    UniversidadResponse(newId, req.nombre, req.localidad, req.sitioWeb, req.ofertaEducativa, req.logoUrl))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al crear universidad"))
            }
        }

        put("/api/universidades/{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val req = call.receive<UniversidadRequest>()
                val updated = transaction {
                    Universidades.update({ Universidades.id eq id }) {
                        it[nombre]          = req.nombre
                        it[localidad]       = req.localidad
                        it[sitioWeb]        = req.sitioWeb
                        it[ofertaEducativa] = req.ofertaEducativa
                        it[logoUrl]         = req.logoUrl
                    }
                }
                if (updated == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "No encontrada"))
                else call.respond(mapOf("mensaje" to "Universidad actualizada"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al actualizar"))
            }
        }

        delete("/api/universidades/{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val deleted = transaction { Universidades.deleteWhere { Universidades.id eq id } }
                if (deleted == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "No encontrada"))
                else call.respond(mapOf("mensaje" to "Universidad eliminada"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al eliminar"))
            }
        }
    }
}