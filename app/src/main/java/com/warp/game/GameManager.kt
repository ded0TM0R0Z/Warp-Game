package com.warp.game

import java.io.File
import java.util.UUID

class GameManager(
    private val storage: PlatformStorage,
    private val database: WarpDatabase
) {
    fun getInstalledGames(): List<InstalledGame> {
        return database.getInstalledGames()
    }

    fun getInstalledGame(gameId: String): InstalledGame? {
        return database.getInstalledGame(gameId)
    }

    fun removeGame(gameId: String): Boolean {
        val installedGame = database.getInstalledGame(gameId) ?: return false
        val gameDirectory = storage.gameDirectory(installedGame.gameId)
        val removalDirectory = File(
            storage.gamesDirectory,
            ".removing-" + UUID.randomUUID()
        )
        var directoryMoved = false

        if (gameDirectory.exists()) {
            if (!gameDirectory.renameTo(removalDirectory)) {
                throw GamePackageException("Could not stage game removal")
            }
            directoryMoved = true
        }

        try {
            if (!database.removeInstalledGameRecord(installedGame.gameId)) {
                throw GamePackageException("Could not remove game registration")
            }
        } catch (error: Exception) {
            if (directoryMoved && removalDirectory.exists()) {
                if (!removalDirectory.renameTo(gameDirectory)) {
                    WarpLogger.error("GameManager", "Could not restore game after failed removal")
                }
            }
            throw error
        }

        if (directoryMoved && removalDirectory.exists() && !removalDirectory.deleteRecursively()) {
            WarpLogger.warning("GameManager", "Removed game files could not be fully deleted")
        }

        // Save data is intentionally kept in the separate saves directory.
        return true
    }
}
