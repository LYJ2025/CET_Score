// core:data —— 数据层：Room 数据库、Entity、DAO、Repository
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.cetscore.core.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testOptions {
            unitTests {
                // Room DAO 单元测试跑在 JVM 上，需要 Robolectric 提供 Android 运行时
                isIncludeAndroidResources = true
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
}

dependencies {
    implementation(project(":core:domain"))   // ExamType 统一在 domain 层，避免两份定义
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Room —— KSP 生成 DAO 实现
    // 用 api 而非 implementation：CetScoreDatabase 继承 RoomDatabase（公开父类），
    // 下游模块直接调用 Room.databaseBuilder()，必须能看到 Room 的类型。
    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // detailJson 序列化
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}
