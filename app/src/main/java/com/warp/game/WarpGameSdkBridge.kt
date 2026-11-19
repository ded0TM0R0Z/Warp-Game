package com.warp.game

import android.app.Activity
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.webkit.JavascriptInterface
import org.json.JSONException
import org.json.JSONObject
import java.io.File

class WarpGameSdkBridge(
    private val activity: Activity,
    private val game: InstalledGame,
    private val storage: PlatformStorage
) {
    private val saveRoot: File
        get() = storage.saveDirectory(game.gameId).canonicalFile

    @JavascriptInterface
    fun getGameInfo(): String {
        return JSONObject()
            .put("gameId", game.gameId)
            .put("name", game.name)
            .put("version", game.version)
            .put("engine", game.engine)
            .put("engineVersion", game.engineVersion)
            .put("gameType", game.gameType)
            .put("platformVersion", GamePackageManifestParser.PLATFORM_VERSION)
            .toString()
    }

    @JavascriptInterface
    @Synchronized
    fun saveData(key: String, value: String): Boolean {
        if (!isValidSaveKey(key) || key == SETTINGS_FILE_KEY) return false
        return try {
            val bytes = value.toByteArray(Charsets.UTF_8)
            if (bytes.size > MAX_SAVE_ITEM_BYTES) return false

            val root = ensureSaveRoot()
            val target = resolveSaveFile(root, key + ".dat")
            val currentTotal = root.listFiles()
                ?.filter { it.isFile && !it.name.endsWith(".tmp") && !it.name.endsWith(".bak") }
                ?.sumOf { it.length() } ?: 0L
            val previousSize = if (target.isFile) target.length() else 0L
            if (currentTotal - previousSize + bytes.size > MAX_SAVE_TOTAL_BYTES) {
                return false
            }
            writeAtomically(target, bytes)
        } catch (error: Exception) {
            WarpLogger.warning("GameSDK", "Could not save game data", error)
            false
        }
    }

    @JavascriptInterface
    @Synchronized
    fun loadData(key: String): String? {
        if (!isValidSaveKey(key) || key == SETTINGS_FILE_KEY) return null
        return try {
            val root = ensureSaveRoot()
            val file = resolveSaveFile(root, key + ".dat")
            if (!file.isFile || file.length() > MAX_SAVE_ITEM_BYTES) {
                null
            } else {
                file.readText(Charsets.UTF_8)
            }
        } catch (error: Exception) {
            WarpLogger.warning("GameSDK", "Could not load game data", error)
            null
        }
    }

    @JavascriptInterface
    @Synchronized
    fun getSettings(): String {
        return try {
            val file = resolveSaveFile(ensureSaveRoot(), SETTINGS_FILE_NAME)
            if (!file.isFile || file.length() > MAX_SETTINGS_BYTES) {
                "{}"
            } else {
                val text = file.readText(Charsets.UTF_8)
                JSONObject(text)
                text
            }
        } catch (error: Exception) {
            WarpLogger.warning("GameSDK", "Could not read game settings", error)
            "{}"
        }
    }

    @JavascriptInterface
    @Synchronized
    fun setSettings(settingsJson: String): Boolean {
        if (settingsJson.toByteArray(Charsets.UTF_8).size > MAX_SETTINGS_BYTES) return false
        return try {
            JSONObject(settingsJson)
            val root = ensureSaveRoot()
            val file = resolveSaveFile(root, SETTINGS_FILE_NAME)
            val bytes = settingsJson.toByteArray(Charsets.UTF_8)
            val currentTotal = root.listFiles()
                ?.filter { it.isFile && !it.name.endsWith(".tmp") && !it.name.endsWith(".bak") }
                ?.sumOf { it.length() } ?: 0L
            val previousSize = if (file.isFile) file.length() else 0L
            if (currentTotal - previousSize + bytes.size > MAX_SAVE_TOTAL_BYTES) {
                return false
            }
            writeAtomically(file, bytes)
        } catch (error: JSONException) {
            false
        } catch (error: Exception) {
            WarpLogger.warning("GameSDK", "Could not save game settings", error)
            false
        }
    }

    @JavascriptInterface
    fun vibrate(durationMs: Long): Boolean {
        if (durationMs !in 1L..MAX_VIBRATION_MS) return false
        return try {
            val vibrator = activity.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                ?: return false
            if (!vibrator.hasVibrator()) return false
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    durationMs,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
            true
        } catch (error: Exception) {
            WarpLogger.warning("GameSDK", "Vibration request failed", error)
            false
        }
    }

    @JavascriptInterface
    fun setFullscreen(enabled: Boolean) {
        activity.runOnUiThread {
            val decorView = activity.window.decorView
            decorView.systemUiVisibility = if (enabled) {
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            } else {
                0
            }
        }
    }

    @JavascriptInterface
    fun exitGame() {
        activity.runOnUiThread { activity.finish() }
    }

    private fun ensureSaveRoot(): File {
        val root = saveRoot
        if (!root.exists() && !root.mkdirs()) {
            throw IllegalStateException("Cannot create game save directory")
        }
        if (!root.isDirectory) {
            throw IllegalStateException("Game save path is not a directory")
        }
        return root
    }

    private fun resolveSaveFile(root: File, fileName: String): File {
        val canonicalRoot = root.canonicalFile
        val target = File(canonicalRoot, fileName).canonicalFile
        require(target.path.startsWith(canonicalRoot.path + File.separator)) {
            "Save file escapes game save directory"
        }
        return target
    }

    private fun writeAtomically(target: File, bytes: ByteArray): Boolean {
        val temporary = File(target.parentFile, target.name + ".tmp")
        val backup = File(target.parentFile, target.name + ".bak")
        try {
            if (backup.exists()) {
                if (!target.exists()) backup.renameTo(target) else backup.delete()
            }
            FileOutputStreamCompat.write(temporary, bytes)
            if (target.exists() && !target.renameTo(backup)) return false
            if (!temporary.renameTo(target)) {
                if (backup.exists()) backup.renameTo(target)
                return false
            }
            if (backup.exists()) backup.delete()
            return true
        } catch (error: Exception) {
            temporary.delete()
            if (!target.exists() && backup.exists()) backup.renameTo(target)
            throw error
        }
    }

    private fun isValidSaveKey(key: String): Boolean {
        return SAVE_KEY_PATTERN.matches(key)
    }

    private companion object {
        const val SETTINGS_FILE_KEY = "game_settings"
        const val SETTINGS_FILE_NAME = "game_settings.json"
        const val MAX_SAVE_ITEM_BYTES = 1024 * 1024
        const val MAX_SAVE_TOTAL_BYTES = 10L * 1024L * 1024L
        const val MAX_SETTINGS_BYTES = 64 * 1024
        const val MAX_VIBRATION_MS = 5000L

        val SAVE_KEY_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")
    }
}

private object FileOutputStreamCompat {
    fun write(file: File, bytes: ByteArray) {
        java.io.FileOutputStream(file).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
    }
}
