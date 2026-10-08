// core:domain —— 纯 Kotlin 估分算法层。
// 刻意用 kotlin("jvm") 而非 android-library：估分是纯计算，无需 Android 运行时，
// 这样单元测试跑得快（毫秒级），也不受 minSdk / compileSdk 约束。
//
// 注意：这里刻意不声明 jvmToolchain。本机只有 JDK 21 与 26，没有 JDK 17，
// 写死 toolchain(17) 会导致 Gradle 找不到 JDK 而报错。
// 直接跟随运行 Gradle 的 JDK 即可（本项目要求用 JDK 21，见 README）。
plugins {
    // 根脚本已把该插件带上 classpath，这里不能再写版本号，
    // 否则报 "plugin is already on the classpath with an unknown version"
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    // 步进器连发逻辑用到 delay，需要协程核心库
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)   // runTest 虚拟时间
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "failed", "skipped")
    }
}
