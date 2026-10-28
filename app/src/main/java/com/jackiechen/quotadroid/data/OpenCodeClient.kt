package com.jackiechen.quotadroid.data

import com.jackiechen.quotadroid.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * HTTP client for the OpenCode Go usage endpoint.
 *
 * The API key is only ever sent as the bearer token; it is never logged, stored
 * in a snapshot, or included in an error message.
 */
class OpenCodeClient(
    private val http: OkHttpClient = defaultHttpClient(),
) {
    suspend fun fetchUsage(apiKey: String, workspaceId: String?): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(USAGE_ENDPOINT)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .apply {
                workspaceId?.trim()?.takeIf { it.isNotEmpty() }?.let { header("x-opencode-org-id", it) }
            }
            .get()
            .build()

        try {
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    body
                } else {
                    throw when (response.code) {
                        401 -> QuotaError.Unauthorized
                        403 -> QuotaError.Forbidden
                        429 -> QuotaError.Throttled
                        else -> QuotaError.Http(response.code, body.take(500))
                    }
                }
            }
        } catch (error: IOException) {
            throw QuotaError.Network(error.message ?: "connection failed")
        }
    }

    companion object {
        const val USAGE_ENDPOINT = "https://opencode.ai/zen/go/v1/usage"
        val USER_AGENT = "QuotaDroid/${BuildConfig.VERSION_NAME} (Android)"

        private fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
