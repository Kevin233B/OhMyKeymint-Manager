@file:Suppress("UnstableApiUsage")

import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = 37
    namespace = "com.kevin233.omkmanager"

    defaultConfig {
        applicationId = "com.kevin233.omkmanager"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            val props = Properties()
            val f = rootProject.file("keystore.properties")
            if (f.exists()) f.inputStream().use { props.load(it) }
            storeFile = props.getProperty("storeFile")?.let { rootProject.file(it) }
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles("proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    // Miuix UI (MIUI / HyperOS 风格)
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.4")

    // Compose
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation-android:1.12.1")
    implementation("androidx.compose.ui:ui-android:1.12.1")

    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Android 安全公告页面解析（与 WebUI 前端一致）
    implementation("org.jsoup:jsoup:1.18.1")
}
