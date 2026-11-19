package com.warp.game

data class CatalogGame(
    val gameId: String,
    val name: String,
    val description: String,
    val version: String,
    val engine: String,
    val engineVersion: String,
    val gameType: String,
    val orientation: String,
    val packageUrl: String,
    val packageSha256: String,
    val packageSizeBytes: Long,
    val minimumPlatformVersion: String,
    val offlineSupported: Boolean
)

data class CatalogSnapshot(
    val games: List<CatalogGame>,
    val fromCache: Boolean
)
