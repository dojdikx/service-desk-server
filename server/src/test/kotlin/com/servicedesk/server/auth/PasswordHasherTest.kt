package com.servicedesk.server.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordHasherTest {
    @Test
    fun hashAndVerify() {
        val hash = PasswordHasher.hash("secret12")
        assertTrue(PasswordHasher.verify("secret12", hash))
        assertFalse(PasswordHasher.verify("wrong", hash))
    }
}
