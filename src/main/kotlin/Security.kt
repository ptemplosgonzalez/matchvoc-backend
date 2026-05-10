package com

import com.services.JwtConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm
            verifier(JwtConfig.verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asInt()
                val role   = credential.payload.getClaim("role").asString()
                if (userId != null && role != null)
                    JWTPrincipal(credential.payload)
                else null
            }
        }
    }
}