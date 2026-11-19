package com.warp.game

data class InstalledGame(
    val gameId: String,
    val name: String,
    val version: String,
    val engine: String,
    val engineVersion: String,
    val gameType: String,
    val entryPoint: String,
    val orientation: String,
    val offlineSupported: Boolean,
    val minimumPlatformVersion: String,
    val installDirectory: String,
    val packageSha256: String,
    val installedAt: Long,
    val updatedAt: Long
)
