buildscript {
    dependencies {
        classpath(libs.kotlin.gradlePlugin)
        classpath(libs.ksp.gradlePlugin)
        classpath(libs.kotlin.serialization.gradlePlugin)
    }
}

// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.dagger.hilt) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.androidx.nav.safeargs) apply false
}

tasks.register<Delete>("clean") {
    description = "Cleans the build directory"

    delete(rootProject.layout.buildDirectory)
}
