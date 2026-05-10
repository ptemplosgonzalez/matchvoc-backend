package com.routes

import com.Resultados
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
data class ResultadoResponse(
    val id: Int,
    val idUsuario: Int,
    val nombreUsuario: String,
    val sectorSugerido: String,
    val carrerasAfines: List<String>,
    val fecha: String
)

fun Route.resultadoRoutes() {

    authenticate("auth-jwt") {

        // GET resultado de un alumno específico
        get("/api/resultados/{idUsuario}") {
            val idUsuario = call.parameters["idUsuario"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val resultado = transaction {
                (Resultados innerJoin Usuarios)
                    .select { Resultados.idUsuario eq idUsuario }
                    .orderBy(Resultados.fecha, SortOrder.DESC)
                    .limit(1)
                    .map {
                        ResultadoResponse(
                            id             = it[Resultados.id],
                            idUsuario      = it[Resultados.idUsuario],
                            nombreUsuario  = it[Usuarios.nombre],
                            sectorSugerido = it[Resultados.sectorSugerido],
                            carrerasAfines = it[Resultados.carrerasAfines].split(","),
                            fecha          = it[Resultados.fecha].toString()
                        )
                    }.singleOrNull()
            }

            if (resultado == null)
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Sin resultados aún"))
            else
                call.respond(resultado)
        }

        // GET todos los resultados (solo admin)
        get("/api/resultados") {
            val resultados = transaction {
                (Resultados innerJoin Usuarios).selectAll()
                    .orderBy(Resultados.fecha, SortOrder.DESC)
                    .map {
                        ResultadoResponse(
                            id             = it[Resultados.id],
                            idUsuario      = it[Resultados.idUsuario],
                            nombreUsuario  = it[Usuarios.nombre],
                            sectorSugerido = it[Resultados.sectorSugerido],
                            carrerasAfines = it[Resultados.carrerasAfines].split(","),
                            fecha          = it[Resultados.fecha].toString()
                        )
                    }
            }
            call.respond(resultados)
        }

        // GET estadísticas globales
        get("/api/estadisticas") {
            val stats = transaction {
                Resultados.selectAll()
                    .map { it[Resultados.sectorSugerido] }
                    .groupBy { it }
                    .mapValues { it.value.size }
                    .entries
                    .sortedByDescending { it.value }
                    .associate { it.key to it.value }
            }
            call.respond(stats)
        }
    }
}