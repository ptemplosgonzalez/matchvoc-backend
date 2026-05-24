package com.routes

import com.Carreras
import com.RespuestasIndividuales
import com.Resultados
import com.Sectores
import com.Tarjetas
import com.TestProgress
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class SwipeRequest(
    val idUsuario: Int,
    val idTarjeta: Int,
    val leIntereso: Boolean
)

@Serializable
data class ProgressResponse(
    val idUsuario: Int,
    val ultimaTarjeta: Int,
    val totalCompletadas: Int,
    val finalizado: Boolean
)

@Serializable
data class ResultadoCalculado(
    val sectorSugerido: String,
    val carrerasAfines: List<String>,
    val detallePorSector: Map<String, Int>
)

fun Route.testRoutes() {

    authenticate("auth-jwt") {

        // GET respuestas individuales por usuario
        get("/api/test/respuestas/{idUsuario}") {
            val idUsuario = call.parameters["idUsuario"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val respuestas = transaction {
                (RespuestasIndividuales innerJoin Tarjetas innerJoin Carreras innerJoin Sectores)
                    .select { RespuestasIndividuales.idUsuario eq idUsuario }
                    .map {
                        mapOf(
                            "pregunta" to it[Tarjetas.texto],
                            "sector" to it[Sectores.nombre],
                            "carrera" to it[Carreras.nombre],
                            "respuesta" to it[RespuestasIndividuales.leIntereso].toString()
                        )
                    }
            }
            call.respond(respuestas)
        }

        get("/api/test/progress/{idUsuario}") {
            val idUsuario = call.parameters["idUsuario"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

            val progress = transaction {
                TestProgress.select { TestProgress.idUsuario eq idUsuario }.singleOrNull()
            }

            if (progress == null)
                call.respond(ProgressResponse(idUsuario, 0, 0, false))
            else
                call.respond(ProgressResponse(
                    idUsuario        = idUsuario,
                    ultimaTarjeta    = progress[TestProgress.ultimaTarjeta],
                    totalCompletadas = progress[TestProgress.totalCompletadas],
                    finalizado       = progress[TestProgress.finalizado]
                ))
        }

        post("/api/test/swipe") {
            try {
                val req = call.receive<SwipeRequest>()

                transaction {
                    RespuestasIndividuales.insert {
                        it[idUsuario]  = req.idUsuario
                        it[idTarjeta]  = req.idTarjeta
                        it[leIntereso] = req.leIntereso
                    }

                    val existe = TestProgress
                        .select { TestProgress.idUsuario eq req.idUsuario }
                        .count() > 0

                    if (existe) {
                        // Leer el valor actual y sumar 1 en Kotlin
                        val actual = TestProgress
                            .select { TestProgress.idUsuario eq req.idUsuario }
                            .single()[TestProgress.totalCompletadas]

                        TestProgress.update({ TestProgress.idUsuario eq req.idUsuario }) {
                            it[ultimaTarjeta]    = req.idTarjeta
                            it[totalCompletadas] = actual + 1
                        }
                    } else {
                        TestProgress.insert {
                            it[idUsuario]        = req.idUsuario
                            it[ultimaTarjeta]    = req.idTarjeta
                            it[totalCompletadas] = 1
                            it[finalizado]       = false
                        }
                    }
                }

                call.respond(HttpStatusCode.Created, mapOf("mensaje" to "Swipe registrado"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al registrar swipe"))
            }
        }

        post("/api/test/finalizar/{idUsuario}") {
            try {
                val idUsuario = call.parameters["idUsuario"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

                val respuestas = transaction {
                    (RespuestasIndividuales innerJoin Tarjetas innerJoin Carreras innerJoin Sectores)
                        .select {
                            (RespuestasIndividuales.idUsuario eq idUsuario) and
                                    (RespuestasIndividuales.leIntereso eq true)
                        }
                        .map { it[Sectores.nombre] to it[Carreras.nombre] }
                }

                if (respuestas.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No hay respuestas registradas"))
                    return@post
                }

                val porSector = respuestas.groupBy { it.first }.mapValues { it.value.size }
                val sectorGanador = porSector.maxByOrNull { it.value }?.key ?: "Sin determinar"
                val carrerasAfines = respuestas
                    .filter { it.first == sectorGanador }
                    .groupBy { it.second }
                    .entries
                    .sortedByDescending { it.value.size }
                    .take(3)
                    .map { it.key }

                transaction {
                    Resultados.insert {
                        it[Resultados.idUsuario]      = idUsuario
                        it[Resultados.sectorSugerido] = sectorGanador
                        it[Resultados.carrerasAfines] = carrerasAfines.joinToString(",")
                    }
                    TestProgress.update({ TestProgress.idUsuario eq idUsuario }) {
                        it[finalizado] = true
                    }
                }

                call.respond(ResultadoCalculado(
                    sectorSugerido   = sectorGanador,
                    carrerasAfines   = carrerasAfines,
                    detallePorSector = porSector
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al calcular resultado"))
            }
        }

        delete("/api/test/reset/{idUsuario}") {
            try {
                val idUsuario = call.parameters["idUsuario"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))

                transaction {
                    RespuestasIndividuales.deleteWhere { RespuestasIndividuales.idUsuario eq idUsuario }
                    TestProgress.deleteWhere { TestProgress.idUsuario eq idUsuario }
                }

                call.respond(mapOf("mensaje" to "Test reiniciado correctamente"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error al reiniciar"))
            }
        }
    }
}