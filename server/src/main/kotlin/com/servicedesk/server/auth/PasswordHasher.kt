package com.servicedesk.server.auth

import java.security.MessageDigest
import java.security.SecureRandom

object PasswordHasher {
    private val random = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val saltHex = salt.toHex()
        return "$saltHex:${sha256(saltHex + password)}"
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(":", limit = 2)
        if (parts.size != 2 || stored.isBlank()) return false
        return sha256(parts[0] + password) == parts[1]
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(Charsets.UTF_8)).toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
