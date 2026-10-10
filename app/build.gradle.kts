import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ============================================================
// Release 签名配置
//
// 密钥信息放在项目根目录的 keystore.properties（不提交版本库）。
// 该文件缺失时 release 会退化为未签名产物，仍可正常 assembleRelease，
// 便于 CI 或他人克隆仓库后先跑通构建。
// ============================================================
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { stream -> load(stream) }
    }
}
val hasReleaseSigning = keystoreProps.getProperty("storeFile")?.let { path ->
    file(path).exists()
} ?: false

android {
    namespace = "com.cetscore.score"

    // compileSdk 用 36（Haze/Vico/Room 等库要求），
    // 但 targetSdk 按需求保持 34 —— 两者不冲突，compileSdk 只是编译期 API 面。
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cetscore.score"
        minSdk = 26          // Android 8.0
        targetSdk = 34
        versionCode = 2
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
                // v1 兼容 Android 7 及以下，v2/v3 覆盖 8.0 以上的现代校验
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
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
            // 混淆压缩：release 开启，debug 关闭便于排查
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 有密钥时用正式签名，无密钥时仍可构建（产出未签名包）
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
