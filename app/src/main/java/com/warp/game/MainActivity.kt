package com.warp.game

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

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
    private val pageHistory = mutableListOf<String>()
    private lateinit var platformStorage: PlatformStorage
    private lateinit var warpDatabase: WarpDatabase

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

    private fun navigateTo(page: String) {
        if (page == currentPage) return
        pageHistory.add(currentPage)
        currentPage = page
        renderPage(true)
    }

    private fun goBack() {
        if (pageHistory.isNotEmpty()) {
            currentPage = pageHistory.removeAt(pageHistory.lastIndex)
        } else {
            currentPage = "home"
        }
        renderPage(true)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (currentPage != "home" || pageHistory.isNotEmpty()) {
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
        pageContent.addView(label("WARP GAME  •  VERSION 0.1.0", 10f, textMuted, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(4))
        })
    }

    private fun renderCatalog() {
        pageHeading("Каталог игр", "Находи новые игры для своей библиотеки.")
        val empty = emptyState(
            "Каталог готовится",
            "Здесь появятся карточки игр, описание, размер, версия и кнопка установки. Сейчас источник каталога ещё не подключён."
        )
        pageContent.addView(empty)
        pageContent.addView(label(
            "Планируемая поддержка: 2D и 3D • загрузка • обновления • офлайн-запуск",
            12f, textMuted, false
        ).apply { setPadding(dp(4), dp(16), dp(4), 0) })
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

        val installedGames = try {
            warpDatabase.getInstalledGames()
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
            pageContent.addView(detailRow(
                game.name,
                "Версия " + game.version + " • " + game.gameType.uppercase() + " • " + game.engine
            ))
        }
    }

    private fun renderSettings() {
        pageHeading("Настройки", "Информация о приложении и его состоянии.")
        val appCard = panel()
        appCard.setPadding(dp(16), dp(16), dp(16), dp(16))
        appCard.addView(label("WARP GAME", 18f, textMain, true))
        appCard.addView(label("Версия приложения: 0.1.0", 13f, textMuted, false).apply {
            setPadding(0, dp(8), 0, 0)
        })
        appCard.addView(label("Платформа: Android", 13f, textMuted, false).apply {
            setPadding(0, dp(5), 0, 0)
        })
        pageContent.addView(appCard)

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
        if (::warpDatabase.isInitialized) {
            warpDatabase.close()
        }
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
