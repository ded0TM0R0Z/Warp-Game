package com.warp.game

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import java.io.ByteArrayInputStream

class RuntimeTestActivity : Activity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveMode()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(10, 12, 19))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        val back = TextView(this).apply {
            text = "‹  Назад"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, dp(8), dp(18), dp(8))
            setOnClickListener { finish() }
        }
        header.addView(back)

        val title = TextView(this).apply {
            text = "Тест игрового движка"
            textSize = 17f
            setTextColor(Color.WHITE)
        }
        header.addView(title)
        root.addView(header)

        webView = WebView(this)
        configureWebView()
        root.addView(
            webView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
        loadLocalTestGame()
    }

    private fun configureWebView() {
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

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(
                "/test/",
                WebViewAssetLoader.AssetsPathHandler(this)
            )
            .build()

        webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse {
                val uri = request.url
                if (uri.scheme == "https" && uri.host == LOCAL_ASSET_HOST) {
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
                if (uri.scheme == "https" && uri.host == LOCAL_ASSET_HOST) {
                    return assetLoader.shouldInterceptRequest(uri) ?: blockedResponse()
                }
                return blockedResponse()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri = request.url
                return uri.scheme != "https" || uri.host != LOCAL_ASSET_HOST
            }

            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail
            ): Boolean {
                WarpLogger.error(
                    "Runtime",
                    "WebView renderer stopped; crashed=" + detail.didCrash()
                )
                WarpNavigation.returnToHome(this@RuntimeTestActivity)
                return true
            }
        }
    }

    private fun loadLocalTestGame() {
        webView.loadUrl(
            "https://$LOCAL_ASSET_HOST/test/runtime_test/index.html"
        )
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

    @Suppress("DEPRECATION")
    private fun applyImmersiveMode() {
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode()
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onPause() {
        if (::webView.isInitialized) webView.onPause()
        super.onPause()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        WarpNavigation.returnToHome(this)
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.removeAllViews()
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val LOCAL_ASSET_HOST = "appassets.androidplatform.net"
    }
}
