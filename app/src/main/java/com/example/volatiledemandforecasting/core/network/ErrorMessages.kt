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
        ApiErrorCodes.ML_SERVICE_UNAVAILABLE -> UiText.Res(R.string.error_ml_unavailable)
        ApiErrorCodes.FILE_NOT_UTF8 -> UiText.Res(R.string.error_file_not_utf8)
        ApiErrorCodes.FILE_TOO_LARGE -> UiText.Res(R.string.error_file_too_large)
        ApiErrorCodes.EMPTY_FILE -> UiText.Res(R.string.error_empty_file)
        ApiErrorCodes.IMPORT_NOT_READY -> UiText.Res(R.string.error_import_not_ready)
        ApiErrorCodes.REFERENCE_MODEL_FIXED -> UiText.Res(R.string.error_reference_model_fixed)
        ApiErrorCodes.NOT_ELIGIBLE -> UiText.Res(R.string.error_not_eligible)
        ApiErrorCodes.FORBIDDEN -> UiText.Res(R.string.error_forbidden)
        ApiErrorCodes.NOT_FOUND -> UiText.Res(R.string.error_not_found)
        ApiErrorCodes.INSUFFICIENT_HISTORY -> UiText.Res(R.string.error_insufficient_history)
        ApiErrorCodes.TOO_MANY_GAPS -> UiText.Res(R.string.error_too_many_gaps)
        ApiErrorCodes.STALE_HISTORY -> UiText.Res(R.string.error_stale_history)
        ApiErrorCodes.AI_NOT_AVAILABLE -> UiText.Res(R.string.error_ai_not_available)
        ApiErrorCodes.AS_OF_OUT_OF_RANGE -> UiText.Res(R.string.error_as_of_out_of_range)
        ApiErrorCodes.NO_MODEL_FOR_AS_OF -> UiText.Res(R.string.error_no_model_for_as_of)
        ApiErrorCodes.HORIZON_NOT_SUPPORTED -> UiText.Res(R.string.error_horizon_not_supported)
        else -> if (message.isNotBlank()) UiText.Raw(message) else UiText.Res(R.string.error_unknown)
    }
    is IOException -> UiText.Res(R.string.error_network)
    else -> UiText.Res(R.string.error_unknown)
}
