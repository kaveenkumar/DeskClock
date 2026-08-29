import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Single source of truth for the version, used both for `versionName` and for the stamped APK copy
 * dropped in the repo root (same convention as ChargeMeter). `versionCode` increments on every
 * CHANGELOG.md entry and must never be reused.
 */
val appVersionName = "0.8.0"

android {
    namespace = "com.deskclock.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.deskclock.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 9
        versionName = appVersionName
    }

    // Machine-local release signing: keystore.properties and the keystore live in the project root,
    // gitignored. Without them the release build simply comes out unsigned.
    val keystoreProps = rootProject.file("keystore.properties")
        .takeIf { it.exists() }
        ?.let { file -> Properties().apply { file.inputStream().use { load(it) } } }

    signingConfigs {
        if (keystoreProps != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

/**
 * Drops a version-stamped copy of every built APK in the repo root, so the artifact to sideload is
 * always `./DeskClock-v<versionName>.apk`. Same mechanism as ChargeMeter's build script; see the
 * comments there for why it is wired through `androidComponents.onVariants` and not a `Copy` task.
 */
androidComponents {
    onVariants { variant ->
        val capitalised = variant.name.replaceFirstChar { it.uppercaseChar() }
        val suffix = if (variant.buildType == "release") "-release" else ""
        val target = "DeskClock-v$appVersionName$suffix.apk"

        val apkDir = variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.APK)
        val destination = rootProject.layout.projectDirectory.file(target).asFile

        val copyTask = tasks.register("copy${capitalised}ApkToRoot") {
            description = "Copies the $capitalised APK to the repo root as $target."
            inputs.dir(apkDir).withPropertyName("apkDirectory")
            outputs.file(destination).withPropertyName("stampedApk")
            outputs.upToDateWhen { false }
            doLast {
                val built = apkDir.get().asFile.listFiles()
                    ?.filter { it.isFile && it.extension == "apk" }
                    ?.singleOrNull()
                    ?: error("Expected exactly one APK in ${apkDir.get().asFile}")
                built.copyTo(destination, overwrite = true)
                logger.lifecycle("APK: ${destination.name} (${built.length() / 1024} KiB)")
            }
        }

        tasks.matching { it.name == "assemble$capitalised" }.configureEach {
            finalizedBy(copyTask)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
