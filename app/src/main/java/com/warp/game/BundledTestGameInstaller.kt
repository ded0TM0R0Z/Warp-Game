package com.warp.game

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BundledTestGameInstaller(context: Context) {
    private val appContext = context.applicationContext

    fun install(): InstalledGame {
        val packageBytes = createPackageArchive()
        val checksum = MessageDigest.getInstance("SHA-256")
            .digest(packageBytes)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

        val storage = PlatformStorage(appContext)
        val database = WarpDatabase(appContext)
        return try {
            GameInstaller(storage, database).installPackage(
                ByteArrayInputStream(packageBytes),
                checksum
            )
        } finally {
            database.close()
        }
    }

    private fun createPackageArchive(): ByteArray {
        val files = listOf(
            "manifest.json" to "runtime_test_package/manifest.json",
            "index.html" to "runtime_test/index.html",
            "styles.css" to "runtime_test/styles.css",
            "game.js" to "runtime_test/game.js"
        )
        val output = ByteArrayOutputStream()

        ZipOutputStream(output).use { zip ->
            files.forEach { (packagePath, assetPath) ->
                zip.putNextEntry(ZipEntry(packagePath))
                if (packagePath == "index.html") {
                    val html = appContext.assets.open(assetPath).bufferedReader().use { it.readText() }
                    val sdkScript = "<script src=\"/sdk/warp-game-sdk.js\"></script>\\n    "
                    val packagedHtml = html.replace(
                        "<script src=\"game.js\"></script>",
                        sdkScript + "<script src=\"game.js\"></script>"
                    )
                    zip.write(packagedHtml.toByteArray(Charsets.UTF_8))
                } else {
                    appContext.assets.open(assetPath).use { input ->
                        input.copyTo(zip)
                    }
                }
                zip.closeEntry()
            }
        }

        return output.toByteArray()
    }
}
