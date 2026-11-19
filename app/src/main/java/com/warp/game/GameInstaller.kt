package com.warp.game

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream

class GameInstaller(
    private val storage: PlatformStorage,
    private val database: WarpDatabase
) {
    fun installPackage(
        packageStream: InputStream,
        expectedSha256: String
    ): InstalledGame {
        if (!SHA256_PATTERN.matches(expectedSha256)) {
            throw GamePackageException("A trusted SHA-256 checksum is required")
        }

        storage.initialize()
        val packageFile = File(storage.cacheDirectory, "package-" + UUID.randomUUID() + ".zip")
        val stagingDirectory = File(storage.gamesDirectory, ".staging-" + UUID.randomUUID())
        var backupDirectory: File? = null
        var targetDirectory: File? = null
        var promotedDirectory: File? = null
        var previousDirectoryBackedUp = false

        try {
            val actualSha256 = copyAndHash(packageStream, packageFile)
            if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
                throw GamePackageException("Package SHA-256 checksum does not match")
            }

            if (!stagingDirectory.mkdirs()) {
                throw GamePackageException("Could not create temporary installation directory")
            }

            val manifest = extractAndValidate(packageFile, stagingDirectory)
            val entryFile = resolveInside(stagingDirectory, manifest.entryPoint)
            if (!entryFile.isFile) {
                throw GamePackageException("Game entry point does not exist")
            }

            val finalDirectory = storage.gameDirectory(manifest.gameId)
            targetDirectory = finalDirectory
            val existingGame = database.getInstalledGame(manifest.gameId)
            if (finalDirectory.exists()) {
                val backup = File(
                    storage.gamesDirectory,
                    ".backup-" + UUID.randomUUID()
                )
                if (!finalDirectory.renameTo(backup)) {
                    throw GamePackageException("Could not preserve the previous game version")
                }
                backupDirectory = backup
                previousDirectoryBackedUp = true
            }

            if (!stagingDirectory.renameTo(finalDirectory)) {
                throw GamePackageException("Could not activate the installed game")
            }
            promotedDirectory = finalDirectory

            val now = System.currentTimeMillis()
            val installedGame = InstalledGame(
                gameId = manifest.gameId,
                name = manifest.name,
                version = manifest.version,
                engine = manifest.engine,
                engineVersion = manifest.engineVersion,
                gameType = manifest.gameType,
                entryPoint = manifest.entryPoint,
                orientation = manifest.orientation,
                offlineSupported = manifest.offlineSupported,
                minimumPlatformVersion = manifest.minimumPlatformVersion,
                installDirectory = manifest.gameId,
                packageSha256 = actualSha256,
                installedAt = existingGame?.installedAt ?: now,
                updatedAt = now
            )

            database.saveInstalledGame(installedGame)

            backupDirectory?.let { backup ->
                try {
                    if (backup.exists() && !backup.deleteRecursively()) {
                        WarpLogger.warning("Installer", "Previous game backup could not be fully removed")
                    }
                } catch (cleanupError: Exception) {
                    WarpLogger.warning("Installer", "Previous game backup cleanup failed", cleanupError)
                }
            }
            return installedGame
        } catch (error: Exception) {
            promotedDirectory?.let { directory ->
                if (directory.exists() && !directory.deleteRecursively()) {
                    val failedDirectory = File(
                        storage.gamesDirectory,
                        ".failed-" + UUID.randomUUID()
                    )
                    if (directory.renameTo(failedDirectory)) {
                        failedDirectory.deleteRecursively()
                    } else {
                        WarpLogger.error("Installer", "Could not clear failed installation directory")
                    }
                }
            }
            if (previousDirectoryBackedUp) {
                val backup = backupDirectory
                val target = targetDirectory
                if (backup != null && backup.exists() && target != null) {
                    if (!backup.renameTo(target)) {
                        WarpLogger.error("Installer", "Could not restore previous game directory")
                    }
                }
            }
            if (error is GamePackageException) throw error
            throw GamePackageException("Game installation failed", error)
        } finally {
            if (packageFile.exists()) packageFile.delete()
            if (stagingDirectory.exists()) stagingDirectory.deleteRecursively()
        }
    }

    private fun copyAndHash(input: InputStream, destination: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var totalBytes = 0L
        val buffer = ByteArray(BUFFER_SIZE)

        FileOutputStream(destination).use { output ->
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count == 0) continue
                totalBytes += count
                if (totalBytes > MAX_ARCHIVE_BYTES) {
                    throw GamePackageException("Game package exceeds the archive size limit")
                }
                digest.update(buffer, 0, count)
                output.write(buffer, 0, count)
            }
        }

        return digest.digest().joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }

    private fun extractAndValidate(
        archiveFile: File,
        destination: File
    ): GamePackageManifest {
        val root = destination.canonicalFile
        val rootPrefix = root.path + File.separator
        val seenPaths = mutableSetOf<String>()
        var entryCount = 0
        var totalExpandedBytes = 0L
        val extractionBuffer = ByteArray(BUFFER_SIZE)

        ZipInputStream(FileInputStream(archiveFile)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount += 1
                if (entryCount > MAX_ENTRIES) {
                    throw GamePackageException("Game package contains too many files")
                }

                val isDirectory = entry.isDirectory
                val rawPath = if (isDirectory) entry.name.removeSuffix("/") else entry.name
                GamePackageManifestParser.validateRelativePath(rawPath)

                if (!seenPaths.add(rawPath)) {
                    throw GamePackageException("Game package contains duplicate paths")
                }

                val outputFile = File(root, rawPath).canonicalFile
                if (!outputFile.path.startsWith(rootPrefix)) {
                    throw GamePackageException("Archive entry escapes the installation directory")
                }

                if (isDirectory) {
                    if (!outputFile.exists() && !outputFile.mkdirs()) {
                        throw GamePackageException("Could not create package directory")
                    }
                    if (!outputFile.isDirectory) {
                        throw GamePackageException("Package directory conflicts with a file")
                    }
                } else {
                    if (entry.size > MAX_ENTRY_BYTES) {
                        throw GamePackageException("Package file exceeds the file size limit")
                    }
                    val parent = outputFile.parentFile
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw GamePackageException("Could not create package directory")
                    }
                    if (parent != null && !parent.isDirectory) {
                        throw GamePackageException("Package path conflicts with a file")
                    }

                    var entryBytes = 0L
                    FileOutputStream(outputFile).use { output ->
                        while (true) {
                            val count = zip.read(extractionBuffer)
                            if (count < 0) break
                            if (count == 0) continue
                            entryBytes += count
                            totalExpandedBytes += count
                            if (entryBytes > MAX_ENTRY_BYTES) {
                                throw GamePackageException("Package file exceeds the file size limit")
                            }
                            if (totalExpandedBytes > MAX_EXPANDED_BYTES) {
                                throw GamePackageException("Expanded game package is too large")
                            }
                            if (rawPath == MANIFEST_FILE && entryBytes > MAX_MANIFEST_BYTES) {
                                throw GamePackageException("manifest.json is too large")
                            }
                            output.write(extractionBuffer, 0, count)
                        }
                    }
                }
                zip.closeEntry()
            }
        }

        val manifestFile = File(root, MANIFEST_FILE)
        if (!manifestFile.isFile || manifestFile.length() > MAX_MANIFEST_BYTES) {
            throw GamePackageException("Root manifest.json is missing or too large")
        }

        val manifest = GamePackageManifestParser.parse(
            manifestFile.readText(Charsets.UTF_8)
        )
        val entryPoint = resolveInside(root, manifest.entryPoint)
        if (!entryPoint.isFile) {
            throw GamePackageException("Manifest entry_point does not exist")
        }
        return manifest
    }

    private fun resolveInside(root: File, relativePath: String): File {
        GamePackageManifestParser.validateRelativePath(relativePath)
        val canonicalRoot = root.canonicalFile
        val target = File(canonicalRoot, relativePath).canonicalFile
        if (!target.path.startsWith(canonicalRoot.path + File.separator)) {
            throw GamePackageException("Package path escapes its root directory")
        }
        return target
    }

    private companion object {
        const val MANIFEST_FILE = "manifest.json"
        const val BUFFER_SIZE = 8192
        const val MAX_ARCHIVE_BYTES = 256L * 1024L * 1024L
        const val MAX_EXPANDED_BYTES = 512L * 1024L * 1024L
        const val MAX_ENTRY_BYTES = 128L * 1024L * 1024L
        const val MAX_MANIFEST_BYTES = 64L * 1024L
        const val MAX_ENTRIES = 10000

        val SHA256_PATTERN = Regex("[A-Fa-f0-9]{64}")
    }
}
