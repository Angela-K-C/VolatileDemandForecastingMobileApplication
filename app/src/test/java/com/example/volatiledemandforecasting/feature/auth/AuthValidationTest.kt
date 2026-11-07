package com.example.volatiledemandforecasting.feature.auth

import com.example.volatiledemandforecasting.core.network.ApiException
import com.example.volatiledemandforecasting.core.network.rejectedFields
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    @Test
    fun `email needs an at sign and a domain`() {
        assertNull(AuthValidation.email("you@example.com"))
        assertNull(AuthValidation.email("  you@example.com  "))
        assertNotNull(AuthValidation.email(""))
        assertNotNull(AuthValidation.email("you"))
        assertNotNull(AuthValidation.email("you@example"))
        assertNotNull(AuthValidation.email("you @example.com"))
    }

    @Test
    fun `a server-rejected email is reported on the email field`() {
        val details = Json.parseToJsonElement(
            """{"errors":[{"loc":["body","email"],"msg":"value is not a valid email address"}]}"""
        ) as JsonObject
        val rejected = ApiException(422, "INVALID_REQUEST", "Some fields are missing or invalid.", details)
        val other = ApiException(401, "INVALID_CREDENTIALS", "Email or password is incorrect.")

        assertEquals(setOf("email"), rejected.rejectedFields())
        assertNotNull(AuthValidation.serverEmailError(rejected))
        assertNull(AuthValidation.serverEmailError(other))
        assertNull(AuthValidation.serverEmailError(java.io.IOException("offline")))
    }

    @Test
    fun `a new password must be 10 to 200 characters`() {
        assertNotNull(AuthValidation.newPassword(""))
        assertNotNull(AuthValidation.newPassword("a".repeat(9)))
        assertNull(AuthValidation.newPassword("a".repeat(10)))
        assertNull(AuthValidation.newPassword("a".repeat(200)))
        assertNotNull(AuthValidation.newPassword("a".repeat(201)))
    }

    @Test
    fun `signing in only requires some password`() {
        assertNotNull(AuthValidation.existingPassword(""))
        assertNull(AuthValidation.existingPassword("short"))
    }

    @Test
    fun `display name is required and capped at 120`() {
        assertNotNull(AuthValidation.displayName("   "))
        assertNull(AuthValidation.displayName("Ada"))
        assertNull(AuthValidation.displayName("a".repeat(120)))
        assertNotNull(AuthValidation.displayName("a".repeat(121)))
    }

    @Test
    fun `reset code is exactly 8 digits`() {
        assertNull(AuthValidation.resetCode("12345678"))
        assertNotNull(AuthValidation.resetCode("1234567"))
        assertNotNull(AuthValidation.resetCode("123456789"))
        assertNotNull(AuthValidation.resetCode("1234567a"))
    }
}
