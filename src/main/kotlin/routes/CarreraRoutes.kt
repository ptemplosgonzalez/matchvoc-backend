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
data class CarreraRequest(val nombre: String, val idSector: Int)

@Serializable
data class CarreraResponse(val id: Int, val nombre: String, val idSector: Int, val nombreSector: String)

fun Route.carreraRoutes() {

    // GET /api/sectores/{id}/carreras — público
    get("/api/sectores/{id}/carreras") {
        val idSector = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
        val carreras = transaction {
            (Carreras innerJoin Sectores)
                .select { Carreras.idSector eq idSector }
                .map {
                    CarreraResponse(
                        id          = it[Carreras.id],
                        nombre      = it[Carreras.nombre],
                        idSector    = it[Carreras.idSector],
                        nombreSector = it[Sectores.nombre]
                    )
                }
        }
        call.respond(carreras)
    }

    // GET /api/carreras — público
    get("/api/carreras") {
        val carreras = transaction {
            (Carreras innerJoin Sectores).selectAll().map {
                CarreraResponse(
                    id           = it[Carreras.id],
                    nombre       = it[Carreras.nombre],
                    idSector     = it[Carreras.idSector],
                    nombreSector = it[Sectores.nombre]
                )
            }
        }
        call.respond(carreras)
    }

    authenticate("auth-jwt") {

        post("/api/carreras") {
            try {
                val req = call.receive<CarreraRequest>()
                if (req.nombre.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "El nombre es requerido"))
                    return@post
                }
                val newId = transaction {
                    Carreras.insert {
                        it[nombre]   = req.nombre
                        it[idSector] = req.idSector
                    }[Carreras.id]
                }
                val sector = transaction {
                    Sectores.select { Sectores.id eq req.idSector }.single()[Sectores.nombre]
                }
                call.respond(HttpStatusCode.Created,
                    CarreraResponse(newId, req.nombre, req.idSector, sector))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al crear carrera"))
            }
        }

        put("/api/carreras/{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val req = call.receive<CarreraRequest>()
                val updated = transaction {
                    Carreras.update({ Carreras.id eq id }) {
                        it[nombre]   = req.nombre
                        it[idSector] = req.idSector
                    }
                }
                if (updated == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Carrera no encontrada"))
                else call.respond(mapOf("mensaje" to "Carrera actualizada"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al actualizar"))
            }
        }

        delete("/api/carreras/{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val deleted = transaction { Carreras.deleteWhere { Carreras.id eq id } }
                if (deleted == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Carrera no encontrada"))
                else call.respond(mapOf("mensaje" to "Carrera eliminada"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al eliminar"))
            }
        }
    }
}