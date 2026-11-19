package com.warp.game

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.TextView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import java.io.ByteArrayInputStream
import java.security.MessageDigest

class GameRuntimeActivity : Activity() {
    private lateinit var database: WarpDatabase
    private lateinit var webView: WebView
    private var runtimeInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 12, 19)
        window.navigationBarColor = Color.rgb(10, 12, 19)

        val gameId = intent.getStringExtra(EXTRA_GAME_ID)
        if (gameId.isNullOrBlank()) {
            showError("Не указана игра для запуска")
            return
        }

        try {
            val storage = PlatformStorage(this)
            storage.initialize()
            database = WarpDatabase(this)
            val game = database.getInstalledGame(gameId)
                ?: throw GamePackageException("Игра не найдена в библиотеке")

            val gameDirectory = storage.gameDirectory(game.gameId)
            val entryFile = FilePathResolver.resolve(gameDirectory, game.entryPoint)
            if (!entryFile.isFile) {
                throw GamePackageException("Файл запуска игры не найден")
            }

            createRuntime(game.gameId, gameDirectory, game.entryPoint)
            runtimeInitialized = true
        } catch (error: Exception) {
            WarpLogger.error("GameRuntime", "Could not start installed game", error)
            showError(error.message ?: "Не удалось запустить игру")
        }
    }

    private fun createRuntime(gameId: String, gameDirectory: java.io.File, entryPoint: String) {
        webView = WebView(this)
        webView.setBackgroundColor(Color.rgb(10, 12, 19))
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            blockNetworkLoads = true
            mediaPlaybackRequiresUserGesture = true
        }

        val domain = isolatedDomain(gameId)
        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain(domain)
            .addPathHandler("/game/", GameDirectoryPathHandler(gameDirectory))
            .build()

        webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse {
                val uri = request.url
                if (uri.scheme == "https" && uri.host == domain) {
                    return assetLoader.shouldInterceptRequest(uri) ?: blockedResponse()
                }
                return blockedResponse()
            }

            @Suppress("DEPRECATION")
            override fun shouldInterceptRequest(
                view: WebView,
                url: String
            ): WebResourceResponse {
                val uri = Uri.parse(url)
                if (uri.scheme == "https" && uri.host == domain) {
                    return assetLoader.shouldInterceptRequest(uri) ?: blockedResponse()
                }
                return blockedResponse()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri = request.url
                return uri.scheme != "https" || uri.host != domain
            }

            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail
            ): Boolean {
                WarpLogger.error(
                    "GameRuntime",
                    "Game renderer stopped; crashed=" + detail.didCrash()
                )
                finish()
                return true
            }
        }

        setContentView(
            webView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val encodedEntryPoint = entryPoint
            .split("/")
            .joinToString("/") { Uri.encode(it) }
        webView.loadUrl("https://$domain/game/$encodedEntryPoint")
    }

    private fun isolatedDomain(gameId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(gameId.toByteArray(Charsets.UTF_8))
        val suffix = digest.take(8).joinToString("") { byte ->
            "%02x".format(byte)
        }
        return "game-$suffix.warp-game.local"
    }

    private fun blockedResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            403,
            "Blocked",
            emptyMap(),
            ByteArrayInputStream("Network access is disabled".toByteArray())
        )
    }

    private fun showError(message: String) {
        setContentView(
            TextView(this).apply {
                text = message
                textSize = 16f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.rgb(10, 12, 19))
                setPadding(32, 32, 32, 32)
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (runtimeInitialized) webView.onResume()
    }

    override fun onPause() {
        if (runtimeInitialized) webView.onPause()
        super.onPause()
    }

    override fun onBackPressed() {
        if (runtimeInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        if (runtimeInitialized) {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.removeAllViews()
            webView.destroy()
        }
        if (::database.isInitialized) database.close()
        super.onDestroy()
    }

    private companion object {
        const val EXTRA_GAME_ID = "com.warp.game.extra.GAME_ID"
    }
}

private object FilePathResolver {
    fun resolve(root: java.io.File, relativePath: String): java.io.File {
        GamePackageManifestParser.validateRelativePath(relativePath)
        val canonicalRoot = root.canonicalFile
        val target = java.io.File(canonicalRoot, relativePath).canonicalFile
        require(target.path.startsWith(canonicalRoot.path + java.io.File.separator)) {
            "Game entry point escapes its directory"
        }
        return target
    }
}
