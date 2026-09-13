pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven {
            setUrl("https://jitpack.io")

            // Only MPAndroidChart is expected to come from JitPack, everything else must be resolved
            // from the trusted repositories above.
            content {
                includeGroup("com.github.PhilJay")
            }
        }
    }
}

rootProject.name = "Digi Dictionary"
include(":app", ":common", ":commonUi", ":commonTestUtils")
