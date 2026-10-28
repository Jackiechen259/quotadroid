package com.jackiechen.quotadroid.data

/**
 * Typed failures. A parser or transport failure is never reported as zero
 * usage, so the UI can always tell "no data" apart from "0% used".
 */
sealed class QuotaError(message: String) : Exception(message) {
    data object MissingApiKey : QuotaError("no API key configured")
    data object Unauthorized : QuotaError("the API key was rejected (401)")
    data object Forbidden : QuotaError("the workspace is not subscribed to OpenCode Go (403)")
    data object Throttled : QuotaError("the API is rate limiting this client (429)")
    data class Http(val code: Int, val body: String) : QuotaError("unexpected HTTP $code")
    data class Parse(val detail: String) : QuotaError("unreadable usage payload: $detail")
    data class Network(val detail: String) : QuotaError("network failure: $detail")
    data class Unknown(val detail: String) : QuotaError("unexpected failure: $detail")
}
