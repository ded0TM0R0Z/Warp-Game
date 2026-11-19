package com.warp.game

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import java.io.IOException
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val bg = Color.rgb(10, 12, 19)
    private val surface = Color.rgb(21, 25, 36)
    private val surfaceAlt = Color.rgb(29, 34, 48)
    private val accent = Color.rgb(126, 103, 255)
    private val accentSoft = Color.rgb(52, 44, 91)
    private val textMain = Color.rgb(245, 246, 252)
    private val textMuted = Color.rgb(151, 160, 181)
    private val green = Color.rgb(105, 220, 164)

    private lateinit var pageContent: LinearLayout
    private lateinit var backButton: TextView
    private lateinit var navCatalog: TextView
    private lateinit var navLibrary: TextView
    private lateinit var navSettings: TextView

    private var currentPage = "home"
    private lateinit var platformStorage: PlatformStorage
    private lateinit var warpDatabase: WarpDatabase
    private lateinit var gameManager: GameManager
    private val backgroundExecutor = Executors.newSingleThreadExecutor()
    private var testInstallInProgress = false
    private var catalogLoadRequested = false
    private var catalogLoading = false
    private var catalogFromCache = false
    private var catalogError: String? = null
    private var catalogEntries: List<CatalogGame> = emptyList()
    private var downloadingGameId: String? = null
    private var downloadProgressPercent = 0
    private var catalogSearchQuery = ""
    private lateinit var catalogResultsContainer: LinearLayout
    private var activeDownloadToken: DownloadCancellationToken? = null
    private var downloadCancelRequested = false
    private var downloadIsInstalling = false
    private var downloadAttempt = 1
    private var activeDownloadStatusView: TextView? = null
    private var activeDownloadCancelView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        initializePlatformServices()
        buildShell()
        renderPage(false)
    }

    private fun initializePlatformServices() {
        try {
            platformStorage = PlatformStorage(this)
            platformStorage.initialize()
            WarpLogger.info("Storage", "Platform storage initialized")
        } catch (error: Exception) {
            WarpLogger.error("Storage", "Platform storage initialization failed", error)
        }

        try {
            warpDatabase = WarpDatabase(this)
            warpDatabase.readableDatabase
            WarpLogger.info("Database", "Local database initialized")
            if (::platformStorage.isInitialized) {
                gameManager = GameManager(platformStorage, warpDatabase)
            }
        } catch (error: Exception) {
            WarpLogger.error("Database", "Local database initialization failed", error)
        }
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(18), dp(10), dp(18), dp(8))
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(12))
        }

        backButton = TextView(this).apply {
            text = "‹  Назад"
            textSize = 14f
            setTextColor(textMain)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), dp(12), dp(8))
            visibility = View.GONE
            setOnClickListener { goBack() }
        }
        topBar.addView(backButton)

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setOnClickListener { navigateTo("home") }
        }
        brand.addView(label("WARP GAME", 19f, textMain, true))
        brand.addView(label("ИГРОВАЯ ПЛАТФОРМА", 9f, textMuted, true).apply {
            letterSpacing = 0.13f
            setPadding(0, dp(2), 0, 0)
        })
        topBar.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        topBar.addView(label("●", 11f, green, true))
        root.addView(topBar)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        pageContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(22))
        }
        scroll.addView(pageContent)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val divider = View(this).apply { setBackgroundColor(Color.rgb(38, 43, 58)) }
        root.addView(divider, LinearLayout.LayoutParams(-1, dp(1)))

        val bottomNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }
        navCatalog = navItem("Каталог")
        navLibrary = navItem("Мои игры")
        navSettings = navItem("Настройки")
        navCatalog.setOnClickListener { navigateTo("catalog") }
        navLibrary.setOnClickListener { navigateTo("library") }
        navSettings.setOnClickListener { navigateTo("settings") }
        bottomNav.addView(navCatalog, LinearLayout.LayoutParams(0, dp(52), 1f))
        bottomNav.addView(navLibrary, LinearLayout.LayoutParams(0, dp(52), 1f))
        bottomNav.addView(navSettings, LinearLayout.LayoutParams(0, dp(52), 1f))
        root.addView(bottomNav)

        setContentView(root)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_RETURN_HOME, false)) {
            currentPage = "home"
            if (::pageContent.isInitialized) renderPage(true)
        }
    }

    private fun navigateTo(page: String) {
        if (page == currentPage) return
        currentPage = page
        renderPage(true)
    }

    private fun goBack() {
        if (currentPage == "home") return
        currentPage = "home"
        renderPage(true)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (currentPage != "home") {
            goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun renderPage(animate: Boolean) {
        backButton.visibility = if (currentPage == "home") View.GONE else View.VISIBLE
        pageContent.removeAllViews()

        when (currentPage) {
            "catalog" -> renderCatalog()
            "library" -> renderLibrary()
            "settings" -> renderSettings()
            else -> renderHome()
        }

        navCatalog.isSelected = currentPage == "catalog"
        navLibrary.isSelected = currentPage == "library"
        navSettings.isSelected = currentPage == "settings"
        styleNav(navCatalog)
        styleNav(navLibrary)
        styleNav(navSettings)

        if (animate) {
            pageContent.alpha = 0f
            pageContent.translationY = dp(8).toFloat()
            pageContent.animate().cancel()
            pageContent.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(190)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun renderHome() {
        pageContent.addView(label("ТВОЯ БИБЛИОТЕКА ИГР", 10f, accent, true).apply {
            letterSpacing = 0.12f
        })
        pageContent.addView(label("Играй. Открывай. Исследуй.", 27f, textMain, true).apply {
            setPadding(0, dp(8), 0, dp(7))
        })
        pageContent.addView(label(
            "Единая платформа для мобильных 2D- и 3D-игр.",
            14f, textMuted, false
        ).apply { setPadding(0, 0, 0, dp(20)) })

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = gradientCard()
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        hero.addView(label("WARP  /  PLATFORM", 10f, Color.rgb(205, 196, 255), true))
        hero.addView(label("Игры в одном месте", 23f, textMain, true).apply {
            setPadding(0, dp(10), 0, dp(7))
        })
        hero.addView(label(
            "Загружай игры, храни их на устройстве и запускай без постоянного подключения к интернету.",
            13f, Color.rgb(211, 211, 228), false
        ))
        val status = label("●  ПРИЛОЖЕНИЕ ЗАПУЩЕНО", 10f, green, true).apply {
            setPadding(0, dp(18), 0, 0)
            letterSpacing = 0.04f
        }
        hero.addView(status)
        pageContent.addView(hero)

        sectionTitle("Быстрый доступ")
        pageContent.addView(actionCard(
            "Каталог игр",
            "Найти и установить новую игру",
            "01"
        ) { navigateTo("catalog") })
        pageContent.addView(actionCard(
            "Мои игры",
            "Открыть установленную библиотеку",
            "02"
        ) { navigateTo("library") })

        sectionTitle("Что дальше")
        val info = panel()
        info.setPadding(dp(16), dp(16), dp(16), dp(16))
        info.addView(label("Платформа развивается", 16f, textMain, true))
        info.addView(label(
            "Следующий этап — подключение каталога, установка игровых пакетов и запуск первой встроенной игры.",
            13f, textMuted, false
        ).apply { setPadding(0, dp(7), 0, 0) })
        pageContent.addView(info)
        pageContent.addView(label("WARP GAME  •  VERSION 0.2.0", 10f, textMuted, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(4))
        })
    }

    private fun renderCatalog() {
        pageHeading("Каталог игр", "Загружай игры и запускай их без постоянного подключения к сети.")

        if (!catalogLoadRequested) {
            catalogLoadRequested = true
            catalogLoading = true
            requestCatalog()
        }

        if (catalogLoading) {
            pageContent.addView(emptyState(
                "Загрузка каталога",
                "Получаем список доступных игр."
            ))
            return
        }

        if (catalogError != null && catalogEntries.isEmpty()) {
            pageContent.addView(emptyState(
                "Каталог недоступен",
                catalogError ?: "Не удалось загрузить список игр."
            ))
            pageContent.addView(actionCard(
                "Повторить",
                "Попробовать загрузить каталог ещё раз",
                "↻"
            ) {
                catalogLoadRequested = false
                catalogError = null
                renderPage(true)
            })
            pageContent.addView(actionCard(
                "Установить тестовую игру",
                "Встроенная демонстрация установки и запуска",
                "2D"
            ) {
                installBundledTestGame()
            })
            return
        }

        if (catalogFromCache) {
            pageContent.addView(label(
                "Показана сохранённая версия каталога",
                11f,
                textMuted,
                false
            ).apply { setPadding(0, 0, 0, dp(12)) })
        }

        if (catalogEntries.isEmpty()) {
            pageContent.addView(emptyState(
                "Каталог пока пуст",
                "Новые игры появятся здесь после публикации пакетов."
            ))
            pageContent.addView(actionCard(
                "Установить тестовую игру",
                "Встроенная демонстрация установки и запуска",
                "2D"
            ) {
                installBundledTestGame()
            })
            return
        }

        val searchField = EditText(this).apply {
            setSingleLine(true)
            setText(catalogSearchQuery)
            hint = "Поиск по названию, описанию или движку"
            textSize = 14f
            setTextColor(textMain)
            setHintTextColor(textMuted)
            background = rounded(surfaceAlt, 12)
            setPadding(dp(13), dp(11), dp(13), dp(11))
        }
        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                catalogSearchQuery = s?.toString().orEmpty()
                renderCatalogResults()
            }
        })
        val searchParams = LinearLayout.LayoutParams(-1, -2)
        searchParams.bottomMargin = dp(12)
        pageContent.addView(searchField, searchParams)

        catalogResultsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        pageContent.addView(catalogResultsContainer)
        renderCatalogResults()
    }

    private fun renderCatalogResults() {
        if (!::catalogResultsContainer.isInitialized) return
        catalogResultsContainer.removeAllViews()
        activeDownloadStatusView = null
        activeDownloadCancelView = null

        val query = catalogSearchQuery.trim()
        val filteredGames = catalogEntries.filter { game ->
            game.gameId == downloadingGameId ||
                query.isEmpty() ||
                game.name.contains(query, ignoreCase = true) ||
                game.description.contains(query, ignoreCase = true) ||
                game.engine.contains(query, ignoreCase = true) ||
                game.gameType.contains(query, ignoreCase = true) ||
                game.gameId.contains(query, ignoreCase = true)
        }

        if (filteredGames.isEmpty()) {
            catalogResultsContainer.addView(emptyState(
                if (query.isEmpty()) "Каталог пока пуст" else "Ничего не найдено",
                if (query.isEmpty()) {
                    "Новые игры появятся здесь после публикации пакетов."
                } else {
                    "Измени поисковый запрос и попробуй ещё раз."
                }
            ))
            return
        }

        filteredGames.forEach { game ->
            catalogResultsContainer.addView(catalogCard(game))
        }
    }

    private fun requestCatalog() {
        backgroundExecutor.execute {
            try {
                val snapshot = CatalogRepository(this).loadCatalog()
                runOnUiThread {
                    catalogLoading = false
                    catalogEntries = snapshot.games
                    catalogFromCache = snapshot.fromCache
                    catalogError = null
                    if (!isFinishing && !isDestroyed && currentPage == "catalog") {
                        renderPage(true)
                    }
                }
            } catch (error: Exception) {
                WarpLogger.error("Catalog", "Catalog loading failed", error)
                runOnUiThread {
                    catalogLoading = false
                    catalogError = "Проверь подключение к интернету и повтори загрузку."
                    if (!isFinishing && !isDestroyed && currentPage == "catalog") {
                        renderPage(true)
                    }
                }
            }
        }
    }

    private fun catalogCard(game: CatalogGame): LinearLayout {
        val card = panel().apply {
            setPadding(dp(16), dp(16), dp(16), dp(14))
        }
        card.addView(label(game.name, 17f, textMain, true))
        card.addView(label(
            game.description,
            12f,
            textMuted,
            false
        ).apply { setPadding(0, dp(7), 0, dp(10)) })

        val sizeMb = String.format(
            java.util.Locale.US,
            "%.1f MB",
            game.packageSizeBytes / (1024f * 1024f)
        )
        card.addView(label(
            game.gameType.uppercase() + " • " + game.engine + " • " + game.version + " • " + sizeMb,
            10f,
            accent,
            true
        ))

        val installedGame = if (::gameManager.isInitialized) {
            gameManager.getInstalledGame(game.gameId)
        } else {
            null
        }
        val isCurrentVersionInstalled = installedGame?.version == game.version
        val isDownloading = downloadingGameId == game.gameId
        val anotherDownloadActive = downloadingGameId != null && !isDownloading

        val buttonText = when {
            isDownloading && downloadIsInstalling -> "Проверка и установка…"
            isDownloading && downloadAttempt > 1 ->
                "Повтор $downloadAttempt/$MAX_DOWNLOAD_ATTEMPTS · $downloadProgressPercent%"
            isDownloading -> "Загрузка: $downloadProgressPercent%"
            isCurrentVersionInstalled -> "Установлено"
            anotherDownloadActive -> "Другая загрузка"
            installedGame != null -> "Обновить"
            else -> "Установить"
        }

        val installButton = TextView(this).apply {
            text = buttonText
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(if (isCurrentVersionInstalled) textMuted else textMain)
            background = rounded(if (isCurrentVersionInstalled) surfaceAlt else accentSoft, 11)
            setPadding(dp(14), dp(11), dp(14), dp(11))
            isClickable = !isCurrentVersionInstalled && !anotherDownloadActive && !isDownloading
            isFocusable = isClickable
            setOnClickListener {
                if (isClickable) downloadGame(game)
            }
        }
        val buttonParams = LinearLayout.LayoutParams(-1, -2)
        buttonParams.topMargin = dp(12)
        card.addView(installButton, buttonParams)

        if (isDownloading) {
            activeDownloadStatusView = installButton
            if (!downloadIsInstalling) {
                val cancelButton = TextView(this).apply {
                    text = if (downloadCancelRequested) "Отмена…" else "Отменить загрузку"
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setTextColor(textMuted)
                    background = rounded(surfaceAlt, 10)
                    setPadding(dp(12), dp(9), dp(12), dp(9))
                    isClickable = !downloadCancelRequested
                    isFocusable = isClickable
                    setOnClickListener {
                        if (isClickable) cancelGameDownload()
                    }
                }
                val cancelParams = LinearLayout.LayoutParams(-1, -2)
                cancelParams.topMargin = dp(7)
                card.addView(cancelButton, cancelParams)
                activeDownloadCancelView = cancelButton
            }
        }

        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = dp(10)
        card.layoutParams = params
        return card
    }

    private fun downloadGame(game: CatalogGame) {
        if (downloadingGameId != null || testInstallInProgress) return

        val cancellationToken = DownloadCancellationToken()
        activeDownloadToken = cancellationToken
        downloadCancelRequested = false
        downloadIsInstalling = false
        downloadAttempt = 1
        downloadingGameId = game.gameId
        downloadProgressPercent = 0
        renderCatalogResults()

        backgroundExecutor.execute {
            var attempt = 1
            var lastReportedPercent = -5
            try {
                var installedGame: InstalledGame? = null
                while (installedGame == null) {
                    cancellationToken.throwIfCancelled()
                    try {
                        installedGame = GameDownloadManager(this).downloadAndInstall(
                            game = game,
                            cancellationToken = cancellationToken,
                            onInstallStarted = {
                                runOnUiThread {
                                    downloadIsInstalling = true
                                    activeDownloadStatusView?.text = "Проверка и установка…"
                                    activeDownloadCancelView?.visibility = View.GONE
                                }
                            }
                        ) { downloaded, total ->
                            val percent = if (total > 0L) {
                                ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }
                            if (percent >= lastReportedPercent + 5 || percent == 100) {
                                lastReportedPercent = percent
                                runOnUiThread {
                                    downloadProgressPercent = percent
                                    if (!downloadIsInstalling) {
                                        activeDownloadStatusView?.text = when {
                                            downloadAttempt > 1 ->
                                                "Повтор $downloadAttempt/$MAX_DOWNLOAD_ATTEMPTS · $percent%"
                                            else -> "Загрузка: $percent%"
                                        }
                                    }
                                }
                            }
                        }
                    } catch (error: Exception) {
                        if (cancellationToken.isCancelled || error is DownloadCancelledException) {
                            throw DownloadCancelledException()
                        }
                        if (
                            cancellationToken.isInstalling ||
                            attempt >= MAX_DOWNLOAD_ATTEMPTS ||
                            !isRetryableDownloadError(error)
                        ) {
                            throw error
                        }

                        attempt += 1
                        lastReportedPercent = -5
                        runOnUiThread {
                            downloadAttempt = attempt
                            downloadProgressPercent = 0
                            activeDownloadStatusView?.text =
                                "Повторная попытка $attempt/$MAX_DOWNLOAD_ATTEMPTS…"
                        }
                        Thread.sleep(RETRY_DELAY_MS * (attempt - 1L))
                    }
                }

                val completedGame = installedGame
                    ?: throw IllegalStateException("Download finished without an installed game")
                runOnUiThread {
                    activeDownloadToken = null
                    downloadingGameId = null
                    downloadProgressPercent = 0
                    downloadAttempt = 1
                    downloadCancelRequested = false
                    downloadIsInstalling = false
                    activeDownloadStatusView = null
                    activeDownloadCancelView = null
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    android.widget.Toast.makeText(
                        this,
                        "Игра установлена: " + completedGame.name,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    navigateTo("library")
                }
            } catch (error: Exception) {
                val wasCancelled = cancellationToken.isCancelled ||
                    error is DownloadCancelledException
                WarpLogger.error(
                    "Download",
                    if (wasCancelled) "Game download cancelled" else "Game download or installation failed",
                    error
                )
                runOnUiThread {
                    activeDownloadToken = null
                    downloadingGameId = null
                    downloadProgressPercent = 0
                    downloadAttempt = 1
                    downloadCancelRequested = false
                    downloadIsInstalling = false
                    activeDownloadStatusView = null
                    activeDownloadCancelView = null
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    android.widget.Toast.makeText(
                        this,
                        if (wasCancelled) "Загрузка отменена"
                        else "Не удалось скачать или установить игру",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    if (currentPage == "catalog") renderCatalogResults()
                }
            }
        }
    }

    private fun cancelGameDownload() {
        if (downloadIsInstalling || activeDownloadToken?.isInstalling == true || downloadCancelRequested) return
        downloadCancelRequested = true
        activeDownloadCancelView?.text = "Отмена…"
        activeDownloadCancelView?.isClickable = false
        activeDownloadToken?.cancel()
    }

    private fun isRetryableDownloadError(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is IOException && current !is DownloadCancelledException) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun renderLibrary() {
        pageHeading("Мои игры", "Все установленные игры будут собраны здесь.")

        if (!::warpDatabase.isInitialized) {
            pageContent.addView(emptyState(
                "База данных недоступна",
                "Не удалось открыть локальное хранилище. Подробности записаны в системный журнал."
            ))
            return
        }

        if (!::gameManager.isInitialized) {
            pageContent.addView(emptyState(
                "Менеджер игр недоступен",
                "Локальные службы не удалось инициализировать."
            ))
            return
        }

        val installedGames = try {
            gameManager.getInstalledGames()
        } catch (error: Exception) {
            WarpLogger.error("Library", "Could not read installed games", error)
            pageContent.addView(emptyState(
                "Не удалось загрузить библиотеку",
                "Произошла ошибка чтения локальной базы данных."
            ))
            return
        }

        if (installedGames.isEmpty()) {
            pageContent.addView(emptyState(
                "Библиотека пока пуста",
                "После установки игры она появится здесь. Сохранения хранятся отдельно от файлов игры."
            ))
            return
        }

        installedGames.forEach { game ->
            pageContent.addView(gameCard(game))
        }
    }

    private fun gameCard(game: InstalledGame): LinearLayout {
        val card = panel().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(10), dp(14))
        }

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            setOnClickListener {
                startActivity(
                    android.content.Intent(this@MainActivity, GameRuntimeActivity::class.java)
                        .putExtra("com.warp.game.extra.GAME_ID", game.gameId)
                )
            }
        }
        info.addView(label(game.name, 15f, textMain, true))
        info.addView(label(
            "Версия " + game.version + " • " + game.gameType.uppercase() + " • " + game.engine,
            11f,
            textMuted,
            false
        ).apply { setPadding(0, dp(5), 0, 0) })
        card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))

        val remove = TextView(this).apply {
            text = "Удалить"
            textSize = 11f
            setTextColor(Color.rgb(255, 139, 139))
            setPadding(dp(10), dp(10), dp(6), dp(10))
            isClickable = true
            isFocusable = true
            setOnClickListener { confirmGameRemoval(game) }
        }
        card.addView(remove)
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = dp(9)
        card.layoutParams = params
        return card
    }

    private fun confirmGameRemoval(game: InstalledGame) {
        AlertDialog.Builder(this)
            .setTitle("Удалить игру?")
            .setMessage(
                "Файлы игры «" + game.name + "» будут удалены. Сохранения останутся на устройстве."
            )
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить") { _, _ ->
                try {
                    gameManager.removeGame(game.gameId)
                    android.widget.Toast.makeText(
                        this,
                        "Игра удалена",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    renderPage(true)
                } catch (error: Exception) {
                    WarpLogger.error("Library", "Game removal failed", error)
                    android.widget.Toast.makeText(
                        this,
                        "Не удалось удалить игру",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }

    private fun installBundledTestGame() {
        if (testInstallInProgress) return
        testInstallInProgress = true
        android.widget.Toast.makeText(
            this,
            "Проверка и установка пакета…",
            android.widget.Toast.LENGTH_SHORT
        ).show()

        backgroundExecutor.execute {
            try {
                val installedGame = BundledTestGameInstaller(this).install()
                runOnUiThread {
                    testInstallInProgress = false
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    android.widget.Toast.makeText(
                        this,
                        "Тестовая игра установлена",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    navigateTo("library")
                }
                WarpLogger.info("Installer", "Bundled test game installed: " + installedGame.gameId)
            } catch (error: Exception) {
                WarpLogger.error("Installer", "Bundled test game installation failed", error)
                runOnUiThread {
                    testInstallInProgress = false
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    android.widget.Toast.makeText(
                        this,
                        "Не удалось установить тестовую игру",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun installPhaserDemoGame() {
        if (testInstallInProgress) return
        testInstallInProgress = true
        android.widget.Toast.makeText(
            this,
            "Подготовка Phaser 2D…",
            android.widget.Toast.LENGTH_SHORT
        ).show()

        backgroundExecutor.execute {
            try {
                val installedGame = BundledPhaserDemoInstaller(this).install()
                runOnUiThread {
                    testInstallInProgress = false
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    android.widget.Toast.makeText(
                        this,
                        "Phaser-игра установлена офлайн",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    navigateTo("library")
                }
                WarpLogger.info(
                    "Installer",
                    "Bundled Phaser demo installed: " + installedGame.gameId
                )
            } catch (error: Exception) {
                WarpLogger.error("Installer", "Phaser demo installation failed", error)
                runOnUiThread {
                    testInstallInProgress = false
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    android.widget.Toast.makeText(
                        this,
                        "Не удалось установить Phaser-игру",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun installWebGlDemoGame() {
        if (testInstallInProgress) return
        testInstallInProgress = true
        android.widget.Toast.makeText(
            this,
            "Подготовка 3D WebGL…",
            android.widget.Toast.LENGTH_SHORT
        ).show()

        backgroundExecutor.execute {
            try {
                val installedGame = BundledWebGlDemoInstaller(this).install()
                runOnUiThread {
                    testInstallInProgress = false
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    android.widget.Toast.makeText(
                        this,
                        "3D WebGL-тест установлен",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    navigateTo("library")
                }
                WarpLogger.info(
                    "Installer",
                    "Bundled WebGL 3D demo installed: " + installedGame.gameId
                )
            } catch (error: Exception) {
                WarpLogger.error("Installer", "WebGL demo installation failed", error)
                runOnUiThread {
                    testInstallInProgress = false
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    android.widget.Toast.makeText(
                        this,
                        "Не удалось установить 3D WebGL-тест",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun renderSettings() {
        pageHeading("Настройки", "Информация о приложении и его состоянии.")
        val appCard = panel()
        appCard.setPadding(dp(16), dp(16), dp(16), dp(16))
        appCard.addView(label("WARP GAME", 18f, textMain, true))
        appCard.addView(label("Версия приложения: 0.2.0", 13f, textMuted, false).apply {
            setPadding(0, dp(8), 0, 0)
        })
        appCard.addView(label("Платформа: Android", 13f, textMuted, false).apply {
            setPadding(0, dp(5), 0, 0)
        })
        pageContent.addView(appCard)

        sectionTitle("Разработка")
        pageContent.addView(actionCard(
            "Установить тестовую игру",
            "Проверить ZIP, SHA-256, установку и запуск",
            "PKG"
        ) {
            installBundledTestGame()
        })
        pageContent.addView(actionCard(
            "Установить 2D-игру Phaser",
            "Офлайн-демо с касаниями и сохранением рекорда",
            "2D"
        ) {
            installPhaserDemoGame()
        })
        pageContent.addView(actionCard(
            "Установить 3D WebGL-тест",
            "Вращающийся 3D-объект, касания и проверка WebGL",
            "3D"
        ) {
            installWebGlDemoGame()
        })
        pageContent.addView(actionCard(
            "Тест игрового движка",
            "Проверить локальный HTML5, касания и WebGL",
            "TEST"
        ) {
            startActivity(android.content.Intent(this, RuntimeTestActivity::class.java))
        })

        sectionTitle("Принципы платформы")
        pageContent.addView(detailRow("Офлайн-режим", "Игры должны запускаться без интернета после установки."))
        pageContent.addView(detailRow("Сохранения", "Сохранения хранятся отдельно от файлов игры."))
        pageContent.addView(detailRow("Совместимость", "Приоритет — телефоны Android, включая устройства начального уровня."))
        pageContent.addView(label(
            "Настройки загрузок, хранилища и графики появятся после реализации соответствующих модулей.",
            12f, textMuted, false
        ).apply { setPadding(dp(4), dp(16), dp(4), 0) })
    }

    private fun pageHeading(title: String, subtitle: String) {
        pageContent.addView(label(title, 27f, textMain, true))
        pageContent.addView(label(subtitle, 14f, textMuted, false).apply {
            setPadding(0, dp(7), 0, dp(20))
        })
    }

    private fun emptyState(title: String, description: String): LinearLayout {
        val box = panel().apply {
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(28), dp(20), dp(28))
        }
        val symbol = label("W", 30f, accent, true).apply { gravity = Gravity.CENTER }
        box.addView(symbol)
        box.addView(label(title, 18f, textMain, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(7))
        })
        box.addView(label(description, 13f, textMuted, false).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        })
        return box
    }

    private fun actionCard(title: String, subtitle: String, number: String, action: () -> Unit): LinearLayout {
        val card = panel().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(16), dp(15), dp(16))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        val numberView = label(number, 11f, Color.rgb(190, 180, 255), true).apply {
            gravity = Gravity.CENTER
            background = rounded(accentSoft, 10)
        }
        card.addView(numberView, LinearLayout.LayoutParams(dp(38), dp(38)))
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(6), 0)
        }
        info.addView(label(title, 16f, textMain, true))
        info.addView(label(subtitle, 12f, textMuted, false).apply {
            setPadding(0, dp(4), 0, 0)
        })
        card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(label("›", 27f, textMuted, false))
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = dp(10)
        card.layoutParams = params
        return card
    }

    private fun detailRow(title: String, subtitle: String): LinearLayout {
        val box = panel().apply { setPadding(dp(15), dp(14), dp(15), dp(14)) }
        box.addView(label(title, 15f, textMain, true))
        box.addView(label(subtitle, 12f, textMuted, false).apply {
            setPadding(0, dp(5), 0, 0)
        })
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = dp(9)
        box.layoutParams = params
        return box
    }

    private fun sectionTitle(title: String) {
        pageContent.addView(label(title, 18f, textMain, true).apply {
            setPadding(0, dp(24), 0, dp(12))
        })
    }

    private fun navItem(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(textMuted)
            isClickable = true
            isFocusable = true
        }
    }

    private fun styleNav(item: TextView) {
        item.setTextColor(if (item.isSelected) textMain else textMuted)
        item.background = if (item.isSelected) rounded(accentSoft, 13) else null
    }

    private fun panel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(surface, 17)
        }
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun gradientCard(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(43, 37, 83), Color.rgb(25, 31, 51), Color.rgb(20, 27, 39))
        ).apply { cornerRadius = dp(20).toFloat() }
    }

    override fun onDestroy() {
        activeDownloadToken?.cancel()
        backgroundExecutor.shutdownNow()
        if (::warpDatabase.isInitialized) {
            warpDatabase.close()
        }
        super.onDestroy()
    }

    internal companion object {
        const val EXTRA_RETURN_HOME = "com.warp.game.extra.RETURN_HOME"
        const val MAX_DOWNLOAD_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 750L
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
