buildscript {
    dependencies {
        classpath(libs.dagger.hilt.gradlePlugin)
        classpath(libs.androidx.nav.safeArgsGradlePlugin)
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.0.2" apply false
    id("com.android.library") version "8.0.2" apply false

    kotlin("android") version "1.8.0" apply false
    kotlin("jvm") version "1.8.0" apply false
    kotlin("plugin.serialization") version "1.8.0" apply false
}

tasks.create<Delete>("delete") {
    delete(rootProject.buildDir)
}