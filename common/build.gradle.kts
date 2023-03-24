plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "io.github.pelmenstar1.digiDict.common"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    lint {
        // In most situations showLifecycleAwareSnackbar is called to show snackbar, but lint doesn't understand
        // that showLifecycleAwareSnackbar calls show() internally and shows this warning everywhere.
        disable += "ShowToast"
    }

    buildTypes {
        debug {
            enableAndroidTestCoverage = true
            enableUnitTestCoverage = true
        }

        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }

    testOptions {
        testCoverage {
            jacocoVersion = "0.8.7"
        }
    }
}

dependencies {
    implementation(libs.androidx.room.runtime)
    kaptAndroidTest(libs.androidx.room.compiler)

    implementation(libs.bundles.kotlinx.coroutines)
    implementation(libs.bundles.androidx.nav)
    implementation(libs.bundles.androidx.sqlite)

    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.android.material)
    implementation(libs.androidx.datastore.prefs)
    implementation(libs.androidx.recyclerview)

    testImplementation(project(":commonTestUtils"))
    testImplementation(libs.bundles.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":commonTestUtils"))
    androidTestImplementation(libs.bundles.kotlin.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso)
}

tasks.register<JacocoReport>("jacocoMergeCoverageReports") {
    dependsOn("createDebugUnitTestCoverageReport", "createDebugAndroidTestCoverageReport")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter =
        listOf("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*", "**/*Test*.*", "android/**/*.*")
    val kotlinTree = fileTree(mapOf("dir" to "${buildDir}/tmp/kotlin-classes/debug", "excludes" to fileFilter))
    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.from(files(mainSrc))
    classDirectories.from(files(kotlinTree))
    executionData.from(
        fileTree(
            mapOf(
                "dir" to "$buildDir", "includes" to listOf(
                    "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                    "outputs/code_coverage/debugAndroidTest/connected/*/coverage.ec",
                )
            )
        )
    )
}