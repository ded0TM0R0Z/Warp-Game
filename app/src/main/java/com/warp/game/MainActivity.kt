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
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.EOFException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.Executors
import javax.net.ssl.SSLException

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
    private lateinit var navHome: TextView
    private lateinit var navCatalog: TextView
    private lateinit var navLibrary: TextView
    private lateinit var navDownloads: TextView
    private lateinit var navSettings: TextView

    private var currentPage = "home"
    private lateinit var platformStorage: PlatformStorage
    private lateinit var warpDatabase: WarpDatabase
    private lateinit var gameManager: GameManager
    private lateinit var downloadQueueStore: DownloadQueueStore
    private val backgroundExecutor = Executors.newSingleThreadExecutor()
    private val downloadExecutor = Executors.newSingleThreadExecutor()
    private var testInstallInProgress = false
    private var catalogLoadRequested = false
    private var catalogLoading = false
    private var catalogFromCache = false
    private var catalogError: String? = null
    private var catalogEntries: List<CatalogGame> = emptyList()
    private var downloadingGameId: String? = null
    private var downloadProgressPercent = 0
    private var catalogSearchQuery = ""
    private var librarySearchQuery = ""
    private var catalogTypeFilter = "all"
    private var catalogEngineFilter = "all"
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
        applyImmersiveMode()
        initializePlatformServices()
        buildShell()
        renderPage(false)
        startNextQueuedDownload()
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
            downloadQueueStore = DownloadQueueStore(this)
            downloadQueueStore.recoverInterruptedDownloads()
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
            setPadding(0, dp(8), 0, dp(14))
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

        val logo = TextView(this).apply {
            text = "W"
            textSize = 25f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(42, 211, 255), Color.rgb(100, 78, 255), Color.rgb(42, 35, 94))
            ).apply { cornerRadius = dp(15).toFloat() }
        }
        topBar.addView(logo, LinearLayout.LayoutParams(dp(46), dp(46)).apply { rightMargin = dp(12) })
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setOnClickListener { navigateTo("home") }
        }
        brand.addView(label("WARP GAME", 18f, textMain, true))
        brand.addView(label("ИГРОВАЯ ПЛАТФОРМА", 9f, textMuted, true).apply {
            letterSpacing = 0.13f
            setPadding(0, dp(3), 0, 0)
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
        navHome = navItem("⌂\nГлавная")
        navCatalog = navItem("▦\nКаталог")
        navLibrary = navItem("▣\nМои игры")
        navDownloads = navItem("⇩\nЗагрузки")
        navSettings = navItem("⚙\nНастройки")
        navHome.setOnClickListener { navigateTo("home") }
        navCatalog.setOnClickListener { navigateTo("catalog") }
        navLibrary.setOnClickListener { navigateTo("library") }
        navDownloads.setOnClickListener { navigateTo("downloads") }
        navSettings.setOnClickListener { navigateTo("settings") }
        bottomNav.addView(navHome, LinearLayout.LayoutParams(0, dp(62), 1f))
        bottomNav.addView(navCatalog, LinearLayout.LayoutParams(0, dp(62), 1f))
        bottomNav.addView(navLibrary, LinearLayout.LayoutParams(0, dp(62), 1f))
        bottomNav.addView(navDownloads, LinearLayout.LayoutParams(0, dp(62), 1f))
        bottomNav.addView(navSettings, LinearLayout.LayoutParams(0, dp(62), 1f))
        root.addView(bottomNav)

        setContentView(root)
    }

    @Suppress("DEPRECATION")
    private fun applyImmersiveMode() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode()
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
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
            "downloads" -> renderDownloads()
            else -> renderHome()
        }

        navHome.isSelected = currentPage == "home"
        navCatalog.isSelected = currentPage == "catalog"
        navLibrary.isSelected = currentPage == "library"
        navDownloads.isSelected = currentPage == "downloads"
        navSettings.isSelected = currentPage == "settings"
        styleNav(navHome)
        styleNav(navCatalog)
        styleNav(navLibrary)
        styleNav(navDownloads)
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
        pageContent.addView(label("ПЛАТФОРМА ДЛЯ ТВОИХ ИГР", 10f, accent, true).apply {
            letterSpacing = 0.14f
        })
        pageContent.addView(label("Играй без границ", 30f, textMain, true).apply {
            setPadding(0, dp(9), 0, dp(7))
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
        hero.addView(label("W  /  WARP GAME", 10f, Color.rgb(205, 196, 255), true).apply {
            letterSpacing = 0.12f
        })
        hero.addView(label("Твоя игровая вселенная", 23f, textMain, true).apply {
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

        val featureStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(surfaceAlt, 14)
            setPadding(dp(8), dp(13), dp(8), dp(13))
        }
        listOf("2D ИГРЫ", "3D ИГРЫ", "ОФЛАЙН").forEachIndexed { index, value ->
            val item = label(value, 10f, if (index == 1) Color.rgb(76, 205, 255) else textMuted, true)
            item.gravity = Gravity.CENTER
            featureStrip.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        val featureParams = LinearLayout.LayoutParams(-1, -2)
        featureParams.topMargin = dp(14)
        pageContent.addView(featureStrip, featureParams)

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
        pageContent.addView(actionCard(
            "Загрузки",
            "Очередь, ход загрузки и повтор неудачных установок",
            "03"
        ) { navigateTo("downloads") })

        sectionTitle("Что дальше")
        val info = panel()
        info.setPadding(dp(16), dp(16), dp(16), dp(16))
        info.addView(label("Платформа развивается", 16f, textMain, true))
        info.addView(label(
            "Каталог, локальная библиотека, установка игровых пакетов и запуск 2D- и 3D-игр собраны в одном приложении.",
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
                "Получаем список доступных игр. Игры появятся здесь после публикации пакетов на сервере."
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

        sectionTitle("Категории")
        val typeFilters = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        listOf("all" to "Все", "2d" to "2D", "3d" to "3D").forEach { (key, title) ->
            typeFilters.addView(filterChip(title, catalogTypeFilter == key) {
                catalogTypeFilter = key
                renderCatalogResults()
                renderCatalogFilterStyles(typeFilters, catalogTypeFilter)
            }, LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                rightMargin = dp(6)
            })
        }
        pageContent.addView(typeFilters)

        sectionTitle("Движок")
        val engineFilters = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val availableEngines = listOf("all") + catalogEntries.map { it.engine }.distinct().sorted()
        availableEngines.forEach { engine ->
            val title = if (engine == "all") "Все" else engine
            engineFilters.addView(filterChip(title, catalogEngineFilter == engine) {
                catalogEngineFilter = engine
                renderCatalogResults()
                renderCatalogFilterStyles(engineFilters, catalogEngineFilter)
            }, LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                rightMargin = dp(6)
            })
        }
        pageContent.addView(engineFilters)

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
        }.filter { game ->
            (catalogTypeFilter == "all" || game.gameType == catalogTypeFilter) &&
                (catalogEngineFilter == "all" || game.engine.equals(catalogEngineFilter, ignoreCase = true))
        }

        if (filteredGames.isEmpty()) {
            catalogResultsContainer.addView(emptyState(
                when {
                    query.isNotEmpty() -> "Ничего не найдено"
                    catalogTypeFilter != "all" || catalogEngineFilter != "all" -> "В этой категории пока нет игр"
                    else -> "Пока нет опубликованных игр"
                },
                when {
                    query.isNotEmpty() -> "Измени поисковый запрос и попробуй ещё раз."
                    catalogTypeFilter != "all" || catalogEngineFilter != "all" -> "Выбери другую категорию или движок."
                    else -> "Новые игры будут загружаться с сервера после публикации."
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

    private fun filterChip(
        title: String,
        selected: Boolean,
        onClick: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = title
            textSize = 11f
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(if (selected) Color.WHITE else textMuted)
            background = rounded(if (selected) accentSoft else surfaceAlt, 10)
            setPadding(dp(9), 0, dp(9), 0)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    private fun renderCatalogFilterStyles(container: LinearLayout, selected: String) {
        for (index in 0 until container.childCount) {
            val chip = container.getChildAt(index) as? TextView ?: continue
            val key = when {
                chip.text.toString() == "Все" -> "all"
                chip.text.toString() == "2D" -> "2d"
                chip.text.toString() == "3D" -> "3d"
                else -> chip.text.toString()
            }
            val isSelected = key.equals(selected, ignoreCase = true)
            chip.setTextColor(if (isSelected) Color.WHITE else textMuted)
            chip.background = rounded(if (isSelected) accentSoft else surfaceAlt, 10)
        }
    }

    private fun showCatalogGameDetails(game: CatalogGame) {
        val installedGame = if (::gameManager.isInitialized) {
            gameManager.getInstalledGame(game.gameId)
        } else {
            null
        }
        val isCurrentVersionInstalled = installedGame != null && installedGame.version == game.version && installedGame.packageSha256.equals(game.packageSha256, ignoreCase = true)
        val sizeMb = String.format(
            java.util.Locale.US,
            "%.1f MB",
            game.packageSizeBytes / (1024f * 1024f)
        )
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
        }
        content.addView(label(game.description, 14f, textMuted, false))
        content.addView(detailRow("Тип", game.gameType.uppercase()))
        content.addView(detailRow("Движок", game.engine + " " + game.engineVersion))
        content.addView(detailRow("Версия игры", game.version))
        content.addView(detailRow("Размер", sizeMb))
        content.addView(detailRow("Офлайн-запуск", if (game.offlineSupported) "Поддерживается" else "Не поддерживается"))
        content.addView(detailRow("Ориентация", game.orientation))
        val dialog = AlertDialog.Builder(this)
            .setTitle(game.name)
            .setView(content)
            .setNegativeButton("Закрыть", null)
            .setPositiveButton(
                when {
                    isCurrentVersionInstalled -> "Установлено"
                    installedGame != null -> "Обновить"
                    else -> "Установить"
                },
                null
            )
            .create()
        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.isEnabled = !isCurrentVersionInstalled
            button.setOnClickListener {
                dialog.dismiss()
                if (!isCurrentVersionInstalled) downloadGame(game)
            }
        }
        dialog.show()
    }

    private fun catalogCard(game: CatalogGame): LinearLayout {
        val card = panel().apply {
            setPadding(dp(16), dp(16), dp(16), dp(14))
        }
        if (game.gameId == "com.warp.riftsurvivors") {
            val avatar = ImageView(this).apply {
                setImageResource(R.drawable.rift_survivors_icon)
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = rounded(surfaceAlt, 16)
                clipToOutline = true
                contentDescription = "Rift Survivors"
            }
            val avatarParams = LinearLayout.LayoutParams(dp(92), dp(92))
            avatarParams.bottomMargin = dp(12)
            card.addView(avatar, avatarParams)
        }
        card.addView(label(game.name, 17f, textMain, true))
        val detailsButton = TextView(this).apply {
            text = "Подробнее"
            textSize = 11f
            setTextColor(Color.rgb(112, 199, 255))
            setPadding(0, dp(5), 0, dp(2))
            isClickable = true
            isFocusable = true
            setOnClickListener { showCatalogGameDetails(game) }
        }
        card.addView(detailsButton)
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
            (if (game.gameId == "com.warp.riftsurvivors") game.engine else game.gameType.uppercase()) + " • " + game.version + " • " + sizeMb,
            10f,
            accent,
            true
        ))

        val installedGame = if (::gameManager.isInitialized) {
            gameManager.getInstalledGame(game.gameId)
        } else {
            null
        }
        val isCurrentVersionInstalled = installedGame != null && installedGame.version == game.version && installedGame.packageSha256.equals(game.packageSha256, ignoreCase = true)
        val isDownloading = downloadingGameId == game.gameId
        val queuedEntry = if (::downloadQueueStore.isInitialized) {
            downloadQueueStore.getEntries().firstOrNull { it.game.gameId == game.gameId }
        } else null
        val isQueued = queuedEntry?.status == DownloadQueueStore.STATUS_PENDING
        val isFailedInQueue = queuedEntry?.status == DownloadQueueStore.STATUS_FAILED
        val anotherDownloadActive = downloadingGameId != null && !isDownloading

        val buttonText = when {
            isDownloading && downloadIsInstalling -> "Проверка и установка…"
            isDownloading && downloadAttempt > 1 ->
                "Повтор $downloadAttempt/$MAX_DOWNLOAD_ATTEMPTS · $downloadProgressPercent%"
            isDownloading -> "Загрузка: $downloadProgressPercent%"
            isQueued -> "В очереди"
            isFailedInQueue -> "Ошибка · открыть загрузки"
            isCurrentVersionInstalled -> "Установлено"
            anotherDownloadActive -> "Добавить в очередь"
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
            isClickable = !isCurrentVersionInstalled && !isQueued && !isFailedInQueue && !isDownloading
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
        if (!::downloadQueueStore.isInitialized) return
        val added = try {
            downloadQueueStore.enqueue(game)
        } catch (error: Exception) {
            WarpLogger.error("DownloadQueue", "Could not add game to download queue", error)
            false
        }
        if (!added) {
            android.widget.Toast.makeText(this, "Эта игра уже есть в очереди", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        android.widget.Toast.makeText(this, "Игра добавлена в очередь", android.widget.Toast.LENGTH_SHORT).show()
        if (downloadingGameId == null && !testInstallInProgress) startNextQueuedDownload()
        if (currentPage == "downloads") renderDownloads()
        if (currentPage == "catalog") renderCatalogResults()
    }

    private fun startNextQueuedDownload() {
        if (downloadingGameId != null || testInstallInProgress || !::downloadQueueStore.isInitialized) return
        val next = downloadQueueStore.getNextPending() ?: return
        startDownloadNow(next.game)
    }

    private fun startDownloadNow(game: CatalogGame) {
        if (downloadingGameId != null || testInstallInProgress) return
        downloadQueueStore.update(game.gameId, DownloadQueueStore.STATUS_DOWNLOADING, 0, "Загрузка")

        val cancellationToken = DownloadCancellationToken()
        activeDownloadToken = cancellationToken
        downloadCancelRequested = false
        downloadIsInstalling = false
        downloadAttempt = 1
        downloadingGameId = game.gameId
        downloadProgressPercent = 0
        renderCatalogResults()

        downloadExecutor.execute {
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
                                    downloadQueueStore.update(
                                        game.gameId,
                                        DownloadQueueStore.STATUS_DOWNLOADING,
                                        100,
                                        "Проверка и установка…"
                                    )
                                    if (currentPage == "downloads") renderDownloads()
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
                                    downloadQueueStore.update(
                                        game.gameId,
                                        DownloadQueueStore.STATUS_DOWNLOADING,
                                        percent,
                                        "Загрузка пакета"
                                    )
                                    if (currentPage == "downloads") renderDownloads()
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
                    downloadQueueStore.update(
                        game.gameId,
                        DownloadQueueStore.STATUS_COMPLETED,
                        100,
                        "Установлено"
                    )
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
                    startNextQueuedDownload()
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
                    downloadQueueStore.update(
                        game.gameId,
                        if (wasCancelled) DownloadQueueStore.STATUS_CANCELLED else DownloadQueueStore.STATUS_FAILED,
                        0,
                        if (wasCancelled) "Отменено" else (error.message ?: "Ошибка загрузки")
                    )
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
                    if (currentPage == "downloads") renderDownloads()
                    startNextQueuedDownload()
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
            if (
                current is TransientDownloadException ||
                current is EOFException ||
                current is ConnectException ||
                current is SocketException ||
                current is SocketTimeoutException ||
                current is UnknownHostException ||
                current is SSLException
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun renderDownloads() {
        pageHeading("Загрузки", "Очередь установок, текущий прогресс и история операций.")
        if (!::downloadQueueStore.isInitialized) {
            pageContent.addView(emptyState("Очередь недоступна", "Локальное хранилище загрузок не инициализировано."))
            return
        }

        val entries = downloadQueueStore.getEntries().sortedByDescending { it.updatedAt }
        if (entries.isEmpty()) {
            pageContent.addView(emptyState("Очередь пуста", "Добавляй игры в очередь из каталога. Загрузки будут выполняться по одной."))
            pageContent.addView(actionCard("Открыть каталог", "Выбрать игру для установки", "01") { navigateTo("catalog") })
            return
        }

        val activeCount = entries.count { it.status == DownloadQueueStore.STATUS_DOWNLOADING }
        val pendingCount = entries.count { it.status == DownloadQueueStore.STATUS_PENDING }
        pageContent.addView(label("АКТИВНО: $activeCount  •  В ОЧЕРЕДИ: $pendingCount", 10f, accent, true).apply {
            setPadding(0, 0, 0, dp(12))
        })
        entries.forEach { entry ->
            val card = panel().apply { setPadding(dp(15), dp(14), dp(15), dp(14)) }
            card.addView(label(entry.game.name, 16f, textMain, true))
            card.addView(label(
                entry.game.gameType.uppercase() + " • " + entry.game.engine + " • " + entry.game.version,
                10f, accent, true
            ).apply { setPadding(0, dp(5), 0, dp(8)) })
            val statusText = when (entry.status) {
                DownloadQueueStore.STATUS_PENDING -> "Ожидает загрузки"
                DownloadQueueStore.STATUS_DOWNLOADING -> if (downloadingGameId == entry.game.gameId && downloadIsInstalling) "Проверка и установка…" else "Загрузка: ${entry.progressPercent}%"
                DownloadQueueStore.STATUS_COMPLETED -> "Установлено"
                DownloadQueueStore.STATUS_FAILED -> "Ошибка загрузки"
                DownloadQueueStore.STATUS_CANCELLED -> "Отменено"
                else -> entry.status
            }
            card.addView(label(statusText, 12f, if (entry.status == DownloadQueueStore.STATUS_COMPLETED) green else textMain, true))
            if (entry.status == DownloadQueueStore.STATUS_DOWNLOADING) {
                val progress = android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 100
                    this.progress = entry.progressPercent
                    progressTintList = android.content.res.ColorStateList.valueOf(accent)
                    progressBackgroundTintList = android.content.res.ColorStateList.valueOf(surfaceAlt)
                }
                val progressParams = LinearLayout.LayoutParams(-1, dp(6))
                progressParams.topMargin = dp(10)
                card.addView(progress, progressParams)
            }
            if (entry.message.isNotBlank()) {
                card.addView(label(entry.message, 11f, textMuted, false).apply { setPadding(0, dp(7), 0, 0) })
            }
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            when (entry.status) {
                DownloadQueueStore.STATUS_DOWNLOADING -> {
                    if (downloadingGameId == entry.game.gameId && downloadIsInstalling) {
                        actions.addView(label("Отмена недоступна во время установки", 11f, textMuted, false))
                    } else {
                        actions.addView(filterChip("Отменить", false) { cancelGameDownload() }, LinearLayout.LayoutParams(0, dp(38), 1f))
                    }
                }
                DownloadQueueStore.STATUS_FAILED -> {
                    actions.addView(filterChip("Повторить", true) { retryQueuedDownload(entry.game.gameId) }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { rightMargin = dp(6) })
                    actions.addView(filterChip("Убрать", false) { removeQueuedDownload(entry.game.gameId) }, LinearLayout.LayoutParams(0, dp(38), 1f))
                }
                else -> {
                    actions.addView(filterChip("Убрать", false) { removeQueuedDownload(entry.game.gameId) }, LinearLayout.LayoutParams(0, dp(38), 1f))
                }
            }
            val actionParams = LinearLayout.LayoutParams(-1, -2)
            actionParams.topMargin = dp(12)
            card.addView(actions, actionParams)
            val cardParams = LinearLayout.LayoutParams(-1, -2)
            cardParams.bottomMargin = dp(10)
            pageContent.addView(card, cardParams)
        }
    }

    private fun retryQueuedDownload(gameId: String) {
        if (!::downloadQueueStore.isInitialized || !downloadQueueStore.retry(gameId)) return
        startNextQueuedDownload()
        renderDownloads()
    }

    private fun removeQueuedDownload(gameId: String) {
        if (!::downloadQueueStore.isInitialized) return
        downloadQueueStore.remove(gameId)
        renderDownloads()
    }

    private fun renderLibrary() {
        pageHeading("Мои игры", "Установленные игры и локальные сохранения.")

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
                "Установи игру из каталога — она появится здесь. Сохранения будут храниться отдельно от файлов игры."
            ))
            return
        }

        pageContent.addView(label(
            "УСТАНОВЛЕНО ИГР: ${installedGames.size}",
            10f,
            accent,
            true
        ).apply {
            letterSpacing = 0.1f
            setPadding(0, 0, 0, dp(10))
        })

        val searchField = EditText(this).apply {
            setSingleLine(true)
            setText(librarySearchQuery)
            hint = "Поиск по названию или движку"
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
                librarySearchQuery = s?.toString().orEmpty()
                renderLibraryResults(installedGames)
            }
        })
        val searchParams = LinearLayout.LayoutParams(-1, -2)
        searchParams.bottomMargin = dp(12)
        pageContent.addView(searchField, searchParams)

        val results = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            tag = "library_results"
        }
        pageContent.addView(results)
        renderLibraryResults(installedGames, results)
    }

    private fun renderLibraryResults(
        installedGames: List<InstalledGame>,
        container: LinearLayout? = null
    ) {
        val target = container ?: pageContent.findViewWithTag<LinearLayout>("library_results")
            ?: return
        target.removeAllViews()
        val query = librarySearchQuery.trim()
        val filteredGames = installedGames.filter { game ->
            query.isEmpty() ||
                game.name.contains(query, ignoreCase = true) ||
                game.engine.contains(query, ignoreCase = true) ||
                game.gameId.contains(query, ignoreCase = true)
        }

        if (filteredGames.isEmpty()) {
            target.addView(emptyState(
                "Ничего не найдено",
                "Измени запрос и попробуй ещё раз."
            ))
            return
        }

        filteredGames.forEach { game ->
            target.addView(gameCard(game))
        }
    }

    private fun gameCard(game: InstalledGame): LinearLayout {
        val card = panel().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), dp(14), dp(11), dp(14))
        }

        val typeLabel = when {
            game.gameType.contains("3d", ignoreCase = true) -> "3D"
            game.gameType.contains("2d", ignoreCase = true) -> "2D"
            else -> "HTML"
        }
        val gameIcon = TextView(this).apply {
            text = typeLabel
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(220, 225, 255))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(68, 57, 133), Color.rgb(30, 49, 82))
            ).apply { cornerRadius = dp(13).toFloat() }
        }
        card.addView(gameIcon, LinearLayout.LayoutParams(dp(48), dp(48)).apply {
            rightMargin = dp(12)
        })

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
            game.engine + " • " + game.version,
            11f,
            textMuted,
            false
        ).apply { setPadding(0, dp(5), 0, dp(4)) })
        info.addView(label("НАЖМИ, ЧТОБЫ ЗАПУСТИТЬ", 9f, accent, true))
        card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))

        val remove = TextView(this).apply {
            text = "Удалить"
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(255, 151, 151))
            background = rounded(Color.rgb(61, 35, 48), 9)
            setPadding(dp(9), dp(9), dp(9), dp(9))
            isClickable = true
            isFocusable = true
            setOnClickListener { confirmGameRemoval(game) }
        }
        card.addView(remove)
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = dp(10)
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

    private fun renderSettings() {
        pageHeading("Настройки", "Состояние платформы и инструменты разработчика.")
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

        sectionTitle("Возможности платформы")
        pageContent.addView(detailRow("Офлайн-режим", "Игры должны запускаться без интернета после установки."))
        pageContent.addView(detailRow("Сохранения", "Сохранения хранятся отдельно от файлов игры."))
        pageContent.addView(detailRow("Целевые устройства", "Основной профиль — современные телефоны среднего класса и выше. Графика масштабируется под возможности устройства."))
        pageContent.addView(label(
            "Дополнительные параметры графики и производительности будут доступны отдельно и не будут искусственно ограничивать мощные устройства.",
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
            includeFontPadding = false
            maxLines = 2
            setPadding(dp(2), dp(5), dp(2), dp(4))
            isClickable = true
            isFocusable = true
            contentDescription = title.substringAfter('\n', title)
            val divider = title.indexOf('\n')
            if (divider > 0) {
                val styled = android.text.SpannableString(title)
                styled.setSpan(android.text.style.RelativeSizeSpan(1.65f), 0, divider, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                styled.setSpan(android.text.style.StyleSpan(Typeface.BOLD), divider + 1, title.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                text = styled
            } else {
                text = title
            }
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
        downloadExecutor.shutdownNow()
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
