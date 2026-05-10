package com

import com.routes.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") { call.respondText("MatchVoc API v1.0 — Online") }
        authRoutes()
        usuarioRoutes()
        sectorRoutes()
        carreraRoutes()
        tarjetaRoutes()
        testRoutes()
        resultadoRoutes()
        universidadRoutes()
    }
}