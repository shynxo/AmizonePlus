// Top-level build file.
// Android Gradle Plugin 9.2.1 (already cached on this PC) with its built-in Kotlin
// support, upgraded to Kotlin 2.4.21 because the Haze blur library is built with 2.4.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21" apply false
}
