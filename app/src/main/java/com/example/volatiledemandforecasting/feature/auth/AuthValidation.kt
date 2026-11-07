package com.example.volatiledemandforecasting.feature.auth

import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.network.ApiException
import com.example.volatiledemandforecasting.core.network.rejectedFields
import com.example.volatiledemandforecasting.core.ui.UiText

/**
 * Inline checks that catch obvious mistakes before a request is sent. The limits are the
 * backend's (openapi.json); the backend remains the authority and can still say no.
 */
object AuthValidation {
    const val MIN_PASSWORD_LENGTH = 10
    const val MAX_PASSWORD_LENGTH = 200
    const val MAX_NAME_LENGTH = 120
    const val RESET_CODE_LENGTH = 8

    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun email(value: String): UiText? = when {
        value.isBlank() -> UiText.Res(R.string.error_email_required)
        !EMAIL.matches(value.trim()) -> UiText.Res(R.string.error_email_invalid)
        else -> null
    }

    /**
     * The backend's email check is stricter than [email] (it rejects reserved domains such as
     * `.invalid`). When it refuses an address, say so on the email field, not in a vague banner.
     */
    fun serverEmailError(failure: Throwable): UiText? =
        if (failure is ApiException && "email" in failure.rejectedFields()) UiText.Res(R.string.error_email_invalid) else null

    /** Sign-in only checks that something was typed; existing passwords may predate any rule. */
    fun existingPassword(value: String): UiText? =
        if (value.isEmpty()) UiText.Res(R.string.error_password_required) else null

    fun newPassword(value: String): UiText? = when {
        value.isEmpty() -> UiText.Res(R.string.error_password_required)
        value.length < MIN_PASSWORD_LENGTH -> UiText.Res(R.string.error_password_short, listOf(MIN_PASSWORD_LENGTH))
        value.length > MAX_PASSWORD_LENGTH -> UiText.Res(R.string.error_password_long, listOf(MAX_PASSWORD_LENGTH))
        else -> null
    }

    fun displayName(value: String): UiText? = when {
        value.isBlank() -> UiText.Res(R.string.error_name_required)
        value.trim().length > MAX_NAME_LENGTH -> UiText.Res(R.string.error_name_long, listOf(MAX_NAME_LENGTH))
        else -> null
    }

    fun resetCode(value: String): UiText? =
        if (value.length == RESET_CODE_LENGTH && value.all(Char::isDigit)) null
        else UiText.Res(R.string.error_code_invalid, listOf(RESET_CODE_LENGTH))
}
