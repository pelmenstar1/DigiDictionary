pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            version("kotlinx-coroutines", "1.6.4")
            version("kotlin", "1.7.0")
            version("androidx-lifecycle", "2.5.1")
            version("androidx-room", "2.4.3")
            version("androidx-sqlite", "2.2.0")
            version("androidx-nav", "2.5.1")
            version("dagger-hilt", "2.43.2")
            version("androidx-paging", "3.1.1")

            // Kotlin related.
            library(
                "kotlinx-coroutines-core",
                "org.jetbrains.kotlinx",
                "kotlinx-coroutines-core"
            ).versionRef("kotlinx-coroutines")
            library(
                "kotlinx-coroutines-android",
                "org.jetbrains.kotlinx",
                "kotlinx-coroutines-android"
            ).versionRef("kotlinx-coroutines")
            bundle("kotlinx-coroutines", listOf("kotlinx-coroutines-core", "kotlinx-coroutines-android"))

            // Android related.
            library("androidx-core", "androidx.core", "core-ktx").version("1.8.0")
            library("androidx-appcompat", "androidx.appcompat", "appcompat").version("1.5.0")
            library("android-material", "com.google.android.material", "material").version("1.6.1")
            library("androidx-constraintlayout", "androidx.constraintlayout", "constraintlayout").version("2.1.4")
            library("androidx-viewpager", "androidx.viewpager2", "viewpager2").version("1.0.0")

            library(
                "androidx-lifecycle-viewModel",
                "androidx.lifecycle",
                "lifecycle-viewmodel-ktx"
            ).versionRef("androidx-lifecycle")
            library(
                "androidx-lifecycle-runtime",
                "androidx.lifecycle",
                "lifecycle-runtime-ktx"
            ).versionRef("androidx-lifecycle")
            bundle("androidx-lifecycle", listOf("androidx-lifecycle-viewModel", "androidx-lifecycle-runtime"))

            library("androidx-room-runtime", "androidx.room", "room-runtime").versionRef("androidx-room")
            library("androidx-room-ktx", "androidx.room", "room-ktx").versionRef("androidx-room")
            library("androidx-room-paging", "androidx.room", "room-paging").versionRef("androidx-room")
            library("androidx-room-compiler", "androidx.room", "room-compiler").versionRef("androidx-room")

            library("androidx-sqlite", "androidx.sqlite", "sqlite").versionRef("androidx-sqlite")
            library("androidx-sqlite-ktx", "androidx.sqlite", "sqlite-ktx").versionRef("androidx-sqlite")
            bundle("androidx-sqlite", listOf("androidx-sqlite", "androidx-sqlite-ktx"))

            library("androidx-nav-fragment", "androidx.navigation", "navigation-fragment").versionRef("androidx-nav")
            library(
                "androidx-nav-fragment-ktx",
                "androidx.navigation",
                "navigation-fragment-ktx"
            ).versionRef("androidx-nav")
            library("androidx-nav-ui", "androidx.navigation", "navigation-ui").versionRef("androidx-nav")
            library("androidx-nav-ui-ktx", "androidx.navigation", "navigation-ui-ktx").versionRef("androidx-nav")
            library(
                "androidx-nav-safeArgsGradlePlugin",
                "androidx.navigation",
                "navigation-safe-args-gradle-plugin"
            ).versionRef("androidx-nav")
            bundle(
                "androidx-nav",
                listOf("androidx-nav-fragment", "androidx-nav-fragment-ktx", "androidx-nav-ui", "androidx-nav-ui-ktx")
            )

            library("androidx-browser", "androidx.browser", "browser").version("1.4.0")

            library("dagger-hilt-android", "com.google.dagger", "hilt-android").versionRef("dagger-hilt")
            library("dagger-hilt-compiler", "com.google.dagger", "hilt-compiler").versionRef("dagger-hilt")
            library(
                "dagger-hilt-gradlePlugin",
                "com.google.dagger",
                "hilt-android-gradle-plugin"
            ).versionRef("dagger-hilt")

            library("androidx-paging", "androidx.paging", "paging-runtime").versionRef("androidx-paging")
            library("androidx-paging-ktx", "androidx.paging", "paging-runtime-ktx").versionRef("androidx-paging")
            bundle("androidx-paging", listOf("androidx-paging", "androidx-paging-ktx"))

            library("androidx-datastore-prefs", "androidx.datastore", "datastore-preferences").version("1.0.0")

            // Test related.
            library("junit", "junit", "junit").version("4.13.2")
            library("kotlin-test", "org.jetbrains.kotlin", "kotlin-test").versionRef("kotlin")
            library("kotlin-test-junit", "org.jetbrains.kotlin", "kotlin-test-junit").versionRef("kotlin")
            bundle("kotlin-test", listOf("kotlin-test", "kotlin-test-junit"))
            bundle("kotlin-test-junit", listOf("junit", "kotlin-test", "kotlin-test-junit"))

            library(
                "kotlinx-coroutines-test",
                "org.jetbrains.kotlinx",
                "kotlinx-coroutines-test"
            ).versionRef("kotlinx-coroutines")
            library("androidx-test-ext", "androidx.test.ext", "junit").version("1.1.3")
            library("androidx-test-espresso", "androidx.test.espresso", "espresso-core").version("3.4.0")
            library("androidx-room-testing", "androidx.room", "room-testing").versionRef("androidx-room")

        }
    }
}

rootProject.name = "Digi Dictionary"
include(":app", ":common", ":commonUi")
