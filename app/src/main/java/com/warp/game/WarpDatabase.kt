package com.warp.game

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

class WarpDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    databasePath(context),
    null,
    DATABASE_VERSION
) {
    override fun onConfigure(database: SQLiteDatabase) {
        super.onConfigure(database)
        database.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE $TABLE_INSTALLED_GAMES (
                game_id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                version TEXT NOT NULL,
                engine TEXT NOT NULL,
                engine_version TEXT NOT NULL,
                game_type TEXT NOT NULL CHECK(game_type IN ('2d', '3d')),
                entry_point TEXT NOT NULL,
                orientation TEXT NOT NULL,
                offline_supported INTEGER NOT NULL CHECK(offline_supported IN (0, 1)),
                minimum_platform_version TEXT NOT NULL,
                install_directory TEXT NOT NULL,
                package_sha256 TEXT NOT NULL,
                installed_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX index_installed_games_name ON $TABLE_INSTALLED_GAMES(name COLLATE NOCASE)"
        )
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            database.execSQL(
                "ALTER TABLE $TABLE_INSTALLED_GAMES ADD COLUMN engine_version TEXT NOT NULL DEFAULT ''"
            )
            database.execSQL(
                "ALTER TABLE $TABLE_INSTALLED_GAMES ADD COLUMN orientation TEXT NOT NULL DEFAULT 'landscape'"
            )
            database.execSQL(
                "ALTER TABLE $TABLE_INSTALLED_GAMES ADD COLUMN offline_supported INTEGER NOT NULL DEFAULT 1"
            )
            database.execSQL(
                "ALTER TABLE $TABLE_INSTALLED_GAMES ADD COLUMN minimum_platform_version TEXT NOT NULL DEFAULT '0.1.0'"
            )
        }
    }

    fun saveInstalledGame(game: InstalledGame) {
        require(game.gameId.matches(GAME_ID_PATTERN)) { "Invalid game identifier" }
        require(game.name.isNotBlank()) { "Game name cannot be empty" }
        require(game.gameType == "2d" || game.gameType == "3d") { "Unsupported game type" }

        val values = ContentValues().apply {
            put(COLUMN_GAME_ID, game.gameId)
            put(COLUMN_NAME, game.name)
            put(COLUMN_VERSION, game.version)
            put(COLUMN_ENGINE, game.engine)
            put(COLUMN_ENGINE_VERSION, game.engineVersion)
            put(COLUMN_GAME_TYPE, game.gameType)
            put(COLUMN_ENTRY_POINT, game.entryPoint)
            put(COLUMN_ORIENTATION, game.orientation)
            put(COLUMN_OFFLINE_SUPPORTED, if (game.offlineSupported) 1 else 0)
            put(COLUMN_MINIMUM_PLATFORM_VERSION, game.minimumPlatformVersion)
            put(COLUMN_INSTALL_DIRECTORY, game.installDirectory)
            put(COLUMN_PACKAGE_SHA256, game.packageSha256)
            put(COLUMN_INSTALLED_AT, game.installedAt)
            put(COLUMN_UPDATED_AT, game.updatedAt)
        }

        val database = writableDatabase
        database.beginTransaction()
        try {
            database.insertWithOnConflict(
                TABLE_INSTALLED_GAMES,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            ).also { rowId ->
                check(rowId != -1L) { "Could not save installed game" }
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    fun getInstalledGames(): List<InstalledGame> {
        val games = mutableListOf<InstalledGame>()
        readableDatabase.query(
            TABLE_INSTALLED_GAMES,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_NAME COLLATE NOCASE ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                games.add(cursor.toInstalledGame())
            }
        }
        return games
    }

    fun getInstalledGame(gameId: String): InstalledGame? {
        readableDatabase.query(
            TABLE_INSTALLED_GAMES,
            null,
            "$COLUMN_GAME_ID = ?",
            arrayOf(gameId),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toInstalledGame() else null
        }
    }

    fun removeInstalledGameRecord(gameId: String): Boolean {
        return writableDatabase.delete(
            TABLE_INSTALLED_GAMES,
            "$COLUMN_GAME_ID = ?",
            arrayOf(gameId)
        ) > 0
    }

    private fun Cursor.toInstalledGame(): InstalledGame {
        return InstalledGame(
            gameId = getString(getColumnIndexOrThrow(COLUMN_GAME_ID)),
            name = getString(getColumnIndexOrThrow(COLUMN_NAME)),
            version = getString(getColumnIndexOrThrow(COLUMN_VERSION)),
            engine = getString(getColumnIndexOrThrow(COLUMN_ENGINE)),
            engineVersion = getString(getColumnIndexOrThrow(COLUMN_ENGINE_VERSION)),
            gameType = getString(getColumnIndexOrThrow(COLUMN_GAME_TYPE)),
            entryPoint = getString(getColumnIndexOrThrow(COLUMN_ENTRY_POINT)),
            orientation = getString(getColumnIndexOrThrow(COLUMN_ORIENTATION)),
            offlineSupported = getInt(getColumnIndexOrThrow(COLUMN_OFFLINE_SUPPORTED)) == 1,
            minimumPlatformVersion = getString(getColumnIndexOrThrow(COLUMN_MINIMUM_PLATFORM_VERSION)),
            installDirectory = getString(getColumnIndexOrThrow(COLUMN_INSTALL_DIRECTORY)),
            packageSha256 = getString(getColumnIndexOrThrow(COLUMN_PACKAGE_SHA256)),
            installedAt = getLong(getColumnIndexOrThrow(COLUMN_INSTALLED_AT)),
            updatedAt = getLong(getColumnIndexOrThrow(COLUMN_UPDATED_AT))
        )
    }

    private companion object {
        const val DATABASE_NAME = "warp_game.db"
        const val DATABASE_VERSION = 2
        const val TABLE_INSTALLED_GAMES = "installed_games"

        const val COLUMN_GAME_ID = "game_id"
        const val COLUMN_NAME = "name"
        const val COLUMN_VERSION = "version"
        const val COLUMN_ENGINE = "engine"
        const val COLUMN_ENGINE_VERSION = "engine_version"
        const val COLUMN_GAME_TYPE = "game_type"
        const val COLUMN_ENTRY_POINT = "entry_point"
        const val COLUMN_ORIENTATION = "orientation"
        const val COLUMN_OFFLINE_SUPPORTED = "offline_supported"
        const val COLUMN_MINIMUM_PLATFORM_VERSION = "minimum_platform_version"
        const val COLUMN_INSTALL_DIRECTORY = "install_directory"
        const val COLUMN_PACKAGE_SHA256 = "package_sha256"
        const val COLUMN_INSTALLED_AT = "installed_at"
        const val COLUMN_UPDATED_AT = "updated_at"

        val GAME_ID_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9._-]{2,127}")

        private fun databasePath(context: Context): String {
            val directory = File(context.applicationContext.filesDir, "database")
            if (!directory.exists() && !directory.mkdirs()) {
                throw IllegalStateException("Cannot create database directory")
            }
            return File(directory, DATABASE_NAME).absolutePath
        }
    }
}
