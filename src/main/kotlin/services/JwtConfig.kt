package com.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtConfig {
    private val secret   = System.getenv("JWT_SECRET") ?: "matchvoc-secret-key-2024"
    private val issuer   = "matchvoc"
    private val audience = "matchvoc-users"
    val realm            = "matchvoc"
    val verifier = JWT.require(Algorithm.HMAC256(secret))
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun generateToken(userId: Int, role: String): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId)
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000L * 7)) // 7 días
            .sign(Algorithm.HMAC256(secret))
}