plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.pelmenstar1.digiDict.common.ui"
    testNamespace = "io.github.pelmenstar1.digiDict.common.ui.tests"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    testOptions {
        animationsDisabled = true
    }

    lint {
        // In most situations showLifecycleAwareSnackbar is called to show snackbar, but lint doesn't understand
        // that showLifecycleAwareSnackbar calls show() internally and shows this warning everywhere.
        disable += "ShowToast"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"

        freeCompilerArgs = freeCompilerArgs + arrayOf("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

dependencies {
    implementation(project(":common"))

    implementation(libs.bundles.kotlinx.coroutines)
    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.bundles.androidx.nav)

    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.android.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment)

    testImplementation(libs.bundles.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":commonTestUtils"))
    androidTestImplementation(libs.bundles.kotlin.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.androidx.fragment.testing)
}