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
}
