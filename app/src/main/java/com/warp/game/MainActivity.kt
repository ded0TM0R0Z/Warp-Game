package com.warp.game

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val welcomeText = TextView(this).apply {
            text = "WARP GAME\n\nPlatform foundation"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        setContentView(welcomeText)
    }
}
