package com.warp.game

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private val backgroundColor = Color.rgb(12, 14, 20)
    private val panelColor = Color.rgb(23, 27, 37)
    private val accentColor = Color.rgb(116, 92, 255)
    private val mutedColor = Color.rgb(151, 159, 178)
    private lateinit var content: LinearLayout
    private lateinit var catalogTab: TextView
    private lateinit var libraryTab: TextView
    private lateinit var settingsTab: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        window.decorView.systemUiVisibility = 0
        showHome()
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            setPadding(dp(20), dp(18), dp(20), dp(10))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        header.addView(label("WARP GAME", 25f, Color.WHITE, true))
        header.addView(label("ТВОЯ ИГРОВАЯ ПЛАТФОРМА", 10f, mutedColor, true).apply {
            letterSpacing = 0.12f
            setPadding(0, dp(4), 0, 0)
        })
        root.addView(header)

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(26), 0, dp(20))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        content.addView(label("Играй без ограничений", 26f, Color.WHITE, true))
        content.addView(label(
            "Находи игры, устанавливай и запускай даже без интернета.",
            14f, mutedColor, false
        ).apply { setPadding(0, dp(8), 0, dp(22)) })

        val hero = panel()
        hero.setPadding(dp(18), dp(20), dp(18), dp(20))
        hero.addView(label("WARP", 13f, Color.rgb(183, 170, 255), true))
        hero.addView(label("Твой мир игр", 23f, Color.WHITE, true).apply {
            setPadding(0, dp(8), 0, dp(6))
        })
        hero.addView(label(
            "Здесь будут собраны лучшие 2D- и 3D-игры.",
            14f, mutedColor, false
        ))
        hero.addView(label("ПЛАТФОРМА ЗАПУЩЕНА", 10f, Color.rgb(112, 220, 166), true).apply {
            setPadding(0, dp(22), 0, 0)
        })
        content.addView(hero)

        content.addView(label("Разделы", 19f, Color.WHITE, true).apply {
            setPadding(0, dp(26), 0, dp(12))
        })

        val catalogCard = panel().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val catalogInfo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        catalogInfo.addView(label("Каталог игр", 17f, Color.WHITE, true))
        catalogInfo.addView(label("Поиск и установка игр", 12f, mutedColor, false).apply {
            setPadding(0, dp(5), 0, 0)
        })
        catalogCard.addView(catalogInfo, LinearLayout.LayoutParams(0, -2, 1f))
        catalogCard.addView(label("›", 30f, Color.WHITE, false))
        catalogCard.setOnClickListener { showPage("catalog") }
        content.addView(catalogCard)

        content.addView(label(
            "Сейчас это базовая версия интерфейса. Каталог, загрузка игр и запуск игр будут добавляться следующими этапами.",
            12f, mutedColor, false
        ).apply { setPadding(dp(2), dp(18), dp(2), 0) })

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(4))
        }
        catalogTab = navItem("Каталог")
        libraryTab = navItem("Мои игры")
        settingsTab = navItem("Настройки")
        catalogTab.setOnClickListener { showPage("catalog") }
        libraryTab.setOnClickListener { showPage("library") }
        settingsTab.setOnClickListener { showPage("settings") }
        nav.addView(catalogTab, LinearLayout.LayoutParams(0, dp(48), 1f))
        nav.addView(libraryTab, LinearLayout.LayoutParams(0, dp(48), 1f))
        nav.addView(settingsTab, LinearLayout.LayoutParams(0, dp(48), 1f))
        root.addView(nav)

        setContentView(root)
        selectTab("home")
    }

    private fun showPage(page: String) {
        if (!::content.isInitialized) return
        content.removeAllViews()

        val title: String
        val description: String
        when (page) {
            "catalog" -> {
                title = "Каталог игр"
                description = "Здесь появятся доступные для загрузки игры. Сейчас каталог ещё не подключён."
            }
            "library" -> {
                title = "Мои игры"
                description = "Установленные игры будут отображаться здесь. Пока игр нет."
            }
            else -> {
                title = "Настройки"
                description = "Настройки платформы будут добавлены на следующем этапе разработки."
            }
        }

        content.addView(label(title, 26f, Color.WHITE, true))
        val card = panel().apply {
            setPadding(dp(18), dp(20), dp(18), dp(20))
        }
        card.addView(label("РАЗДЕЛ В РАЗРАБОТКЕ", 10f, Color.rgb(183, 170, 255), true))
        card.addView(label(description, 15f, Color.WHITE, false).apply {
            setPadding(0, dp(12), 0, 0)
        })
        content.addView(card.apply {
            val params = LinearLayout.LayoutParams(-1, -2)
            params.topMargin = dp(20)
            layoutParams = params
        })
        selectTab(page)
    }

    private fun navItem(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(mutedColor)
        }
    }

    private fun selectTab(page: String) {
        catalogTab.setTextColor(if (page == "catalog" || page == "home") Color.WHITE else mutedColor)
        libraryTab.setTextColor(if (page == "library") Color.WHITE else mutedColor)
        settingsTab.setTextColor(if (page == "settings") Color.WHITE else mutedColor)
    }

    private fun panel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panelColor, 18)
        }
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
