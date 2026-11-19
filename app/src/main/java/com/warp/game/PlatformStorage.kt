package com.warp.game

import android.content.Context
import java.io.File

class PlatformStorage(context: Context) {
    private val appContext = context.applicationContext
    private val rootDirectory = appContext.filesDir

    val gamesDirectory: File = File(rootDirectory, "games")
    val savesDirectory: File = File(rootDirectory, "saves")
    val cacheDirectory: File = File(rootDirectory, "cache")
    val databaseDirectory: File = File(rootDirectory, "database")

    fun initialize() {
        listOf(
            gamesDirectory,
            savesDirectory,
            cacheDirectory,
            databaseDirectory
        ).forEach { directory ->
            if (!directory.exists() && !directory.mkdirs()) {
                throw IllegalStateException("Cannot create platform storage directory")
            }
            if (!directory.isDirectory) {
                throw IllegalStateException("Platform storage path is not a directory")
            }
        }
    }

    fun gameDirectory(gameId: String): File {
        require(isValidGameId(gameId)) { "Invalid game identifier" }
        val directory = File(gamesDirectory, gameId)
        val canonicalRoot = gamesDirectory.canonicalFile
        val canonicalDirectory = directory.canonicalFile
        require(
            canonicalDirectory.parentFile == canonicalRoot ||
                canonicalDirectory.parentFile?.canonicalFile == canonicalRoot
        ) { "Game directory escapes storage root" }
        return directory
    }

    fun saveDirectory(gameId: String): File {
        require(isValidGameId(gameId)) { "Invalid game identifier" }
        val directory = File(savesDirectory, gameId)
        val canonicalRoot = savesDirectory.canonicalFile
        val canonicalDirectory = directory.canonicalFile
        require(
            canonicalDirectory.parentFile == canonicalRoot ||
                canonicalDirectory.parentFile?.canonicalFile == canonicalRoot
        ) { "Save directory escapes storage root" }
        return directory
    }

    private fun isValidGameId(gameId: String): Boolean {
        return GAME_ID_PATTERN.matches(gameId)
    }

    private companion object {
        val GAME_ID_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9._-]{2,127}")
    }
}
