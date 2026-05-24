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
import com.Carreras
import com.RespuestasIndividuales
import com.Sectores
import com.Tarjetas

@Serializable
data class ResultadoResponse(
    val id: Int,
    val idUsuario: Int,
    val nombreUsuario: String,
    val sectorSugerido: String,
    val carrerasAfines: List<String>,
    val fecha: String
)

//lo agrege para que muestre la lista de preguntas de cada alumno 23/05
@Serializable
data class DiagnosticoCompleto(
    val estado: String,
    val sectorPrincipal: String,
    val totalContestadas: String,
    val respuestas: List<RespuestaHistorial>,
    val carrerasAfines: List<String> = emptyList()
)

@Serializable
data class RespuestaHistorial(
    val pregunta: String,
    val respuesta: String,
    val sector: String
)
fun Route.resultadoRoutes() {

    authenticate("auth-jwt") {
        //agrego 23/05
        get("/api/diagnostico/{idUsuario}") {
            val idUsuario = call.parameters["idUsuario"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val respuestas = transaction {
                (RespuestasIndividuales innerJoin Tarjetas innerJoin Carreras innerJoin Sectores)
                    .select { RespuestasIndividuales.idUsuario eq idUsuario }
                    .map {
                        RespuestaHistorial(
                            pregunta  = it[Tarjetas.texto],
                            respuesta = it[RespuestasIndividuales.leIntereso].toString(),
                            sector    = it[Sectores.nombre]
                        )
                    }
            }

            if (respuestas.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Sin respuestas aún"))
                return@get
            }

            val resultado = transaction {
                Resultados.select { Resultados.idUsuario eq idUsuario }
                    .orderBy(Resultados.fecha, SortOrder.DESC)
                    .limit(1).singleOrNull()
            }

            val sectorPrincipal = resultado?.get(Resultados.sectorSugerido)
                ?: respuestas.filter { it.respuesta == "true" }
                    .groupBy { it.sector }
                    .maxByOrNull { it.value.size }?.key ?: "Calculando..."

            val carrerasAfines = resultado?.get(Resultados.carrerasAfines)
                ?.split(",") ?: emptyList()

            val estado = if (resultado != null) "finalizado" else "en_progreso"

            call.respond(DiagnosticoCompleto(
                estado           = estado,
                sectorPrincipal  = sectorPrincipal,
                totalContestadas = respuestas.size.toString(),
                respuestas       = respuestas,
                carrerasAfines   = carrerasAfines
            ))
        }


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