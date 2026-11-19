package com.warp.game

import android.content.Context
import java.io.FilterInputStream
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class GameDownloadManager(context: Context) {
    private val appContext = context.applicationContext
    private val storage = PlatformStorage(appContext)
    private val database = WarpDatabase(appContext)

    fun downloadAndInstall(
        game: CatalogGame,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): InstalledGame {
        storage.initialize()
        try {
            var currentUrl = URL(game.packageUrl)
            var redirectCount = 0

            while (true) {
                validateDownloadUrl(currentUrl)
                val connection = currentUrl.openConnection() as HttpsURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/zip")
                connection.setRequestProperty("User-Agent", "WarpGame-Android")

                try {
                    val responseCode = connection.responseCode
                    if (responseCode in REDIRECT_MIN..REDIRECT_MAX) {
                        if (redirectCount >= MAX_REDIRECTS) {
                            throw IllegalStateException("Too many package download redirects")
                        }
                        val location = connection.getHeaderField("Location")
                            ?: throw IllegalStateException("Package redirect has no destination")
                        currentUrl = URL(currentUrl, location)
                        redirectCount += 1
                        continue
                    }

                    if (responseCode != HttpsURLConnection.HTTP_OK) {
                        throw IllegalStateException("Package server returned HTTP $responseCode")
                    }

                    val contentLength = connection.contentLengthLong
                    if (contentLength > MAX_PACKAGE_BYTES) {
                        throw IllegalStateException("Downloaded package exceeds the size limit")
                    }
                    if (contentLength >= 0L && contentLength != game.packageSizeBytes) {
                        throw IllegalStateException("Downloaded package size does not match catalog metadata")
                    }

                    val progressInput = ProgressInputStream(
                        connection.inputStream,
                        game.packageSizeBytes,
                        onProgress
                    )
                    progressInput.use { input ->
                        return GameInstaller(storage, database).installPackage(
                            packageStream = input,
                            expectedSha256 = game.packageSha256,
                            expectedSizeBytes = game.packageSizeBytes,
                            expectedMetadata = ExpectedGameMetadata(
                                gameId = game.gameId,
                                name = game.name,
                                version = game.version,
                                engine = game.engine,
                                engineVersion = game.engineVersion,
                                gameType = game.gameType,
                                orientation = game.orientation,
                                minimumPlatformVersion = game.minimumPlatformVersion,
                                offlineSupported = game.offlineSupported
                            )
                        )
                    }
                } finally {
                    connection.disconnect()
                }
            }
            throw IllegalStateException("Package download did not complete")
        } finally {
            database.close()
        }
    }

    private fun validateDownloadUrl(url: URL) {
        if (
            url.protocol != "https" ||
            url.host.lowercase() !in ALLOWED_HOSTS ||
            url.userInfo != null ||
            (url.port != -1 && url.port != 443)
        ) {
            throw IllegalArgumentException("Package download URL is not trusted")
        }
    }

    private class ProgressInputStream(
        input: InputStream,
        private val expectedBytes: Long,
        private val onProgress: (Long, Long) -> Unit
    ) : FilterInputStream(input) {
        private var downloadedBytes = 0L

        override fun read(): Int {
            val value = super.read()
            if (value >= 0) reportProgress(1)
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val count = super.read(buffer, offset, length)
            if (count > 0) reportProgress(count)
            return count
        }

        private fun reportProgress(count: Int) {
            downloadedBytes += count
            if (downloadedBytes > expectedBytes || downloadedBytes > MAX_PACKAGE_BYTES) {
                throw IllegalStateException("Downloaded package exceeds the expected size")
            }
            onProgress(downloadedBytes, expectedBytes)
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15000
        const val READ_TIMEOUT_MS = 30000
        const val MAX_REDIRECTS = 5
        const val REDIRECT_MIN = 300
        const val REDIRECT_MAX = 399
        const val MAX_PACKAGE_BYTES = 256L * 1024L * 1024L

        val ALLOWED_HOSTS = setOf(
            "github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com"
        )
    }
}
