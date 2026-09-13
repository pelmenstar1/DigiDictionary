import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.instrumentation.logcat.LogcatLevel
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.androidx.nav.safeargs)
    alias(libs.plugins.sentry.android.gradle)
}

// keystore.properties is not checked into the repository, so the release signing config is only
// wired up when it is actually available (e.g. on a release machine).
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}
val hasKeystore = keystoreProperties.isNotEmpty()

val sentryDsn = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}.getProperty("sentryDsn").orEmpty()

android {
    namespace = "io.github.pelmenstar1.digiDict"
    compileSdk = libs.versions.compileSdk.get().toInt()

    if (hasKeystore) {
        signingConfigs {
            create("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            }
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
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)

        optIn.addAll(
            "kotlin.contracts.ExperimentalContracts",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.FlowPreview",
            "kotlinx.serialization.ExperimentalSerializationApi"
        )
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

sentry {
    // Uploading R8 mappings and source context needs an auth token, which only release machines and
    // CI have. Without it the plugin still instruments bytecode, it just skips the uploads instead
    // of failing the build.
    val sentryAuthToken = providers.environmentVariable("SENTRY_AUTH_TOKEN")
    val canUpload = sentryAuthToken.isPresent

    org = providers.environmentVariable("SENTRY_ORG").orNull
    projectName = providers.environmentVariable("SENTRY_PROJECT").orNull
    authToken = sentryAuthToken.orNull

    autoUploadProguardMapping = canUpload
    includeSourceContext = canUpload

    // Every io.sentry artifact is declared explicitly below and pinned by the BOM, so the plugin
    // must not add its own copies at a version we do not control.
    autoInstallation {
        enabled = false
    }

    tracingInstrumentation {
        enabled = true

        features = setOf(InstrumentationFeature.DATABASE, InstrumentationFeature.FILE_IO)

        logcat {
            enabled = true
            minLevel = LogcatLevel.WARNING
        }
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":commonUi"))

    implementation(libs.androidx.browser)
    implementation(libs.bundles.androidx.nav)
    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.androidx.paging)

    implementation(libs.bundles.kotlinx.coroutines)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)

    implementation(libs.androidx.core)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.android.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.viewpager)
    implementation(libs.androidx.datastore.prefs)

    implementation(libs.mpandroidchart)

    implementation(libs.kotlinx.serialization.json)

    // The BOM keeps every io.sentry artifact on one version; none of them declare their own.
    implementation(platform(libs.sentry.bom))
    implementation(libs.sentry.android)
    implementation(libs.sentry.android.sqlite)
    implementation(libs.sentry.android.fragment)

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
