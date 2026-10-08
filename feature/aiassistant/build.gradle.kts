// feature:aiassistant —— AI 评分助手（纯离线提示词模板 + 一键复制）
//
// 刻意不引入任何网络库或 AI SDK：本功能只是把内置的提示词模板
// 复制到系统剪贴板，由用户在外部 AI 中自行粘贴使用。
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// 本模块只提供「公共部分」：UI、状态、提示词组装。
// 专业模式（API 直连）依赖网络库与 INTERNET 权限，
// 放在 app 模块的 online sourceSet 里 —— library 模块没有 flavor 概念，无法这样隔离。
android {
    namespace = "com.cetsix.feature.aiassistant"
    compileSdk = 36
    defaultConfig { minSdk = 26 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:domain"))   // 复用 ExamType / PromptAssembler
    implementation(project(":core:ui"))      // GlassCard / GlassBackground

    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
