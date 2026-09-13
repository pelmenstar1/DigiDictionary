import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.pelmenstar1.digiDict.commonTestUtils"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    testOptions {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)

        optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

dependencies {
    implementation(project(":common"))
    implementation(libs.androidx.core)
    implementation(libs.android.material)
    implementation(libs.bundles.kotlinx.coroutines)
    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment.testing)
    implementation(libs.androidx.test.espresso)

    implementation(libs.junit)
    implementation(libs.androidx.test.ext)
    implementation(libs.kotlin.test.junit)

    androidTestImplementation(libs.bundles.kotlin.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext)
}
