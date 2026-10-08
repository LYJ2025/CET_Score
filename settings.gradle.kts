pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Vico 2.x/3.x 部分变体发布在 Maven Central，google() 中没有
        mavenCentral()
    }
}

rootProject.name = "CetSixScore"

include(":app")

// 核心层
include(":core:ui")
include(":core:data")
include(":core:domain")   // 纯 Kotlin 估分算法，不依赖 Android

// 功能层
include(":feature:home")
include(":feature:aiassistant")
include(":feature:assessment")
include(":feature:result")
include(":feature:history")
include(":feature:trend")
