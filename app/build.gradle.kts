plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.cetsix.score"

    // compileSdk 用 36（Haze/Vico/Room 等库要求），
    // 但 targetSdk 按需求保持 34 —— 两者不冲突，compileSdk 只是编译期 API 面。
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cetsix.score"
        minSdk = 26          // Android 8.0
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    // ============================================================
    // 单一 App：普通模式与专业模式在 AI 助手内部切换
    //
    // 普通模式 —— 生成提示词并复制到剪贴板，全程不联网
    // 专业模式 —— 直连第三方 API 做流式评分（用户主动选择后才联网）
    //
    // 两种模式共存于同一个 APK，由用户在 AI 助手里切换，
    // 不拆成两个包 —— 拆包会导致用户装错版本、也割裂了设置项。
    // ============================================================

    buildFeatures {
        compose = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))

    implementation(project(":feature:home"))
    implementation(project(":feature:aiassistant"))
    implementation(project(":feature:assessment"))
    implementation(project(":feature:result"))
    implementation(project(":feature:history"))
    implementation(project(":feature:trend"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose —— 版本由 BOM 统一管理，此处不写 version
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
