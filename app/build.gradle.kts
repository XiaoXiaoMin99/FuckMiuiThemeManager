@file:Suppress("UnstableApiUsage")
@file:SuppressLint("ChromeOsAbiSupport")

import android.annotation.SuppressLint
import com.android.build.gradle.internal.api.BaseVariantOutputImpl

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 34
    buildToolsVersion = "34.0.0"
    namespace = "com.yuk.fuckMiuiThemeManager"
    defaultConfig {
        applicationId = namespace
        minSdk = 28
        targetSdk = 34
        versionCode = 18
        versionName = "1.8.1"
        ndk.abiFilters += "arm64-v8a"
        ndk.abiFilters += "armeabi-v7a"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            setProguardFiles(listOf("proguard-rules.pro", "proguard-log.pro"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }
    packaging {
        resources {
            excludes += "/META-INF/**"
            excludes += "/kotlin/**"
            excludes += "/*.json"
        }
        dex {
            useLegacyPackaging = true
        }
        applicationVariants.all {
            outputs.all {
                (this as BaseVariantOutputImpl).outputFileName =
                    "Unlock-MIUIThemeManager For Qiqi-$versionName.apk"
            }
        }
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    implementation("com.github.kyuubiran:EzXHelper:2.0.7")
    implementation("org.luckypray:DexKit:1.1.8")
}