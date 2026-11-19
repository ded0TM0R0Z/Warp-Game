package com.warp.game

import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream
import java.io.File

class GameDirectoryPathHandler(
    gameDirectory: File
) : WebViewAssetLoader.PathHandler {
    private val root = gameDirectory.canonicalFile
    private val rootPrefix = root.path + File.separator

    override fun handle(path: String): WebResourceResponse {
        if (
            path.isBlank() ||
            path.contains('\\') ||
            path.split("/").any { it.isBlank() || it == "." || it == ".." }
        ) {
            return errorResponse(403, "Forbidden")
        }

        val target = File(root, path).canonicalFile
        if (
            target.path != root.path &&
            !target.path.startsWith(rootPrefix)
        ) {
            return errorResponse(403, "Forbidden")
        }
        if (!target.isFile) {
            return errorResponse(404, "Not Found")
        }

        val mimeType = mimeTypeFor(target.name)
        val encoding = if (
            mimeType.startsWith("text/") ||
            mimeType.contains("javascript") ||
            mimeType == "application/json" ||
            mimeType == "image/svg+xml"
        ) "UTF-8" else null

        return try {
            WebResourceResponse(mimeType, encoding, target.inputStream())
        } catch (error: Exception) {
            WarpLogger.warning("GameRuntime", "Could not open a local game resource", error)
            errorResponse(404, "Not Found")
        }
    }

    private fun mimeTypeFor(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js", "mjs" -> "application/javascript"
            "json", "map" -> "application/json"
            "xml" -> "application/xml"
            "txt", "glsl", "vert", "frag", "obj", "mtl" -> "text/plain"
            "gltf" -> "model/gltf+json"
            "glb" -> "model/gltf-binary"
            "bin" -> "application/octet-stream"
            "ktx" -> "image/ktx"
            "ktx2" -> "image/ktx2"
            "basis" -> "application/octet-stream"
            "dds" -> "image/vnd-ms.dds"
            "hdr", "env" -> "image/vnd.radiance"
            "svg" -> "image/svg+xml"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "ico" -> "image/x-icon"
            "wasm" -> "application/wasm"
            "mp3" -> "audio/mpeg"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            "m4a", "aac" -> "audio/mp4"
            "flac" -> "audio/flac"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "eot" -> "application/vnd.ms-fontobject"
            "ttf" -> "font/ttf"
            "otf" -> "font/otf"
            else -> "application/octet-stream"
        }
    }

    private fun errorResponse(status: Int, reason: String): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            status,
            reason,
            emptyMap(),
            ByteArrayInputStream(reason.toByteArray())
        )
    }
}
