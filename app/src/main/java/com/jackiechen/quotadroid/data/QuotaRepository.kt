package com.jackiechen.quotadroid.data

/** Fetches usage, parses it, and caches the last successful reading. */
class QuotaRepository(
    private val client: OpenCodeClient,
    private val store: SettingsStore,
) {
    suspend fun refresh(): Result<UsageSnapshot> {
        val apiKey = store.apiKey()
        if (apiKey.isBlank()) return Result.failure(QuotaError.MissingApiKey)

        return try {
            val payload = client.fetchUsage(apiKey, store.currentSettings().workspaceId)
            val snapshot = UsageParser.parse(payload)
            store.saveSnapshot(snapshot)
            Result.success(snapshot)
        } catch (error: QuotaError) {
            Result.failure(error)
        } catch (error: Exception) {
            Result.failure(QuotaError.Unknown(error.message ?: error.javaClass.simpleName))
        }
    }

    suspend fun cached(): UsageSnapshot? = store.loadSnapshot()
}
