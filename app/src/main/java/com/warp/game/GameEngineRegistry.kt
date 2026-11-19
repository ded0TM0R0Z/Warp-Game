package com.warp.game

import java.io.File

object GameEngineRegistry {
    const val HTML5 = "html5"
    const val PHASER = "phaser"
    const val BABYLON = "babylonjs"
    const val CUSTOM = "custom"

    const val PHASER_VERSION = "3.90.0"
    const val BABYLON_VERSION = "9.37.1"

    fun validatePackage(manifest: GamePackageManifest, packageRoot: File) {
        when (manifest.engine.lowercase()) {
            HTML5, CUSTOM -> Unit

            PHASER -> {
                if (manifest.engineVersion != PHASER_VERSION) {
                    throw GamePackageException(
                        "Unsupported Phaser version. Required: $PHASER_VERSION"
                    )
                }
                requireBundledLibrary(packageRoot, "libraries/phaser.min.js")
            }

            BABYLON -> {
                if (manifest.engineVersion != BABYLON_VERSION) {
                    throw GamePackageException(
                        "Unsupported Babylon.js version. Required: $BABYLON_VERSION"
                    )
                }
                requireBundledLibrary(packageRoot, "libraries/babylon.js")
            }

            else -> throw GamePackageException("Unsupported game engine: " + manifest.engine)
        }
    }

    private fun requireBundledLibrary(root: File, relativePath: String) {
        GamePackageManifestParser.validateRelativePath(relativePath)
        val canonicalRoot = root.canonicalFile
        val library = File(canonicalRoot, relativePath).canonicalFile
        if (
            !library.path.startsWith(canonicalRoot.path + File.separator) ||
            !library.isFile
        ) {
            throw GamePackageException("Required offline engine library is missing: $relativePath")
        }
    }
}
