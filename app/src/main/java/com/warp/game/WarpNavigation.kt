package com.warp.game

import android.app.Activity
import android.content.Intent

object WarpNavigation {
    fun returnToHome(activity: Activity) {
        val intent = Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_RETURN_HOME, true)
        }
        activity.startActivity(intent)
        activity.finish()
    }
}
