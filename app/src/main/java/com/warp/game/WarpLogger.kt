package com.warp.game

import android.util.Log

object WarpLogger {
    private const val TAG_PREFIX = "WarpGame"

    fun debug(component: String, message: String) {
        Log.d(tag(component), message)
    }

    fun info(component: String, message: String) {
        Log.i(tag(component), message)
    }

    fun warning(component: String, message: String, error: Throwable? = null) {
        if (error == null) {
            Log.w(tag(component), message)
        } else {
            Log.w(tag(component), message, error)
        }
    }

    fun error(component: String, message: String, error: Throwable? = null) {
        if (error == null) {
            Log.e(tag(component), message)
        } else {
            Log.e(tag(component), message, error)
        }
    }

    private fun tag(component: String): String {
        val safeComponent = component
            .filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            .take(20)
            .ifBlank { "Core" }
        return "$TAG_PREFIX-$safeComponent"
    }
}
