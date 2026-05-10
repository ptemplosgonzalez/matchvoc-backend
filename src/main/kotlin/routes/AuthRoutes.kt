package com.routes

import com.services.AuthService
import com.services.LoginRequest
import com.services.RegisterRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {

    post("/api/auth/register") {
        try {
            val req = call.receive<RegisterRequest>()
            if (req.nombre.isBlank() || req.correo.isBlank() || req.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Todos los campos son requeridos"))
                return@post
            }
            if (req.password.length < 6) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "La contraseña debe tener al menos 6 caracteres"))
                return@post
            }
            val response = AuthService.register(req)
            call.respond(HttpStatusCode.Created, response)
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Conflict, mapOf("error" to (e.message ?: "Error al registrar")))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
        }
    }

    post("/api/auth/login") {
        try {
            val req = call.receive<LoginRequest>()
            if (req.correo.isBlank() || req.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Correo y contraseña requeridos"))
                return@post
            }
            val response = AuthService.login(req)
            call.respond(HttpStatusCode.OK, response)
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to (e.message ?: "Credenciales incorrectas")))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor"))
        }
    }
}