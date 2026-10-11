package com.servicedesk.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.servicedesk.server.config.AppConfig
import com.servicedesk.server.plugins.StaffPrincipal
import java.util.Date

object JwtService {
    private const val ISSUER = "service-desk"
    private val algorithm: Algorithm
        get() = Algorithm.HMAC256(AppConfig.jwtSecret)

    fun issue(staffId: Int, email: String, role: String): String {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(ISSUER)
            .withSubject(staffId.toString())
            .withClaim("staffId", staffId)
            .withClaim("email", email)
            .withClaim("role", role)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + 7L * 24 * 60 * 60 * 1000))
            .sign(algorithm)
    }

    fun verifier() = JWT.require(algorithm).withIssuer(ISSUER).build()

    fun staffId(payload: com.auth0.jwt.interfaces.Payload): Int? =
        payload.getClaim("staffId")?.asInt() ?: payload.subject?.toIntOrNull()

    fun principalFromToken(rawToken: String): StaffPrincipal? =
        runCatching {
            val token = rawToken.removePrefix("Bearer").trim()
            val jwt = verifier().verify(token)
            val staffId = staffId(jwt) ?: return null
            val role = jwt.getClaim("role").asString() ?: "support_agent"
            StaffPrincipal(staffId, role)
        }.getOrNull()
}
