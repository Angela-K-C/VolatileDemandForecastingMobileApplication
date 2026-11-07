package com.example.volatiledemandforecasting.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

/** Every backend error has this shape (api.md, "Errors"). */
@Serializable
data class ErrorEnvelope(val error: ErrorBody)

@Serializable
data class ErrorBody(
    val code: String,
    val message: String = "",
    val details: JsonObject? = null,
    val requestId: String? = null,
)

/** A well-formed error from the backend. Transport failures stay plain `IOException`s. */
class ApiException(
    val httpStatus: Int,
    val code: String,
    override val message: String,
    val details: JsonObject? = null,
    val requestId: String? = null,
    /** From the `Retry-After` header or `details.retryAfterSeconds`, for `RATE_LIMITED`. */
    val retryAfterSeconds: Int? = null,
) : Exception(message)

/**
 * The request fields a `422 INVALID_REQUEST` names, e.g. `email`: the last part of each
 * `details.errors[].loc` (api.md). Empty for any other error.
 */
fun ApiException.rejectedFields(): Set<String> {
    val errors = details?.get("errors") as? JsonArray ?: return emptySet()
    return errors.mapNotNull { error ->
        ((error as? JsonObject)?.get("loc") as? JsonArray)?.lastOrNull()?.jsonPrimitive?.content
    }.toSet()
}

object ApiErrorCodes {
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
    const val INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN"
    const val EMAIL_TAKEN = "EMAIL_TAKEN"
    const val REGISTRATION_CLOSED = "REGISTRATION_CLOSED"
    const val INVALID_RESET_CODE = "INVALID_RESET_CODE"
    const val RATE_LIMITED = "RATE_LIMITED"
}

internal fun parseErrorBody(json: Json, body: String?): ErrorBody? =
    body?.takeIf { it.isNotBlank() }
        ?.let { runCatching { json.decodeFromString<ErrorEnvelope>(it).error }.getOrNull() }

internal fun HttpException.toApiException(json: Json): ApiException {
    val response = response()
    val parsed = parseErrorBody(json, response?.errorBody()?.string())
    val retryAfter = response?.headers()?.get("Retry-After")?.toIntOrNull()
        ?: parsed?.details?.get("retryAfterSeconds")?.jsonPrimitive?.intOrNull
    return ApiException(
        httpStatus = code(),
        code = parsed?.code ?: "HTTP_${code()}",
        message = parsed?.message.orEmpty(),
        details = parsed?.details,
        requestId = parsed?.requestId,
        retryAfterSeconds = retryAfter,
    )
}

/**
 * Captures a failure as a [Result] for a ViewModel to show, but never swallows coroutine
 * cancellation, which must keep propagating when a screen goes away.
 */
suspend fun <T> attempt(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

/** Runs a Retrofit call and turns HTTP failures into [ApiException]. Other exceptions pass through. */
suspend fun <T> apiCall(json: Json, block: suspend () -> T): T =
    try {
        block()
    } catch (e: HttpException) {
        throw e.toApiException(json)
    }
