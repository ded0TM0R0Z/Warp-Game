package com.warp.game

import org.json.JSONException
import org.json.JSONObject

data class GamePackageManifest(
    val gameId: String,
    val name: String,
    val version: String,
    val engine: String,
    val engineVersion: String,
    val gameType: String,
    val entryPoint: String,
    val orientation: String,
    val offlineSupported: Boolean,
    val minimumPlatformVersion: String
)

object GamePackageManifestParser {
    const val PLATFORM_VERSION = "0.2.0"

    private val gameIdPattern = Regex("[A-Za-z0-9][A-Za-z0-9._-]{2,127}")
    private val platformVersionPattern = Regex("\\d+\\.\\d+\\.\\d+")

    fun isPlatformVersionCompatible(minimumPlatformVersion: String): Boolean {
        if (!platformVersionPattern.matches(minimumPlatformVersion)) {
            throw GamePackageException("Invalid minimum_platform_version")
        }
        return compareVersions(minimumPlatformVersion, PLATFORM_VERSION) <= 0
    }

    fun parse(jsonText: String): GamePackageManifest {
        val json = try {
            JSONObject(jsonText)
        } catch (error: JSONException) {
            throw GamePackageException("manifest.json is not valid JSON", error)
        }

        val gameId = requiredString(json, "game_id")
        if (!gameIdPattern.matches(gameId)) {
            throw GamePackageException("Invalid game_id")
        }

        val gameType = requiredString(json, "game_type")
        if (gameType != "2d" && gameType != "3d") {
            throw GamePackageException("game_type must be 2d or 3d")
        }

        val entryPoint = requiredString(json, "entry_point")
        validateRelativePath(entryPoint)

        val offlineSupported = json.opt("offline_supported")
        if (offlineSupported !is Boolean) {
            throw GamePackageException("offline_supported must be a boolean")
        }
        if (!offlineSupported) {
            throw GamePackageException("Game package must support offline startup")
        }

        val minimumPlatformVersion = requiredString(json, "minimum_platform_version")
        if (!isPlatformVersionCompatible(minimumPlatformVersion)) {
            throw GamePackageException("Game requires a newer WARP GAME version")
        }

        return GamePackageManifest(
            gameId = gameId,
            name = requiredString(json, "name").takeIf { it.length <= 80 }
                ?: throw GamePackageException("Game name is too long"),
            version = requiredString(json, "version"),
            engine = requiredString(json, "engine"),
            engineVersion = requiredString(json, "engine_version"),
            gameType = gameType,
            entryPoint = entryPoint,
            orientation = requiredString(json, "orientation"),
            offlineSupported = offlineSupported,
            minimumPlatformVersion = minimumPlatformVersion
        )
    }

    fun validateRelativePath(path: String) {
        if (
            path.isBlank() ||
            path.startsWith("/") ||
            path.startsWith("\\\\") ||
            path.contains('\\') ||
            path.contains(':') ||
            path.any { it.isISOControl() }
        ) {
            throw GamePackageException("Invalid package path")
        }

        val segments = path.split("/")
        if (segments.any { it.isBlank() || it == "." || it == ".." }) {
            throw GamePackageException("Package path contains an unsafe segment")
        }
        if (segments.any { it.length > 240 } || path.length > 1024) {
            throw GamePackageException("Package path is too long")
        }
    }

    private fun requiredString(json: JSONObject, key: String): String {
        val value = json.opt(key)
        if (value !is String || value.isBlank()) {
            throw GamePackageException("Missing or invalid manifest field: $key")
        }
        return value
    }

    private fun compareVersions(left: String, right: String): Int {
        val leftParts = left.split(".").map {
            it.toLongOrNull() ?: throw GamePackageException("Invalid minimum_platform_version")
        }
        val rightParts = right.split(".").map {
            it.toLongOrNull() ?: throw GamePackageException("Invalid platform version")
        }
        for (index in 0..2) {
            val result = leftParts[index].compareTo(rightParts[index])
            if (result != 0) return result
        }
        return 0
    }
}

class GamePackageException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
