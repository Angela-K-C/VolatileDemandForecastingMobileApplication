package com.example.volatiledemandforecasting.core.network

import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.UiText
import java.io.IOException

/**
 * Maps a failure to something a person can act on. Known codes get a string resource; any other
 * code falls back to the API's own `message`, which is already plain language (api.md).
 * "Something went wrong" is only the last resort.
 */
fun Throwable.toUiText(): UiText = when (this) {
    is ApiException -> when (code) {
        ApiErrorCodes.INVALID_CREDENTIALS -> UiText.Res(R.string.error_invalid_credentials)
        ApiErrorCodes.EMAIL_TAKEN -> UiText.Res(R.string.error_email_taken)
        ApiErrorCodes.REGISTRATION_CLOSED -> UiText.Res(R.string.error_registration_closed)
        ApiErrorCodes.INVALID_RESET_CODE -> UiText.Res(R.string.error_invalid_reset_code)
        ApiErrorCodes.UNAUTHORIZED, ApiErrorCodes.INVALID_REFRESH_TOKEN -> UiText.Res(R.string.error_session_expired)
        ApiErrorCodes.RATE_LIMITED ->
            if (retryAfterSeconds != null) UiText.Res(R.string.error_rate_limited, listOf(retryAfterSeconds))
            else UiText.Res(R.string.error_rate_limited_unknown)
        "ML_SERVICE_UNAVAILABLE" -> UiText.Res(R.string.error_ml_unavailable)
        else -> if (message.isNotBlank()) UiText.Raw(message) else UiText.Res(R.string.error_unknown)
    }
    is IOException -> UiText.Res(R.string.error_network)
    else -> UiText.Res(R.string.error_unknown)
}
