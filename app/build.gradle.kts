import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.easycash.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.easycash.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 5
        versionName = "1.0.0-rc.4"
    }

    val signingFile = rootProject.file(".signing/release.properties")
    if (signingFile.exists()) {
        val signing = Properties().apply { signingFile.inputStream().use { load(it) } }
        val releaseSigning = signingConfigs.create("privateRelease") {
            storeFile = rootProject.file(requireNotNull(signing.getProperty("storeFile")))
            storePassword = requireNotNull(signing.getProperty("storePassword"))
            keyAlias = requireNotNull(signing.getProperty("keyAlias"))
            keyPassword = requireNotNull(signing.getProperty("keyPassword"))
        }
        buildTypes.getByName("release").signingConfig = releaseSigning
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

dependencies {
    implementation(project(":ui"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(libs.room.runtime)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}
