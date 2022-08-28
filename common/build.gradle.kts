plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 32

    defaultConfig {
        minSdk = 21
        targetSdk = 32

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"

        freeCompilerArgs = freeCompilerArgs + arrayOf(
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }
}

dependencies {
    val coroutines_version = "1.6.4"
    val nav_version = "2.5.1"
    val sqlite_version = "2.2.0"
    val room_version = "2.4.3"

    implementation("androidx.room:room-runtime:$room_version")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")

    implementation("androidx.navigation:navigation-fragment:$nav_version")
    implementation("androidx.navigation:navigation-ui:$nav_version")
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")

    implementation("androidx.sqlite:sqlite:$sqlite_version")
    implementation("androidx.sqlite:sqlite-ktx:$sqlite_version")

    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.5.0")
    implementation("com.google.android.material:material:1.6.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.7.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.7.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutines_version")

    androidTestImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.7.0")
    androidTestImplementation("org.jetbrains.kotlin:kotlin-test:1.7.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutines_version")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}