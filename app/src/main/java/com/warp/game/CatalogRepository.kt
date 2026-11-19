package com.warp.game

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class CatalogRepository(context: Context) {
    private val appContext = context.applicationContext
    private val storage = PlatformStorage(appContext)
    private val cacheFile = File(storage.cacheDirectory, CATALOG_CACHE_FILE)

    fun loadCatalog(): CatalogSnapshot {
        storage.initialize()
        return try {
            val bytes = fetchCatalog()
            val games = parseCatalog(bytes)
            saveCache(bytes)
            CatalogSnapshot(games, false)
        } catch (networkOrParseError: Exception) {
            if (!cacheFile.isFile) throw networkOrParseError
            val cachedGames = FileInputStream(cacheFile).use { input ->
                parseCatalog(readLimited(input))
            }
            WarpLogger.warning("Catalog", "Using cached game catalog", networkOrParseError)
            CatalogSnapshot(cachedGames, true)
        }
    }

    private fun fetchCatalog(): ByteArray {
        val url = URL(CATALOG_URL)
        validateCatalogUrl(url)
        val connection = url.openConnection() as HttpsURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "WarpGame-Android")

        try {
            val responseCode = connection.responseCode
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw IllegalStateException("Catalog server returned HTTP $responseCode")
            }
            return connection.inputStream.use { input ->
                readLimited(input)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun validateCatalogUrl(url: URL) {
        if (
            url.protocol != "https" ||
            url.host.lowercase() != CATALOG_HOST ||
            url.userInfo != null ||
            (url.port != -1 && url.port != 443)
        ) {
            throw IllegalArgumentException("Catalog URL is not trusted")
        }
    }

    private fun parseCatalog(bytes: ByteArray): List<CatalogGame> {
        val root = try {
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (error: JSONException) {
            throw IllegalStateException("Catalog JSON is invalid", error)
        }

        if (root.optInt("catalog_version", -1) != CATALOG_VERSION) {
            throw IllegalStateException("Unsupported catalog version")
        }

        val gamesArray = root.optJSONArray("games")
            ?: throw IllegalStateException("Catalog games list is missing")
        val games = ArrayList<CatalogGame>(gamesArray.length())
        val seenIds = mutableSetOf<String>()

        for (index in 0 until gamesArray.length()) {
            val item = gamesArray.optJSONObject(index)
                ?: throw IllegalStateException("Catalog contains an invalid game entry")
            val game = parseGame(item)
            if (!seenIds.add(game.gameId)) {
                throw IllegalStateException("Catalog contains duplicate game identifiers")
            }
            games.add(game)
        }

        return games
    }

    private fun parseGame(item: JSONObject): CatalogGame {
        val gameId = requiredString(item, "game_id")
        if (!GAME_ID_PATTERN.matches(gameId)) {
            throw IllegalStateException("Catalog contains an invalid game identifier")
        }

        val gameType = requiredString(item, "game_type")
        if (gameType != "2d" && gameType != "3d") {
            throw IllegalStateException("Catalog contains an unsupported game type")
        }

        val offlineSupported = item.opt("offline_supported")
        if (offlineSupported !is Boolean || !offlineSupported) {
            throw IllegalStateException("Catalog game must support offline startup")
        }

        val packageUrl = requiredString(item, "package_url")
        validatePackageUrl(packageUrl)

        val packageSha256 = requiredString(item, "package_sha256")
        if (!SHA256_PATTERN.matches(packageSha256)) {
            throw IllegalStateException("Catalog contains an invalid package checksum")
        }

        val packageSizeBytes = item.optLong("package_size_bytes", -1L)
        if (packageSizeBytes <= 0L || packageSizeBytes > MAX_PACKAGE_BYTES) {
            throw IllegalStateException("Catalog package size is outside supported limits")
        }

        val minimumPlatformVersion = requiredString(item, "minimum_platform_version")
        if (!PLATFORM_VERSION_PATTERN.matches(minimumPlatformVersion)) {
            throw IllegalStateException("Catalog contains an invalid platform version")
        }
        if (!GamePackageManifestParser.isPlatformVersionCompatible(minimumPlatformVersion)) {
            throw IllegalStateException("Catalog game requires a newer platform version")
        }

        return CatalogGame(
            gameId = gameId,
            name = requiredString(item, "name").takeIf { it.length <= 80 }
                ?: throw IllegalStateException("Catalog game name is too long"),
            description = requiredString(item, "description").takeIf { it.length <= 500 }
                ?: throw IllegalStateException("Catalog game description is too long"),
            version = requiredString(item, "version"),
            engine = requiredString(item, "engine"),
            engineVersion = requiredString(item, "engine_version"),
            gameType = gameType,
            orientation = requiredString(item, "orientation"),
            packageUrl = packageUrl,
            packageSha256 = packageSha256.lowercase(),
            packageSizeBytes = packageSizeBytes,
            minimumPlatformVersion = minimumPlatformVersion,
            offlineSupported = offlineSupported
        )
    }

    private fun validatePackageUrl(value: String) {
        val url = try {
            URL(value)
        } catch (error: Exception) {
            throw IllegalStateException("Catalog package URL is invalid", error)
        }
        if (
            url.protocol != "https" ||
            url.host.lowercase() !in ALLOWED_PACKAGE_HOSTS ||
            url.userInfo != null ||
            (url.port != -1 && url.port != 443)
        ) {
            throw IllegalStateException("Catalog package URL is not trusted")
        }
    }

    private fun requiredString(item: JSONObject, key: String): String {
        val value = item.opt(key)
        if (value !is String || value.isBlank()) {
            throw IllegalStateException("Catalog field is missing or invalid: $key")
        }
        return value
    }

    private fun saveCache(bytes: ByteArray) {
        val temporaryFile = File(storage.cacheDirectory, CATALOG_CACHE_FILE + ".tmp")
        try {
            FileOutputStream(temporaryFile).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            if (cacheFile.exists() && !cacheFile.delete()) {
                throw IllegalStateException("Could not replace catalog cache")
            }
            if (!temporaryFile.renameTo(cacheFile)) {
                throw IllegalStateException("Could not save catalog cache")
            }
        } catch (error: Exception) {
            temporaryFile.delete()
            WarpLogger.warning("Catalog", "Could not cache the latest catalog", error)
        }
    }

    private fun readLimited(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var totalBytes = 0

        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            totalBytes += count
            if (totalBytes > MAX_CATALOG_BYTES) {
                throw IllegalStateException("Catalog response is too large")
            }
            output.write(buffer, 0, count)
        }

        return output.toByteArray()
    }

    private companion object {
        const val CATALOG_URL = "https://raw.githubusercontent.com/ded0TM0R0Z/Warp-Game/main/catalog/catalog.json"
        const val CATALOG_HOST = "raw.githubusercontent.com"
        const val CATALOG_CACHE_FILE = "catalog.json"
        const val CATALOG_VERSION = 1
        const val CONNECT_TIMEOUT_MS = 15000
        const val READ_TIMEOUT_MS = 20000
        const val BUFFER_SIZE = 8192
        const val MAX_CATALOG_BYTES = 2 * 1024 * 1024
        const val MAX_PACKAGE_BYTES = 256L * 1024L * 1024L

        val GAME_ID_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9._-]{2,127}")
        val SHA256_PATTERN = Regex("[A-Fa-f0-9]{64}")
        val PLATFORM_VERSION_PATTERN = Regex("\\d+\\.\\d+\\.\\d+")
        val ALLOWED_PACKAGE_HOSTS = setOf(
            "github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com"
        )
    }
}
