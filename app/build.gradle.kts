import java.util.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("androidx.navigation.safeargs.kotlin")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(keystorePropertiesFile.inputStream())

android {
    namespace = "io.github.pelmenstar1.digiDict"
    compileSdk = 33

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
            storeFile = file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
        }
    }

    lint {
        // In most situations showLifecycleAwareSnackbar is called to show snackbar, but lint doesn't understand
        // that showLifecycleAwareSnackbar calls show() internally and shows this warning everywhere.
        disable += "ShowToast"

        // Lint gives false-positives about BadgeContainer not being instantiable
        // TODO: Fix this
        checkReleaseBuilds = false
    }

    defaultConfig {
        applicationId = "io.github.pelmenstar1.digiDict"
        minSdk = 21
        targetSdk = 33
        versionCode = 2
        versionName = "1.0.1"

        signingConfig = signingConfigs.getByName("release")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"

        freeCompilerArgs = freeCompilerArgs + arrayOf(
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }

    sourceSets {
        getByName("androidTest") {
            // TODO: Wait until 'duplicate contents root detected' warning is fixed and uncomment.
            //java.srcDirs += "$projectDir/src/test"

            assets.srcDirs(files("$projectDir/schemas"))
        }
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":commonUi"))

    implementation(libs.androidx.browser)
    implementation(libs.bundles.androidx.nav)
    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.bundles.androidx.paging)

    implementation(libs.bundles.kotlinx.coroutines)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    kapt(libs.androidx.room.compiler)

    implementation(libs.dagger.hilt.android)
    kapt(libs.dagger.hilt.compiler)

    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.android.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.viewpager)
    implementation(libs.androidx.datastore.prefs)

    implementation(libs.mpandroidchart)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(project(":commonTestUtils"))
    testImplementation(libs.bundles.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":commonTestUtils"))
    androidTestImplementation(libs.bundles.kotlin.test.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.androidx.room.testing)
}