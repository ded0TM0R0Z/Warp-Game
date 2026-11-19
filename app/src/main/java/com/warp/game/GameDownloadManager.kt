package com.warp.game

import android.content.Context
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HttpsURLConnection

class DownloadCancellationToken {
    private val cancelled = AtomicBoolean(false)

    @Volatile
    private var activeConnection: HttpsURLConnection? = null

    @Volatile
    private var installing = false

    val isCancelled: Boolean
        get() = cancelled.get()

    val isInstalling: Boolean
        get() = installing

    @Synchronized
    fun cancel() {
        if (installing) return
        cancelled.set(true)
        activeConnection?.disconnect()
    }

    @Synchronized
    internal fun attach(connection: HttpsURLConnection) {
        throwIfCancelled()
        activeConnection = connection
        if (cancelled.get()) {
            connection.disconnect()
            throw DownloadCancelledException()
        }
    }

    @Synchronized
    internal fun detach(connection: HttpsURLConnection) {
        if (activeConnection === connection) {
            activeConnection = null
        }
    }

    @Synchronized
    internal fun markInstalling() {
        throwIfCancelled()
        installing = true
        activeConnection = null
    }

    fun throwIfCancelled() {
        if (cancelled.get()) throw DownloadCancelledException()
    }
}

class DownloadCancelledException : IOException("Game download was cancelled")

class TransientDownloadException(message: String) : IOException(message)

class GameDownloadManager(context: Context) {
    private val appContext = context.applicationContext
    private val storage = PlatformStorage(appContext)
    private val database = WarpDatabase(appContext)

    fun downloadAndInstall(
        game: CatalogGame,
        cancellationToken: DownloadCancellationToken = DownloadCancellationToken(),
        onInstallStarted: () -> Unit = {},
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): InstalledGame {
        try {
            storage.initialize()
            var currentUrl = URL(game.packageUrl)
            var redirectCount = 0

            while (true) {
                cancellationToken.throwIfCancelled()
                validateDownloadUrl(currentUrl, redirectCount > 0)
                val connection = currentUrl.openConnection() as HttpsURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/zip")
                connection.setRequestProperty("User-Agent", "WarpGame-Android")

                try {
                    cancellationToken.attach(connection)
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

                    if (responseCode == HTTP_TOO_MANY_REQUESTS ||
                        responseCode == HTTP_REQUEST_TIMEOUT ||
                        responseCode in HTTP_SERVER_ERROR_MIN..HTTP_SERVER_ERROR_MAX
                    ) {
                        throw TransientDownloadException(
                            "Temporary package server error: HTTP $responseCode"
                        )
                    }
                    if (responseCode != HttpsURLConnection.HTTP_OK) {
                        throw IllegalStateException("Package server returned HTTP $responseCode")
                    }

                    val contentLength = connection.contentLengthLong
                    if (contentLength > MAX_PACKAGE_BYTES) {
                        throw IllegalStateException("Downloaded package exceeds the size limit")
                    }
                    if (contentLength >= 0L && contentLength != game.packageSizeBytes) {
                        throw IllegalStateException(
                            "Downloaded package size does not match catalog metadata"
                        )
                    }

                    val progressInput = ProgressInputStream(
                        connection.inputStream,
                        game.packageSizeBytes,
                        cancellationToken,
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
                            ),
                            onPackageVerified = {
                                cancellationToken.markInstalling()
                                onInstallStarted()
                            }
                        )
                    }
                } catch (error: IOException) {
                    cancellationToken.throwIfCancelled()
                    throw error
                } finally {
                    cancellationToken.detach(connection)
                    connection.disconnect()
                }
            }
            throw IllegalStateException("Package download did not complete")
        } finally {
            database.close()
        }
    }

    private fun validateDownloadUrl(url: URL, isRedirect: Boolean) {
        val host = url.host.lowercase()
        val isOfficialReleaseUrl = host == "github.com" &&
            url.path.startsWith("/ded0TM0R0Z/Warp-Game/releases/download/")
        val isAllowedReleaseCdn = isRedirect && host in RELEASE_CDN_HOSTS

        if (
            url.protocol != "https" ||
            (!isOfficialReleaseUrl && !isAllowedReleaseCdn) ||
            url.userInfo != null ||
            (url.port != -1 && url.port != 443)
        ) {
            throw IllegalArgumentException("Package download URL is not trusted")
        }
    }

    private class ProgressInputStream(
        input: InputStream,
        private val expectedBytes: Long,
        private val cancellationToken: DownloadCancellationToken,
        private val onProgress: (Long, Long) -> Unit
    ) : FilterInputStream(input) {
        private var downloadedBytes = 0L

        override fun read(): Int {
            cancellationToken.throwIfCancelled()
            val value = try {
                super.read()
            } catch (error: IOException) {
                cancellationToken.throwIfCancelled()
                throw error
            }
            if (value >= 0) reportProgress(1)
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            cancellationToken.throwIfCancelled()
            val count = try {
                super.read(buffer, offset, length)
            } catch (error: IOException) {
                cancellationToken.throwIfCancelled()
                throw error
            }
            if (count > 0) reportProgress(count)
            return count
        }

        private fun reportProgress(count: Int) {
            cancellationToken.throwIfCancelled()
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
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_REQUEST_TIMEOUT = 408
        const val HTTP_SERVER_ERROR_MIN = 500
        const val HTTP_SERVER_ERROR_MAX = 599
        const val MAX_PACKAGE_BYTES = 256L * 1024L * 1024L

        val RELEASE_CDN_HOSTS = setOf(
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com"
        )
    }
}
