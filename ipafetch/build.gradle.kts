import org.gradle.api.JavaVersion

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "com.ipafetch.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.ipafetch.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    kotlin {
        jvmToolchain(17)
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Core
    implementation(libs.android.core.ktx)
    implementation(libs.android.material)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    implementation(libs.compose.activity)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.compose.hilt.navigation)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // SQLCipher
    implementation(libs.net.sqlcipher)

    // Tink
    implementation(libs.tink.android)

    // Argon2id KDF
    implementation(libs.argon2)

    // Biometric
    implementation(libs.biometric)

    // EncryptedSharedPreferences / MasterKeys
    implementation(libs.security.crypto)

    // Serialization
    implementation(libs.kotlinx.serialization)

    // WorkManager
    implementation(libs.work.runtime.ktx)
    implementation(libs.compose.hilt.work)

    // Coil
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)
}
