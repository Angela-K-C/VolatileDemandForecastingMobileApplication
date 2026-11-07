package com.example.volatiledemandforecasting.core.network

import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.UiText
import org.junit.Assert.assertEquals
import java.io.IOException
import org.junit.Test

class ErrorMessagesTest {

    private fun api(code: String, message: String = "", retryAfter: Int? = null) =
        ApiException(httpStatus = 400, code = code, message = message, retryAfterSeconds = retryAfter)

    @Test
    fun `known codes use string resources`() {
        assertEquals(UiText.Res(R.string.error_invalid_credentials), api(ApiErrorCodes.INVALID_CREDENTIALS).toUiText())
        assertEquals(UiText.Res(R.string.error_email_taken), api(ApiErrorCodes.EMAIL_TAKEN).toUiText())
        assertEquals(UiText.Res(R.string.error_invalid_reset_code), api(ApiErrorCodes.INVALID_RESET_CODE).toUiText())
    }

    @Test
    fun `rate limiting names the wait when the server gives one`() {
        assertEquals(
            UiText.Res(R.string.error_rate_limited, listOf(30)),
            api(ApiErrorCodes.RATE_LIMITED, retryAfter = 30).toUiText(),
        )
        assertEquals(UiText.Res(R.string.error_rate_limited_unknown), api(ApiErrorCodes.RATE_LIMITED).toUiText())
    }

    @Test
    fun `an unknown code falls back to the API's own message`() {
        assertEquals(UiText.Raw("Pick another SKU."), api("SKU_TAKEN", "Pick another SKU.").toUiText())
    }

    @Test
    fun `an unknown code with no message is the last resort`() {
        assertEquals(UiText.Res(R.string.error_unknown), api("SOMETHING_NEW").toUiText())
    }

    @Test
    fun `transport failures say the server can't be reached`() {
        assertEquals(UiText.Res(R.string.error_network), IOException("timeout").toUiText())
    }

    @Test
    fun `forecast problems get plain-language resources`() {
        assertEquals(UiText.Res(R.string.error_insufficient_history), api(ApiErrorCodes.INSUFFICIENT_HISTORY).toUiText())
        assertEquals(UiText.Res(R.string.error_too_many_gaps), api(ApiErrorCodes.TOO_MANY_GAPS).toUiText())
        assertEquals(UiText.Res(R.string.error_ai_not_available), api(ApiErrorCodes.AI_NOT_AVAILABLE).toUiText())
        assertEquals(UiText.Res(R.string.error_as_of_out_of_range), api(ApiErrorCodes.AS_OF_OUT_OF_RANGE).toUiText())
        assertEquals(UiText.Res(R.string.error_no_model_for_as_of), api(ApiErrorCodes.NO_MODEL_FOR_AS_OF).toUiText())
        assertEquals(UiText.Res(R.string.error_stale_history), api(ApiErrorCodes.STALE_HISTORY).toUiText())
    }
}
