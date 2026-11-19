plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val warpKeystorePath = System.getenv("WARP_KEYSTORE_FILE")

android {
    namespace = "com.warp.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.warp.game"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (!warpKeystorePath.isNullOrBlank()) {
            create("warp") {
                storeFile = file(warpKeystorePath)
                storePassword = System.getenv("WARP_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("WARP_KEY_ALIAS")
                keyPassword = System.getenv("WARP_KEY_PASSWORD")
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        debug {
            if (!warpKeystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("warp")
            }
        }
        release {
            isMinifyEnabled = false
            if (!warpKeystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("warp")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
