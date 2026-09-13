import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    id("com.google.devtools.ksp")
}

android {
    namespace = "io.github.pelmenstar1.digiDict.common"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

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
    }

    buildFeatures {
        // Logging.kt and the instrumented tests rely on BuildConfig.DEBUG.
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        targetSdk = libs.versions.targetSdk.get().toInt()

        testCoverage {
            jacocoVersion = libs.versions.jacoco.get()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)

        optIn.addAll(
            "kotlin.contracts.ExperimentalContracts",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.FlowPreview"
        )
    }
}

dependencies {
    implementation(libs.androidx.room.runtime)
    kspAndroidTest(libs.androidx.room.compiler)

    implementation(libs.bundles.kotlinx.coroutines)
    implementation(libs.bundles.androidx.nav)
    implementation(libs.bundles.androidx.sqlite)

    implementation(platform(libs.sentry.bom))
    implementation(libs.sentry.kotlin.extensions)

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
    description = "Generates coverage report"
    dependsOn("createDebugUnitTestCoverageReport", "createDebugAndroidTestCoverageReport")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val buildDirectory = layout.buildDirectory
    val fileFilter =
        listOf("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*", "**/*Test*.*", "android/**/*.*")

    sourceDirectories.from(files("${project.projectDir}/src/main/java"))
    classDirectories.from(
        buildDirectory.dir("tmp/kotlin-classes/debug").map { it.asFileTree.matching { exclude(fileFilter) } })
    executionData.from(
        buildDirectory.map {
            it.asFileTree.matching {
                include(
                    "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                    "outputs/code_coverage/debugAndroidTest/connected/*/coverage.ec"
                )
            }
        }
    )
}
