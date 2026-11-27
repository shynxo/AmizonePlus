import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.student.amizoneplus"
    compileSdk = 37

    defaultConfig {
        // Different ID from the Expo version so both can be installed side by side while testing.
        applicationId = "com.student.amizoneplus.native"
        minSdk = 26
        targetSdk = 37
        // Bump BOTH for every release: versionCode +1, versionName to the new tag (v2.1.0 → "2.1.0").
        versionCode = 4
        versionName = "2.1.6"

        // Where the in-app updater looks for new versions: "<github-username>/<repo>".
        buildConfigField("String", "UPDATE_REPO", "\"shynxo/AmizonePlus\"")
    }

    // Release signing: read from keystore.properties (never committed). Without it — e.g. someone
    // else cloning the repo — release builds fall back to the debug key so they still build.
    val keyProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    signingConfigs {
        if (keyProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(keyProps.getProperty("storeFile"))
                storePassword = keyProps.getProperty("storePassword")
                keyAlias = keyProps.getProperty("keyAlias")
                keyPassword = keyProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.02.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.work:work-runtime-ktx:2.10.5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Networking + HTML parsing (the Amizone & LMS "scraper")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.24.1")

    // Progressive background blur behind the bottom bar
    implementation("dev.chrisbanes.haze:haze:2.1.0")
    implementation("dev.chrisbanes.haze:haze-blur:2.1.0")
}
