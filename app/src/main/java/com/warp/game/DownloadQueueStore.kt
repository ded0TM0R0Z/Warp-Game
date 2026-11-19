package com.warp.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class DownloadQueueEntry(
    val game: CatalogGame,
    val status: String,
    val progressPercent: Int = 0,
    val message: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

class DownloadQueueStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun getEntries(): List<DownloadQueueEntry> = readEntries()

    @Synchronized
    fun enqueue(game: CatalogGame): Boolean {
        val entries = readEntries().toMutableList()
        val existingIndex = entries.indexOfFirst { it.game.gameId == game.gameId }
        if (existingIndex >= 0) {
            val existing = entries[existingIndex]
            if (existing.status == STATUS_PENDING || existing.status == STATUS_DOWNLOADING) {
                return false
            }
            entries.removeAt(existingIndex)
        }
        entries.add(
            DownloadQueueEntry(
                game = game,
                status = STATUS_PENDING,
                message = "Ожидает загрузки"
            )
        )
        writeEntries(entries)
        return true
    }

    @Synchronized
    fun getNextPending(): DownloadQueueEntry? =
        readEntries().firstOrNull { it.status == STATUS_PENDING }

    @Synchronized
    fun update(
        gameId: String,
        status: String,
        progressPercent: Int = 0,
        message: String = ""
    ) {
        require(status in ALL_STATUSES) { "Unsupported download status" }
        val entries = readEntries().toMutableList()
        val index = entries.indexOfFirst { it.game.gameId == gameId }
        if (index < 0) return
        entries[index] = entries[index].copy(
            status = status,
            progressPercent = progressPercent.coerceIn(0, 100),
            message = message.take(MAX_MESSAGE_LENGTH),
            updatedAt = System.currentTimeMillis()
        )
        writeEntries(entries)
    }

    @Synchronized
    fun remove(gameId: String): Boolean {
        val entries = readEntries().toMutableList()
        val removed = entries.removeAll { it.game.gameId == gameId && it.status != STATUS_DOWNLOADING }
        if (removed) writeEntries(entries)
        return removed
    }

    @Synchronized
    fun retry(gameId: String): Boolean {
        val entries = readEntries().toMutableList()
        val index = entries.indexOfFirst { it.game.gameId == gameId }
        if (index < 0 || entries[index].status != STATUS_FAILED) return false
        entries[index] = entries[index].copy(
            status = STATUS_PENDING,
            progressPercent = 0,
            message = "Повторная загрузка",
            updatedAt = System.currentTimeMillis()
        )
        writeEntries(entries)
        return true
    }

    @Synchronized
    fun recoverInterruptedDownloads() {
        val entries = readEntries().map { entry ->
            if (entry.status == STATUS_DOWNLOADING) {
                entry.copy(
                    status = STATUS_FAILED,
                    progressPercent = 0,
                    message = "Загрузка прервана закрытием приложения",
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                entry
            }
        }
        writeEntries(entries)
    }

    private fun readEntries(): List<DownloadQueueEntry> {
        val raw = preferences.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val gameObject = item.optJSONObject("game") ?: continue
                    val game = parseGame(gameObject) ?: continue
                    val status = item.optString("status")
                    if (status !in ALL_STATUSES) continue
                    add(
                        DownloadQueueEntry(
                            game = game,
                            status = status,
                            progressPercent = item.optInt("progress_percent").coerceIn(0, 100),
                            message = item.optString("message").take(MAX_MESSAGE_LENGTH),
                            updatedAt = item.optLong("updated_at")
                        )
                    )
                }
            }
        } catch (error: Exception) {
            WarpLogger.error("DownloadQueue", "Could not read saved download queue", error)
            emptyList()
        }
    }

    private fun writeEntries(entries: List<DownloadQueueEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            val game = entry.game
            val gameObject = JSONObject().apply {
                put("game_id", game.gameId)
                put("name", game.name)
                put("description", game.description)
                put("version", game.version)
                put("engine", game.engine)
                put("engine_version", game.engineVersion)
                put("game_type", game.gameType)
                put("orientation", game.orientation)
                put("package_url", game.packageUrl)
                put("package_sha256", game.packageSha256)
                put("package_size_bytes", game.packageSizeBytes)
                put("minimum_platform_version", game.minimumPlatformVersion)
                put("offline_supported", game.offlineSupported)
            }
            array.put(JSONObject().apply {
                put("game", gameObject)
                put("status", entry.status)
                put("progress_percent", entry.progressPercent)
                put("message", entry.message)
                put("updated_at", entry.updatedAt)
            })
        }
        check(preferences.edit().putString(KEY_ENTRIES, array.toString()).commit()) {
            "Could not persist download queue"
        }
    }

    private fun parseGame(item: JSONObject): CatalogGame? {
        return try {
            CatalogGame(
                gameId = item.getString("game_id"),
                name = item.getString("name"),
                description = item.getString("description"),
                version = item.getString("version"),
                engine = item.getString("engine"),
                engineVersion = item.getString("engine_version"),
                gameType = item.getString("game_type"),
                orientation = item.getString("orientation"),
                packageUrl = item.getString("package_url"),
                packageSha256 = item.getString("package_sha256"),
                packageSizeBytes = item.getLong("package_size_bytes"),
                minimumPlatformVersion = item.getString("minimum_platform_version"),
                offlineSupported = item.getBoolean("offline_supported")
            )
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_DOWNLOADING = "downloading"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_FAILED = "failed"
        const val STATUS_CANCELLED = "cancelled"

        private const val PREFERENCES_NAME = "warp_download_queue"
        private const val KEY_ENTRIES = "entries"
        private const val MAX_MESSAGE_LENGTH = 180
        private val ALL_STATUSES = setOf(
            STATUS_PENDING,
            STATUS_DOWNLOADING,
            STATUS_COMPLETED,
            STATUS_FAILED,
            STATUS_CANCELLED
        )
    }
}
