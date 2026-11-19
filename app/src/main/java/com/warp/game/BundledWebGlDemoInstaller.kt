package com.warp.game

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BundledWebGlDemoInstaller(context: Context) {
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
                checksum,
                expectedSizeBytes = packageBytes.size.toLong()
            )
        } finally {
            database.close()
        }
    }

    private fun createPackageArchive(): ByteArray {
        val files = listOf(
            "manifest.json" to "webgl3d_demo/manifest.json",
            "index.html" to "webgl3d_demo/index.html",
            "styles.css" to "webgl3d_demo/styles.css",
            "game.js" to "webgl3d_demo/game.js"
        )
        val output = ByteArrayOutputStream()

        ZipOutputStream(output).use { zip ->
            files.forEach { (packagePath, assetPath) ->
                zip.putNextEntry(ZipEntry(packagePath))
                appContext.assets.open(assetPath).use { input ->
                    input.copyTo(zip)
                }
                zip.closeEntry()
            }
        }

        return output.toByteArray()
    }
}
