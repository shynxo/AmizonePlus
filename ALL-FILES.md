# Amizone+ (native) — every file, ready to copy-paste

Generated from the working project in `Q:\amz-native`. Paths are relative to the project root.
Binary files (fonts in `res/font`, launcher icons, `gradle-wrapper.jar`, `gradlew` scripts) are not included — copy those from the project folder.

---

## `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AmizonePlus"
include(":app")
```

---

## `build.gradle.kts`

```kotlin
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
```

---

## `gradle.properties`

```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official

# Enabled parallel sync for Gradle 9.4+
org.gradle.tooling.parallel=true
```

---

## `gradle/wrapper/gradle-wrapper.properties`

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

---

## `app/build.gradle.kts`

```kotlin
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
        versionCode = 3
        versionName = "2.1.5"

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
```

---

## `app/proguard-rules.pro`

```proguard
# WorkManager creates its Room database by reflection.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }

# kotlinx.serialization: keep generated serializers for our models.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.student.amizoneplus.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.student.amizoneplus.**$$serializer { *; }

# Jsoup ships optional re2j support we don't use.
-dontwarn com.google.re2j.**

# OkHttp optional platform classes.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
```

---

## `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <!-- Lets exam reminders fire on time (8 pm the night before) when the phone allows it -->
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <!-- Lets the in-app updater hand a downloaded update to Android's installer -->
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

    <application
        android:name=".AmizoneApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.AmizonePlus">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize"
            android:theme="@style/Theme.AmizonePlus">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Fires class / exam reminders scheduled with AlarmManager -->
        <receiver
            android:name=".work.ReminderReceiver"
            android:exported="false" />

        <!-- Lets other apps (PDF / PowerPoint viewers) open downloaded LMS files -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.files"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
    </application>
</manifest>
```

---

## `app/src/main/res/values/strings.xml`

```xml
<resources>
    <string name="app_name">Amizone+</string>
</resources>
```

---

## `app/src/main/res/values/themes.xml`

```xml
<resources>
    <!-- Compose draws everything; this only sets the window colours before the first frame. -->
    <style name="Theme.AmizonePlus" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">@color/canvas</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">true</item>
    </style>

    <color name="canvas">#EFEEE9</color>
</resources>
```

---

## `app/src/main/res/values-night/themes.xml`

```xml
<resources>
    <style name="Theme.AmizonePlus" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@color/canvas</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>

    <color name="canvas">#161617</color>
</resources>
```

---

## `app/src/main/res/xml/file_paths.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <!-- Downloaded LMS files live in the app cache under lms/ -->
    <cache-path name="lms" path="lms/" />
    <!-- Downloaded app updates -->
    <cache-path name="updates" path="updates/" />
</paths>
```

---

## `app/src/main/res/drawable/ic_launcher_background.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- App icon background: the attendance-card periwinkle. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#6F73F4" android:pathData="M0,0h108v108h-108z" />
</vector>
```

---

## `app/src/main/res/drawable/ic_launcher_foreground.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- App icon: a cream "A" with cat ears (hi, pets) and a sun-yellow "+" badge.
     Drawn on the 108dp adaptive canvas, scaled into the 66dp safe zone. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <group android:scaleX="0.74" android:scaleY="0.74" android:translateX="12.56" android:translateY="16.26">
        <!-- A -->
        <path android:fillColor="#F4F1EA" android:fillType="evenOdd"
            android:pathData="M28,80 L37.5,28 L54.5,28 L64,80 L52.5,80 L50.6,69.5 L41.4,69.5 L39.5,80 Z M43.2,60 L48.8,60 L46,42.5 Z" />
        <!-- ears -->
        <path android:fillColor="#F4F1EA"
            android:pathData="M36.2,32 L38.4,19.6 Q39,17.8 40.4,19.2 L47,28 Z M45,28 L51.6,19.2 Q53,17.8 53.6,19.6 L55.8,32 Z" />
        <path android:fillColor="#F7A8B8"
            android:pathData="M39.5,23.8 L40.2,21.4 L43.2,25.6 Z M48.8,25.6 L51.8,21.4 L52.5,23.8 Z" />
        <!-- + badge -->
        <path android:fillColor="#F7C54A" android:pathData="M60,40 a12,12 0 1,1 24,0 a12,12 0 1,1 -24,0 Z" />
        <path android:fillColor="#161617" android:pathData="M70,33 h4 v5 h5 v4 h-5 v5 h-4 v-5 h-5 v-4 h5 Z" />
    </group>
</vector>
```

---

## `app/src/main/res/drawable/ic_launcher_monochrome.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Themed-icon (Android 13+) version: one colour, the "+" cut out of the badge. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <group android:scaleX="0.74" android:scaleY="0.74" android:translateX="12.56" android:translateY="16.26">
        <path android:fillColor="#FFFFFFFF" android:fillType="evenOdd"
            android:pathData="M28,80 L37.5,28 L54.5,28 L64,80 L52.5,80 L50.6,69.5 L41.4,69.5 L39.5,80 Z M43.2,60 L48.8,60 L46,42.5 Z" />
        <path android:fillColor="#FFFFFFFF"
            android:pathData="M36.2,32 L38.4,19.6 Q39,17.8 40.4,19.2 L47,28 Z M45,28 L51.6,19.2 Q53,17.8 53.6,19.6 L55.8,32 Z" />
        <path android:fillColor="#FFFFFFFF" android:fillType="evenOdd"
            android:pathData="M60,40 a12,12 0 1,1 24,0 a12,12 0 1,1 -24,0 Z M70,33 h4 v5 h5 v4 h-5 v5 h-4 v-5 h-5 v-4 h5 Z" />
    </group>
</vector>
```

---

## `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
```

---

## `app/src/main/java/com/student/amizoneplus/data/Models.kt`

```kotlin
package com.student.amizoneplus.data

import kotlinx.serialization.Serializable

/** Everything the app shows, cached as JSON so it opens instantly and works offline. */

@Serializable
data class AttendanceRecord(val code: String, val name: String, val attended: Int, val held: Int)

/** present / absent / pending / na */
@Serializable
data class ScheduledClass(
    val code: String,
    val name: String,
    val startMs: Long,
    val endMs: Long,
    val faculty: String,
    val room: String,
    val state: String,
)

@Serializable
data class ScheduledExam(
    val code: String,
    val name: String,
    val timeMs: Long?,
    val mode: String,
    val location: String,
)

@Serializable
data class ExamSchedule(val title: String, val exams: List<ScheduledExam>)

@Serializable
data class Internals(val have: Double, val max: Double)

@Serializable
data class Course(
    val code: String,
    val name: String,
    val type: String,
    val attended: Int,
    val held: Int,
    val internals: Internals?,
    val syllabusUrl: String?,
)

@Serializable
data class Profile(val name: String, val program: String, val batch: String, val enrollmentNo: String)

@Serializable
data class CourseResult(val code: String, val name: String, val grade: String, val gradePoint: Double, val credits: Double, val max: Double)

@Serializable
data class SemesterResult(val semester: String, val sgpa: Double, val cgpa: Double, val backPapers: String)

@Serializable
data class Results(val courses: List<CourseResult>, val semesters: List<SemesterResult>)

@Serializable
data class NoticeDoc(val title: String, val description: String, val url: String)

@Serializable
data class AmizoneData(
    val profile: Profile? = null,
    val attendance: List<AttendanceRecord> = emptyList(),
    val schedule: List<ScheduledClass> = emptyList(), // today + next 7 days
    val history: List<ScheduledClass> = emptyList(), // last 28 days
    val exams: ExamSchedule? = null,
    val courses: List<Course> = emptyList(),
    val results: Results? = null,
    val notices: List<NoticeDoc> = emptyList(),
    val credits: Map<String, Double> = emptyMap(), // course code -> credit units
    val hours: Map<String, CourseHours> = emptyMap(), // course code -> weekly L / T / P from Programme Structure
    val fetchedAt: Long = 0,
    val errors: List<String> = emptyList(),
)

/** Weekly teaching hours from the Programme Structure: lectures, tutorials, practicals. */
@Serializable
data class CourseHours(val cu: Double = 0.0, val l: Double = 0.0, val t: Double = 0.0, val p: Double = 0.0) {
    /**
     * Classes per week that count for attendance: L + T + P. A course with no
     * lectures (languages, etc.) counts its tutorials at half: T 1 → 0.5 a week.
     */
    val weekly: Double get() = l + p + (if (l == 0.0) t / 2 else t)
}

@Serializable
data class Settings(
    val threshold: Int = 75,
    val lowAttendanceAlerts: Boolean = true,
    val classReminders: Boolean = true,
    val reminderMinutes: Int = 10,
    val examReminders: Boolean = true,
    val haptics: Boolean = true,
    val creditOverrides: Map<String, Double> = emptyMap(), // old (credits-based) — no longer used
    val weeklyOverrides: Map<String, Double> = emptyMap(), // course code -> classes per week, set by you
    val pets: String = "both", // "cat", "dog", "both" or "none"
)

data class Credentials(val username: String, val password: String)

/* ── LMS (Moodle) ── */

@Serializable
data class LmsSemester(val label: String, val action: String, val body: String)

@Serializable
data class LmsCourse(val id: String, val fullname: String, val shortname: String, val code: String)

@Serializable
data class LmsItem(val id: String, val name: String, val modname: String, val url: String)

@Serializable
data class LmsSection(val id: String, val title: String, val items: List<LmsItem>)

@Serializable
data class LmsFile(val name: String, val url: String, val path: String)

/* ── Errors ── */

sealed class AmizoneException(message: String) : Exception(message) {
    class InvalidCredentials(message: String = "Wrong username or password.") : AmizoneException(message)
    class NeedsWebLogin(message: String) : AmizoneException(message)
    class Network(message: String) : AmizoneException(message)
    class Parse(message: String) : AmizoneException(message)
}
```

---

## `app/src/main/java/com/student/amizoneplus/data/Http.kt`

```kotlin
package com.student.amizoneplus.data

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * One cookie store for everything: OkHttp reads and writes Android's WebView
 * CookieManager, so the app's own connection and its in-app web pages share a session.
 */
class WebViewCookieJar : CookieJar {
    private val cm: CookieManager = CookieManager.getInstance().apply { setAcceptCookie(true) }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (c in cookies) cm.setCookie(url.toString(), Http.keep(c).toString())
        cm.flush()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val header = cm.getCookie(url.toString()) ?: return emptyList()
        return header.split(';').mapNotNull { Cookie.parse(url, it.trim()) }
    }
}

object Http {
    /**
     * The exact User-Agent of this phone's WebView. The app's connection and the web
     * view MUST present the same identity: Amizone sits behind Cloudflare, which ties
     * its clearance (and so the session) to the browser's User-Agent.
     */
    var UA: String = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"
        private set

    fun init(context: Context) {
        runCatching { UA = WebSettings.getDefaultUserAgent(context.applicationContext) }
    }

    private const val KEEP_MS = 14L * 86_400_000

    /**
     * Amizone's sign-in cookies are "session" cookies, which the phone may forget when
     * the app is closed — then every launch needs a fresh sign-in. Keep them for 14 days.
     */
    fun keep(c: Cookie): Cookie {
        if (c.persistent || !c.domain.endsWith("amizone.net")) return c
        return Cookie.Builder().name(c.name).value(c.value).path(c.path)
            .apply { if (c.hostOnly) hostOnlyDomain(c.domain) else domain(c.domain) }
            .apply { if (c.secure) secure(); if (c.httpOnly) httpOnly() }
            .expiresAt(System.currentTimeMillis() + KEEP_MS)
            .build()
    }

    /** Same, for cookies the web view set itself (after a website sign-in). */
    fun persistSession() {
        val cm = CookieManager.getInstance()
        val base = AmizoneClient.BASE
        val header = cm.getCookie(base) ?: return
        for (part in header.split(';')) {
            val kv = part.trim()
            if (!kv.contains('=')) continue
            cm.setCookie(base, "$kv; Path=/; Max-Age=${KEEP_MS / 1000}; Secure; HttpOnly")
        }
        cm.flush()
    }

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(WebViewCookieJar())
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("User-Agent", UA).build()) }
            .build()
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/data/Parsers.kt`

```kotlin
package com.student.amizoneplus.data

import org.json.JSONArray
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.Calendar

/**
 * HTML/JSON parsers for Amizone pages. Selectors follow the open-source go-amizone
 * project and were verified against your live account. If Amizone changes a page,
 * this is the file to update.
 */
object Parsers {

    private fun clean(s: String?) = (s ?: "").replace('\u00a0', ' ').replace(Regex("\\s+"), " ").trim()
    private fun cell(row: Element, title: String): Element? = row.selectFirst("td[data-title=\"$title\"]")
    private fun cellText(row: Element, title: String) = clean(cell(row, title)?.text())
    private val BR = Regex("<br\\b[^>]*>", RegexOption.IGNORE_CASE)
    private fun htmlText(s: String) = clean(Jsoup.parse(s).text())

    /* ---------- Login ---------- */

    fun isLoginPage(html: String) =
        Regex("id\\s*=\\s*[\"']loginform[\"']", RegexOption.IGNORE_CASE).containsMatchIn(html) ||
            Regex("name\\s*=\\s*[\"']_Password[\"']", RegexOption.IGNORE_CASE).containsMatchIn(html)

    fun mentionsBadCredentials(html: String) =
        Regex("invalid (user ?name|password|credentials)|incorrect (user ?name|password)|wrong password", RegexOption.IGNORE_CASE)
            .containsMatchIn(html)

    /** Every named field of Amizone's login form, with the values the page set. */
    fun loginForm(html: String): MutableMap<String, String>? {
        val doc = org.jsoup.Jsoup.parse(html)
        val form = doc.selectFirst("form#loginform") ?: doc.selectFirst("form:has(input[name=_Password])") ?: return null
        val out = LinkedHashMap<String, String>()
        for (inp in form.select("input[name]")) {
            val name = inp.attr("name")
            if (name.isEmpty() || name in out) continue
            out[name] = inp.attr("value")
        }
        return if ("__RequestVerificationToken" in out) out else null
    }

    fun verificationToken(html: String): String? =
        Jsoup.parse(html).selectFirst("input[name=__RequestVerificationToken]")?.attr("value")?.takeIf { it.isNotEmpty() }

    /* ---------- Attendance (home page widget) ---------- */

    fun attendance(html: String): List<AttendanceRecord> {
        val doc = Jsoup.parse(html)
        val header = doc.select(".widget-header").firstOrNull { it.text().contains("my attendance", ignoreCase = true) }
        val items = (header?.parent() ?: doc).select("ul#tasks li")
        if (items.isEmpty()) throw AmizoneException.Parse("Attendance widget not found")
        return items.map { li ->
            val nums = clean(li.selectFirst("div.class-count span")?.text()).replace("\"", "").split("/")
            val attended = nums.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
            val held = nums.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            val code = clean(li.selectFirst("span.sub-code")?.text())
            val label = clean(li.selectFirst("span.lbl")?.text())
            val name = if (label.startsWith(code)) clean(label.removePrefix(code)) else label.replaceFirst(Regex("^\\S+\\s"), "")
            AttendanceRecord(code, name.ifEmpty { code }, attended, held)
        }
    }

    /* ---------- Class schedule (calendar JSON) ---------- */

    /** Accepts "2024/01/15 09:15:00 AM" and "2024-01-15 09:15:00" / ISO formats. Local time. */
    private fun diaryTime(s: String?): Long? {
        if (s.isNullOrBlank()) return null
        val m = Regex("(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})[ T](\\d{1,2}):(\\d{2})(?::(\\d{2}))?\\s*(AM|PM)?", RegexOption.IGNORE_CASE)
            .find(s) ?: return null
        val g = m.groupValues
        var h = g[4].toInt()
        val ampm = g[7].uppercase()
        if (ampm == "PM" && h < 12) h += 12
        if (ampm == "AM" && h == 12) h = 0
        return Calendar.getInstance().apply {
            set(g[1].toInt(), g[2].toInt() - 1, g[3].toInt(), h, g[5].toInt(), g[6].ifEmpty { "0" }.toInt())
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun attendanceState(color: String?) = when ((color ?: "").uppercase()) {
        "#F00", "#FF0000" -> "absent"
        "#4FCC4F" -> "present"
        "#3A87AD" -> "pending"
        else -> "na"
    }

    fun schedule(json: String): List<ScheduledClass> {
        val arr = try {
            JSONArray(json)
        } catch (e: Exception) {
            throw AmizoneException.Parse("Timetable response was not JSON")
        }
        val out = ArrayList<ScheduledClass>()
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: continue
            if (e.optString("sType") != "C") continue
            val start = diaryTime(e.optString("start")) ?: continue
            val end = diaryTime(e.optString("end")) ?: start
            out += ScheduledClass(
                code = htmlText(e.optString("CourseCode")),
                name = htmlText(e.optString("title")),
                startMs = start,
                endMs = end,
                faculty = htmlText(e.optString("FacultyName")),
                room = htmlText(e.optString("RoomNo")),
                state = attendanceState(e.optString("AttndColor")),
            )
        }
        return out.sortedBy { it.startMs }
    }

    /* ---------- Courses (My Courses page) ---------- */

    private fun absolute(href: String, base: String) =
        if (Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(href)) href
        else base + (if (href.startsWith("/")) "" else "/") + href

    fun courses(html: String, base: String): List<Course> {
        val doc = Jsoup.parse(html)
        val rows = doc.select("tr").filter { cell(it, "Course Code") != null }
        if (rows.isEmpty()) throw AmizoneException.Parse("No courses found")
        val seen = HashSet<String>()
        val out = ArrayList<Course>()
        for (row in rows) {
            val code = cellText(row, "Course Code")
            if (code.isEmpty() || !seen.add(code)) continue
            val att = Regex("(\\d+)\\s*/\\s*(\\d+)").find(cellText(row, "Attendance"))
            // e.g. "35.00[30.00+5.00]/40.00" or "12/30"
            val marksRaw = cellText(row, "Internal Asses.")
            val have = Regex("^(\\d+(?:\\.\\d+)?)").find(marksRaw)?.groupValues?.get(1)?.toDoubleOrNull()
            val max = Regex("/\\s*(\\d+(?:\\.\\d+)?)\\s*$").find(marksRaw)?.groupValues?.get(1)?.toDoubleOrNull()
            val syllabus = cell(row, "Course Syllabus")?.selectFirst("a")?.attr("href").orEmpty()
            out += Course(
                code = code,
                name = cellText(row, "Course Name"),
                type = cellText(row, "Type"),
                attended = att?.groupValues?.get(1)?.toInt() ?: 0,
                held = att?.groupValues?.get(2)?.toInt() ?: 0,
                internals = if (have != null && max != null) Internals(have, max) else null,
                syllabusUrl = syllabus.takeIf { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("javascript:", true) }
                    ?.let { absolute(it, base) },
            )
        }
        return out
    }

    /* ---------- Exam schedule ---------- */

    fun examSchedule(html: String): ExamSchedule {
        val doc = Jsoup.parse(html)
        val rows = doc.select("table.table tbody tr").filter { cell(it, "Course Code") != null }
        val exams = rows.map { row ->
            val d = Regex("(\\d{1,2})/(\\d{1,2})/(\\d{4})").find(cellText(row, "Exam Date"))
            val timeText = cellText(row, "Time")
            val t = Regex("(\\d{1,2}):(\\d{2})\\s*([AaPp][Mm])?").find(timeText)
            var hour = t?.groupValues?.get(1)?.toInt() ?: 9
            when (t?.groupValues?.get(3)?.uppercase()) {
                "PM" -> if (hour < 12) hour += 12
                "AM" -> if (hour == 12) hour = 0
            }
            val time = d?.let {
                Calendar.getInstance().apply {
                    set(it.groupValues[3].toInt(), it.groupValues[2].toInt() - 1, it.groupValues[1].toInt(),
                        hour, t?.groupValues?.get(2)?.toInt() ?: 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            val typeCell = cell(row, "Paper Type")
            val modeRaw = clean(typeCell?.selectFirst("b")?.text())
            val mode = if (modeRaw.contains(':')) clean(modeRaw.substringAfter(':')) else modeRaw
            var location = ""
            val red = typeCell?.select("b")?.firstOrNull { Regex("color\\s*:\\s*red", RegexOption.IGNORE_CASE).containsMatchIn(it.attr("style")) }
            if (red != null) {
                val lines = red.html().split(BR).map { htmlText(it) }
                val first = lines.firstOrNull { it.contains(':') } ?: lines.firstOrNull().orEmpty()
                location = clean(if (first.contains(':')) first.substringAfter(':') else first).trim('-', ' ')
            }
            ScheduledExam(cellText(row, "Course Code"), cellText(row, "Course Title"), time, mode, location)
        }
        val title = clean(doc.selectFirst("div.page-header h1")?.text()).ifEmpty { "Examination Schedule" }
        return ExamSchedule(title, exams.sortedBy { it.timeMs ?: Long.MAX_VALUE })
    }

    /* ---------- Profile ---------- */

    fun profile(html: String): Profile {
        val doc = Jsoup.parse(html)
        val front = doc.selectFirst("#lblNameIDCardFront1") ?: throw AmizoneException.Parse("ID card not found")
        val parts = front.html().split(BR).map { htmlText(it) }.filter { it.isNotEmpty() }
        val backText = clean(doc.selectFirst("#lblInfoIDCardBack1")?.text() ?: html)
        val enrollment = Regex("enrol+ment\\s*no\\.?\\s*:\\s*([A-Za-z0-9]+)", RegexOption.IGNORE_CASE).find(backText)?.groupValues?.get(1).orEmpty()
        return Profile(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" }, parts.getOrElse(2) { "" }, enrollment)
    }

    /** Name from the header of any logged-in page, e.g. <span class="user-info"><small>Welcome,</small> JOHN DOE</span>. */
    fun headerName(html: String): String {
        val info = Jsoup.parse(html).selectFirst(".user-info") ?: return ""
        info.select("small").remove()
        val name = clean(info.text())
            .replaceFirst(Regex("^(welcome|hi|hello)\\s*,?\\s*", RegexOption.IGNORE_CASE), "")
            .replaceFirst(Regex("^(mr|mrs|ms|miss|dr)\\.?\\s+", RegexOption.IGNORE_CASE), "")
        if (name != name.uppercase()) return name
        return name.lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
    }

    /* ---------- Results ---------- */

    private fun num(s: String) = s.replace(Regex("[^\\d.]"), "").toDoubleOrNull() ?: 0.0

    fun results(html: String): Results {
        val doc = Jsoup.parse(html)
        val courseRows = doc.select("tr").filter { cell(it, "Course Code") != null && cell(it, "Go") != null }
        val semRows = doc.select("tr").filter { cell(it, "SGPA") != null }
        if (courseRows.isEmpty() && semRows.isEmpty()) throw AmizoneException.Parse("No results found")
        return Results(
            courses = courseRows.map {
                CourseResult(cellText(it, "Course Code"), cellText(it, "Course Title"), cellText(it, "Go"),
                    num(cellText(it, "GP")), num(cellText(it, "ACU")), num(cellText(it, "Max Total")))
            },
            semesters = semRows.map {
                SemesterResult(cellText(it, "Semester"), num(cellText(it, "SGPA")), num(cellText(it, "CGPA")), cellText(it, "Back Papers"))
            },
        )
    }

    /* ---------- Document downloads ---------- */

    fun noticeDocs(html: String): List<NoticeDoc> =
        Jsoup.parse(html).select("tr").filter { cell(it, "Title") != null }.map {
            NoticeDoc(cellText(it, "Title"), cellText(it, "Description"), cell(it, "Downloads")?.selectFirst("a")?.attr("href").orEmpty())
        }.filter { it.url.startsWith("http", ignoreCase = true) }

    /* ---------- Programme structure → credits per course code ---------- */

    /**
     * Teaching hours from the Programme Structure, where every course reads like
     * "MATH211 - Applied Mathematics- III [ CU- 4.00 , L- 3.00 , T- 0.00 , P- 0.00 , … ]".
     */
    fun structure(html: String): Map<String, CourseHours> {
        val text = Jsoup.parse(html).text()
        val re = Regex(
            "([A-Z]{2,6}\\s?\\d{3}[A-Z]?)\\s*-[^\\[]{0,160}\\[\\s*CU\\s*-\\s*([\\d.]+)\\s*,\\s*L\\s*-\\s*([\\d.]+)\\s*,\\s*T\\s*-\\s*([\\d.]+)\\s*,\\s*P\\s*-\\s*([\\d.]+)",
            RegexOption.IGNORE_CASE,
        )
        val out = LinkedHashMap<String, CourseHours>()
        for (m in re.findAll(text)) {
            val code = m.groupValues[1].replace(" ", "").uppercase()
            val h = CourseHours(
                m.groupValues[2].toDoubleOrNull() ?: 0.0, m.groupValues[3].toDoubleOrNull() ?: 0.0,
                m.groupValues[4].toDoubleOrNull() ?: 0.0, m.groupValues[5].toDoubleOrNull() ?: 0.0,
            )
            if (h.cu > 0 || h.weekly > 0) out[code] = h
        }
        return out
    }

    fun credits(html: String): Map<String, Double> {
        val doc: Document = Jsoup.parse(html)
        val out = HashMap<String, Double>()
        val codeRe = Regex("^[A-Z]{2,6}\\s?\\d{3}[A-Z]?$")
        for (table in doc.select("table")) {
            val headers = (table.select("thead th").ifEmpty { table.selectFirst("tr")?.select("th,td") ?: emptyList() })
                .map { clean(it.text()).lowercase() }
            val codeIdx = headers.indexOfFirst { it.contains("code") }
            val creditIdx = headers.indexOfFirst { it.contains("credit") }
            for (row in table.select("tr")) {
                var code = clean(row.selectFirst("td[data-title*=Code]")?.text())
                var credit = clean(row.selectFirst("td[data-title*=Credit], td[data-title*=credit]")?.text()).toDoubleOrNull()
                if ((code.isEmpty() || credit == null) && codeIdx >= 0 && creditIdx >= 0) {
                    val cells = row.select("td")
                    if (cells.size > maxOf(codeIdx, creditIdx)) {
                        code = clean(cells[codeIdx].text())
                        credit = clean(cells[creditIdx].text()).toDoubleOrNull()
                    }
                }
                code = code.replace(" ", "").uppercase()
                if (codeRe.matches(code) && credit != null && credit > 0 && credit <= 30) out[code] = credit
            }
        }
        return out
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/data/AmizoneClient.kt`

```kotlin
package com.student.amizoneplus.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.FormBody
import okhttp3.Request
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Talks to s.amizone.net exactly like a browser: a GET for the login page's
 * anti-forgery token, then a form POST with your ID/password. Every request
 * re-signs in automatically when the session has expired.
 */
class AmizoneClient(private val credentials: () -> Credentials?) {

    companion object {
        const val BASE = "https://s.amizone.net"
    }

    data class Page(val text: String, val url: String, val code: Int)

    private suspend fun raw(path: String, form: Map<String, String>? = null, ajax: Boolean = false): Page = withContext(Dispatchers.IO) {
        val url = if (path.startsWith("http")) path else BASE + path
        val b = Request.Builder().url(url).header("Referer", "$BASE/")
        if (ajax) b.header("X-Requested-With", "XMLHttpRequest")
        if (form != null) b.post(FormBody.Builder().apply { form.forEach { (k, v) -> add(k, v) } }.build())
        try {
            Http.client.newCall(b.build()).execute().use { r ->
                if (r.code >= 500) throw AmizoneException.Network("Amizone returned ${r.code}. It may be down.")
                Page(r.body?.string().orEmpty(), r.request.url.toString(), r.code)
            }
        } catch (e: IOException) {
            throw AmizoneException.Network("Couldn't reach Amizone. Check your internet connection.")
        }
    }

    // Only one sign-in at a time: a refresh loads many pages in parallel, and several
    // simultaneous sign-ins overwrite each other's anti-forgery cookie and all fail.
    private val loginLock = Mutex()
    @Volatile private var lastLogin = 0L

    /** Signs in with the given (or saved) credentials. Returns when a session exists. */
    suspend fun login(creds: Credentials? = null): Unit = loginLock.withLock {
        // Another request just signed in while this one waited: nothing to do.
        if (creds == null && System.currentTimeMillis() - lastLogin < 15_000) return@withLock
        val c = creds ?: credentials() ?: throw AmizoneException.InvalidCredentials("No saved login.")
        val loginPage = raw("/")
        if (!Parsers.isLoginPage(loginPage.text)) { lastLogin = System.currentTimeMillis(); return@withLock } // already signed in
        // Send the form exactly as the page would: every hidden field it carries
        // (anti-forgery token, Salt/Challenge/Signature, honeypot…) plus your details.
        val form = Parsers.loginForm(loginPage.text)
            ?: throw AmizoneException.NeedsWebLogin("The Amizone login page has changed. Please sign in through the website view.")
        form["_UserName"] = c.username
        form["_Password"] = c.password
        val res = raw("/", form)
        if (Parsers.isLoginPage(res.text)) {
            if (Parsers.mentionsBadCredentials(res.text)) throw AmizoneException.InvalidCredentials()
            throw AmizoneException.NeedsWebLogin("Amizone wants a quick human check. Tap Sign in, tick the box and log in once.")
        }
        lastLogin = System.currentTimeMillis()
        Http.persistSession()
    }

    /** GET a page, signing in again if the session expired. */
    suspend fun get(path: String, ajax: Boolean = false): String {
        var res = raw(path, ajax = ajax)
        if (Parsers.isLoginPage(res.text)) {
            login()
            res = raw(path, ajax = ajax)
            if (Parsers.isLoginPage(res.text)) throw AmizoneException.NeedsWebLogin("Could not stay signed in to Amizone.")
        }
        return res.text
    }

    suspend fun home(): String = get("/Home")

    /**
     * A tiny signed-in request so the Amizone session doesn't expire from inactivity.
     * Never signs in by itself (that's left to the app), so it can't trip the human check.
     */
    suspend fun keepAlive(): Boolean {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        val res = raw("/Calendar/home/GetDiaryEvents?start=$d&end=$d", ajax = true)
        val ok = !Parsers.isLoginPage(res.text)
        if (ok) Http.persistSession()
        return ok
    }

    suspend fun schedule(fromMs: Long, days: Int): List<ScheduledClass> {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val start = Calendar.getInstance().apply {
            timeInMillis = fromMs; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, days) }
        return Parsers.schedule(get("/Calendar/home/GetDiaryEvents?start=${fmt.format(start.time)}&end=${fmt.format(end.time)}", ajax = true))
    }

    /**
     * Weekly L / T / P for your courses. Amizone's Programme Structure page is only a
     * semester picker; each semester's courses come from BindProgramList. Start at the
     * likely current semester and widen until every current course is found.
     */
    private suspend fun programmeHours(codes: List<String>, guess: Int): Map<String, CourseHours> {
        val want = codes.map { it.replace(" ", "").uppercase() }.toSet()
        val start = guess.coerceIn(1, 8)
        val order = (1..8).sortedBy { kotlin.math.abs(it - start) * 2 + (if (it < start) 1 else 0) }
        val out = HashMap<String, CourseHours>()
        for (sem in order) {
            if (want.isNotEmpty() && want.all { it in out }) break
            val found = runCatching { Parsers.structure(get("/Academics/ProgrammeStructure/BindProgramList?iSemester=$sem", ajax = true)) }.getOrDefault(emptyMap())
            found.forEach { (k, v) -> out.putIfAbsent(k, v) }
            if (want.isEmpty() && found.isNotEmpty()) break
        }
        return out
    }

    /** Fetches everything; sections that fail are listed in `errors` instead of failing the refresh. */
    suspend fun fetchAll(): AmizoneData = coroutineScope {
        val home = home()
        val errors = mutableListOf<String>()

        suspend fun <T> attempt(label: String, fallback: T, block: suspend () -> T): T = try {
            block()
        } catch (e: AmizoneException.Parse) {
            synchronized(errors) { errors += label }
            fallback
        } catch (e: AmizoneException.Network) {
            if (label in setOf("History", "Documents", "Credits", "Results")) {
                synchronized(errors) { errors += label }; fallback
            } else throw e
        }

        val now = System.currentTimeMillis()
        val courses = async { attempt("Courses", emptyList()) { Parsers.courses(get("/Academics/MyCourses"), BASE) } }
        val schedule = async { attempt("Timetable", emptyList()) { schedule(now, 8) } }
        val exams = async { attempt<ExamSchedule?>("Exam schedule", null) { Parsers.examSchedule(get("/Examination/ExamSchedule")) } }
        val profile = async {
            attempt<Profile?>("Profile", null) {
                try {
                    Parsers.profile(get("/IDCard"))
                } catch (e: AmizoneException.Parse) {
                    val name = Parsers.headerName(home)
                    if (name.isEmpty()) throw e
                    Profile(name, "", "", "")
                }
            }
        }
        val history = async { attempt("History", emptyList()) { schedule(now - 28L * 86_400_000, 28) } }
        val results = async { attempt<Results?>("Results", null) { Parsers.results(get("/Examination/Examination")) } }
        val notices = async { attempt("Documents", emptyList()) { Parsers.noticeDocs(get("/StudentDocument/StudentDocument")) } }

        val c = courses.await()
        var attendance = attempt("Attendance", emptyList()) { Parsers.attendance(home) }
        if (attendance.isEmpty() && c.isNotEmpty()) {
            attendance = c.filter { it.held > 0 }.map { AttendanceRecord(it.code, it.name, it.attended, it.held) }
            errors.remove("Attendance")
        }
        val r = results.await()
        val hours = programmeHours(c.map { it.code }, (r?.semesters?.size ?: 0) + 1)
        val creditMap = (r?.courses.orEmpty().filter { it.credits > 0 }.associate { it.code to it.credits }) +
            hours.filterValues { it.cu > 0 }.mapValues { it.value.cu }

        val quiet = setOf("History", "Documents", "Credits")
        AmizoneData(
            profile = profile.await(),
            attendance = attendance,
            schedule = schedule.await(),
            history = history.await(),
            exams = exams.await(),
            courses = c,
            results = r,
            notices = notices.await(),
            credits = creditMap,
            hours = hours,
            fetchedAt = System.currentTimeMillis(),
            errors = errors.filterNot { it in quiet },
        )
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/data/Lms.kt`

```kotlin
package com.student.amizoneplus.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import java.io.File
import java.net.URLDecoder

/**
 * Amizone LMS (Moodle at alms1.amizone.net/moodle_<semester>).
 *
 * Sign-in: Amizone's Home page has an "LMS" widget with one hidden form per
 * semester (usercode, token, userId) that POSTs to <moodle>/submit.php. Posting
 * it gives a Moodle session cookie — which, thanks to the shared cookie store,
 * also signs in every LMS page opened in the app's web view.
 *
 * Data: Moodle's standard AJAX API with the page's sesskey — the course list
 * and each course's sections exactly as the teacher arranged them.
 */
class Lms(private val amizone: AmizoneClient) {

    private data class Session(val wwwroot: String, val sesskey: String, val semester: String)

    private var session: Session? = null
    private val json = Json { ignoreUnknownKeys = true }

    private fun decodeHtml(s: String) =
        s.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")

    /** Semester buttons in the Amizone "LMS" widget, newest first. */
    suspend fun semesters(): List<LmsSemester> {
        val home = amizone.home()
        val out = ArrayList<LmsSemester>()
        val btn = Regex("<(a|button)\\b([^>]*)>([\\s\\S]{0,200}?)</\\1>", RegexOption.IGNORE_CASE)
        for (m in btn.findAll(home)) {
            val label = m.groupValues[3].replace(Regex("<[^>]+>"), "").replace(Regex("\\s+"), " ").trim()
            if (!Regex("20\\d\\d-20\\d\\d\\s*/\\s*(odd|even)", RegexOption.IGNORE_CASE).containsMatchIn(label)) continue
            val formId = Regex("getElementById\\(['\"]([^'\"]+)['\"]\\)\\.submit").find(m.groupValues[2])?.groupValues?.get(1) ?: continue
            val at = maxOf(home.indexOf("id=\"$formId\""), home.indexOf("id='$formId'"))
            if (at < 0) continue
            val start = home.lastIndexOf("<form", at)
            val end = home.indexOf("</form>", at)
            if (start < 0 || end < start) continue
            val form = home.substring(start, end)
            val action = decodeHtml(Regex("action=[\"']([^\"']+)", RegexOption.IGNORE_CASE).find(form)?.groupValues?.get(1).orEmpty())
            val body = Regex("<input\\b([^>]*)>", RegexOption.IGNORE_CASE).findAll(form).mapNotNull { inp ->
                val name = Regex("name=[\"']([^\"']+)", RegexOption.IGNORE_CASE).find(inp.groupValues[1])?.groupValues?.get(1) ?: return@mapNotNull null
                val value = decodeHtml(Regex("value=[\"']([^\"']*)", RegexOption.IGNORE_CASE).find(inp.groupValues[1])?.groupValues?.get(1).orEmpty())
                java.net.URLEncoder.encode(name, "UTF-8") + "=" + java.net.URLEncoder.encode(value, "UTF-8")
            }.joinToString("&")
            if (action.isNotEmpty()) out += LmsSemester(label, action, body)
        }
        return out
    }

    private suspend fun signIn(sem: LmsSemester, retried: Boolean = false): Session {
        val found = withContext(Dispatchers.IO) {
            val url = if (sem.action.startsWith("http")) sem.action else AmizoneClient.BASE + sem.action
            val req = Request.Builder().url(url)
                .header("Referer", AmizoneClient.BASE + "/Home")
                .post(sem.body.toRequestBody("application/x-www-form-urlencoded".toMediaType())).build()
            val html = Http.client.newCall(req).execute().use { it.body?.string().orEmpty() }
            val wwwroot = Regex("\"wwwroot\":\"([^\"]+)\"").find(html)?.groupValues?.get(1)?.replace("\\/", "/")
            val sesskey = Regex("\"sesskey\":\"([^\"]+)\"").find(html)?.groupValues?.get(1)
            if (wwwroot != null && sesskey != null) Session(wwwroot, sesskey, sem.label) else null
        }
        if (found != null) return found.also { session = it }
        // The widget's sign-in tokens go stale: take fresh ones from Home and try once more.
        if (!retried) semesters().firstOrNull { it.label == sem.label }?.let { return signIn(it, true) }
        throw AmizoneException.Network("Couldn't sign in to the ${sem.label} LMS. It may be closed for that semester.")
    }

    private suspend fun ensure(sem: LmsSemester): Session =
        session?.takeIf { it.semester == sem.label } ?: signIn(sem)

    private suspend fun ajax(sem: LmsSemester, method: String, args: JsonObject, retried: Boolean = false): kotlinx.serialization.json.JsonElement {
        val s = ensure(sem)
        val payload = buildJsonArray { add(buildJsonObject { put("index", 0); put("methodname", method); put("args", args) }) }.toString()
        val text = withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url("${s.wwwroot}/lib/ajax/service.php?sesskey=${s.sesskey}&info=$method")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            Http.client.newCall(req).execute().use { it.body?.string().orEmpty() }
        }
        val first = runCatching { json.parseToJsonElement(text).jsonArray[0].jsonObject }.getOrNull()
        val failed = first == null || (first["error"] as? JsonPrimitive)?.contentOrNull == "true"
        if (failed) {
            if (!retried) {
                session = null
                return ajax(sem, method, args, true)
            }
            throw AmizoneException.Network("LMS request failed.")
        }
        return first!!["data"]!!
    }

    suspend fun courses(sem: LmsSemester): List<LmsCourse> {
        val data = ajax(sem, "core_course_get_enrolled_courses_by_timeline_classification", buildJsonObject {
            put("offset", 0); put("limit", 0); put("classification", "all"); put("sort", "fullname")
        })
        val list = data.jsonObject["courses"]?.jsonArray ?: JsonArray(emptyList())
        return list.map { el ->
            val o = el.jsonObject
            val short = decodeHtml(o["shortname"]?.jsonPrimitive?.contentOrNull.orEmpty())
            val code = Regex("/\\s*([A-Z]{2,6}\\s?\\d{3}[A-Z]?)\\s*/").find(short)?.groupValues?.get(1)?.replace(" ", "").orEmpty()
            LmsCourse(o["id"]!!.jsonPrimitive.content, decodeHtml(o["fullname"]?.jsonPrimitive?.contentOrNull.orEmpty()), short, code)
        }
    }

    /** A course's sections and items in the teacher's own order. */
    suspend fun content(sem: LmsSemester, courseId: String): List<LmsSection> {
        val raw = ajax(sem, "core_courseformat_get_state", buildJsonObject { put("courseid", courseId.toInt()) })
        val state = (raw as? JsonPrimitive)?.contentOrNull?.let { json.parseToJsonElement(it) } ?: raw
        val obj = state.jsonObject
        val cms = obj["cm"]?.jsonArray.orEmpty().associateBy { it.jsonObject["id"]!!.jsonPrimitive.content }
        return obj["section"]?.jsonArray.orEmpty().map { s ->
            val so = s.jsonObject
            LmsSection(
                id = so["id"]!!.jsonPrimitive.content,
                title = decodeHtml(so["title"]?.jsonPrimitive?.contentOrNull.orEmpty()),
                items = so["cmlist"]?.jsonArray.orEmpty().mapNotNull { id ->
                    val cm = cms[id.jsonPrimitive.content]?.jsonObject ?: return@mapNotNull null
                    if (cm["uservisible"]?.jsonPrimitive?.contentOrNull == "false") return@mapNotNull null
                    LmsItem(
                        id = cm["id"]!!.jsonPrimitive.content,
                        name = decodeHtml(cm["name"]?.jsonPrimitive?.contentOrNull.orEmpty()),
                        // "module" is the internal type (folder, resource, url…); "modname" is only the
                        // translated display name ("Folder", "File"), so it must not be used for logic.
                        modname = (cm["module"]?.jsonPrimitive?.contentOrNull ?: cm["modname"]?.jsonPrimitive?.contentOrNull.orEmpty()).lowercase(),
                        url = cm["url"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    )
                },
            )
        }
    }

    /** Files inside a Moodle folder activity. */
    suspend fun folderFiles(sem: LmsSemester, item: LmsItem): List<LmsFile> {
        val s = ensure(sem)
        val html = withContext(Dispatchers.IO) {
            val url = item.url.ifEmpty { "${s.wwwroot}/mod/folder/view.php?id=${item.id}" }
            Http.client.newCall(Request.Builder().url(url).build()).execute().use { it.body?.string().orEmpty() }
        }
        val doc = Jsoup.parse(html)
        val seen = HashSet<String>()
        return doc.select("a[href*=pluginfile.php]").mapNotNull { a ->
            val href = a.attr("href")
            if (!seen.add(href)) return@mapNotNull null
            val fromUrl = URLDecoder.decode(href.substringBefore('?').substringAfterLast('/'), "UTF-8")
            val name = (a.selectFirst(".fp-filename")?.text() ?: a.text()).trim().ifEmpty { fromUrl }
            val sub = href.substringBefore('?').substringAfter("/mod_folder/content/", "").split('/').drop(1).dropLast(1)
            LmsFile(name, href, sub.joinToString(" / ") { URLDecoder.decode(it, "UTF-8") })
        }
    }

    sealed interface OpenResult {
        data object Opened : OpenResult
        data class Web(val url: String) : OpenResult
    }

    /** Downloads with the LMS session and opens it in a viewer app. Web pages come back as [OpenResult.Web]. */
    suspend fun open(context: Context, sem: LmsSemester, url: String, nameHint: String): OpenResult {
        val s = ensure(sem)
        return when (val r = Downloads.open(context, url, nameHint, referer = s.wwwroot + "/")) {
            is Downloads.Result.Web -> OpenResult.Web(r.url)
            Downloads.Result.Opened -> OpenResult.Opened
        }
    }

    /** Resource / link items open their target directly. */
    fun itemUrl(item: LmsItem) =
        if (item.modname == "resource" || item.modname == "url") item.url + (if (item.url.contains('?')) "&" else "?") + "redirect=1" else item.url

    companion object {
        val MIME = mapOf(
            "pdf" to "application/pdf",
            "ppt" to "application/vnd.ms-powerpoint",
            "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "doc" to "application/msword",
            "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls" to "application/vnd.ms-excel",
            "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "zip" to "application/zip",
            "txt" to "text/plain",
            "png" to "image/png",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "mp4" to "video/mp4",
        )
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/data/Downloads.kt`

```kotlin
package com.student.amizoneplus.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.net.URLDecoder

/**
 * Downloads a file with the app's signed-in connection (shared cookies) into the
 * cache and opens it in the phone's viewer app (PDF, PowerPoint, Word…).
 */
object Downloads {
    sealed interface Result {
        data object Opened : Result
        data class Web(val url: String) : Result // turned out to be a web page
    }

    val MIME = mapOf(
        "pdf" to "application/pdf",
        "ppt" to "application/vnd.ms-powerpoint",
        "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "doc" to "application/msword",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xls" to "application/vnd.ms-excel",
        "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "zip" to "application/zip",
        "txt" to "text/plain",
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "mp4" to "video/mp4",
    )

    private data class Got(val bytes: ByteArray?, val name: String, val mime: String, val finalUrl: String, val html: Boolean)

    suspend fun open(context: Context, url: String, nameHint: String, referer: String? = null): Result {
        val got = withContext(Dispatchers.IO) {
            val req = Request.Builder().url(url).apply { if (referer != null) header("Referer", referer) }.build()
            Http.client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) throw AmizoneException.Network("Download failed (HTTP ${r.code}).")
                val type = r.header("Content-Type").orEmpty().substringBefore(';').trim()
                if (type == "text/html") return@use Got(null, "", type, r.request.url.toString(), true)
                val disp = r.header("Content-Disposition").orEmpty()
                val fromHeader = Regex("filename\\*?=(?:UTF-8'')?[\"']?([^\"';]+)", RegexOption.IGNORE_CASE).find(disp)?.groupValues?.get(1)
                    ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
                val fromUrl = runCatching { URLDecoder.decode(r.request.url.encodedPath.substringAfterLast('/'), "UTF-8") }.getOrDefault("")
                var n = (fromHeader ?: fromUrl.takeIf { it.contains('.') } ?: URLUtil.guessFileName(url, disp, type).takeIf { it.contains('.') } ?: nameHint)
                    .replace(Regex("[\\\\/:*?\"<>|]+"), "_").take(120)
                if (!n.contains('.')) MIME.entries.firstOrNull { it.value == type }?.let { n = "$n.${it.key}" }
                Got(r.body?.bytes(), n, type, r.request.url.toString(), false)
            }
        }
        if (got.html || got.bytes == null) return Result.Web(got.finalUrl)

        val dir = File(context.cacheDir, "lms").apply { mkdirs() }
        val file = File(dir, got.name).apply { writeBytes(got.bytes) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val type = MIME[got.name.substringAfterLast('.', "").lowercase()] ?: got.mime.ifEmpty { "*/*" }
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, type)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: ActivityNotFoundException) {
            val share = Intent(Intent.ACTION_SEND).setType(type).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(share, got.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        return Result.Opened
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/data/Updater.kt`

```kotlin
package com.student.amizoneplus.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.student.amizoneplus.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.io.File

/**
 * In-app updates from GitHub Releases.
 *
 * Publishing an update = a GitHub Release tagged like "v2.2.0" with the signed APK attached.
 * The app reads the latest release, compares it with its own version, and offers it; the
 * APK is downloaded into the cache and handed to Android's installer (the user confirms).
 * Android only accepts it when it is signed with the same key as the installed app.
 */
object Updater {
    data class Release(val version: String, val tag: String, val notes: String, val apkUrl: String, val sizeBytes: Long)

    private val json = Json { ignoreUnknownKeys = true }
    private const val PREFS = "updater"

    val repo: String get() = BuildConfig.UPDATE_REPO
    val enabled: Boolean get() = repo.contains('/') && !repo.startsWith("YOUR_")
    val current: String get() = BuildConfig.VERSION_NAME

    /** "v2.10.1" > "2.9" — numeric, part by part. */
    fun newer(candidate: String, than: String): Boolean {
        fun parts(v: String) = v.trim().removePrefix("v").removePrefix("V").split('.', '-', '+')
            .map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val a = parts(candidate); val b = parts(than)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** Latest release if it's newer than this app, else null. Throws on network trouble. */
    suspend fun check(): Release? = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext null
        val req = Request.Builder().url("https://api.github.com/repos/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json").build()
        val text = Http.client.newCall(req).execute().use { r ->
            if (r.code == 404) return@withContext null // no releases yet
            if (!r.isSuccessful) throw AmizoneException.Network("Couldn't check for updates (HTTP ${r.code}).")
            r.body?.string().orEmpty()
        }
        val o = json.parseToJsonElement(text).jsonObject
        val tag = o["tag_name"]?.jsonPrimitive?.contentOrNull ?: return@withContext null
        val apk = o["assets"]?.jsonArray.orEmpty().map { it.jsonObject }
            .firstOrNull { it["name"]?.jsonPrimitive?.contentOrNull.orEmpty().endsWith(".apk", true) } ?: return@withContext null
        val rel = Release(
            version = tag.removePrefix("v").removePrefix("V"),
            tag = tag,
            notes = plain(o["body"]?.jsonPrimitive?.contentOrNull.orEmpty()),
            apkUrl = apk["browser_download_url"]?.jsonPrimitive?.contentOrNull ?: return@withContext null,
            sizeBytes = apk["size"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
        )
        if (newer(rel.version, current)) rel else null
    }

    /** Release notes are Markdown; show them as plain text with bullets. */
    private fun plain(md: String): String = md.replace("\r", "").lines().joinToString("\n") { line ->
        val l = line.trimEnd()
        when {
            l.trimStart().startsWith("#") -> l.trimStart().trimStart('#').trim().uppercase()
            Regex("^\\s*[*\\-+] ").containsMatchIn(l) -> "• " + l.trimStart().drop(2)
            else -> l
        }.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1").replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("\\[([^\\]]+)]\\([^)]+\\)"), "$1")
    }.trim()

    /** Check at most every few hours on launch; "Later" hides a version for a day. */
    fun shouldAutoCheck(context: Context): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return System.currentTimeMillis() - p.getLong("lastCheck", 0) > 4 * 3_600_000L
    }

    fun markChecked(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("lastCheck", System.currentTimeMillis()).apply()

    fun snoozed(context: Context, version: String): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getString("snoozedVersion", null) == version && System.currentTimeMillis() < p.getLong("snoozedUntil", 0)
    }

    fun snooze(context: Context, version: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("snoozedVersion", version).putLong("snoozedUntil", System.currentTimeMillis() + 86_400_000L).apply()

    /** Downloads the APK into the cache, reporting progress 0..1. */
    suspend fun download(context: Context, rel: Release, progress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val out = File(dir, "amizoneplus-${rel.version}.apk")
        Http.client.newCall(Request.Builder().url(rel.apkUrl).build()).execute().use { r ->
            if (!r.isSuccessful) throw AmizoneException.Network("Download failed (HTTP ${r.code}).")
            val body = r.body ?: throw AmizoneException.Network("Download failed.")
            val total = body.contentLength().takeIf { it > 0 } ?: rel.sizeBytes
            body.byteStream().use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        done += n
                        if (total > 0) progress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        }
        out
    }

    /** Android needs a one-time "allow installs from this app" for updates. */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Hands the APK to Android's installer; the user taps Install. Data and login are kept. */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/data/Storage.kt`

```kotlin
package com.student.amizoneplus.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Local storage:
 *  - credentials: AES-GCM encrypted with a key that never leaves the Android Keystore
 *  - data cache / LMS cache: JSON files in app storage (shown instantly, works offline)
 *  - settings & small flags: SharedPreferences
 */
class Storage(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("amizone", Context.MODE_PRIVATE)
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /* ── Credentials ── */

    private val keyAlias = "amizone_credentials_key"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return gen.generateKey()
    }

    fun saveCredentials(c: Credentials) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val enc = cipher.doFinal("${c.username}\u0000${c.password}".toByteArray())
        prefs.edit()
            .putString("cred_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("cred_data", Base64.encodeToString(enc, Base64.NO_WRAP))
            .apply()
    }

    fun loadCredentials(): Credentials? {
        val iv = prefs.getString("cred_iv", null) ?: return null
        val data = prefs.getString("cred_data", null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            val (u, p) = String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP))).split('\u0000', limit = 2)
            Credentials(u, p)
        }.getOrNull()
    }

    /* ── JSON files ── */

    private fun file(name: String) = File(app.filesDir, name)

    fun <T> writeJson(name: String, serializer: KSerializer<T>, value: T) {
        runCatching { file(name).apply { parentFile?.mkdirs() }.writeText(json.encodeToString(serializer, value)) }
    }

    fun <T> readJson(name: String, serializer: KSerializer<T>): T? =
        runCatching { file(name).takeIf { it.exists() }?.readText()?.let { json.decodeFromString(serializer, it) } }.getOrNull()

    fun saveCache(d: AmizoneData) = writeJson("cache.json", AmizoneData.serializer(), d)
    fun loadCache(): AmizoneData? = readJson("cache.json", AmizoneData.serializer())

    /* ── Settings & flags ── */

    fun loadSettings(): Settings =
        prefs.getString("settings", null)?.let { runCatching { json.decodeFromString(Settings.serializer(), it) }.getOrNull() } ?: Settings()

    fun saveSettings(s: Settings) = prefs.edit().putString("settings", json.encodeToString(Settings.serializer(), s)).apply()

    var alertedCourses: Set<String>
        get() = prefs.getStringSet("alerted", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("alerted", v).apply()

    var seenBadges: Set<String>
        get() = prefs.getStringSet("badges_seen", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("badges_seen", v).apply()

    var reminderIds: Set<String>
        get() = prefs.getStringSet("reminder_ids", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("reminder_ids", v).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
        app.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(keyAlias) }
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/data/Repository.kt`

```kotlin
package com.student.amizoneplus.data

import android.content.Context
import com.student.amizoneplus.work.Notifier

/**
 * Single place that knows how to refresh: fetch from Amizone, keep previous
 * values for sections that failed, cache, then update alerts and reminders.
 * Used by both the UI and the background worker.
 */
class Repository private constructor(context: Context) {
    val storage = Storage(context)
    val client = AmizoneClient { storage.loadCredentials() }
    val lms = Lms(client)
    private val app = context.applicationContext

    suspend fun refreshAll(): AmizoneData {
        val fresh = client.fetchAll()
        val old = storage.loadCache()
        val merged = fresh.copy(
            profile = fresh.profile ?: old?.profile,
            exams = fresh.exams ?: old?.exams,
            courses = fresh.courses.ifEmpty { old?.courses.orEmpty() },
            attendance = fresh.attendance.ifEmpty { old?.attendance.orEmpty() },
            schedule = if ("Timetable" in fresh.errors) old?.schedule.orEmpty() else fresh.schedule,
            history = fresh.history.ifEmpty { old?.history.orEmpty() },
            results = fresh.results ?: old?.results,
            notices = fresh.notices.ifEmpty { old?.notices.orEmpty() },
            credits = fresh.credits.ifEmpty { old?.credits.orEmpty() },
            hours = fresh.hours.ifEmpty { old?.hours.orEmpty() },
        )
        storage.saveCache(merged)
        Notifier.process(app, merged, storage.loadSettings(), storage)
        return merged
    }

    companion object {
        @Volatile private var instance: Repository? = null
        fun get(context: Context): Repository =
            instance ?: synchronized(this) { instance ?: Repository(context).also { instance = it } }
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/logic/Attendance.kt`

```kotlin
package com.student.amizoneplus.logic

import com.student.amizoneplus.data.AmizoneData
import com.student.amizoneplus.data.Settings
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

fun percent(attended: Int, held: Int): Double = if (held == 0) 100.0 else attended * 100.0 / held

/** Amity: a course meets for 15 weeks, so planned classes = 15 × (lectures + practicals per week). */
const val WEEKS = 15

/** Targets you can pick. 75% is Amizone's minimum. */
val TARGETS = listOf(75, 80, 85, 90, 95)

data class SemesterPlan(
    val planned: Int, // 15 × (L + P), or more if extra classes were already held
    val held: Int,
    val attended: Int,
    val remaining: Int,
    val required: Int,
    val need: Int, // of the remaining, how many you must attend
    val canMiss: Int, // of the remaining, how many you can still skip
    val reachable: Boolean,
    val bestPct: Double, // final % if you attend every remaining class
    val extraNeeded: Int, // when out of reach: extra classes to get scheduled (and attend) to hit the target
    val basePlanned: Int = planned, // 15 × (L + P), before any extra classes
    val extraHeld: Int = 0, // classes already held beyond the plan
)

/** Extra classes x (all attended) so that (attended + remaining + x) / (planned + x) ≥ target. */
fun extraClassesFor(attended: Int, remaining: Int, planned: Int, threshold: Int): Int {
    if (threshold >= 100) return 0
    val x = (threshold * planned - 100.0 * (attended + remaining)) / (100 - threshold)
    return max(0, ceil(x - 1e-9).toInt())
}

/** The target applies to the whole semester's planned classes: 15 × weekly classes. */
fun semesterPlan(attended: Int, held: Int, weekly: Double, threshold: Int): SemesterPlan {
    val base = floor(weekly * WEEKS + 1e-9).toInt() // 0.5 a week → 7, not 8
    if (held >= base && base > 0) {
        // Every planned class is done and extras are running: judge on what's been held.
        val ok = percent(attended, held) >= threshold
        return SemesterPlan(
            planned = held, held = held, attended = attended, remaining = 0,
            required = ceil(threshold / 100.0 * held - 1e-9).toInt(),
            need = if (ok) 0 else mustAttend(attended, held, threshold),
            canMiss = if (ok) canSkip(attended, held, threshold) else 0,
            reachable = ok, bestPct = percent(attended, held),
            extraNeeded = if (ok) 0 else mustAttend(attended, held, threshold),
            basePlanned = base, extraHeld = held - base,
        )
    }
    val planned = max(base, held)
    val remaining = planned - held
    val required = ceil(threshold / 100.0 * planned - 1e-9).toInt()
    val need = max(0, required - attended)
    val reachable = need <= remaining
    return SemesterPlan(
        planned, held, attended, remaining, required, need,
        if (reachable) remaining - need else 0, reachable,
        if (planned > 0) (attended + remaining) * 100.0 / planned else 100.0,
        if (reachable) 0 else extraClassesFor(attended, remaining, planned, threshold),
    )
}

fun canSkip(attended: Int, held: Int, threshold: Int) = max(0, floor(attended * 100.0 / threshold - held).toInt())

fun mustAttend(attended: Int, held: Int, threshold: Int) =
    if (threshold >= 100) (if (attended < held) Int.MAX_VALUE else 0)
    else max(0, ceil((threshold * held - 100.0 * attended) / (100 - threshold)).toInt())

private fun plural(n: Int, w: String) = "$n $w${if (n == 1) "" else "es"}"

/** One-line status; uses the semester plan when the weekly classes are known. */
fun statusText(attended: Int, held: Int, threshold: Int, weekly: Double? = null): String {
    if (weekly != null && weekly > 0) {
        val p = semesterPlan(attended, held, weekly, threshold)
        if (p.extraHeld > 0 || (p.remaining == 0 && p.basePlanned > 0)) {
            val done = "All ${p.basePlanned} planned held" + if (p.extraHeld > 0) " (+${p.extraHeld} extra)" else ""
            return if (p.reachable) "$done · can still skip ${p.canMiss}" else "$done · attend the next ${p.need} extra"
        }
        if (!p.reachable) return "Out of reach for $threshold% — needs ${plural(p.extraNeeded, "extra class")}"
        if (p.need == 0) return if (p.remaining > 0) "Safe — you can miss ${p.canMiss} of ${p.remaining} left" else "Semester complete"
        return "Attend ${p.need} of ${p.remaining} left · can miss ${p.canMiss}"
    }
    if (held == 0) return "No classes held yet"
    if (percent(attended, held) >= threshold) {
        val k = canSkip(attended, held, threshold)
        return if (k == 0) "On the edge — don't miss the next class" else "You can skip ${plural(k, "class")}"
    }
    return "Attend ${plural(mustAttend(attended, held, threshold), "class")} more to reach $threshold%"
}

/** Where a course's weekly classes came from. */
enum class Basis { Override, Structure, Credits }

/**
 * Classes per week that count for attendance: your override first, then
 * L + P from Amizone's Programme Structure, then (if that's missing) credit units.
 */
fun weeklyFor(code: String, data: AmizoneData?, settings: Settings): Pair<Double, Basis>? {
    val key = code.replace(" ", "").uppercase()
    (settings.weeklyOverrides[key] ?: settings.weeklyOverrides[code])?.let { return it to Basis.Override }
    (data?.hours?.get(key) ?: data?.hours?.get(code))?.weekly?.takeIf { it > 0 }?.let { return it to Basis.Structure }
    (data?.credits?.get(key) ?: data?.credits?.get(code))?.let { return it to Basis.Credits }
    return null
}

/** Old name, still used by a few screens: just the weekly number. */
fun creditsFor(code: String, data: AmizoneData?, settings: Settings): Double? = weeklyFor(code, data, settings)?.first
```

---

## `app/src/main/java/com/student/amizoneplus/logic/Insights.kt`

```kotlin
package com.student.amizoneplus.logic

import com.student.amizoneplus.data.AmizoneData
import com.student.amizoneplus.data.ScheduledClass
import java.util.Calendar

private fun ScheduledClass.marked() = state == "present" || state == "absent"

/** Attendance rate per weekday (Mon..Sun); null when no marked classes that day. */
fun weekdayRates(history: List<ScheduledClass>, courseCode: String? = null): List<Double?> {
    val p = IntArray(7)
    val t = IntArray(7)
    for (c in history) {
        if (!c.marked() || (courseCode != null && c.code != courseCode)) continue
        val dow = Calendar.getInstance().apply { timeInMillis = c.startMs }.get(Calendar.DAY_OF_WEEK)
        val d = (dow + 5) % 7 // Mon=0
        t[d]++
        if (c.state == "present") p[d]++
    }
    return (0 until 7).map { if (t[it] > 0) p[it].toDouble() / t[it] else null }
}

/** One day of the current week, as the Today screen's circles show it. */
data class WeekDay(val label: String, val epochDay: Long, val kind: Kind, val present: Int, val marked: Int, val total: Int) {
    enum class Kind { Rate, Free, Pending, Future }
    val rate: Double get() = if (marked > 0) present.toDouble() / marked else 0.0
}

/**
 * Monday..Sunday of this week from the real classes on each date:
 * a percentage only for days whose classes were actually marked, [WeekDay.Kind.Free]
 * for past days with no classes at all (no fake 100%), and [WeekDay.Kind.Future] ahead.
 */
fun currentWeek(history: List<ScheduledClass>, schedule: List<ScheduledClass>, now: Long = System.currentTimeMillis()): List<WeekDay> {
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val byDay = (history + schedule)
        .distinctBy { it.code + "@" + it.startMs }
        .groupBy { java.time.Instant.ofEpochMilli(it.startMs).atZone(zone).toLocalDate() }
    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return (0 until 7).map { i ->
        val d = monday.plusDays(i.toLong())
        val cs = byDay[d].orEmpty()
        val marked = cs.count { it.marked() }
        val present = cs.count { it.state == "present" }
        val kind = when {
            d.isAfter(today) -> WeekDay.Kind.Future
            cs.isEmpty() -> WeekDay.Kind.Free
            marked == 0 -> WeekDay.Kind.Pending
            else -> WeekDay.Kind.Rate
        }
        WeekDay(labels[i], d.toEpochDay(), kind, present, marked, cs.size)
    }
}

/** Consecutive "present" classes, counting back from the most recent marked class. */
fun presentStreak(history: List<ScheduledClass>): Int =
    history.filter { it.marked() }.sortedByDescending { it.startMs }.takeWhile { it.state == "present" }.size

data class Badge(val id: String, val short: String, val tag: String, val title: String, val detail: String)

fun earnedBadges(data: AmizoneData, threshold: Int): List<Badge> {
    val out = mutableListOf<Badge>()
    val att = data.attendance.filter { it.held > 0 }
    if (att.isNotEmpty() && att.all { percent(it.attended, it.held) >= threshold }) {
        out += Badge("all-above-$threshold", "$threshold%", "Steady", "All subjects on track", "Every subject is at or above $threshold%.")
    }
    val weekAgo = System.currentTimeMillis() - 7L * 86_400_000
    val lastWeek = data.history.filter { it.marked() && it.startMs >= weekAgo }
    if (lastWeek.size >= 5 && lastWeek.all { it.state == "present" }) {
        val wk = Calendar.getInstance().run { "${get(Calendar.YEAR)}w${get(Calendar.WEEK_OF_YEAR)}" }
        out += Badge("perfect-week-$wk", "7d", "Perfect", "Perfect week", "You attended all ${lastWeek.size} classes in the last 7 days.")
    }
    val streak = presentStreak(data.history)
    for (n in listOf(10, 25, 50)) if (streak >= n) {
        out += Badge("streak-$n", "$n", "Streak", "$n-class streak", "$streak classes in a row without missing one.")
    }
    val cgpa = data.results?.semesters?.lastOrNull()?.cgpa ?: 0.0
    if (cgpa >= 9) out += Badge("cgpa-9", "%.1f".format(cgpa), "Scholar", "CGPA 9+", "Your CGPA is %.2f.".format(cgpa))
    else if (cgpa >= 8) out += Badge("cgpa-8", "%.1f".format(cgpa), "Achiever", "CGPA 8+", "Your CGPA is %.2f.".format(cgpa))
    return out
}

val ALL_BADGES = listOf(
    Badge("l1", "75%", "Steady", "All subjects on track", "Every subject at or above your target."),
    Badge("l2", "7d", "Perfect", "Perfect week", "Attend every class for 7 days."),
    Badge("l3", "10", "Streak", "10-class streak", "10 classes in a row."),
    Badge("l4", "25", "Streak", "25-class streak", "25 classes in a row."),
    Badge("l5", "50", "Streak", "50-class streak", "50 classes in a row."),
    Badge("l6", "8.0", "Achiever", "CGPA 8+", "Reach a CGPA of 8."),
    Badge("l7", "9.0", "Scholar", "CGPA 9+", "Reach a CGPA of 9."),
)
```

---

## `app/src/main/java/com/student/amizoneplus/logic/When.kt`

```kotlin
package com.student.amizoneplus.logic

import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date

/** How long an exam is treated as "happening now" after it starts. */
const val EXAM_LENGTH_MS = 3 * 3_600_000L

/** Calendar days from [now] to [t] (0 = same day, 1 = tomorrow), not 24-hour blocks. */
fun daysUntil(t: Long, now: Long = System.currentTimeMillis()): Int {
    val z = ZoneId.systemDefault()
    return ChronoUnit.DAYS.between(Instant.ofEpochMilli(now).atZone(z).toLocalDate(), Instant.ofEpochMilli(t).atZone(z).toLocalDate()).toInt()
}

/** "happening now" · "today at 10:00 am" · "tomorrow at 2:00 pm" · "in 4 days" · "done". */
fun examWhen(t: Long, now: Long = System.currentTimeMillis()): String {
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(t))
    val d = daysUntil(t, now)
    return when {
        now >= t && now < t + EXAM_LENGTH_MS -> "happening now"
        now >= t -> "done"
        d == 0 -> "today at $time"
        d == 1 -> "tomorrow at $time"
        else -> "in $d days"
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/logic/CourseNames.kt`

```kotlin
package com.student.amizoneplus.logic

/**
 * Short, familiar names for courses ("Object Oriented Programming Using C++" → "OOPs",
 * "Applied Mathematics- III" → "Maths III"). Known courses use the names students
 * actually say; anything else long becomes an acronym. Roman-numeral parts are kept.
 */
private val KNOWN = listOf(
    "object oriented programming" to "OOPs",
    "data structure" to "DSA",
    "design and analysis of algorithm" to "DAA",
    "applied mathematics" to "Maths",
    "engineering mathematics" to "Maths",
    "discrete mathematics" to "Discrete Maths",
    "probability and statistics" to "Prob & Stats",
    "basic electronics" to "Electronics",
    "digital electronics and computer organi" to "DECO",
    "digital electronics" to "Digital Elec.",
    "machine learning: theory" to "ML",
    "electronic devices" to "EDC",
    "elementary german" to "German",
    "elementary french" to "French",
    "elementary spanish" to "Spanish",
    "elementary japanese" to "Japanese",
    "operating system" to "OS",
    "database management" to "DBMS",
    "computer network" to "CN",
    "computer organi" to "COA",
    "computer architecture" to "COA",
    "theory of computation" to "TOC",
    "compiler" to "Compilers",
    "software engineering" to "SE",
    "artificial intelligence" to "AI",
    "machine learning" to "ML",
    "deep learning" to "DL",
    "web development" to "Web Dev",
    "environmental studies" to "EVS",
    "environmental science" to "EVS",
    "communication skill" to "Comm. Skills",
    "professional communication" to "Prof. Comm.",
    "behavioural science" to "Behavioural Sci.",
    "building self-reliance" to "BSR",
    "human values" to "Human Values",
    "engineering physics" to "Physics",
    "engineering chemistry" to "Chemistry",
)

private val SKIP = setOf("of", "and", "&", "using", "in", "for", "the", "to", "with", "a", "an", "on")

private val NUMERAL = Regex("""(?:-|\s)\s*(I{1,3}|IV|V|VI{0,3}|IX|X)\s*$""", RegexOption.IGNORE_CASE)

fun shortCourseName(name: String, max: Int = 16): String {
    val clean = name.replace(Regex("""\s+"""), " ").trim()
    val roman = NUMERAL.find(clean)?.groupValues?.get(1)?.uppercase()
    val base = if (roman != null) clean.substring(0, NUMERAL.find(clean)!!.range.first).trim().trimEnd('-', ' ') else clean
    val known = KNOWN.firstOrNull { base.lowercase().contains(it.first) }?.second
    val short = when {
        known != null -> known
        base.length <= max -> base
        else -> base.split(' ', '-', '/').filter { it.isNotBlank() && it.lowercase() !in SKIP }
            .joinToString("") { w -> if (w.all { it.isUpperCase() || it.isDigit() || it == '+' }) w else w.first().uppercase() }
    }
    return if (roman != null) "$short $roman" else short
}
```

---

## `app/src/main/java/com/student/amizoneplus/work/Notifier.kt`

```kotlin
package com.student.amizoneplus.work

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.student.amizoneplus.MainActivity
import com.student.amizoneplus.R
import com.student.amizoneplus.data.AmizoneData
import com.student.amizoneplus.data.Settings
import com.student.amizoneplus.data.Storage
import com.student.amizoneplus.logic.percent
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/** Low-attendance alerts (immediate) and class / exam reminders (AlarmManager). */
object Notifier {
    const val CH_REMINDERS = "class-reminders"
    const val CH_ALERTS = "attendance-alerts"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_REMINDERS, "Class & exam reminders", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_ALERTS, "Low attendance alerts", NotificationManager.IMPORTANCE_DEFAULT))
    }

    private fun canPost(context: Context) =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** A reminder drawn as an app-styled card (see [NotifArt]). Falls back to plain text if drawing fails. */
    fun showCard(context: Context, id: Int, title: String, body: String, tag: String, name: String, detail: String, kind: String, mood: NotifArt.Mood) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val b = NotificationCompat.Builder(context, CH_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setColor(0xFF6F73F4.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
        runCatching {
            val small = android.widget.RemoteViews(context.packageName, R.layout.notif_art_small)
            small.setImageViewBitmap(R.id.art, NotifArt.card(context, tag, name, detail, kind, mood, big = false))
            val big = android.widget.RemoteViews(context.packageName, R.layout.notif_art_big)
            big.setImageViewBitmap(R.id.art, NotifArt.card(context, tag, name, detail, kind, mood, big = true))
            b.setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(small).setCustomBigContentView(big)
        }.onFailure { b.setStyle(NotificationCompat.BigTextStyle().bigText(body)) }
        try {
            NotificationManagerCompat.from(context).notify(id, b.build())
        } catch (_: SecurityException) {
        }
    }

    fun show(context: Context, id: Int, channel: String, title: String, body: String) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
        }
    }

    fun process(context: Context, data: AmizoneData, s: Settings, storage: Storage) {
        lowAttendance(context, data, s, storage)
        scheduleReminders(context, data, s, storage)
    }

    /** Alert once per course when it drops below the target; re-arm when it recovers. */
    private fun lowAttendance(context: Context, data: AmizoneData, s: Settings, storage: Storage) {
        val alerted = storage.alertedCourses.toMutableSet()
        for (a in data.attendance) {
            if (a.held == 0) continue
            val p = percent(a.attended, a.held)
            if (p < s.threshold && a.code !in alerted) {
                alerted += a.code
                if (s.lowAttendanceAlerts) {
                    show(context, a.code.hashCode(), CH_ALERTS, "Low attendance: ${a.name}",
                        "You're at %.1f%% (%d/%d), below %d%%.".format(p, a.attended, a.held, s.threshold))
                }
            } else if (p >= s.threshold) {
                alerted -= a.code
            }
        }
        storage.alertedCourses = alerted
    }

    private fun scheduleReminders(context: Context, data: AmizoneData, s: Settings, storage: Storage) {
        val am = context.getSystemService(AlarmManager::class.java)
        // Cancel what we scheduled last time.
        for (id in storage.reminderIds) am.cancel(pending(context, id.toInt(), "", ""))
        val now = System.currentTimeMillis()
        val ids = mutableSetOf<String>()
        val tf = DateFormat.getTimeInstance(DateFormat.SHORT)

        fun at(time: Long, title: String, body: String, tag: String, name: String, kind: String, mood: String) {
            if (time <= now || ids.size >= 60) return
            val id = (title + time).hashCode()
            val pi = pending(context, id, title, body, tag, name, kind, mood)
            // Exact where allowed (so "8 pm the night before" really is 8 pm), else best effort.
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
            ids += id.toString()
        }

        if (s.classReminders) for (c in data.schedule) {
            val detail = listOf(tf.format(Date(c.startMs)), c.room.takeIf { it.isNotBlank() }?.let { "Room $it" }, c.faculty)
                .filter { !it.isNullOrBlank() }.joinToString(" · ")
            at(c.startMs - s.reminderMinutes * 60_000L, "${c.name} in ${s.reminderMinutes} min", detail,
                "Class in ${s.reminderMinutes} min", c.name, "class", "sit")
        }
        if (s.examReminders) for (e in data.exams?.exams.orEmpty()) {
            val t = e.timeMs ?: continue
            // The evening before (8 pm). If the exam is early on the day after tomorrow's
            // midnight edge, this is still the previous calendar day.
            val eve = Calendar.getInstance().apply { timeInMillis = t; add(Calendar.DAY_OF_YEAR, -1); set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            val body = listOf(tf.format(Date(t)), e.mode, e.location).filter { it.isNotBlank() }.joinToString(" · ")
            at(eve.timeInMillis, "Exam tomorrow: ${e.name}", body, "Exam tomorrow · ${tf.format(Date(t))}", e.name, "exam", "sleep")
            at(t - 3_600_000L, "Exam in 1 hour: ${e.name}", body, "Exam in 1 hour", e.name, "exam", "sit")
        }
        storage.reminderIds = ids
    }

    private fun pending(context: Context, id: Int, title: String, body: String, tag: String = "", name: String = "", kind: String = "", mood: String = ""): PendingIntent =
        PendingIntent.getBroadcast(
            context, id,
            Intent(context, ReminderReceiver::class.java).putExtra("title", title).putExtra("body", body).putExtra("id", id)
                .putExtra("tag", tag).putExtra("name", name).putExtra("kind", kind).putExtra("mood", mood),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
```

---

## `app/src/main/java/com/student/amizoneplus/work/ReminderReceiver.kt`

```kotlin
package com.student.amizoneplus.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Shows a class / exam reminder scheduled by [Notifier]. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: return
        val id = intent.getIntExtra("id", title.hashCode())
        val body = intent.getStringExtra("body").orEmpty()
        val kind = intent.getStringExtra("kind").orEmpty()
        if (kind.isEmpty()) return Notifier.show(context, id, Notifier.CH_REMINDERS, title, body)
        Notifier.showCard(
            context, id, title, body,
            tag = intent.getStringExtra("tag").orEmpty(), name = intent.getStringExtra("name").orEmpty(), detail = body, kind = kind,
            mood = if (intent.getStringExtra("mood") == "sleep") NotifArt.Mood.Sleep else NotifArt.Mood.Sit,
        )
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/work/RefreshWorker.kt`

```kotlin
package com.student.amizoneplus.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.student.amizoneplus.data.Repository
import java.util.concurrent.TimeUnit

/** Refreshes Amizone data about every 6 hours so alerts and reminders stay current. */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = Repository.get(applicationContext)
        if (repo.storage.loadCredentials() == null) return Result.success()
        return try {
            repo.refreshAll()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val NAME = "amizone-refresh"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<RefreshWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(NAME)
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/work/KeepAliveWorker.kt`

```kotlin
package com.student.amizoneplus.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.student.amizoneplus.data.Repository
import java.util.concurrent.TimeUnit

/**
 * Keeps the Amizone session warm. Amizone signs you out after a stretch of no activity,
 * and signing in again sometimes needs its human check. A tiny request every ~15 minutes
 * (Android may stretch this while the phone sleeps) keeps the session from going idle,
 * so opening the app hours later usually needs no sign-in at all.
 */
class KeepAliveWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = Repository.get(applicationContext)
        if (repo.storage.loadCredentials() == null) return Result.success()
        runCatching { repo.client.keepAlive() }
        return Result.success()
    }

    companion object {
        private const val NAME = "amizone-keepalive"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<KeepAliveWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(NAME)
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/work/NotifArt.kt`

```kotlin
package com.student.amizoneplus.work

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.student.amizoneplus.R
import com.student.amizoneplus.ui.pets.PetSprites

/**
 * Reminder notifications drawn in the app's own style: a rounded colour card, big
 * condensed type, and a pixel pet keeping you company (sleeping the night before an
 * exam, sitting up when it's close).
 */
object NotifArt {
    private const val PERI = 0xFF6F73F4.toInt()
    private const val SUN = 0xFFF7C54A.toInt()
    private const val SAGE = 0xFFC6D4CB.toInt()
    private const val INK = 0xFF161617.toInt()
    private const val CREAM = 0xFFF4F1EA.toInt()

    enum class Mood { Sleep, Sit }

    /** Card for reminders. [tag] is the small caps line, [title] the big one, [detail] the last. */
    fun card(context: Context, tag: String, title: String, detail: String, kind: String, mood: Mood, big: Boolean): Bitmap {
        val w = 1080
        val h = if (big) 460 else 210
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = when (kind) { "exam" -> PERI; "class" -> SAGE; else -> SUN }
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg }
        c.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 56f, 56f, p)

        val condensed = runCatching { ResourcesCompat.getFont(context, R.font.barlow_condensed_bold) }.getOrNull() ?: Typeface.DEFAULT_BOLD
        val mono = runCatching { ResourcesCompat.getFont(context, R.font.dm_mono_medium) }.getOrNull() ?: Typeface.MONOSPACE

        // Pet on the right.
        val petW = if (big) 300 else 150
        val pet = pickPet(context)
        if (pet != null) drawPet(c, pet, mood, w - petW - (if (big) 40 else 26), h - (if (big) 40 else 18), petW)

        val left = if (big) 56f else 44f
        val textW = (w - petW - left - 70).toFloat()
        val ink = if (kind == "exam") CREAM else INK
        val tagP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono; textSize = if (big) 36f else 28f; color = ink; alpha = 200; letterSpacing = 0.12f }
        val detP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono; textSize = if (big) 32f else 25f; color = ink; alpha = 215 }

        // Big short name ("OOPs", "Maths III", "DSA"), sized down until it fits in full —
        // never "…". The full course name sits underneath in small type.
        val short = com.student.amizoneplus.logic.shortCourseName(title).uppercase().replace("OOPS", "OOPs")
        val full = title.trim()
        val titleP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = condensed; color = ink }
        var size = if (big) 112f else 70f
        val minSize = if (big) 58f else 40f
        titleP.textSize = size
        while (titleP.measureText(short) > textW && size > minSize) { size -= 2f; titleP.textSize = size }

        val lines = mutableListOf<Pair<String, TextPaint>>()
        lines += tag.uppercase() to tagP
        lines += short to titleP
        if (!full.equals(short, true)) lines += full to detP
        if (big && detail.isNotBlank()) lines += detail to detP

        // Lay out top to bottom, centred vertically in the card.
        val gaps = lines.map { (_, pt) -> pt.textSize * (if (pt === titleP) 1.02f else 1.35f) }
        var y = (h - gaps.sum()) / 2f
        lines.forEachIndexed { i, (txt, pt) ->
            y += gaps[i]
            if (pt === titleP) c.drawText(txt, left, y - pt.textSize * 0.12f, pt)
            else y += smallFit(txt, pt, textW, if (big && pt === detP) 2 else 1, c, left, y, gaps[i])
        }
        return bmp
    }

    /**
     * Draws small text without "…" where possible: shrinks it a little, then wraps to a
     * second line (when [maxLines] is 2). Returns the extra height used.
     */
    private fun smallFit(text: String, p: TextPaint, width: Float, maxLines: Int, c: Canvas, x: Float, y: Float, lineH: Float): Float {
        val base = p.textSize
        while (p.measureText(text) > width && p.textSize > base * 0.8f) p.textSize -= 1f
        var extra = 0f
        if (p.measureText(text) <= width) c.drawText(text, x, y, p)
        else if (maxLines >= 2) {
            val words = text.split(' '); var first = ""
            for (wd in words) { val n = if (first.isEmpty()) wd else "$first $wd"; if (p.measureText(n) <= width) first = n else break }
            c.drawText(first, x, y, p)
            extra = lineH * 0.85f
            c.drawText(TextUtils.ellipsize(text.removePrefix(first).trim(), p, width, TextUtils.TruncateAt.END).toString(), x, y + extra, p)
        } else c.drawText(TextUtils.ellipsize(text, p, width, TextUtils.TruncateAt.END).toString(), x, y, p)
        p.textSize = base
        return extra
    }

    private data class Pet(val frames: Map<String, Array<String>>, val pal: Map<Char, Int>, val cat: Boolean)

    private fun pickPet(context: Context): Pet? {
        val choice = runCatching { com.student.amizoneplus.data.Storage(context).loadSettings().pets }.getOrDefault("both")
        val cat = when (choice) { "cat" -> true; "dog" -> false; "none" -> return null; else -> Math.random() < 0.5 }
        return if (cat) Pet(PetSprites.CAT, PetSprites.CAT_PALETTE, true) else Pet(PetSprites.DOG, PetSprites.DOG_PALETTE, false)
    }

    /** Draws the sprite with its feet on (x .. x+width, baseline). */
    private fun drawPet(c: Canvas, pet: Pet, mood: Mood, x: Int, baseline: Int, width: Int) {
        val name = when (mood) {
            Mood.Sleep -> if (pet.cat) "loaf0" else "lie0"
            Mood.Sit -> if (pet.cat) "sit0" else "sit_tongue"
        }
        val rows = pet.frames[name] ?: return
        val px = width / PetSprites.W.toFloat()
        val top = baseline - PetSprites.H * px
        val paint = Paint()
        rows.forEachIndexed { ry, line ->
            line.forEachIndexed { rx, ch ->
                if (ch == '.') return@forEachIndexed
                paint.color = pet.pal[ch] ?: return@forEachIndexed
                c.drawRect(x + rx * px, top + ry * px, x + (rx + 1) * px + 0.5f, top + (ry + 1) * px + 0.5f, paint)
            }
        }
        if (mood == Mood.Sleep) { // pixel z's
            paint.color = CREAM
            val zs = listOf("KKKK", "..K.", ".K..", "KKKK")
            for ((k, sz) in listOf(0 to 0.9f, 1 to 0.65f)) {
                val zx = x + width * (0.62f + k * 0.16f)
                val zy = top + PetSprites.H * px * (0.1f - k * 0.12f)
                zs.forEachIndexed { ry, l -> l.forEachIndexed { rx, ch -> if (ch == 'K') c.drawRect(zx + rx * px * sz, zy + ry * px * sz, zx + (rx + 1) * px * sz, zy + (ry + 1) * px * sz, paint) } }
            }
        }
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/AmizoneApp.kt`

```kotlin
﻿package com.student.amizoneplus

import android.app.Application
import com.student.amizoneplus.work.Notifier

class AmizoneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.student.amizoneplus.data.Http.init(this)
        Notifier.createChannels(this)
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/theme/Theme.kt`

```kotlin
package com.student.amizoneplus.ui.theme

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.amizoneplus.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Palette after the reference: near-black base, bold colour heroes (sage, coral,
 * periwinkle), sky-blue highlights. Light mode keeps the heroes and flips the base.
 */
data class Palette(
    val dark: Boolean,
    val bg: Color, // screen background
    val sheet: Color, // sheet that slides over a hero
    val card: Color,
    val cardRaised: Color,
    val text: Color,
    val sub: Color,
    val faint: Color,
    val line: Color,
    val cream: Color, // white pills & round buttons
    val onCream: Color,
    val good: Color,
    val warn: Color,
    val bad: Color,
)

private val DarkPalette = Palette(
    dark = true,
    bg = Color(0xFF161617), sheet = Color(0xFF1F1F21), card = Color(0xFF29292C), cardRaised = Color(0xFF34343A),
    text = Color(0xFFF3F2EE), sub = Color(0xFFA3A3A8), faint = Color(0xFF6C6C72), line = Color(0xFF3A3A40),
    cream = Color(0xFFF4F3EF), onCream = Color(0xFF161617),
    good = Color(0xFF8FDCAA), warn = Color(0xFFF7C54A), bad = Color(0xFFFF7B6B),
)

private val LightPalette = Palette(
    dark = false,
    bg = Color(0xFFEFEEE9), sheet = Color(0xFFF8F7F4), card = Color(0xFFFFFFFF), cardRaised = Color(0xFFF1F0EC),
    text = Color(0xFF161617), sub = Color(0xFF616166), faint = Color(0xFF9D9DA2), line = Color(0xFFD9D8D2),
    cream = Color(0xFFFFFFFF), onCream = Color(0xFF161617),
    good = Color(0xFF1E8452), warn = Color(0xFFB07612), bad = Color(0xFFC6402F),
)

/**
 * Current colours. Read anywhere (including draw code); set once per composition by
 * [applyTheme] from the system light/dark setting (a change recreates the activity).
 */
object P {
    var cur: Palette = DarkPalette
        private set

    fun applyTheme(dark: Boolean) {
        cur = if (dark) DarkPalette else LightPalette
    }

    // Hero colours — identical in both modes.
    val sage = Color(0xFFC6D4CB)
    val coral = Color(0xFFF06A55)
    val coralSoft = Color(0xFFF89A86)
    val peri = Color(0xFF6F73F4)
    val periSoft = Color(0xFF8C90F7)
    val sky = Color(0xFFBDE9F4)
    val sun = Color(0xFFF7C54A)
    val lilac = Color(0xFF9A9DFA)
    val red = Color(0xFFF14B3B)
    val onHero = Color(0xFF161617) // text on any colour hero
    val nav = Color(0xFF222226) // the bottom nav pill stays dark in both modes

    val bg get() = cur.bg
    val sheet get() = cur.sheet
    val card get() = cur.card
    val cardRaised get() = cur.cardRaised
    val text get() = cur.text
    val sub get() = cur.sub
    val faint get() = cur.faint
    val line get() = cur.line
    val cream get() = cur.cream
    val onCream get() = cur.onCream
    val good get() = cur.good
    val warn get() = cur.warn
    val bad get() = cur.bad
    val goodSoft get() = good.copy(alpha = 0.18f)
    val warnSoft get() = warn.copy(alpha = 0.2f)
    val badSoft get() = bad.copy(alpha = 0.18f)
    val hairline get() = line

    // ── Aliases kept so older code keeps compiling ──
    val canvas get() = cur.bg
    val ink get() = cur.card
    val inkRaised get() = cur.cardRaised
    val onInk get() = cur.text
    val onInkSub get() = cur.sub
    val onInkFaint get() = cur.faint
    val inkLine get() = cur.line
    val lav get() = sky
    val lavSoft get() = sky.copy(alpha = 0.4f)
    val goodOnInk get() = cur.good
    val warnOnInk get() = cur.warn
    val badOnInk get() = cur.bad
}

/** Layout rhythm. */
object L {
    val page = 14.dp // side margins
    val block = 12.dp // gap between blocks
    val joint = 10.dp
    val radius = 28.dp // cards
    val sheetRadius = 36.dp // sheet sliding over a hero
    val notch = 44.dp
    val pad = 20.dp // card padding
    val header = 64.dp
    val nav = 56.dp
}

/**
 * Motion: subtle morphs and smoothing — no bounce. Emphasized-decelerate for
 * things arriving, near-critically-damped springs for things that move or reshape.
 */
object Motion {
    val emphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    fun <T> spring() = spring<T>(dampingRatio = 0.92f, stiffness = 380f)
    fun <T> press() = spring<T>(dampingRatio = 0.9f, stiffness = 900f)
    fun <T> soft() = spring<T>(dampingRatio = 0.95f, stiffness = 200f)
    const val MORPH_MS = 520
    const val ENTER_MS = 460
    const val STAGGER_MS = 30L
}

/* ── Type: Barlow Condensed for display, DM Mono for everything else ── */

val Condensed = FontFamily(
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)
val Mono = FontFamily(
    Font(R.font.dm_mono_light, FontWeight.Light),
    Font(R.font.dm_mono_regular, FontWeight.Normal),
    Font(R.font.dm_mono_medium, FontWeight.Medium),
)

object Type {
    // Condensed, uppercase by convention (callers pass .uppercase()).
    val hero = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 76.sp, lineHeight = 70.sp, letterSpacing = (-1).sp)
    val display = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 46.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp)
    val headline = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp)
    val title = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 28.sp)
    val num = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp)
    val numSm = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 30.sp)
    // Mono
    val label = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp)
    val body = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.2.sp)
    val caption = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp)
    val caps = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.6.sp)
}

fun attendanceTone(pct: Double, threshold: Int, onInk: Boolean = true): Color = when {
    pct >= threshold -> P.good
    pct >= threshold - 5 -> P.warn
    else -> P.bad
}

/**
 * Haptic vocabulary: each interaction has its own feel. All no-ops when the user
 * switches haptics off in More.
 */
class Haptics(private val view: View, private val enabled: () -> Boolean) {
    private fun h(c: Int) {
        if (enabled()) view.performHapticFeedback(c)
    }

    fun select() = h(HapticFeedbackConstants.CLOCK_TICK)
    fun tap() = h(HapticFeedbackConstants.KEYBOARD_TAP)
    fun press() = h(HapticFeedbackConstants.VIRTUAL_KEY)
    fun expand() = h(HapticFeedbackConstants.CONTEXT_CLICK)
    fun collapse() = h(HapticFeedbackConstants.KEYBOARD_TAP)
    fun tick() = h(HapticFeedbackConstants.CLOCK_TICK)
    fun pull() = h(HapticFeedbackConstants.GESTURE_START)
    fun success() = h(HapticFeedbackConstants.CONFIRM)
    fun warning() = h(HapticFeedbackConstants.REJECT)
    fun error() = h(HapticFeedbackConstants.REJECT)

    /* ── Pull-to-refresh tension (LRA primitives; simple ticks on older phones) ── */

    private val vibrator: android.os.Vibrator? by lazy {
        val ctx = view.context
        if (android.os.Build.VERSION.SDK_INT >= 31) ctx.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") ctx.getSystemService(android.os.Vibrator::class.java)
    }
    private val rich: Boolean by lazy {
        android.os.Build.VERSION.SDK_INT >= 31 && vibrator?.areAllPrimitivesSupported(
            android.os.VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
            android.os.VibrationEffect.Composition.PRIMITIVE_CLICK,
            android.os.VibrationEffect.Composition.PRIMITIVE_THUD,
            android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
            android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
        ) == true
    }

    private fun compose(vararg parts: Pair<Int, Float>) {
        if (!enabled()) return
        if (android.os.Build.VERSION.SDK_INT < 31 || !rich) return
        val c = android.os.VibrationEffect.startComposition()
        parts.forEach { (p, s) -> c.addPrimitive(p, s.coerceIn(0.05f, 1f)) }
        runCatching { vibrator?.vibrate(c.compose()) }
    }

    /** One tension tick; [level] 0..1 grows as the pull nears the threshold. */
    fun tension(level: Float) {
        if (rich && android.os.Build.VERSION.SDK_INT >= 31) compose(android.os.VibrationEffect.Composition.PRIMITIVE_LOW_TICK to 0.12f + 0.88f * level * level)
        else h(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Pulled far enough: a firm thunk. */
    fun armed() {
        if (rich && android.os.Build.VERSION.SDK_INT >= 31) compose(
            android.os.VibrationEffect.Composition.PRIMITIVE_CLICK to 1f,
            android.os.VibrationEffect.Composition.PRIMITIVE_THUD to 0.7f,
        ) else h(HapticFeedbackConstants.LONG_PRESS)
    }

    /** Backed off below the threshold again. */
    fun disarm() {
        if (rich && android.os.Build.VERSION.SDK_INT >= 31) compose(android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_FALL to 0.5f)
        else h(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Let go to refresh: a quick swell. */
    fun launch() {
        if (rich && android.os.Build.VERSION.SDK_INT >= 31) compose(android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_RISE to 0.8f)
        else h(HapticFeedbackConstants.GESTURE_END)
    }

    fun celebrate() {
        if (!enabled()) return
        CoroutineScope(Dispatchers.Main).launch {
            for ((wait, c) in listOf(0L to HapticFeedbackConstants.LONG_PRESS, 90L to HapticFeedbackConstants.VIRTUAL_KEY,
                80L to HapticFeedbackConstants.KEYBOARD_TAP, 150L to HapticFeedbackConstants.CONFIRM)) {
                delay(wait)
                view.performHapticFeedback(c)
            }
        }
    }
}

val LocalHaptics = staticCompositionLocalOf<Haptics> { error("Haptics not provided") }
```

---

## `app/src/main/java/com/student/amizoneplus/ui/kit/Kit.kt`

```kotlin
package com.student.amizoneplus.ui.kit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.student.amizoneplus.ui.pets.hangPoints
import com.student.amizoneplus.ui.pets.perch
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** True inside a fused shape: children draw no background and dim instead of shrinking. */
val LocalInFused = compositionLocalOf { false }

/* ───────────── Text ───────────── */

@Composable
fun T(
    text: String,
    style: TextStyle = Type.body,
    modifier: Modifier = Modifier,
    ink: Boolean = false,
    sub: Boolean = false,
    faint: Boolean = false,
    color: Color? = null,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
) {
    val c = color ?: if (faint) P.faint else if (sub) P.sub else P.text
    // Big titles are things pets can stand on.
    val m = if (style.fontSize.isSp && style.fontSize.value >= 30f) modifier.perch() else modifier
    FitText(text, m, c, style, maxLines, align)
}

@Composable
fun Caps(text: String, color: Color, modifier: Modifier = Modifier) = FitText(text, modifier, color, Type.caps, 1, null)

/**
 * Text that ends in "…" when it doesn't fit — and then opens up to show everything
 * when tapped (tap again to fold it back). Text that fits behaves like plain text.
 */
@Composable
fun FitText(text: String, modifier: Modifier, color: Color, style: TextStyle, maxLines: Int, align: TextAlign?) {
    var open by remember(text) { mutableStateOf(false) }
    var cut by remember(text) { mutableStateOf(false) }
    val haptics = LocalHaptics.current
    val canOpen = maxLines != Int.MAX_VALUE && (cut || open)
    Text(
        text,
        modifier
            .then(
                if (canOpen) Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { haptics.select(); open = !open }
                else Modifier,
            )
            .animateContentSize(tween(Motion.MORPH_MS, easing = Motion.emphasized)),
        color = color, style = style,
        maxLines = if (open) Int.MAX_VALUE else maxLines,
        overflow = TextOverflow.Ellipsis, textAlign = align,
        onTextLayout = { if (!open) cut = it.hasVisualOverflow },
    )
}

/** Attendance target: 75 (Amizone's minimum) to 95, in steps of 5. */
@Composable
fun TargetPicker(value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, bg: Color = P.cardRaised, fg: Color = P.text, activeBg: Color = P.cream, activeFg: Color = P.onCream) {
    val haptics = LocalHaptics.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        com.student.amizoneplus.logic.TARGETS.forEach { v ->
            val on = v == value
            val bgc by animateColorAsState(if (on) activeBg else bg, tween(260), label = "tgt")
            Box(
                Modifier.weight(1f).height(40.dp).press({ if (!on) { haptics.select(); onChange(v) } }, Feel.None, 0.92f)
                    .background(bgc, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) { Text("$v%", style = Type.label, color = if (on) activeFg else fg, maxLines = 1) }
        }
    }
}

/* ───────────── Press: scale, or dim inside fused shapes; with haptics ───────────── */

enum class Feel { Tap, Press, Select, None }

fun Modifier.press(onClick: (() -> Unit)?, feel: Feel = Feel.Tap, scaleTo: Float = 0.97f): Modifier = composed {
    if (onClick == null) return@composed this
    val haptics = LocalHaptics.current
    val inFused = LocalInFused.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val k by animateFloatAsState(if (pressed) 1f else 0f, if (pressed) Motion.press() else Motion.spring(), label = "press")
    this
        .graphicsLayer {
            if (inFused) alpha = 1f - 0.25f * k
            else { val s = 1f - (1f - scaleTo) * k; scaleX = s; scaleY = s }
        }
        .clickable(interactionSource = source, indication = null) {
            when (feel) {
                Feel.Tap -> haptics.tap()
                Feel.Press -> haptics.press()
                Feel.Select -> haptics.select()
                Feel.None -> Unit
            }
            onClick()
        }
}

/* ───────────── Rise: smooth glide-in, whole block as one layer ───────────── */

@Composable
fun Rise(index: Int = 0, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(30 + index * Motion.STAGGER_MS)
        p.animateTo(1f, tween(Motion.ENTER_MS, easing = Motion.emphasized))
    }
    Box(
        modifier.graphicsLayer {
            alpha = (p.value * 1.4f).coerceAtMost(1f)
            translationY = 12f * density * (1f - p.value)
            compositingStrategy = CompositingStrategy.ModulateAlpha
        },
    ) { content() }
}

/* ───────────── Surfaces ───────────── */

/** Rounded card. Dark grey in dark mode, white in light mode. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    color: Color = P.card,
    onClick: (() -> Unit)? = null,
    feel: Feel = Feel.Tap,
    padding: Dp = L.pad,
    radius: Dp = L.radius,
    content: @Composable ColumnScope.() -> Unit,
) {
    val inFused = LocalInFused.current
    Column(
        modifier
            .perch()
            .press(onClick, feel)
            .then(if (inFused) Modifier else Modifier.background(color, RoundedCornerShape(radius)))
            .padding(padding),
        content = content,
    )
}

/** Old name for [Card]; kept for screens not yet restyled. */
@Composable
fun Ink(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, feel: Feel = Feel.Tap, padding: Dp = L.pad, content: @Composable ColumnScope.() -> Unit) =
    Card(modifier, P.card, onClick, feel, padding, content = content)

/** Round button: cream disc with a dark icon (or any colours). */
@Composable
fun CircleButton(
    icon: ImageVector,
    onClick: (() -> Unit)?,
    size: Dp = 48.dp,
    bg: Color = P.cream,
    fg: Color = P.onCream,
    feel: Feel = Feel.Tap,
    badge: Boolean = false,
) {
    Box(Modifier.perch().size(size).press(onClick, feel, 0.9f).background(bg, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(size * 0.44f))
        if (badge) Box(Modifier.align(Alignment.TopEnd).offset(x = (-8).dp, y = 9.dp).size(8.dp).background(P.red, CircleShape))
    }
}

/** The reference's ↗ button. */
@Composable
fun ArrowButton(onClick: (() -> Unit)?, size: Dp = 44.dp) = CircleButton(Icons.Outlined.NorthEast, onClick, size)

/** Red floating + button. */
@Composable
fun Fab(onClick: () -> Unit, icon: ImageVector = Icons.Outlined.Add) =
    CircleButton(icon, onClick, 56.dp, P.red, Color.White, Feel.Press)

@Composable
fun Avatar(text: String, color: Color = P.sun, size: Dp = 48.dp) {
    val initials = text.split(' ', '/', '-').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase().ifEmpty { "?" }
    Box(Modifier.perch().size(size).background(color, RoundedCornerShape(size * 0.32f)), contentAlignment = Alignment.Center) {
        Text(initials, style = Type.title.copy(fontSize = (size.value * 0.42f).sp), color = P.onHero)
    }
}

/* ───────────── Pills & chips ───────────── */

enum class Tone { Lav, Ink, Good, Warn, Bad, Ghost, Sage, Sky, Sun, Cream }

@Composable
fun Chip(text: String, tone: Tone = Tone.Lav, dot: Color? = null) {
    val (bg, fg) = when (tone) {
        Tone.Lav -> P.lilac to P.onHero
        Tone.Sage -> P.sage to P.onHero
        Tone.Sky -> P.sky to P.onHero
        Tone.Sun -> P.sun.copy(alpha = 0.22f) to P.sun
        Tone.Cream -> P.cream to P.onCream
        Tone.Ink -> P.cardRaised to P.text
        Tone.Good -> P.goodSoft to P.good
        Tone.Warn -> P.warnSoft to P.warn
        Tone.Bad -> P.badSoft to P.bad
        Tone.Ghost -> Color.Transparent to P.sub
    }
    Row(
        Modifier
            .background(bg, RoundedCornerShape(50))
            .then(if (tone == Tone.Ghost) Modifier.border(1.dp, P.line, RoundedCornerShape(50)) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) { Box(Modifier.size(9.dp).background(dot, CircleShape)); Spacer(Modifier.width(7.dp)) }
        Caps(text, fg)
    }
}

/** Pill with a ⌄ — filters and selectors ("all ⌄", "this week ⌄"). */
@Composable
fun DropdownPill(text: String, onClick: () -> Unit, bg: Color = P.cream, fg: Color = P.onCream) {
    Row(
        Modifier.perch().press(onClick, Feel.Select).background(bg, RoundedCornerShape(50)).padding(start = 16.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        T(text, Type.label, color = fg, maxLines = 1)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Outlined.KeyboardArrowDown, null, tint = fg, modifier = Modifier.size(18.dp))
    }
}

/** Big white selector pill (the reference's "The Daily Grind, NY ⌄"). */
@Composable
fun SelectorPill(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier.perch().fillMaxWidth().press(onClick).background(P.cream, RoundedCornerShape(50)).padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = P.onCream, modifier = Modifier.size(20.dp))
        T(text, Type.label, color = P.onCream, maxLines = 1, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
        Box(Modifier.size(42.dp).background(P.onCream, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.KeyboardArrowDown, null, tint = P.cream, modifier = Modifier.size(20.dp))
        }
    }
}

/** "64 employees" — a big number with a small label, on a translucent tile. */
@Composable
fun RowScope.StatTile(value: String, label: String, bg: Color, fg: Color = P.onHero, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.weight(1f).perch().press(onClick).background(bg, RoundedCornerShape(22.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(value, style = Type.num, color = fg, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        T(label, Type.caption, color = fg.copy(alpha = 0.8f), modifier = Modifier.padding(top = 6.dp), maxLines = 2)
    }
}

@Composable
fun SearchPill(value: String, onChange: (String) -> Unit, placeholder: String) {
    Row(
        Modifier.perch().fillMaxWidth().background(P.cardRaised, RoundedCornerShape(50)).padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, null, tint = P.sub, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f).padding(start = 12.dp)) {
            if (value.isEmpty()) T(placeholder, Type.body, sub = true, maxLines = 1)
            BasicTextField(value, onChange, singleLine = true, textStyle = Type.body.copy(color = P.text), modifier = Modifier.fillMaxWidth(),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(P.text))
        }
    }
}

/** Live "2:59:12"-style countdown chip. */
@Composable
fun CountdownChip(targetMs: Long, color: Color = P.good) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val ms = (targetMs - now).coerceAtLeast(0)
    Box(Modifier.background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 1.dp)) {
        T("%d:%02d:%02d".format(ms / 3_600_000, (ms % 3_600_000) / 60_000, (ms % 60_000) / 1000), Type.label, color = color)
    }
}

/* ───────────── Fused geometry (kept for the menu's unfolding rows) ───────────── */

private fun rr(l: Float, t: Float, r: Float, b: Float, rad: Float) =
    Path().apply { addRoundRect(RoundRect(Rect(l, t, r, b), CornerRadius(rad, rad))) }

private fun Path.unionWith(other: Path): Path = Path().also { it.op(this, other, PathOperation.Union) }
private fun Path.minus(other: Path): Path = Path().also { it.op(this, other, PathOperation.Difference) }

fun stackPath(w: Float, blocks: List<Pair<Float, Float>>, radius: Float, notch: Float): Path {
    var p = Path()
    blocks.forEach { (t, h) -> p = p.unionWith(rr(0f, t, w, t + h, min(radius, h / 2))) }
    for (i in 0 until blocks.size - 1) {
        val bot = blocks[i].first + blocks[i].second
        val top = blocks[i + 1].first
        val g = (top - bot) / 2
        if (g <= 0f) continue
        p = p.unionWith(Path().apply { addRect(Rect(0f, bot, w, top)) })
        val n = min(notch, w / 2 - g)
        p = p.minus(rr(-4 * g, bot, n, top, g)).minus(rr(w - n, bot, w + 4 * g, top, g))
    }
    return p
}

fun pairPath(w: Float, h: Float, gap: Float, radius: Float, notch: Float): Path {
    val w1 = (w - gap) / 2
    val r = min(radius, h / 2)
    val g = gap / 2
    val n = min(notch, h / 2 - g)
    return rr(0f, 0f, w1, h, r).unionWith(rr(w1 + gap, 0f, w, h, r))
        .unionWith(Path().apply { addRect(Rect(w1, 0f, w1 + gap, h)) })
        .minus(rr(w1, -4 * g, w1 + gap, n, g)).minus(rr(w1, h - n, w1 + gap, h + 4 * g, g))
}

@Composable
fun Fused(modifier: Modifier = Modifier, color: Color = P.card, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    val joint = with(d) { L.joint.roundToPx() }
    val radius = with(d) { L.radius.toPx() }
    val notch = with(d) { L.notch.toPx() }
    val blocks = remember { mutableStateOf(listOf<Pair<Float, Float>>()) }
    Layout(
        content = { CompositionLocalProvider(LocalInFused provides true) { content() } },
        modifier = modifier.drawBehind { if (blocks.value.isNotEmpty()) drawPath(stackPath(size.width, blocks.value, radius, notch), color) },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        var y = 0
        val list = ArrayList<Pair<Float, Float>>()
        placeables.forEachIndexed { i, p -> if (i > 0) y += joint; list += y.toFloat() to p.height.toFloat(); y += p.height }
        if (list != blocks.value) blocks.value = list
        layout(constraints.maxWidth, y) {
            var yy = 0
            placeables.forEachIndexed { i, p -> if (i > 0) yy += joint; p.place(0, yy); yy += p.height }
        }
    }
}

@Composable
fun PairCards(modifier: Modifier = Modifier, left: @Composable () -> Unit, right: @Composable () -> Unit) {
    Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(L.block)) {
        Box(Modifier.weight(1f).fillMaxSize().background(P.card, RoundedCornerShape(L.radius))) { left() }
        Box(Modifier.weight(1f).fillMaxSize().background(P.card, RoundedCornerShape(L.radius))) { right() }
    }
}

/* ───────────── Buttons & rows ───────────── */

enum class BtnKind { Lav, Ghost, Ink, Outline }

@Composable
fun RowScope.Btn(label: String, kind: BtnKind = BtnKind.Lav, icon: ImageVector? = null, onClick: (() -> Unit)?) {
    val bg = when (kind) { BtnKind.Lav -> P.cream; BtnKind.Ink -> P.text; else -> Color.Transparent }
    val fg = when (kind) { BtnKind.Lav -> P.onCream; BtnKind.Ink -> P.bg; BtnKind.Ghost, BtnKind.Outline -> P.text }
    val border = when (kind) { BtnKind.Ghost, BtnKind.Outline -> P.line; else -> bg }
    Row(
        Modifier.weight(1f).press(onClick, Feel.Press).height(54.dp).background(bg, RoundedCornerShape(50))
            .border(1.dp, border, RoundedCornerShape(50)).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Caps(label, fg)
    }
}

/** A list row card: title, subtitle, and a trailing ↗ (or custom). */
@Composable
fun OutlineRow(title: String, sub: String? = null, onClick: (() -> Unit)? = null, right: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.perch().fillMaxWidth().press(onClick).heightIn(min = 72.dp).background(P.card, RoundedCornerShape(24.dp)).padding(start = L.pad, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            T(title, Type.title.copy(fontSize = 21.sp, lineHeight = 23.sp), maxLines = 2)
            if (sub != null) T(sub, Type.caption, sub = true, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
        }
        if (right != null) right() else if (onClick != null) ArrowButton(onClick, 40.dp) else Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = P.sub)
    }
}

@Composable
fun SectionLabel(text: String, color: Color = P.sub) = Caps(text, color, Modifier.padding(start = 6.dp, top = 6.dp))

/* ───────────── Numbers ───────────── */

@Composable
fun CountUp(value: Double, decimals: Int = 0, suffix: String = "", style: TextStyle = Type.num, color: Color = P.text, modifier: Modifier = Modifier) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(value) { anim.animateTo(value.toFloat(), tween(900, easing = Motion.emphasized)) }
    Text("%.${decimals}f".format(anim.value) + suffix, modifier, color = color, style = style, maxLines = 1)
}

/** Segmented bar, filling smoothly. */
@Composable
fun SegBar(pct: Double, marker: Int?, tone: Color, modifier: Modifier = Modifier, segments: Int = 20, height: Dp = 32.dp, track: Color = P.cardRaised) {
    val haptics = LocalHaptics.current
    val target = ((pct.coerceIn(0.0, 100.0) / 100.0) * segments).roundToInt()
    var filled by remember { mutableIntStateOf(0) }
    LaunchedEffect(target) {
        filled = 0; delay(200)
        for (n in 1..target) { filled = n; if (n % 4 == 0) haptics.tick(); delay((14 + 30 * (n.toFloat() / target).pow(3)).toLong()) }
    }
    val markIdx = marker?.let { ((it / 100.0) * segments).roundToInt() - 1 } ?: -1
    Row(modifier.height(height), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(segments) { i ->
            val c by animateColorAsState(if (i < filled) tone else track, tween(220), label = "seg")
            Box(Modifier.weight(1f).fillMaxSize().background(c, RoundedCornerShape(4.dp))
                .then(if (i == markIdx && i >= filled) Modifier.border(1.dp, P.sub, RoundedCornerShape(4.dp)) else Modifier))
        }
    }
}

/** Mon–Sun bars, growing smoothly. */
@Composable
fun WeekBars(values: List<Double?>, height: Dp = 72.dp, bar: Color = P.sky, track: Color = P.cardRaised) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(height).background(track, RoundedCornerShape(10.dp)), contentAlignment = Alignment.BottomCenter) {
                    if (v != null) {
                        val f = remember { Animatable(0f) }
                        LaunchedEffect(v) { delay(100L + i * 40L); f.animateTo(v.coerceAtLeast(0.06).toFloat(), tween(650, easing = Motion.emphasized)) }
                        Box(Modifier.fillMaxWidth().height(height * f.value).background(bar, RoundedCornerShape(10.dp)))
                    }
                }
                T(days[i], Type.caption.copy(fontSize = 11.sp), sub = true, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

/** Weekday % circles (reference "Venue Capacity" row). Null days are hatched. */
private val WeekDayFree = com.student.amizoneplus.logic.WeekDay.Kind.Free
private val WeekDayFuture = com.student.amizoneplus.logic.WeekDay.Kind.Future

/** No classes on this day (past or ahead): drawn with an emoji instead of a number. */
val com.student.amizoneplus.logic.WeekDay.restDay: Boolean
    get() = total == 0 && (kind == WeekDayFree || kind == WeekDayFuture)

@Composable
fun DayCircles(days: List<com.student.amizoneplus.logic.WeekDay>, selected: Int, onSelect: (Int) -> Unit, onColor: Color = P.onHero, todayIdx: Int = -1) {
    // The "no classes" bubble opens only when a free day is tapped (tap again to close).
    var bubble by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEachIndexed { i, d ->
                val active = i == selected
                val scale by animateFloatAsState(if (active) (if (d.restDay) 1.16f else 1.08f) else 1f, Motion.spring(), label = "dc")
                val emojiScale by animateFloatAsState(if (active && d.restDay) 1.22f else 1f, Motion.spring(), label = "de")
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.press({ bubble = if (i == selected && bubble) false else d.restDay; onSelect(i) }, Feel.Select, 0.9f)) {
                    T(d.label, Type.caption, color = onColor.copy(alpha = 0.75f))
                    Box(
                        Modifier.padding(top = 8.dp).size(40.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                            .drawBehind {
                                val r = size.minDimension / 2
                                when {
                                    active -> drawCircle(P.onHero)
                                    d.kind == WeekDayFuture -> {
                                        drawCircle(Color.White.copy(alpha = 0.18f))
                                        clipRect { // hatched: still ahead
                                            var x = -size.width
                                            while (x < size.width) {
                                                drawLine(Color.White.copy(alpha = 0.30f), Offset(x, size.height), Offset(x + size.width, 0f), 1.5f)
                                                x += 6.dp.toPx()
                                            }
                                        }
                                    }
                                    d.kind == WeekDayFree -> drawCircle(Color.White.copy(alpha = 0.30f))
                                    d.kind == com.student.amizoneplus.logic.WeekDay.Kind.Pending -> drawCircle(Color.White.copy(alpha = 0.55f), r - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
                                    else -> { drawCircle(Color.White.copy(alpha = 0.55f)); drawCircle(Color.White, r - 3.dp.toPx()) }
                                }
                            }
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        val ink = if (active) Color.White else P.onHero
                        when {
                            d.restDay -> Text(Quips.freeDayFor(d.epochDay), fontSize = 18.sp,
                                modifier = Modifier.graphicsLayer { scaleX = emojiScale; scaleY = emojiScale; alpha = if (d.kind == WeekDayFuture && !active) 0.75f else 1f })
                            d.kind == com.student.amizoneplus.logic.WeekDay.Kind.Rate ->
                                T("${(d.rate * 100).roundToInt()}", Type.caption.copy(fontSize = 11.sp), color = ink)
                            else -> T("${d.total}", Type.caption.copy(fontSize = 11.sp), color = ink) // classes, not marked yet / ahead
                        }
                    }
                }
            }
        }

        // The speech bubble that unfolds from a tapped emoji.
        val sel = days.getOrNull(selected)
        val caretX by animateFloatAsState((selected + 0.5f) / days.size.coerceAtLeast(1), Motion.spring(), label = "caret")
        androidx.compose.animation.AnimatedVisibility(
            visible = bubble && sel?.restDay == true,
            enter = androidx.compose.animation.expandVertically(tween(Motion.MORPH_MS, easing = Motion.emphasized), expandFrom = Alignment.Top) +
                androidx.compose.animation.scaleIn(tween(Motion.MORPH_MS, easing = Motion.emphasized), initialScale = 0.4f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(caretX, 0f)) +
                androidx.compose.animation.fadeIn(tween(220)),
            exit = androidx.compose.animation.shrinkVertically(tween(300, easing = Motion.emphasized), shrinkTowards = Alignment.Top) +
                androidx.compose.animation.scaleOut(tween(300, easing = Motion.emphasized), targetScale = 0.5f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(caretX, 0f)) +
                androidx.compose.animation.fadeOut(tween(160)),
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                // caret pointing at the tapped day
                Box(Modifier.fillMaxWidth().height(9.dp).drawBehind {
                    val cx = size.width * caretX
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx - 9.dp.toPx(), size.height); lineTo(cx, 0f); lineTo(cx + 9.dp.toPx(), size.height); close()
                    }
                    drawPath(path, P.onHero)
                })
                Row(
                    Modifier.fillMaxWidth().background(P.onHero, RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    androidx.compose.animation.AnimatedContent(sel?.epochDay ?: 0L, transitionSpec = {
                        (androidx.compose.animation.fadeIn(tween(220)) + androidx.compose.animation.scaleIn(tween(320, easing = Motion.emphasized), initialScale = 0.6f)) togetherWith
                            androidx.compose.animation.fadeOut(tween(120))
                    }, label = "bubbleEmoji") { e -> Text(Quips.freeDayFor(e), fontSize = 30.sp) }
                    Column(Modifier.weight(1f)) {
                        T("No classes scheduled", Type.label, color = Color.White)
                        val name = if (selected == todayIdx) "Today" else sel?.let { fullDay(it.label) }.orEmpty()
                        T(if (sel?.kind == WeekDayFuture) "$name is free. Plan something fun." else "$name was a free day. Nothing to count.",
                            Type.caption, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}



private fun fullDay(short: String) = when (short) {
    "Mon" -> "Monday"; "Tue" -> "Tuesday"; "Wed" -> "Wednesday"; "Thu" -> "Thursday"
    "Fri" -> "Friday"; "Sat" -> "Saturday"; else -> "Sunday"
}

/** One line about the selected day under the circles (rest days get the bubble instead). */
fun dayLine(d: com.student.amizoneplus.logic.WeekDay, isToday: Boolean): String? {
    if (d.restDay) return null
    val name = if (isToday) "Today" else d.label
    return when (d.kind) {
        com.student.amizoneplus.logic.WeekDay.Kind.Rate -> "$name · ${d.present} of ${d.marked} present" + if (d.total > d.marked) " · ${d.total - d.marked} not marked yet" else ""
        com.student.amizoneplus.logic.WeekDay.Kind.Pending -> "$name · ${d.total} class${if (d.total == 1) "" else "es"}, attendance not marked yet"
        com.student.amizoneplus.logic.WeekDay.Kind.Future -> "$name · ${d.total} class${if (d.total == 1) "" else "es"} ahead"
        else -> null
    }
}

/**
 * Area chart (0..1 values) that draws itself in and morphs between data sets.
 * A dotted guide marks [highlight].
 */
@Composable
fun AreaChart(values: List<Float>, modifier: Modifier = Modifier, fill: Color = P.coral, highlight: Int? = null, guide: Color = P.onHero) {
    val reveal = remember { Animatable(0f) }
    val morph = remember { Animatable(1f) }
    var from by remember { mutableStateOf(values) }
    var to by remember { mutableStateOf(values) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(900, easing = Motion.emphasized)) }
    LaunchedEffect(values) {
        if (values != to) {
            from = interp(from, to, morph.value); to = values
            morph.snapTo(0f); morph.animateTo(1f, tween(Motion.MORPH_MS, easing = Motion.emphasized))
        }
    }
    Canvas(modifier) {
        val pts = interp(from, to, morph.value)
        if (pts.size < 2) return@Canvas
        val w = size.width; val h = size.height
        val step = w / (pts.size - 1)
        fun y(v: Float) = h - (v.coerceIn(0f, 1f) * h * 0.86f) - h * 0.04f
        val line = Path().apply {
            moveTo(0f, y(pts[0]))
            for (i in 1 until pts.size) {
                val x0 = (i - 1) * step; val x1 = i * step
                cubicTo(x0 + step / 2, y(pts[i - 1]), x1 - step / 2, y(pts[i]), x1, y(pts[i]))
            }
        }
        val area = Path().apply { addPath(line); lineTo(w, h); lineTo(0f, h); close() }
        clipRect(right = w * reveal.value) {
            drawPath(area, Brush.verticalGradient(listOf(fill, fill.copy(alpha = 0.55f))))
            drawPath(line, guide, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            pts.forEachIndexed { i, v -> if (i % 3 == 0 || i == pts.lastIndex) drawCircle(guide, 3.5.dp.toPx(), Offset(i * step, y(v))) }
        }
        highlight?.takeIf { it in pts.indices }?.let { i ->
            val x = i * step
            drawLine(guide, Offset(x, 0f), Offset(x, h), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 6.dp.toPx())), cap = StrokeCap.Round)
            drawCircle(guide, 5.dp.toPx(), Offset(x, y(pts[i])))
        }
    }
}

private fun interp(a: List<Float>, b: List<Float>, t: Float): List<Float> {
    if (a.isEmpty()) return b
    val n = b.size
    return List(n) { i ->
        val av = a.getOrElse((i.toFloat() / (n - 1).coerceAtLeast(1) * (a.size - 1)).roundToInt()) { 0f }
        av + (b[i] - av) * t
    }
}

/**
 * Clock dial (reference "Operational Timing"): 12-hour face with tick marks,
 * today's classes as arcs (current/next highlighted), and live hands.
 */
@Composable
fun ClockDial(arcs: List<Triple<Long, Long, Boolean>>, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(arcs) { sweep.snapTo(0f); sweep.animateTo(1f, tween(1100, easing = Motion.emphasized)) }
    // A cat can hang from the hour and minute hands (a point ~60% along each).
    val hands = Modifier.hangPoints { b ->
        val cal = Calendar.getInstance()
        val r = min(b.width, b.height) / 2
        val c = b.center
        val hA = Math.toRadians(((cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f) / 12f * 360f - 90f).toDouble())
        val mA = Math.toRadians(((cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f) / 60f * 360f - 90f).toDouble())
        listOf(
            Offset((c.x + cos(mA) * r * 0.55f).toFloat(), (c.y + sin(mA) * r * 0.55f).toFloat()),
            Offset((c.x + cos(hA) * r * 0.36f).toFloat(), (c.y + sin(hA) * r * 0.36f).toFloat()),
        )
    }
    Canvas(modifier.then(hands)) {
        val c = center
        val r = size.minDimension / 2
        drawCircle(Color.White, r)
        // Fine ticks
        for (i in 0 until 120) {
            val a = Math.toRadians(i * 3.0 - 90)
            val len = if (i % 10 == 0) r * 0.1f else r * 0.04f
            val o = Offset((c.x + cos(a) * r * 0.97f).toFloat(), (c.y + sin(a) * r * 0.97f).toFloat())
            val inn = Offset((c.x + cos(a) * (r * 0.97f - len)).toFloat(), (c.y + sin(a) * (r * 0.97f - len)).toFloat())
            drawLine(if (i % 10 == 0) P.onHero else P.onHero.copy(alpha = 0.18f), inn, o, if (i % 10 == 0) 2.dp.toPx() else 1.dp.toPx())
        }
        fun angleOf(ms: Long): Float {
            val cal = Calendar.getInstance().apply { timeInMillis = ms }
            val h = cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f + cal.get(Calendar.SECOND) / 3600f
            return h / 12f * 360f - 90f
        }
        // Class arcs on two rings: highlighted (current/next) on the inner ring in coral.
        val ringW = r * 0.13f
        arcs.forEach { (start, end, hot) ->
            val a0 = angleOf(start)
            val span = ((end - start) / 3_600_000f) * 30f * sweep.value
            val rr2 = if (hot) r * 0.42f else r * 0.56f
            drawArc(if (hot) P.coral else P.sage.copy(alpha = 0.9f), a0, span.coerceAtMost(359f), false,
                topLeft = Offset(c.x - rr2, c.y - rr2), size = Size(rr2 * 2, rr2 * 2), style = Stroke(ringW, cap = StrokeCap.Butt))
        }
        // Hands
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val hA = Math.toRadians(((cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f) / 12f * 360f - 90f).toDouble())
        val mA = Math.toRadians(((cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f) / 60f * 360f - 90f).toDouble())
        val sA = Math.toRadians((cal.get(Calendar.SECOND) / 60f * 360f - 90f).toDouble())
        drawLine(P.onHero, c, Offset((c.x + cos(hA) * r * 0.45f).toFloat(), (c.y + sin(hA) * r * 0.45f).toFloat()), 3.dp.toPx(), cap = StrokeCap.Round)
        drawLine(P.onHero, c, Offset((c.x + cos(mA) * r * 0.7f).toFloat(), (c.y + sin(mA) * r * 0.7f).toFloat()), 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(P.red, Offset((c.x - cos(sA) * r * 0.12f).toFloat(), (c.y - sin(sA) * r * 0.12f).toFloat()),
            Offset((c.x + cos(sA) * r * 0.78f).toFloat(), (c.y + sin(sA) * r * 0.78f).toFloat()), 1.5.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(P.onHero, 4.dp.toPx(), c)
    }
}

/** Segmented toggle with a sliding indicator. */
@Composable
fun <V> Segmented(options: List<Pair<V, String>>, value: V, onChange: (V) -> Unit, modifier: Modifier = Modifier, bg: Color = P.cardRaised, active: Color = P.cream) {
    val haptics = LocalHaptics.current
    BoxWithConstraints(modifier.fillMaxWidth().height(52.dp).background(bg, RoundedCornerShape(50)).padding(4.dp)) {
        val segW = maxWidth / options.size
        val idx = options.indexOfFirst { it.first == value }.coerceAtLeast(0)
        val segPx = with(androidx.compose.ui.platform.LocalDensity.current) { segW.toPx() }
        val scope = rememberCoroutineScope()
        val pos = remember { Animatable(segPx * idx) }
        var dragging by remember { mutableStateOf(false) }
        var dragX by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(idx, segPx) { if (!dragging) pos.animateTo(segPx * idx, androidx.compose.animation.core.spring(0.78f, 420f)) }
        val idxNow by rememberUpdatedState(idx)
        val valueNow by rememberUpdatedState(onChange)
        val grow by animateFloatAsState(if (dragging) 1.04f else 1f,
            if (dragging) androidx.compose.animation.core.spring(0.7f, 500f) else androidx.compose.animation.core.spring(0.45f, 380f), label = "seggrow")
        val shown = if (dragging) (dragX / segPx).roundToInt().coerceIn(0, options.lastIndex) else idx
        Box(
            Modifier.offset { IntOffset((if (dragging) dragX else pos.value).roundToInt(), 0) }.width(segW).fillMaxSize()
                .graphicsLayer { scaleX = grow; scaleY = grow }
                .background(active, RoundedCornerShape(50)),
        )
        Row(Modifier.fillMaxSize().pointerInput(segPx) {
            detectHorizontalDragGestures(
                onDragStart = { dragging = true; dragX = segPx * idxNow; haptics.press() },
                onDragEnd = { dragging = false; scope.launch { pos.snapTo(dragX); pos.animateTo(segPx * idxNow, androidx.compose.animation.core.spring(0.6f, 380f)) } },
                onDragCancel = { dragging = false },
            ) { ch, dx ->
                ch.consume()
                dragX = (dragX + dx).coerceIn(0f, segPx * options.lastIndex)
                val t = (dragX / segPx).roundToInt().coerceIn(0, options.lastIndex)
                if (t != idxNow) { haptics.tick(); valueNow(options[t].first) }
            }
        }) {
            options.forEach { (v, label) ->
                Box(
                    Modifier.weight(1f).fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { if (v != value) { haptics.select(); onChange(v) } },
                    contentAlignment = Alignment.Center,
                ) { Caps(label, if (options.indexOfFirst { it.first == v } == shown) P.onCream else P.sub) }
            }
        }
    }
}

/* ───────────── UnfoldCard: row → filled card → detail grows downward ───────────── */

@Composable
fun UnfoldCard(open: Boolean, onToggle: () -> Unit, head: @Composable RowScope.(onInk: Boolean) -> Unit, body: @Composable ColumnScope.() -> Unit) {
    val d = LocalDensity.current
    val radius = with(d) { 24.dp.toPx() }
    val notch = with(d) { L.notch.toPx() }
    val joint = with(d) { L.joint.toPx() }
    val fill = remember { Animatable(if (open) 1f else 0f) }
    val grow = remember { Animatable(if (open) 1f else 0f) }
    var headH by remember { mutableFloatStateOf(0f) }
    var bodyH by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(open) {
        if (open) coroutineScope {
            launch { fill.animateTo(1f, tween(280, easing = Motion.standard)) }
            delay(100); grow.animateTo(1f, tween(Motion.MORPH_MS, easing = Motion.emphasized))
        } else coroutineScope {
            launch { grow.animateTo(0f, tween(300, easing = Motion.standard)) }
            delay(160); fill.animateTo(0f, tween(240, easing = Motion.standard))
        }
    }

    Column(
        Modifier.fillMaxWidth().drawBehind {
            if (headH <= 0f) return@drawBehind
            val h = bodyH * grow.value
            val blocks = if (h > 2f) listOf(0f to headH, headH + joint to h) else listOf(0f to headH)
            val path = stackPath(size.width, blocks, radius, notch)
            drawPath(path, P.card)
            drawPath(path, P.sky, alpha = fill.value * 0.0f)
        },
    ) {
        Box(Modifier.fillMaxWidth().onSizeChanged { headH = it.height.toFloat() }.clickable(remember { MutableInteractionSource() }, null, onClick = onToggle)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = L.pad).graphicsLayer { alpha = 1f - fill.value },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { head(false) }
            Row(Modifier.matchParentSize().padding(horizontal = L.pad).graphicsLayer { alpha = fill.value },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { head(true) }
        }
        Layout(
            content = {
                Column(Modifier.graphicsLayer {
                    alpha = ((grow.value - 0.2f) / 0.6f).coerceIn(0f, 1f)
                    translationY = -10f * density * (1f - grow.value)
                }) { CompositionLocalProvider(LocalInFused provides true) { body() } }
            },
            modifier = Modifier.graphicsLayer { clip = true },
        ) { measurables, constraints ->
            val p = measurables.first().measure(constraints.copy(minHeight = 0, maxHeight = androidx.compose.ui.unit.Constraints.Infinity))
            if (p.height.toFloat() != bodyH) bodyH = p.height.toFloat()
            val visible = if (grow.value > 0.001f) ((p.height + joint) * grow.value).roundToInt() else 0
            layout(constraints.maxWidth, visible) { p.place(0, joint.roundToInt()) }
        }
    }
}

/* ───────────── Big faint countdown ───────────── */

@Composable
fun BigCountdown(targetMs: Long, caption: String) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val ms = (targetMs - now).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        T(caption, Type.caption, sub = true, maxLines = 1)
        Text("%d:%02d:%02d".format(ms / 3_600_000, (ms % 3_600_000) / 60_000, (ms % 60_000) / 1000),
            style = Type.hero.copy(fontSize = 96.sp, lineHeight = 96.sp), color = P.text.copy(alpha = 0.10f), maxLines = 1)
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/kit/Quips.kt`

```kotlin
package com.student.amizoneplus.ui.kit

/** Little lines that give the app its character. Signed, lovingly, by Shynxo. */
object Quips {
    private val loadingLines = listOf(
        "Shynxo is fetching your stuff…",
        "Bribing the Amizone servers…",
        "Warming up the hamsters…",
        "Asking Amizone nicely…",
        "Shynxo pinky-promises this is fast…",
        "Untangling the internet…",
    )
    private val footerLines = listOf(
        "Handcrafted by Shynxo ✦",
        "Shynxo made this. Go attend a class.",
        "Built at 3 a.m. by Shynxo",
        "A Shynxo production",
        "Shynxo was here ✌",
        "Powered by chai and Shynxo",
    )
    private val emptyLines = listOf(
        "Nothing here. Shynxo checked twice.",
        "Empty as a Monday 8 a.m. class.",
        "Nothing yet. Enjoy the silence.",
    )
    private val tapLines = listOf(
        "Hey! That tickles.",
        "Shynxo says hi 👋",
        "You found the secret. There is no secret.",
        "Go drink some water 💧",
        "75% or bust.",
        "Bunking? Shynxo is watching 👀",
        "Okay okay, you can stop now.",
    )

    /** Funny faces for days with no classes. */
    val freeDay = listOf("😴", "🏖️", "🎮", "🍕", "🛌", "🦥", "🎧", "🍿", "🧋", "🐢")

    fun loading() = loadingLines.random()
    fun footer() = footerLines.random()
    fun empty() = emptyLines.random()
    fun tap(n: Int) = tapLines[n % tapLines.size]

    /** The same emoji for a given day, so circles don't reshuffle on every redraw. */
    fun freeDayFor(epochDay: Long) = freeDay[Math.floorMod(epochDay * 7 + 3, freeDay.size.toLong()).toInt()]
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/pets/PetWorld.kt`

```kotlin
package com.student.amizoneplus.ui.pets

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.student.amizoneplus.ui.shell.Route

/*
 * The pets' world. One instance for the whole app: it knows which tab is showing,
 * every element a pet can stand on ("perches": cards, pills, buttons, big titles),
 * things a cat can hang from (the clock hands), and where the bottom-nav tabs are
 * (so a carried pet can be taken to another tab).
 */

/** The top edge of an on-screen element that a pet can stand on. */
class Perch(val id: Int) {
    var route: Route? = null
    var rect: Rect = Rect.Zero
    /** Dips a little when a pet lands on it. */
    val dip = mutableFloatStateOf(0f)
}

/** Points a cat can hang from (e.g. clock hands), recomputed every frame. */
class HangSource(val id: Int) {
    var route: Route? = null
    var rect: Rect = Rect.Zero
    var points: (Rect) -> List<Offset> = { emptyList() }
}

class PetWorld {
    var route by mutableStateOf(Route.Today)
    var switchTab: (Route) -> Unit = {}

    val perches = HashMap<Int, Perch>()
    val hangs = HashMap<Int, HangSource>()
    val navTabs = HashMap<Route, Rect>()
    var navTop = 0f

    var width = 0f
    var height = 0f
    var statusTop = 0f
    var density = 1f

    val pets = mutableStateListOf<Pet>()
    val fx = mutableStateListOf<Fx>()

    private var next = 0
    fun nextId() = ++next

    /** Where pets walk when not on anything: just above the nav pill. */
    fun floor() = (if (navTop > 0f) navTop else height) - 2f * density

    /** A perch is usable when it is on this tab, wide enough, and fully on screen. */
    fun usable(p: Perch, r: Route): Boolean {
        val b = p.rect
        return p.route == r && b.width > 30f * density &&
            b.top > statusTop + 34f * density && b.top < floor() - 24f * density &&
            b.right > 0f && b.left < width
    }

    fun perchesOn(r: Route) = perches.values.filter { usable(it, r) }

    fun hangPointsOn(r: Route): List<Offset> = hangs.values.filter { it.route == r && it.rect.width > 0f }
        .flatMap { h -> h.points(h.rect) }
        .filter { it.y > statusTop + 40f * density && it.y < floor() - 60f * density }
}

val LocalPetWorld = staticCompositionLocalOf<PetWorld?> { null }
val LocalPetRoute = staticCompositionLocalOf<Route?> { null }

/** Lets pets stand on this element (its top edge). It dips a touch when one lands. */
fun Modifier.perch(): Modifier = this.then(Modifier.composed {
    val world = LocalPetWorld.current
    val route = LocalPetRoute.current
    if (world == null || route == null) Modifier else {
        val p = remember { Perch(world.nextId()) }
        DisposableEffect(p) {
            world.perches[p.id] = p
            onDispose { world.perches.remove(p.id) }
        }
        Modifier
            .onGloballyPositioned { p.route = route; p.rect = it.boundsInWindow() }
            .graphicsLayer { translationY = p.dip.floatValue }
    }
})

/** Things a cat can hang from inside this element, e.g. the clock's hands. */
fun Modifier.hangPoints(points: (Rect) -> List<Offset>): Modifier = this.then(Modifier.composed {
    val world = LocalPetWorld.current
    val route = LocalPetRoute.current
    if (world == null || route == null) Modifier else {
        val h = remember { HangSource(world.nextId()) }
        h.points = points
        DisposableEffect(h) {
            world.hangs[h.id] = h
            onDispose { world.hangs.remove(h.id) }
        }
        Modifier.onGloballyPositioned { h.route = route; h.rect = it.boundsInWindow() }
    }
})

/** Little effects: hearts, z's, dirt, speech bubbles. */
class Fx(
    val kind: Kind,
    var x: Float,
    var y: Float,
    val text: String = "",
    var vx: Float = 0f,
    var vy: Float = 0f,
    val life: Float = 1f,
    val route: Route,
) {
    enum class Kind { Heart, Zed, Dirt, Bubble, Spark }
    var age = 0f
    var follow: Pet? = null
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/pets/Pets.kt`

```kotlin
package com.student.amizoneplus.ui.pets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.amizoneplus.ui.shell.Route
import com.student.amizoneplus.ui.theme.Haptics
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.Mono
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * The app's pets: one cat and/or one dog that live across the whole app. Each pet
 * is on exactly one tab at a time; it walks, runs, sits, naps and plays there, jumps
 * onto cards, buttons and titles, hangs from the timetable clock's hands, and now
 * and then wanders off to another tab (or comes to visit yours). You can pet it
 * (tap), pick it up (drag), throw it, drop it onto anything, or carry it to another
 * tab by hovering over that tab in the nav.
 */

enum class Species { Cat, Dog }

enum class Act {
    Idle, Walk, Run, Sit, Groom, Loaf, Sleep, Knead, Stretch, Yawn, Meow, Crouch, Air, Hang, Held,
    Bark, Scratch, Lie, Belly, Dig, Sniff, Bow, Zoomies, Spin, Wag, Happy, Startle, Away,
}

private fun rnd(a: Float, b: Float) = a + Random.nextFloat() * (b - a)
private fun chance(p: Float) = Random.nextFloat() < p

class Pet(val species: Species, startRoute: Route) {
    var route by mutableStateOf(startRoute)
    // Drawn state (read only during layout / draw, so moving never recomposes).
    var x by mutableFloatStateOf(0f)          // anchor x (feet, or grip when top-anchored)
    var y by mutableFloatStateOf(0f)          // anchor y
    var facing by mutableFloatStateOf(1f)
    var frame by mutableStateOf("idle0")
    var topAnchored by mutableStateOf(false)  // hanging / carried: anchor is the grip at the top
    var tilt by mutableFloatStateOf(0f)
    var squash by mutableFloatStateOf(0f)
    var boxLeft = 0f
    var boxTop = 0f

    // Simulation
    var act = Act.Idle
    var t = 0f
    var dur = 1.5f
    var anim = 0f
    var vx = 0f
    var vy = 0f
    var perch: Perch? = null
    var relX = 0f
    var targetX = 0f
    var free = false               // may walk past the screen edges (arriving / leaving)
    var leaving = false
    var then: (() -> Unit)? = null // what to do after a walk / crouch
    var aim: (() -> Offset?)? = null
    var aimGrab = false
    var hangAt: (() -> Offset?)? = null
    var swingV = 0f
    var blinkT = 3f
    var held = false
    var lastHeldX = 0f
    var hoverRoute: Route? = null
    var hoverT = 0f
    var wander = rnd(90f, 180f)
    var beats = 0
    var beatT = 0f
    var placed = false

    val pal get() = if (species == Species.Cat) PetSprites.CAT_PALETTE else PetSprites.DOG_PALETTE
    val frames get() = if (species == Species.Cat) PetSprites.CAT else PetSprites.DOG
}

/** Decoded sprite pixels, cached per frame. */
private class Art(val xs: IntArray, val ys: IntArray, val colors: IntArray)

private val artCache = HashMap<String, Art>()

private fun art(pet: Pet, name: String): Art {
    val key = pet.species.name + ":" + name
    return artCache.getOrPut(key) {
        val rows = pet.frames[name] ?: pet.frames.getValue("idle0")
        val xs = ArrayList<Int>(); val ys = ArrayList<Int>(); val cs = ArrayList<Int>()
        rows.forEachIndexed { y, line ->
            line.forEachIndexed { x, c -> if (c != '.') { xs += x; ys += y; cs += pet.pal[c] ?: 0xFFFF00FF.toInt() } }
        }
        Art(xs.toIntArray(), ys.toIntArray(), cs.toIntArray())
    }
}

/* ═══════════════════════════════ Engine ═══════════════════════════════ */

class PetEngine(private val world: PetWorld, private val haptics: Haptics) {
    private val d get() = world.density
    private val px get() = 1.4f * d                        // one sprite pixel
    val spriteW get() = PetSprites.W * px
    val spriteH get() = PetSprites.H * px
    private val g get() = 2600f * d
    private var visitT = 8f
    private var lastRoute: Route? = null
    private val dipping = HashSet<Perch>()

    /* ── public: input ── */

    fun tap(p: Pet) {
        if (p.held) return
        haptics.select()
        when {
            p.act == Act.Sleep -> { bubble(p, "!"); start(p, Act.Startle, 0.5f) }
            p.species == Species.Cat -> {
                hearts(p, 3); bubble(p, listOf("prrr", "mrrp?", "meow!", "♥").random())
                if (grounded(p)) start(p, Act.Happy, 1.4f)
            }
            else -> {
                hearts(p, 3); bubble(p, listOf("woof!", "arf!", "♥").random())
                if (grounded(p)) { leap(p, 0f, -380f * d); p.then = { start(p, Act.Wag, 1.6f) } }
            }
        }
    }

    fun grab(p: Pet) {
        haptics.press()
        p.held = true; p.topAnchored = true; p.perch = null
        p.act = Act.Held; p.t = 0f; p.anim = 0f; p.then = null; p.aim = null; p.hangAt = null
        p.lastHeldX = p.x
        if (chance(0.5f)) bubble(p, if (p.species == Species.Cat) "mew?!" else "!")
    }

    fun carry(p: Pet, finger: Offset) {
        p.x = finger.x
        p.y = finger.y + 4f * d
    }

    fun release(p: Pet, vx: Float, vy: Float) {
        p.held = false
        p.topAnchored = false
        p.y += spriteH * 0.92f // grip → feet
        p.tilt = 0f
        val lim = 2600f * d
        startAir(p, vx.coerceIn(-lim, lim), vy.coerceIn(-lim, lim))
    }

    /* ── per frame ── */

    fun step(dt: Float) {
        if (world.width <= 0f) return
        val route = world.route
        if (lastRoute != route) {
            lastRoute = route
            if (world.pets.none { it.route == route }) visitT = rnd(3f, 8f)
        }
        // nothing on this tab? someone may come to visit
        if (world.pets.none { it.route == route }) {
            visitT -= dt
            if (visitT <= 0f) {
                world.pets.filter { !it.held }.randomOrNull()?.let { arrive(it) }
                visitT = rnd(12f, 25f)
            }
        }
        for (p in world.pets) {
            if (!p.placed) place(p)
            if (p.held) { stepHeld(p, dt); continue }
            if (p.route != route) continue
            stepPet(p, dt)
        }
        // perch dips settle back
        val it = dipping.iterator()
        while (it.hasNext()) {
            val pr = it.next()
            pr.dip.floatValue *= exp(-dt * 12f)
            if (abs(pr.dip.floatValue) < 0.2f) { pr.dip.floatValue = 0f; it.remove() }
        }
        stepFx(dt)
    }

    private fun place(p: Pet) {
        p.placed = true
        p.x = world.width * (if (p.species == Species.Cat) rnd(0.2f, 0.4f) else rnd(0.6f, 0.8f))
        p.y = world.floor()
        p.facing = if (chance(0.5f)) 1f else -1f
        start(p, Act.Idle, rnd(1f, 2f))
    }

    private fun arrive(p: Pet) {
        p.route = world.route
        val left = chance(0.5f)
        p.x = if (left) -spriteW else world.width + spriteW
        p.y = world.floor(); p.perch = null; p.topAnchored = false; p.leaving = false
        p.facing = if (left) 1f else -1f
        walkTo(p, world.width * rnd(0.25f, 0.75f), run = chance(0.4f), free = true)
        if (chance(0.6f)) p.then = { bubble(p, if (p.species == Species.Cat) "hi!" else "woof!"); start(p, Act.Idle, 1.2f) }
    }

    /** A pet that moved to this tab earlier: it's simply there, lounging somewhere. */
    private fun settleIn(p: Pet) {
        p.x = world.width * rnd(0.15f, 0.85f)
        p.y = world.floor(); p.perch = null; p.topAnchored = false; p.free = false; p.leaving = false
        p.facing = if (chance(0.5f)) 1f else -1f
        val r = Random.nextFloat()
        if (p.species == Species.Cat) when {
            r < 0.4f -> start(p, Act.Sleep, rnd(4f, 9f))
            r < 0.7f -> start(p, Act.Loaf, rnd(2f, 4f))
            else -> start(p, Act.Sit, rnd(2f, 4f))
        } else when {
            r < 0.4f -> start(p, Act.Sleep, rnd(4f, 9f))
            r < 0.7f -> start(p, Act.Lie, rnd(2f, 4f))
            else -> start(p, Act.Wag, rnd(1.5f, 3f))
        }
    }

    private fun stepHeld(p: Pet, dt: Float) {
        p.route = world.route
        p.anim += dt
        p.frame = "held" + ((p.anim / 0.18f).toInt() % 2)
        // swing with the hand's motion
        val v = (p.x - p.lastHeldX) / max(dt, 1e-3f)
        p.lastHeldX = p.x
        p.tilt += ((-v / (40f * d)).coerceIn(-28f, 28f) - p.tilt) * min(1f, dt * 8f)
        if (abs(v) > 10f * d) p.facing = if (v > 0) 1f else -1f
        // carry to another tab: hover over it in the nav
        val finger = Offset(p.x, p.y - 4f * d)
        val over = world.navTabs.entries.firstOrNull { it.value.inflate(6f * d).contains(finger) }?.key
        if (over != null && over != world.route) {
            if (p.hoverRoute != over) { p.hoverRoute = over; p.hoverT = 0f }
            p.hoverT += dt
            if (p.hoverT > 0.45f) { haptics.select(); world.switchTab(over); p.route = over; p.hoverT = -10f }
        } else { p.hoverRoute = null; p.hoverT = 0f }
    }

    private fun grounded(p: Pet) = p.act != Act.Air && p.act != Act.Hang && !p.held && !p.topAnchored

    private fun stepPet(p: Pet, dt: Float) {
        if (p.act == Act.Away) { settleIn(p); return } // you came to the tab it moved to
        p.t += dt; p.anim += dt
        p.wander -= dt
        p.squash = max(0f, p.squash - dt * 5f)

        // Stay glued to whatever we stand on.
        if (p.act != Act.Air && p.act != Act.Hang) {
            val pr = p.perch
            if (pr != null) {
                if (!world.usable(pr, p.route)) { p.perch = null; startAir(p, 0f, 0f) }
                else { p.x = pr.rect.left + p.relX; p.y = pr.rect.top + pr.dip.floatValue }
            } else p.y = world.floor()
        }

        when (p.act) {
            Act.Air -> stepAir(p, dt)
            Act.Hang -> stepHang(p, dt)
            Act.Walk, Act.Run, Act.Sniff, Act.Zoomies -> stepWalk(p, dt)
            Act.Spin -> { if ((p.anim / 0.16f).toInt() % 2 == 0) p.facing = 1f else p.facing = -1f; timed(p) }
            Act.Dig -> {
                p.beatT -= dt
                if (p.beatT <= 0f) { p.beatT = 0.07f; dirt(p) }
                timed(p)
            }
            Act.Bark -> {
                p.beatT -= dt
                if (p.beatT <= 0f && p.beats > 0) { p.beats--; p.beatT = 0.7f; bubble(p, listOf("WOOF!", "woof", "ARF!").random()); haptics.tick() }
                timed(p)
            }
            Act.Sleep -> {
                p.beatT -= dt
                if (p.beatT <= 0f) { p.beatT = 1.1f; zed(p) }
                timed(p)
            }
            Act.Knead -> {
                p.beatT -= dt
                if (p.beatT <= 0f) { p.beatT = 1.6f; bubble(p, "prrr") }
                timed(p)
            }
            Act.Crouch -> if (p.t >= p.dur) { val n = p.then; p.then = null; if (n != null) n() else think(p) }
            else -> timed(p)
        }
        if (p.act != Act.Hang) p.tilt *= exp(-dt * 10f)
        blink(p, dt)
        p.frame = frameFor(p)
    }

    private fun timed(p: Pet) { if (p.t >= p.dur) { val n = p.then; p.then = null; if (n != null) n() else think(p) } }

    private fun stepWalk(p: Pet, dt: Float) {
        val speed = d * when (p.act) {
            Act.Run, Act.Zoomies -> if (p.species == Species.Cat) 130f else 150f
            Act.Sniff -> 18f
            else -> if (p.species == Species.Cat) 36f else 42f
        }
        val dir = when { p.targetX > p.x + 1f -> 1f; p.targetX < p.x - 1f -> -1f; else -> 0f }
        if (dir != 0f) p.facing = dir
        var nx = p.x + dir * speed * dt
        if ((dir > 0 && nx >= p.targetX) || (dir < 0 && nx <= p.targetX)) nx = p.targetX
        val (lo, hi) = bounds(p)
        if (!p.free) nx = nx.coerceIn(lo, hi)
        p.x = nx
        p.perch?.let { p.relX = nx - it.rect.left }
        // left the screen: off to another tab
        if (p.leaving && (p.x < -spriteW || p.x > world.width + spriteW)) {
            val others = Route.entries.filter { it != world.route }
            p.route = others.random()
            p.leaving = false; p.free = false
            start(p, Act.Away, 1f)
            return
        }
        val arrived = abs(p.x - p.targetX) < 1.5f || dir == 0f || (p.t > 9f && !p.leaving) ||
            (!p.free && (p.x <= lo + 0.5f && dir < 0 || p.x >= hi - 0.5f && dir > 0))
        if (arrived) {
            p.free = false
            if (p.act == Act.Zoomies && p.beats > 0) {
                p.beats--
                walkTo(p, if (p.x < world.width / 2) world.width * rnd(0.75f, 0.92f) else world.width * rnd(0.08f, 0.25f), run = true)
                p.act = Act.Zoomies
                return
            }
            val n = p.then; p.then = null
            if (n != null) n() else think(p)
        }
    }

    private fun stepAir(p: Pet, dt: Float) {
        // aimed jump onto a moving target (clock hand / edge)
        p.aim?.let { aimFn ->
            val tgt = aimFn()
            if (tgt == null) { p.aim = null } else if (p.t >= p.dur) {
                p.aim = null
                if (p.aimGrab) { hang(p, p.hangAt ?: { tgt }); return }
            }
        }
        val ny = p.y + p.vy * dt + 0.5f * g * dt * dt
        p.vy += g * dt
        var nx = p.x + p.vx * dt
        if (nx < 6f * d) { nx = 6f * d; p.vx = abs(p.vx) * 0.35f }
        if (nx > world.width - 6f * d) { nx = world.width - 6f * d; p.vx = -abs(p.vx) * 0.35f }
        if (p.vx != 0f) p.facing = if (p.vx > 0) 1f else -1f
        p.tilt = (p.vy / (90f * d)).coerceIn(-12f, 12f) * p.facing
        if (p.vy > 0f) {
            val land = world.perchesOn(p.route)
                .filter { it.rect.top + 0.5f >= p.y && it.rect.top <= ny && nx in (it.rect.left + 4f * d)..(it.rect.right - 4f * d) }
                .minByOrNull { it.rect.top }
            if (land != null) { landOn(p, land, nx); return }
            if (ny >= world.floor()) { landOn(p, null, nx); return }
        }
        p.x = nx; p.y = ny
    }

    private fun landOn(p: Pet, pr: Perch?, nx: Float) {
        val hard = p.vy > 1400f * d
        p.x = nx
        p.perch = pr
        if (pr != null) {
            p.relX = nx - pr.rect.left; p.y = pr.rect.top
            pr.dip.floatValue = (if (hard) 6f else 3.5f) * d; dipping += pr
        } else p.y = world.floor()
        p.vx = 0f; p.vy = 0f; p.tilt = 0f
        p.squash = if (hard) 1f else 0.6f
        haptics.tick()
        val n = p.then; p.then = null
        if (n != null) n() else if (hard) {
            bubble(p, if (p.species == Species.Cat) listOf("!", "hmph", "mrow!").random() else listOf("woof!", "!", "again!").random())
            start(p, Act.Idle, 0.9f)
        } else start(p, Act.Idle, rnd(0.4f, 1.2f))
    }

    private fun stepHang(p: Pet, dt: Float) {
        val pt = p.hangAt?.invoke()
        if (pt == null) { drop(p); return }
        p.x = pt.x; p.y = pt.y
        // pendulum
        p.swingV += (-34f * p.tilt - 1.4f * p.swingV) * dt
        p.tilt += p.swingV * dt
        if (p.t > 1.2f && chance(dt * 0.6f)) p.swingV += rnd(-50f, 50f) // a little kick now and then
        if (p.t >= p.dur) { val n = p.then; p.then = null; if (n != null) n() else drop(p) }
    }

    private fun drop(p: Pet) {
        p.topAnchored = false
        p.hangAt = null
        p.y += spriteH * 0.92f
        startAir(p, 0f, 0f)
    }

    private fun blink(p: Pet, dt: Float) {
        p.blinkT -= dt
        if (p.blinkT < -0.13f) p.blinkT = rnd(2f, 5f)
    }

    /* ── starting things ── */

    private fun start(p: Pet, act: Act, dur: Float) {
        p.act = act; p.t = 0f; p.dur = dur; p.anim = 0f; p.beatT = 0f
        if (act != Act.Hang) p.topAnchored = false
    }

    private fun startAir(p: Pet, vx: Float, vy: Float) {
        p.perch = null; p.topAnchored = false
        p.vx = vx; p.vy = vy
        start(p, Act.Air, 99f)
    }

    private fun leap(p: Pet, vx: Float, vy: Float) {
        p.perch = null
        p.vx = vx; p.vy = vy
        start(p, Act.Air, 99f)
    }

    private fun bounds(p: Pet): Pair<Float, Float> {
        val pr = p.perch
        return if (pr != null) (pr.rect.left + 8f * d) to (pr.rect.right - 8f * d)
        else (12f * d) to (world.width - 12f * d)
    }

    private fun walkTo(p: Pet, x: Float, run: Boolean = false, free: Boolean = false, sniff: Boolean = false) {
        start(p, when { sniff -> Act.Sniff; run -> Act.Run; else -> Act.Walk }, 99f)
        p.free = free
        p.targetX = if (free) x else bounds(p).let { (lo, hi) -> x.coerceIn(lo, hi) }
    }

    /** Ballistic jump that lands the feet (or the paws, for a grab) on a point at time T. */
    private fun jumpAt(p: Pet, target: () -> Offset?, grab: Boolean) {
        val tgt = target() ?: return think(p)
        val dy = tgt.y - p.y
        val dx = tgt.x - p.x
        val minT = sqrt(2f * max(0f, -dy) / g) * 1.18f
        val tt = max(min(0.42f + abs(dx) / (2400f * d), 0.75f), minT)
        p.facing = if (dx >= 0) 1f else -1f
        leap(p, dx / tt, (dy - 0.5f * g * tt * tt) / tt)
        p.dur = tt
        p.aim = target
        p.aimGrab = grab
        haptics.tick()
    }

    private fun hang(p: Pet, at: () -> Offset?) {
        val pt = at() ?: return drop(p)
        p.hangAt = at
        p.x = pt.x; p.y = pt.y
        p.topAnchored = true
        p.swingV = (p.vx / d) * 0.12f
        p.vx = 0f; p.vy = 0f
        start(p, Act.Hang, rnd(3f, 7f))
        p.topAnchored = true
        haptics.tick()
        if (chance(0.5f)) bubble(p, listOf("wheee", "hehe", "mine!", "nya~").random())
    }

    /* ── deciding what to do next ── */

    private fun think(p: Pet) {
        p.free = false
        val onPerch = p.perch != null
        val r = p.route
        // time to see the rest of the app?
        if (p.wander <= 0f && r == world.route && world.pets.count { it.route == r } >= 1) {
            p.wander = rnd(90f, 200f)
            if (onPerch) return hopDown(p) { leave(p) }
            return leave(p)
        }
        if (p.species == Species.Cat) thinkCat(p, onPerch) else thinkDog(p, onPerch)
    }

    private fun leave(p: Pet) {
        val toLeft = p.x < world.width / 2
        walkTo(p, if (toLeft) -spriteW * 2 else world.width + spriteW * 2, run = chance(0.5f), free = true)
        p.leaving = true
        if (chance(0.5f)) bubble(p, listOf("brb", "bye!", "exploring~").random())
    }

    private fun hopDown(p: Pet, after: (() -> Unit)? = null) {
        val pr = p.perch ?: return think(p)
        val toRight = p.x > pr.rect.center.x
        val edge = if (toRight) pr.rect.right - 9f * d else pr.rect.left + 9f * d
        walkTo(p, edge)
        p.then = {
            p.facing = if (toRight) 1f else -1f
            leap(p, p.facing * rnd(70f, 130f) * d, -rnd(260f, 380f) * d)
            p.then = after
        }
    }

    private fun perchTargets(p: Pet, maxRise: Float): List<Perch> =
        world.perchesOn(p.route).filter {
            it != p.perch && it.rect.top < p.y - 20f * d && p.y - it.rect.top < maxRise &&
                abs(it.rect.center.x - p.x) < world.width * 0.85f
        }

    private fun jumpUp(p: Pet, maxRise: Float): Boolean {
        val options = perchTargets(p, maxRise)
        val pr = options.randomOrNull() ?: return false
        val tx = rnd(pr.rect.left + 14f * d, pr.rect.right - 14f * d)
        val edgeGrab = p.species == Species.Cat && chance(0.3f)
        start(p, Act.Crouch, rnd(0.35f, 0.7f))
        p.facing = if (tx >= p.x) 1f else -1f
        p.then = {
            if (edgeGrab) {
                val leftEdge = abs(p.x - pr.rect.left) < abs(p.x - pr.rect.right)
                val gx = { if (leftEdge) pr.rect.left + 12f * d else pr.rect.right - 12f * d }
                val grip = { if (world.usable(pr, p.route)) Offset(gx(), pr.rect.top + 2f * d) else null }
                p.hangAt = grip
                jumpAt(p, { grip()?.let { Offset(it.x, it.y + spriteH * 0.92f) } }, grab = true)
                // after hanging a moment, scramble up onto it
                p.then = { climb(p, pr) }
            } else {
                jumpAt(p, { if (world.usable(pr, p.route)) Offset(pr.rect.left + (tx - pr.rect.left), pr.rect.top) else null }, grab = false)
            }
        }
        return true
    }

    private fun climb(p: Pet, pr: Perch) {
        if (p.act != Act.Hang) return start(p, Act.Idle, 0.6f)
        p.topAnchored = false; p.hangAt = null; p.tilt = 0f
        if (!world.usable(pr, p.route)) return drop(p)
        p.perch = pr
        p.relX = (p.x - pr.rect.left).coerceIn(10f * d, pr.rect.width - 10f * d)
        p.y = pr.rect.top; p.squash = 0.7f
        pr.dip.floatValue = 3f * d; dipping += pr
        start(p, Act.Idle, 0.8f)
    }

    private fun hangFromClock(p: Pet): Boolean {
        val pts = world.hangPointsOn(p.route)
        if (pts.isEmpty()) return false
        val i = Random.nextInt(pts.size)
        val at = { world.hangPointsOn(p.route).getOrNull(i) }
        start(p, Act.Crouch, 0.5f)
        p.facing = if ((pts[i].x) >= p.x) 1f else -1f
        p.then = {
            p.hangAt = at
            jumpAt(p, { at()?.let { Offset(it.x, it.y + spriteH * 0.92f) } }, grab = true)
        }
        return true
    }

    private fun thinkCat(p: Pet, onPerch: Boolean) {
        val dog = world.pets.firstOrNull { it.species == Species.Dog && it.route == p.route && it.act == Act.Run && abs(it.x - p.x) < 140f * d }
        if (dog != null && !onPerch) {
            bubble(p, "!"); start(p, Act.Startle, 0.35f)
            p.then = { if (!jumpUp(p, world.height * 0.6f)) walkTo(p, if (dog.x < p.x) world.width - 20f * d else 20f * d, run = true) }
            return
        }
        val roll = Random.nextFloat() * 100f
        when {
            roll < 24 && hangFromClock(p) -> Unit
            roll < 34 && jumpUp(p, world.height * 0.55f) -> Unit
            roll < 40 && onPerch -> hopDown(p)
            roll < 50 -> walkTo(p, p.x + rnd(-1f, 1f) * world.width * 0.5f)
            roll < 55 -> walkTo(p, if (p.x < world.width / 2) world.width * 0.85f else world.width * 0.15f, run = true)
            roll < 64 -> {
                start(p, Act.Sit, rnd(3f, 6f))
                when {
                    chance(0.3f) -> p.then = { start(p, Act.Meow, 0.9f); bubble(p, listOf("meow", "mew", "nya", "mrrp").random()) }
                    chance(0.3f) -> p.then = { start(p, Act.Yawn, 1.4f) }
                }
            }
            roll < 71 -> start(p, Act.Groom, rnd(2f, 3.5f))
            roll < 79 -> {
                start(p, Act.Loaf, rnd(2.5f, 4f))
                if (chance(0.55f)) p.then = { start(p, Act.Sleep, rnd(7f, 14f)); p.then = { start(p, Act.Stretch, 1.2f); p.then = { start(p, Act.Yawn, 1.2f) } } }
            }
            roll < 84 -> start(p, Act.Knead, rnd(2f, 3f))
            roll < 88 -> start(p, Act.Stretch, 1.2f)
            roll < 94 -> { // pounce on nothing in particular
                start(p, Act.Crouch, rnd(0.6f, 1.1f))
                p.then = { leap(p, p.facing * rnd(150f, 240f) * d, -rnd(380f, 520f) * d) }
            }
            else -> start(p, Act.Idle, rnd(1.5f, 3.5f))
        }
    }

    private fun thinkDog(p: Pet, onPerch: Boolean) {
        val cat = world.pets.firstOrNull { it.species == Species.Cat && it.route == p.route && !it.held }
        val roll = Random.nextFloat() * 100f
        when {
            cat != null && roll < 9 && !onPerch -> { // chase the cat!
                bubble(p, "!")
                walkTo(p, cat.x, run = true)
                p.then = { start(p, Act.Bark, 2f); p.beats = 2; p.then = { start(p, Act.Wag, 1.5f) } }
            }
            roll < 15 && jumpUp(p, world.height * 0.28f) -> Unit
            roll < 26 && onPerch -> hopDown(p)
            roll < 38 -> walkTo(p, p.x + rnd(-1f, 1f) * world.width * 0.5f)
            roll < 45 && !onPerch -> walkTo(p, p.x + rnd(-1f, 1f) * world.width * 0.3f, sniff = true).also { p.then = { if (chance(0.5f)) start(p, Act.Dig, rnd(1.5f, 2.5f)) else think(p) } }
            roll < 50 -> { start(p, Act.Bow, 0.9f); p.then = { start(p, Act.Zoomies, 99f); p.beats = Random.nextInt(2, 5); walkTo(p, if (p.x < world.width / 2) world.width * 0.9f else world.width * 0.1f, run = true); p.act = Act.Zoomies } }
            roll < 58 -> start(p, Act.Sit, rnd(3f, 6f))
            roll < 63 -> { start(p, Act.Bark, 1.8f); p.beats = Random.nextInt(1, 3) }
            roll < 68 -> start(p, Act.Scratch, rnd(1.2f, 2.2f))
            roll < 76 -> {
                start(p, Act.Lie, rnd(2.5f, 4f))
                if (chance(0.6f)) p.then = { start(p, Act.Sleep, rnd(7f, 13f)); p.then = { start(p, Act.Wag, 1.2f) } }
            }
            roll < 81 -> { start(p, Act.Belly, rnd(2f, 3.2f)); hearts(p, 2) }
            roll < 86 && !onPerch -> start(p, Act.Dig, rnd(1.5f, 2.5f))
            roll < 90 -> start(p, Act.Spin, rnd(1.2f, 2f))
            roll < 95 -> start(p, Act.Wag, rnd(1.5f, 3f))
            else -> start(p, Act.Idle, rnd(1.5f, 3f))
        }
    }

    /* ── which picture to show ── */

    private fun frameFor(p: Pet): String {
        val a = p.anim
        val blink = p.blinkT < 0f
        fun cyc(names: List<String>, step: Float) = names[(a / step).toInt() % names.size]
        return if (p.species == Species.Cat) when (p.act) {
            Act.Idle -> if (blink) "idle_blink" else if (a % 6f > 4.8f) "idle_up" else cyc(listOf("idle0", "idle1", "idle2", "idle1"), 0.38f)
            Act.Walk, Act.Sniff -> cyc(listOf("walk0", "walk1", "walk2", "walk3"), 0.13f)
            Act.Run, Act.Zoomies -> cyc(listOf("run0", "run1"), 0.085f)
            Act.Sit, Act.Away, Act.Bark, Act.Scratch, Act.Wag, Act.Bow -> if (blink) "sit_blink" else if (a % 5f > 4f) "sit_up" else cyc(listOf("sit0", "sit1"), 1.2f)
            Act.Happy -> cyc(listOf("sit_happy", "idle_happy"), 0.7f)
            Act.Meow -> "meow"
            Act.Yawn -> if (a < 0.9f) "yawn" else "sit_blink"
            Act.Groom -> cyc(listOf("groom0", "groom1"), 0.28f)
            Act.Loaf, Act.Lie -> if (blink) "loaf0" else "loaf_awake"
            Act.Sleep, Act.Belly -> cyc(listOf("loaf0", "loaf1"), 1.0f)
            Act.Knead -> cyc(listOf("knead0", "knead1"), 0.22f)
            Act.Stretch -> "stretch"
            Act.Crouch, Act.Dig -> cyc(listOf("crouch0", "crouch1"), 0.1f)
            Act.Startle -> "crouch0"
            Act.Spin -> cyc(listOf("run0", "run1"), 0.085f)
            Act.Air -> if (p.vy < 0f) "jump" else "fall"
            Act.Hang -> if (a % 4f > 3.2f) "hang_happy" else cyc(listOf("hang0", "hang1"), 0.45f)
            Act.Held -> cyc(listOf("held0", "held1"), 0.18f)
        } else when (p.act) {
            Act.Idle -> if (blink) "idle_blink" else if (a % 5f > 3.6f) "idle_tongue" else cyc(listOf("idle0", "idle1"), 0.35f)
            Act.Wag, Act.Happy -> cyc(listOf("wag0", "wag1"), 0.1f)
            Act.Walk -> cyc(listOf("walk0", "walk1", "walk2", "walk3"), 0.13f)
            Act.Run, Act.Zoomies, Act.Spin -> cyc(listOf("run0", "run1"), 0.08f)
            Act.Sniff -> cyc(listOf("sniff0", "sniff1"), 0.25f)
            Act.Sit, Act.Away, Act.Meow, Act.Yawn, Act.Groom, Act.Knead, Act.Stretch ->
                if (blink) "sit_blink" else if (a % 4f > 2.6f) "sit_tongue" else cyc(listOf("sit0", "sit1"), 0.5f)
            Act.Bark -> if (p.beatT > 0.45f) "bark0" else "sit0"
            Act.Scratch -> cyc(listOf("scratch0", "scratch1"), 0.08f)
            Act.Lie, Act.Loaf -> if (blink) "lie0" else "lie_awake"
            Act.Sleep -> cyc(listOf("lie0", "lie1"), 1.0f)
            Act.Belly -> cyc(listOf("belly0", "belly1"), 0.25f)
            Act.Dig -> cyc(listOf("dig0", "dig1"), 0.12f)
            Act.Bow -> cyc(listOf("bow0", "bow1"), 0.14f)
            Act.Crouch, Act.Startle -> cyc(listOf("bow0", "bow1"), 0.1f)
            Act.Air, Act.Hang -> "jump"
            Act.Held -> cyc(listOf("held0", "held1"), 0.2f)
        }
    }

    /* ── effects ── */

    private fun bubble(p: Pet, text: String) {
        world.fx.removeAll { it.kind == Fx.Kind.Bubble && it.follow === p }
        world.fx += Fx(Fx.Kind.Bubble, p.x, p.y, text, life = 1.6f, route = p.route).also { it.follow = p }
    }

    private fun hearts(p: Pet, n: Int) = repeat(n) {
        world.fx += Fx(Fx.Kind.Heart, p.x + rnd(-10f, 10f) * d, p.y - spriteH * 0.8f, vx = rnd(-20f, 20f) * d, vy = -rnd(40f, 70f) * d, life = 1.2f + it * 0.15f, route = p.route)
    }

    private fun zed(p: Pet) {
        world.fx += Fx(Fx.Kind.Zed, p.x + p.facing * 8f * d, p.y - spriteH * 0.6f, vx = p.facing * 10f * d, vy = -22f * d, life = 2f, route = p.route)
    }

    private fun dirt(p: Pet) {
        world.fx += Fx(Fx.Kind.Dirt, p.x + p.facing * 10f * d, p.y - 2f * d, vx = -p.facing * rnd(40f, 120f) * d, vy = -rnd(120f, 240f) * d, life = 0.7f, route = p.route)
    }

    private fun stepFx(dt: Float) {
        val it = world.fx.iterator()
        val dead = ArrayList<Fx>()
        while (it.hasNext()) {
            val f = it.next()
            f.age += dt
            if (f.age >= f.life) { dead += f; continue }
            when (f.kind) {
                Fx.Kind.Dirt -> { f.vy += g * 0.5f * dt; f.x += f.vx * dt; f.y += f.vy * dt }
                Fx.Kind.Bubble -> f.follow?.let { f.x = it.x; f.y = if (it.topAnchored) it.y else it.y - spriteH }
                else -> { f.x += f.vx * dt; f.y += f.vy * dt }
            }
        }
        if (dead.isNotEmpty()) world.fx.removeAll(dead)
    }
}

/* ═══════════════════════════════ UI layer ═══════════════════════════════ */

/**
 * Draws the pets (and their hearts / z's / bubbles) over the app and handles touch:
 * tap to pet, drag to carry, release to drop or throw. [choice] is "cat", "dog",
 * "both" or "none". [active] pauses everything while a sheet / web page covers the app.
 */
@Composable
fun PetLayer(world: PetWorld, choice: String, active: Boolean) {
    val haptics = LocalHaptics.current
    val density = LocalDensity.current
    val statusTop = with(density) { WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx() }
    val engine = remember { PetEngine(world, haptics) }
    var tick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(choice) {
        val want = when (choice) { "cat" -> listOf(Species.Cat); "dog" -> listOf(Species.Dog); "both" -> listOf(Species.Cat, Species.Dog); else -> emptyList() }
        world.pets.removeAll { it.species !in want }
        want.filter { s -> world.pets.none { it.species == s } }.forEach { world.pets += Pet(it, world.route) }
        world.fx.clear()
    }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceIn(0f, 0.05f)
                last = now
                engine.step(dt)
                tick = now
            }
        }
    }
    if (!active || world.pets.isEmpty()) return

    BoxWithConstraints(Modifier.fillMaxSize()) {
        world.width = constraints.maxWidth.toFloat()
        world.height = constraints.maxHeight.toFloat()
        world.statusTop = statusTop
        world.density = density.density
        val measurer = rememberTextMeasurer()

        val route = world.route
        world.pets.forEach { pet -> key(pet) { if (pet.route == route || pet.held) PetSprite(pet, engine) } }

        // hearts, z's, dirt, bubbles (no touch handling, so taps pass through)
        Canvas(Modifier.fillMaxSize()) {
            if (tick < 0L) return@Canvas
            val px = 1.4f * density.density
            world.fx.forEach { f ->
                if (f.route != route) return@forEach
                val k = 1f - f.age / f.life
                when (f.kind) {
                    Fx.Kind.Heart -> pixelArt(HEART, f.x, f.y, px, Color(0xFFF7A8B8).copy(alpha = k))
                    Fx.Kind.Zed -> pixelArt(ZED, f.x, f.y, px, Color(0xFFDCD6EA).copy(alpha = k))
                    Fx.Kind.Dirt -> drawRect(Color(0xFFA8693F).copy(alpha = k), Offset(f.x, f.y), Size(px * 1.4f, px * 1.4f))
                    Fx.Kind.Spark -> drawRect(Color.White.copy(alpha = k), Offset(f.x, f.y), Size(px, px))
                    Fx.Kind.Bubble -> {
                        val a = (min(1f, f.age * 8f) * min(1f, (f.life - f.age) * 4f)).coerceIn(0f, 1f)
                        val layout = measurer.measure(f.text, TextStyle(fontFamily = Mono, fontSize = 11.sp, color = Color(0xFF2B2433)))
                        val w = layout.size.width + 14f * density.density
                        val h = layout.size.height + 8f * density.density
                        val left = (f.x - w / 2).coerceIn(4f, size.width - w - 4f)
                        val top = f.y - h - 8f * density.density
                        drawRoundRect(Color.White.copy(alpha = a), Offset(left, top), Size(w, h), CornerRadius(h / 2))
                        drawRect(Color.White.copy(alpha = a), Offset(f.x - 3f * density.density, top + h - 1f), Size(6f * density.density, 4f * density.density))
                        drawText(layout, topLeft = Offset(left + 7f * density.density, top + 4f * density.density), alpha = a)
                    }
                }
            }
        }
    }
}

private val HEART = listOf(".KK.KK.", "KKKKKKK", "KKKKKKK", ".KKKKK.", "..KKK..", "...K...")
private val ZED = listOf("KKKK", "..K.", ".K..", "KKKK")

private fun DrawScope.pixelArt(rows: List<String>, cx: Float, cy: Float, px: Float, color: Color) {
    val w = rows[0].length * px
    rows.forEachIndexed { y, line ->
        line.forEachIndexed { x, c -> if (c != '.') drawRect(color, Offset(cx - w / 2 + x * px, cy + y * px), Size(px + 0.5f, px + 0.5f)) }
    }
}

@Composable
private fun PetSprite(pet: Pet, engine: PetEngine) {
    val density = LocalDensity.current
    val px = 1.4f * density.density
    val wPx = PetSprites.W * px
    val hPx = PetSprites.H * px
    Box(
        Modifier
            .offset {
                val flip = pet.facing < 0f
                val col = if (pet.topAnchored) PetSprites.GRIP_X else PetSprites.FOOT_X
                val anchorCol = if (flip) PetSprites.W - 1 - col else col
                val left = pet.x - (anchorCol + 0.5f) * px
                val top = if (pet.topAnchored) pet.y else pet.y - hPx
                pet.boxLeft = left; pet.boxTop = top
                IntOffset(left.roundToInt(), top.roundToInt())
            }
            .size(with(density) { wPx.toDp() }, with(density) { hPx.toDp() })
            .pointerInput(pet) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val t0 = down.uptimeMillis
                    val start = down.position + Offset(pet.boxLeft, pet.boxTop)
                    val vt = VelocityTracker()
                    vt.addPosition(down.uptimeMillis, start)
                    var dragging = false
                    var lastUp = t0
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        val abs = ch.position + Offset(pet.boxLeft, pet.boxTop)
                        lastUp = ch.uptimeMillis
                        if (!ch.pressed) break
                        vt.addPosition(ch.uptimeMillis, abs)
                        if (!dragging && (abs - start).getDistance() > viewConfiguration.touchSlop) {
                            dragging = true
                            engine.grab(pet)
                        }
                        if (dragging) { ch.consume(); engine.carry(pet, abs) }
                    }
                    if (dragging) {
                        val v = vt.calculateVelocity()
                        engine.release(pet, v.x, v.y)
                    } else if (lastUp - t0 < 500) engine.tap(pet)
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val a = art(pet, pet.frame)
            val flip = pet.facing < 0f
            val col = if (pet.topAnchored) PetSprites.GRIP_X else PetSprites.FOOT_X
            val anchorCol = if (flip) PetSprites.W - 1 - col else col
            val pivot = Offset((anchorCol + 0.5f) * px, if (pet.topAnchored) 0f else hPx)
            val sq = pet.squash
            withTransform({
                rotate(pet.tilt, pivot)
                scale(1f + 0.12f * sq, 1f - 0.16f * sq, pivot)
            }) {
                val n = a.xs.size
                for (i in 0 until n) {
                    val x = if (flip) PetSprites.W - 1 - a.xs[i] else a.xs[i]
                    drawRect(Color(a.colors[i]), Offset(x * px, a.ys[i] * px), Size(px + 0.6f, px + 0.6f))
                }
            }
        }
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/pets/PetSprites.kt`

```kotlin
package com.student.amizoneplus.ui.pets

// GENERATED by tools/pets/sprites.py — edit the Python, not this file.
// Frames are 26x22 pixel grids facing right; '.' is transparent.

object PetSprites {
    const val W = 26
    const val H = 22
    const val FOOT_X = 13
    const val GRIP_X = 13

    val CAT_PALETTE: Map<Char, Int> = mapOf('K' to 0xFF2B2433.toInt(), 'W' to 0xFFFFFFFF.toInt(), 'G' to 0xFFDCD6EA.toInt(), 'P' to 0xFFF7A8B8.toInt(), 'N' to 0xFFF07C93.toInt(), 'E' to 0xFF2B2433.toInt(), 'H' to 0xFFFFFFFF.toInt(), 'O' to 0xFFF4B26A.toInt())
    val DOG_PALETTE: Map<Char, Int> = mapOf('K' to 0xFF2B2433.toInt(), 'T' to 0xFFE6B27E.toInt(), 'B' to 0xFFA8693F.toInt(), 'W' to 0xFFFFF8F0.toInt(), 'N' to 0xFF2B2433.toInt(), 'P' to 0xFFF27C8E.toInt(), 'E' to 0xFF2B2433.toInt(), 'H' to 0xFFFFFFFF.toInt(), 'R' to 0xFFE2483D.toInt(), 'Y' to 0xFFF7C54A.toInt(), 'G' to 0xFFC9925F.toInt())

    val CAT: Map<String, Array<String>> = mapOf(
        "idle0" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..KK......................", ".KOOK.........K.......K...", ".KWK.........KPK.....KPK..", ".KWK.........KWOKKKKKWWK..", "..KWK........KWOOWWWWWWK..", "..KWK........KWEHWWWEHWK..", "...KWK.......KWEEWWWEEWK..", "....KWKKKKKKKKPWWWNWWWPK..", ".....KWWWWWWWWKWWKWKWWK...", "....KWWWWWWWWWWKKKKKKK....", "....KWWWWWWWWWWWK.........", "....KGWWWWWWWWWGK.........", ".....KGGWWWWWGGK..........", "......KKKKKKKKK...........", "......KWK...KWK...........", "......KWK...KWK...........", "......KKK...KKK..........."),
        "idle1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "...KK.........K.......K...", "..KOOK.......KPK.....KPK..", ".KWK.........KWOKKKKKWWK..", ".KWK.........KWOOWWWWWWK..", "..KWK........KWEHWWWEHWK..", "...KWK.......KWEEWWWEEWK..", "....KWKKKKKKKKPWWWNWWWPK..", ".....KWWWWWWWWKWWKWKWWK...", "....KWWWWWWWWWWKKKKKKK....", "....KWWWWWWWWWWWK.........", "....KGWWWWWWWWWGK.........", ".....KGGWWWWWGGK..........", "......KKKKKKKKK...........", "......KWK...KWK...........", "......KWK...KWK...........", "......KKK...KKK..........."),
        "idle2" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", ".KK.......................", ".KOK..........K.......K...", "..KWK........KPK.....KPK..", "..KWK........KWOKKKKKWWK..", "..KWK........KWOOWWWWWWK..", "..KWK........KWEHWWWEHWK..", "...KWK.......KWEEWWWEEWK..", "....KWKKKKKKKKPWWWNWWWPK..", ".....KWWWWWWWWKWWKWKWWK...", "....KWWWWWWWWWWKKKKKKK....", "....KWWWWWWWWWWWK.........", "....KGWWWWWWWWWGK.........", ".....KGGWWWWWGGK..........", "......KKKKKKKKK...........", "......KWK...KWK...........", "......KWK...KWK...........", "......KKK...KKK..........."),
        "idle_blink" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..KK......................", ".KOOK.........K.......K...", ".KWK.........KPK.....KPK..", ".KWK.........KWOKKKKKWWK..", "..KWK........KWOOWWWWWWK..", "..KWK........KWWWWWWWWWK..", "...KWK.......KWKKWWWKKWK..", "....KWKKKKKKKKPWWWNWWWPK..", ".....KWWWWWWWWKWWKWKWWK...", "....KWWWWWWWWWWKKKKKKK....", "....KWWWWWWWWWWWK.........", "....KGWWWWWWWWWGK.........", ".....KGGWWWWWGGK..........", "......KKKKKKKKK...........", "......KWK...KWK...........", "......KWK...KWK...........", "......KKK...KKK..........."),
        "idle_happy" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "...KK.........K.......K...", "..KOOK.......KPK.....KPK..", ".KWK.........KWOKKKKKWWK..", ".KWK.........KWOOWWWWWWK..", "..KWK........KWKKWWWKKWK..", "...KWK.......KWWWWWWWWWK..", "....KWKKKKKKKKPWWWNWWWPK..", ".....KWWWWWWWWKWWKWKWWK...", "....KWWWWWWWWWWKKKKKKK....", "....KWWWWWWWWWWWK.........", "....KGWWWWWWWWWGK.........", ".....KGGWWWWWGGK..........", "......KKKKKKKKK...........", "......KWK...KWK...........", "......KWK...KWK...........", "......KKK...KKK..........."),
        "idle_up" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..KK......................", ".KOOK.........K.......K...", ".KWK.........KPK.....KPK..", ".KWK.........KWOKKKKKWWK..", "..KWK........KWEHWWWEHWK..", "..KWK........KWEEWWWEEWK..", "...KWK.......KWWWWWWWWWK..", "....KWKKKKKKKKPWWWNWWWPK..", ".....KWWWWWWWWKWWKWKWWK...", "....KWWWWWWWWWWKKKKKKK....", "....KWWWWWWWWWWWK.........", "....KGWWWWWWWWWGK.........", ".....KGGWWWWWGGK..........", "......KKKKKKKKK...........", "......KWK...KWK...........", "......KWK...KWK...........", "......KKK...KKK..........."),
        "walk0" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "...KK.........K.......K...", "..KOOK.......KPK.....KPK..", ".KWK.........KWOKKKKKWWK..", ".KWK.........KWOOWWWWWWK..", "..KWK........KWEHWWWEHWK..", "...KWK.......KWEEWWWEEWK..", "....KWKKKKKKKKPWWWNWWWPK..", ".....KWWWWWWWWKWWKWKWWK...", "....KWWWWWWWWWWKKKKKKK....", "....KWWWWWWWWWWWK.........", "....KGWWWWWWWWWGK.........", ".....KGGWWWWWGGK..........", "......KKKKKKKKK...........", "......KWK....KWK..........", ".....KWK......KWK.........", ".....KK........KK........."),
        "walk1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "...KK.........K.......K...", "..KOOK.......KPK.....KPK..", ".KWK.........KWOKKKKKWWK..", ".KWK.........KWOOWWWWWWK..", "..KWK........KWEHWWWEHWK..", "...KWK.......KWEEWWWEEWK..", "....KWKKKKKKKKPWWWNWWWPK..", ".....KWWWWWWWWKWWKWKWWK...", "....KWWWWWWWWWWKKKKKKK....", "....KWWWWWWWWWWWK.........", "....KGWWWWWWWWWGK.........", ".....KGGWWWWWGGK..........", "......KKKKKKKKK...........", ".......KWK.KWK............", ".......KWK.KWK............", ".......KK..KK.............", ".........................."),
        "walk2" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..KK......................", ".KOOK.........K.......K...", ".KWK.........KPK.....KPK..", ".KWK.........KWOKKKKKWWK..", "..KWK........KWOOWWWWWWK..", "..KWK........KWEHWWWEHWK..", "...KWK.......KWEEWWWEEWK..", "....KWKKKKKKKKPWWWNWWWPK..", ".....KWWWWWWWWKWWKWKWWK...", "....KWWWWWWWWWWKKKKKKK....", "....KWWWWWWWWWWWK.........", "....KGWWWWWWWWWGK.........", ".....KGGWWWWWGGK..........", "......KKKKKKKKK...........", "......KWK...KWK...........", ".......KWKKWK.............", "........KKKK.............."),
        "walk3" to arrayOf("..........................", "..........................", "..........................", "..........................", "..KK......................", ".KOOK.........K.......K...", ".KWK.........KPK.....KPK..", ".KWK.........KWOKKKKKWWK..", "..KWK........KWOOWWWWWWK..", "..KWK........KWEHWWWEHWK..", "...KWK.......KWEEWWWEEWK..", "....KWKKKKKKKKPWWWNWWWPK..", ".....KWWWWWWWWKWWKWKWWK...", "....KWWWWWWWWWWKKKKKKK....", "....KWWWWWWWWWWWK.........", "....KGWWWWWWWWWGK.........", ".....KGGWWWWWGGK..........", "......KKKKKKKKK...........", ".......KWK.KWK............", ".......KWK.KWK............", ".......KK..KK.............", ".........................."),
        "run0" to arrayOf("..........................", "..........................", "..........................", "..........................", ".KK.......................", ".KOK..........K.......K...", "..KWK........KPK.....KPK..", "..KWK........KWOKKKKKWWK..", "..KWK........KWOOWWWWWWK..", "..KWK........KWEHWWWEHWK..", "...KWK.......KWEEWWWEEWK..", "....KWKKKKKKKKPWWWNWWWPK..", ".....KWWWWWWWWKWWKWKWWK...", "....KWWWWWWWWWWKKKKKKK....", "....KWWWWWWWWWWWK.........", "....KGWWWWWWWWWGK.........", ".....KGGWWWWWGGK..........", "......KKKKKKKKK...........", ".....KWK......KWK.........", "....KWK........KW.........", "....KK..........K.........", ".........................."),
        "run1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "...KK.........K.......K...", "..KOOK.......KPK.....KPK..", ".KWK.........KWOKKKKKWWK..", ".KWK.........KWOOWWWWWWK..", "..KWK........KWEHWWWEHWK..", "...KWK.......KWEEWWWEEWK..", "....KWKKKKKKKKPWWWNWWWPK..", ".....KWWWWWWWWKWWKWKWWK...", "....KWWWWWWWWWWKKKKKKK....", "....KWWWWWWWWWWWK.........", "....KGWWWWWWWWWGK.........", ".....KGGWWWWWGGK..........", "......KKKKKKKKK...........", "........KWKKWK............", "........KWKKWK............", "........KK.KK............."),
        "sit0" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "........K.......K.........", ".......KPK.....KPK........", ".......KWOKKKKKWWK........", ".......KWOOWWWWWWK........", ".......KWEHWWWEHWK........", ".......KWEEWWWEEWK........", ".......KPWWWNWWWPK........", "........KWWKWKWWK.........", ".........KKKKKKK..........", ".........KWWWWWK..........", ".......KKWWWWWWWK.........", "......KOKWWWWWWWK.........", "....KKWWKWWWWWWWK.........", "....KWWKKGWWWWWGK.........", ".....KK.KGWWKWWGK.........", "........KGWKWKWGK.........", ".........KKK.KKK.........."),
        "sit1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "........K.......K.........", ".......KPK.....KPK........", ".......KWOKKKKKWWK........", ".......KWOOWWWWWWK........", ".......KWEHWWWEHWK........", ".......KWEEWWWEEWK........", ".......KPWWWNWWWPK........", "........KWWKWKWWK.........", ".........KKKKKKK..........", ".........KWWWWWK..........", "........KWWWWWWWK.........", ".......KKWWWWWWWK.........", "....KKKOKWWWWWWWK.........", "....KWWWKGWWWWWGK.........", ".....KKKKGWWKWWGK.........", "........KGWKWKWGK.........", ".........KKK.KKK.........."),
        "sit_blink" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "........K.......K.........", ".......KPK.....KPK........", ".......KWOKKKKKWWK........", ".......KWOOWWWWWWK........", ".......KWWWWWWWWWK........", ".......KWKKWWWKKWK........", ".......KPWWWNWWWPK........", "........KWWKWKWWK.........", ".........KKKKKKK..........", ".........KWWWWWK..........", ".......KKWWWWWWWK.........", "......KOKWWWWWWWK.........", "....KKWWKWWWWWWWK.........", "....KWWKKGWWWWWGK.........", ".....KK.KGWWKWWGK.........", "........KGWKWKWGK.........", ".........KKK.KKK.........."),
        "sit_up" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "........K.......K.........", ".......KPK.....KPK........", ".......KWOKKKKKWWK........", ".......KWEHWWWEHWK........", ".......KWEEWWWEEWK........", ".......KWWWWWWWWWK........", ".......KPWWWNWWWPK........", "........KWWKWKWWK.........", ".........KKKKKKK..........", ".........KWWWWWK..........", ".......KKWWWWWWWK.........", "......KOKWWWWWWWK.........", "....KKWWKWWWWWWWK.........", "....KWWKKGWWWWWGK.........", ".....KK.KGWWKWWGK.........", "........KGWKWKWGK.........", ".........KKK.KKK.........."),
        "sit_happy" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "........K.......K.........", ".......KPK.....KPK........", ".......KWOKKKKKWWK........", ".......KWOOWWWWWWK........", ".......KWKKWWWKKWK........", ".......KWWWWWWWWWK........", ".......KPWWWNWWWPK........", "........KWWKWKWWK.........", ".........KKKKKKK..........", ".........KWWWWWK..........", "........KWWWWWWWK.........", ".......KKWWWWWWWK.........", "....KKKOKWWWWWWWK.........", "....KWWWKGWWWWWGK.........", ".....KKKKGWWKWWGK.........", "........KGWKWKWGK.........", ".........KKK.KKK.........."),
        "yawn" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "........K.......K.........", ".......KPK.....KPK........", ".......KWOKKKKKWWK........", ".......KWOOWWWWWWK........", ".......KWWWWWWWWWK........", ".......KWKKWWWKKWK........", ".......KPWWKKKWWPK........", "........KWWKNKWWK.........", ".........KKKKKKK..........", ".........KWWWWWK..........", ".......KKWWWWWWWK.........", "......KOKWWWWWWWK.........", "....KKWWKWWWWWWWK.........", "....KWWKKGWWWWWGK.........", ".....KK.KGWWKWWGK.........", "........KGWKWKWGK.........", ".........KKK.KKK.........."),
        "meow" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "........K.......K.........", ".......KPK.....KPK........", ".......KWOKKKKKWWK........", ".......KWOOWWWWWWK........", ".......KWEHWWWEHWK........", ".......KWEEWWWEEWK........", ".......KPWWWNWWWPK........", "........KWWKNKWWK.........", ".........KKKKKKK..........", ".........KWWWWWK..........", ".......KKWWWWWWWK.........", "......KOKWWWWWWWK.........", "....KKWWKWWWWWWWK.........", "....KWWKKGWWWWWGK.........", ".....KK.KGWWKWWGK.........", "........KGWKWKWGK.........", ".........KKK.KKK.........."),
        "groom0" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", ".........K.......K........", "........KPK.....KPK.......", "........KWOKKKKKWWK.......", "........KWOOWWWWWWK.......", "........KWWWWWWWWWK.......", "........KWKKWWWKKWK.......", "........KPWWWNKWKPK.......", ".........KWWKWKWKK........", ".........KKKKKKKK.........", ".......KKWWWWWWWK.........", "......KOKWWWWWWWK.........", "....KKWWKWWWWWWWK.........", "....KWWKKGWWWWWGK.........", ".....KK.KGWWKWWGK.........", "........KGWKWKWGK.........", ".........KKK.KKK.........."),
        "groom1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........K.......K.......", ".........KPK.....KPK......", ".........KWOKKKKKWWK......", ".........KWOOWWWWWWK......", ".........KWWWWWKKWWK......", ".........KWKKWKWKKWK......", ".........KPWWWKWKWPK......", ".........KKWWKWKWWK.......", ".......KKWWKKKKKKK........", "......KOKWWWWWWWK.........", "....KKWWKWWWWWWWK.........", "....KWWKKGWWWWWGK.........", ".....KK.KGWWKWWGK.........", "........KGWKWKWGK.........", ".........KKK.KKK.........."),
        "loaf0" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", ".............K.......K....", "............KPK.....KPK...", "............KWOKKKKKWWK...", "............KWOOWWWWWWK...", "............KWWWWWWWWWK...", ".......KKKKKKWKKWWWKKWK...", "......KWWWWWKPWWWNWWWPK...", ".KKKKKWWWWWWWKWWKWKWWK....", ".KOWKWWWWWWWWWKKKKKKK.....", "..KKKGWWWWWWWWWWWGK.......", ".....KKKKKKKKKKKKK........"),
        "loaf1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", ".............K.......K....", "............KPK.....KPK...", "............KWOKKKKKWWK...", "............KWOOWWWWWWK...", ".......KKKKKKWWWWWWWWWK...", "......KWWWWWKWKKWWWKKWK...", ".....KWWWWWWKPWWWNWWWPK...", ".KKKKWWWWWWWWKWWKWKWWK....", ".KOWKGWWWWWWWWKKKKKKK.....", "..KKKKKKKKKKKKKKKK........", ".........................."),
        "loaf_awake" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", ".............K.......K....", "............KPK.....KPK...", "............KWOKKKKKWWK...", "............KWOOWWWWWWK...", "............KWEHWWWEHWK...", ".......KKKKKKWEEWWWEEWK...", "......KWWWWWKPWWWNWWWPK...", ".KKKKKWWWWWWWKWWKWKWWK....", ".KOWKWWWWWWWWWKKKKKKK.....", "..KKKGWWWWWWWWWWWGK.......", ".....KKKKKKKKKKKKK........"),
        "knead0" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", ".............K.......K....", "............KPK.....KPK...", "............KWOKKKKKWWK...", "............KWOOWWWWWWK...", "............KWKKWWWKKWK...", ".......KKKKKKWWWWWWWWWK...", "......KWWWWWKPWWWNWWWPK...", ".KKKKKWWWWWWWKWWKWKWWK....", ".KOWKWWWWWWWWWKKKKKKK.....", "..KKKGWWWWWWWWWKWKKKWK....", ".....KKKKKKKKKKKKK.KKK...."),
        "knead1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", ".............K.......K....", "............KPK.....KPK...", "............KWOKKKKKWWK...", "............KWOOWWWWWWK...", "............KWKKWWWKKWK...", ".......KKKKKKWWWWWWWWWK...", "......KWWWWWKPWWWNWWWPK...", ".KKKKKWWWWWWWKWWKWKWWK....", ".KOWKWWWWWWWWWKKKKKKK.....", "..KKKGWWWWWWWWWWWGK.......", ".....KKKKKKKKKKKWK.KWK...."),
        "crouch0" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "KK........................", "KOK.......................", ".KWK..........K.......K...", ".KWK.........KPK.....KPK..", ".KWK.........KWOKKKKKWWK..", ".KWK.........KWOOWWWWWWK..", "..KWK........KEHWWWWWEHK..", "...KKKKKKKKK.KEEWWWWWEEK..", "....KWWWWWWWKKPWWWNWWWPK..", "....KWWWWWWWWWKWWKWKWWK...", "....KGWWWWWWWWWKKKKKKK....", ".....KKKKKKKKKKK..........", ".....KKWK...KWKK.........."),
        "crouch1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "...KK.....................", "..KOOK........K.......K...", ".KWK.........KPK.....KPK..", ".KWK.........KWOKKKKKWWK..", "..KWK........KWOOWWWWWWK..", "...KWK.......KEHWWWWWEHK..", "....KKKKKKKKKKEEWWWWWEEK..", ".....KWWWWWWWKPWWWNWWWPK..", ".....KWWWWWWWWKWWKWKWWK...", ".....KGWWWWWWWWKKKKKKK....", "......KKKKKKKKKKK.........", "......KKWK...KWKK........."),
        "stretch" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..KK......................", ".KOOK.....................", ".KWK......................", ".KWK......................", "..KWK.....................", "..KWK.....................", "...KWK.........K.......K..", "....KW........KPK.....KPK.", "....KKKK......KWOKKKKKWWK.", "....KWWWKK....KWOOWWWWWWK.", "....KWWWWWKKK.KWWWWWWWWWK.", "....KWWWWWWWWKKWKKWWWKKWK.", ".....KGWWWWWWWKPWWWNWWWPK.", "......KWK..KKKKKWWKWKWWK..", "......KWK...KWWWKKKKKKK...", "......KKK...KKKKKKK......."),
        "jump" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..............K.......K...", ".............KPK.....KPK..", ".............KWOKKKKKWWK..", "..KK.........KWEHWWWEHWK..", ".KOOK........KWEEWWWEEWK..", "KWK..........KWWWWWWWWWK..", "KWK...KKKKKKKKPWWWNWWWPK..", ".KWK.KWWWWWWWWKWWKWKWWK...", "..KWKWWWWWWWWWWKKKKKKK....", "...KKWWWWWWWWWWWK.........", "....KGWWWWWWWWWGK.........", ".....KGGWWWWWGGK..........", "......KKKKKKKKK...........", ".....KWK......KWK.........", "....KWK........KW.........", "....KK..........K.........", ".........................."),
        "fall" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..............K.......K...", ".KK..........KPK.....KPK..", ".KOK.........KWOKKKKKWWK..", "..KWK........KWOOWWWWWWK..", "..KWK........KEHWWWWWEHK..", "..KWK........KEEWWWWWEEK..", "..KWK.KKKKKKKKPWWWNWWWPK..", "...KWKWWWWWWWWKWWKWKWWK...", "....KWWWWWWWWWWKKKKKKK....", "....KWWWWWWWWWWWK.........", "....KGWWWWWWWWWGK.........", ".....KGGWWWWWGGK..........", "......KKKKKKKKK...........", ".....KWK......KWK.........", "....KWK........KW.........", "....KK..........K.........", ".........................."),
        "hang0" to arrayOf("........KK.......KK.......", ".......KWWK.....KWWK......", ".......KWK......KWK.......", ".......KWK......KWK.......", ".......KWKK.....KWK.......", "........KWOKKKKKWWK.......", "........KWEHWWWEHWK.......", "........KWEEWWWEEWK.......", "........KWWWWWWWWWK.......", "........KPWWWNWWWPK.......", ".........KWWKWKWWK........", "..........KKKKKKK.........", ".........KWWWWWWWK........", ".........KWWWWWWWK........", ".........KWWWWWWWK........", ".........KGWWWWWGK........", "..........KGWWWGKKWK......", "...........KKKKK.KWK......", "..........KWK.KWKWK.......", "..........KWK.KWKOK.......", "..........KKK.KKKKK.......", ".........................."),
        "hang1" to arrayOf("........KK.......KK.......", ".......KWWK.....KWWK......", ".......KWK......KWK.......", ".......KWK......KWK.......", ".......KWKK.....KWK.......", "........KWOKKKKKWWK.......", "........KWEHWWWEHWK.......", "........KWEEWWWEEWK.......", "........KWWWWWWWWWK.......", "........KPWWWNWWWPK.......", ".........KWWKWKWWK........", "..........KKKKKKK.........", ".........KWWWWWWWK........", ".........KWWWWWWWK........", ".........KWWWWWWWK........", ".........KGWWWWWGK........", "..........KGWWWGKKWK......", "...........KKKKK.KWK......", "..........KWK.KWKWK.......", "...........KWKWKKOK.......", "...........KK.KK.KK.......", ".........................."),
        "hang_happy" to arrayOf("........KK.......KK.......", ".......KWWK.....KWWK......", ".......KWK......KWK.......", ".......KWK......KWK.......", ".......KWKK.....KWK.......", "........KWOKKKKKWWK.......", "........KWOOWWWWWWK.......", "........KWKKWWWKKWK.......", "........KWWWWWWWWWK.......", "........KPWWWNWWWPK.......", ".........KWWKWKWWK........", "..........KKKKKKK.........", ".........KWWWWWWWK........", ".........KWWWWWWWK........", ".........KWWWWWWWK........", ".........KGWWWWWGK........", "..........KGWWWGKKWK......", "...........KKKKK.KWK......", "..........KWK.KWKWK.......", "..........KWK.KWKOK.......", "..........KKK.KKKKK.......", ".........................."),
        "held0" to arrayOf(".........K.......K........", "........KPK.....KPK.......", "........KWOKKKKKWWK.......", "........KWOOWWWWWWK.......", "........KEHWWWWWEHK.......", "........KEEWWWWWEEK.......", "........KPWWWNWWWPK.......", ".........KWWKWKWWK........", "..........KKKKKKK.........", ".........KKWWWWWKK........", ".........KKWWWWWKK........", ".........KWWWWWWWK........", ".........KGWWWWWGK........", "..........KGWWWGKKWK......", "...........KKKKK.KWK......", "..........KWK.KWKWK.......", "..........KWK.KWKOK.......", "..........KKK.KKKKK.......", "..........................", "..........................", "..........................", ".........................."),
        "held1" to arrayOf(".........K.......K........", "........KPK.....KPK.......", "........KWOKKKKKWWK.......", "........KWOOWWWWWWK.......", "........KEHWWWWWEHK.......", "........KEEWWWWWEEK.......", "........KPWWWNWWWPK.......", ".........KWWKWKWWK........", "..........KKKKKKK.........", ".........KKWWWWWKK........", ".........KKWWWWWKK........", ".........KWWWWWWWK........", ".........KGWWWWWGK........", "..........KGWWWGKKWK......", "...........KKKKK.KWK......", "..........KWK.KWKWK.......", "...........KWKWKKOK.......", "...........KK.KK.KK.......", "..........................", "..........................", "..........................", ".........................."),
    )

    val DOG: Map<String, Array<String>> = mapOf(
        "idle0" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", ".K..........KBBKTTTWTTTKBB", ".KK.........KBBKTEHWEHTKBB", ".KTK........KBBKTEEWEETKBB", "..KTK.......KBBKTWWNWWTKBB", "...KTKKKKKKKKKBKTWKWKWTKBK", "....KTTBBBBBTTKKKKWWWKKKK.", "...KTTBBBBBBBTTTKRRRRRK...", "...KTTTBBBBBTTTTTKRYRK....", "...KGTTTTTTTTTWWWK........", "....KGGWWWWWWWWWK.........", ".....KKKKKKKKKKK..........", ".....KTK.....KTK..........", ".....KTK.....KTK..........", "....KKWK....KKWK.........."),
        "idle1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", ".KK.........KBBKTTTWTTTKBB", ".KTK........KBBKTEHWEHTKBB", "..KTK.......KBBKTEEWEETKBB", "..KTK.......KBBKTWWNWWTKBB", "...KTKKKKKKKKKBKTWKWKWTKBK", "....KTTBBBBBTTKKKKWWWKKKK.", "...KTTBBBBBBBTTTKRRRRRK...", "...KTTTBBBBBTTTTTKRYRK....", "...KGTTTTTTTTTWWWK........", "....KGGWWWWWWWWWK.........", ".....KKKKKKKKKKK..........", ".....KTK.....KTK..........", ".....KTK.....KTK..........", "....KKWK....KKWK.........."),
        "idle_blink" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", ".K..........KBBKTTTWTTTKBB", ".KK.........KBBKTTTWTTTKBB", ".KTK........KBBKTKKWKKTKBB", "..KTK.......KBBKTWWNWWTKBB", "...KTKKKKKKKKKBKTWKWKWTKBK", "....KTTBBBBBTTKKKKWWWKKKK.", "...KTTBBBBBBBTTTKRRRRRK...", "...KTTTBBBBBTTTTTKRYRK....", "...KGTTTTTTTTTWWWK........", "....KGGWWWWWWWWWK.........", ".....KKKKKKKKKKK..........", ".....KTK.....KTK..........", ".....KTK.....KTK..........", "....KKWK....KKWK.........."),
        "idle_tongue" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", "...KK.......KBBKTTTWTTTKBB", "..KTK.......KBBKTEHWEHTKBB", "..KTK.......KBBKTEEWEETKBB", "..KTK.......KBBKTWWNWWTKBB", "...KTKKKKKKKKKBKTWKWKWTKBK", "....KTTBBBBBTTKKKKWPWKKKK.", "...KTTBBBBBBBTTTKRRRRRK...", "...KTTTBBBBBTTTTTKRYRK....", "...KGTTTTTTTTTWWWK........", "....KGGWWWWWWWWWK.........", ".....KKKKKKKKKKK..........", ".....KTK.....KTK..........", ".....KTK.....KTK..........", "....KKWK....KKWK.........."),
        "idle_up" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", ".K..........KBBKTEHWEHTKBB", ".KK.........KBBKTEEWEETKBB", ".KTK........KBBKTTTWTTTKBB", "..KTK.......KBBKTWWNWWTKBB", "...KTKKKKKKKKKBKTWKWKWTKBK", "....KTTBBBBBTTKKKKWWWKKKK.", "...KTTBBBBBBBTTTKRRRRRK...", "...KTTTBBBBBTTTTTKRYRK....", "...KGTTTTTTTTTWWWK........", "....KGGWWWWWWWWWK.........", ".....KKKKKKKKKKK..........", ".....KTK.....KTK..........", ".....KTK.....KTK..........", "....KKWK....KKWK.........."),
        "wag0" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", ".KK.........KBBKTTTWTTTKBB", ".KTK........KBBKTEHWEHTKBB", "..KTK.......KBBKTEEWEETKBB", "..KTK.......KBBKTWWNWWTKBB", "...KTKKKKKKKKKBKTWKWKWTKBK", "....KTTBBBBBTTKKKKWPWKKKK.", "...KTTBBBBBBBTTTKRRRRRK...", "...KTTTBBBBBTTTTTKRYRK....", "...KGTTTTTTTTTWWWK........", "....KGGWWWWWWWWWK.........", ".....KKKKKKKKKKK..........", ".....KTK.....KTK..........", ".....KTK.....KTK..........", "....KKWK....KKWK.........."),
        "wag1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", "...KK.......KBBKTTTWTTTKBB", "..KTK.......KBBKTEHWEHTKBB", "..KTK.......KBBKTEEWEETKBB", "..KTK.......KBBKTWWNWWTKBB", "...KTKKKKKKKKKBKTWKWKWTKBK", "....KTTBBBBBTTKKKKWPWKKKK.", "...KTTBBBBBBBTTTKRRRRRK...", "...KTTTBBBBBTTTTTKRYRK....", "...KGTTTTTTTTTWWWK........", "....KGGWWWWWWWWWK.........", ".....KKKKKKKKKKK..........", ".....KTK.....KTK..........", ".....KTK.....KTK..........", "....KKWK....KKWK.........."),
        "walk0" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", ".KK.........KBBKTTTWTTTKBB", ".KTK........KBBKTEHWEHTKBB", "..KTK.......KBBKTEEWEETKBB", "..KTK.......KBBKTWWNWWTKBB", "...KTKKKKKKKKKBKTWKWKWTKBK", "....KTTBBBBBTTKKKKWWWKKKK.", "...KTTBBBBBBBTTTKRRRRRK...", "...KTTTBBBBBTTTTTKRYRK....", "...KGTTTTTTTTTWWWK........", "....KGGWWWWWWWWWK.........", ".....KKKKKKKKKKK..........", ".....KTK......KTK.........", "....KTK........KTK........", "...KKW.........KWK........"),
        "walk1" to arrayOf("..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", ".K..........KBBKTTTWTTTKBB", ".KK.........KBBKTEHWEHTKBB", ".KTK........KBBKTEEWEETKBB", "..KTK.......KBBKTWWNWWTKBB", "...KTKKKKKKKKKBKTWKWKWTKBK", "....KTTBBBBBTTKKKKWWWKKKK.", "...KTTBBBBBBBTTTKRRRRRK...", "...KTTTBBBBBTTTTTKRYRK....", "...KGTTTTTTTTTWWWK........", "....KGGWWWWWWWWWK.........", ".....KKKKKKKKKKK..........", "......KTK...KTK...........", "......KTK...KTK...........", ".....KKW...KKW............", ".........................."),
        "walk2" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", "...KK.......KBBKTTTWTTTKBB", "..KTK.......KBBKTEHWEHTKBB", "..KTK.......KBBKTEEWEETKBB", "..KTK.......KBBKTWWNWWTKBB", "...KTKKKKKKKKKBKTWKWKWTKBK", "....KTTBBBBBTTKKKKWWWKKKK.", "...KTTBBBBBBBTTTKRRRRRK...", "...KTTTBBBBBTTTTTKRYRK....", "...KGTTTTTTTTTWWWK........", "....KGGWWWWWWWWWK.........", ".....KKKKKKKKKKK..........", ".....KTK.....KTK..........", "......KTK..KTK............", "......KKW.KKW............."),
        "walk3" to arrayOf("..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", ".K..........KBBKTTTWTTTKBB", ".KK.........KBBKTEHWEHTKBB", ".KTK........KBBKTEEWEETKBB", "..KTK.......KBBKTWWNWWTKBB", "...KTKKKKKKKKKBKTWKWKWTKBK", "....KTTBBBBBTTKKKKWWWKKKK.", "...KTTBBBBBBBTTTKRRRRRK...", "...KTTTBBBBBTTTTTKRYRK....", "...KGTTTTTTTTTWWWK........", "....KGGWWWWWWWWWK.........", ".....KKKKKKKKKKK..........", "......KTK...KTK...........", "......KTK...KTK...........", ".....KKW...KKW............", ".........................."),
        "run0" to arrayOf("..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", ".KK.........KBBKTTTWTTTKBB", ".KTK........KBBKTEHWEHTKBB", "..KTK.......KBBKTEEWEETKBB", "..KTK.......KBBKTWWNWWTKBB", "...KTKKKKKKKKKBKTWKWKWTKBK", "....KTTBBBBBTTKKKKWPWKKKK.", "...KTTBBBBBBBTTTKRRRRRK...", "...KTTTBBBBBTTTTTKRYRK....", "...KGTTTTTTTTTWWWK........", "....KGGWWWWWWWWWK.........", ".....KKKKKKKKKKK..........", "....KTK.......KTK.........", "...KTK.........KTK........", "...KW...........KW........", ".........................."),
        "run1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", "...KK.......KBBKTTTWTTTKBB", "..KTK.......KBBKTEHWEHTKBB", "..KTK.......KBBKTEEWEETKBB", "..KTK.......KBBKTWWNWWTKBB", "...KTKKKKKKKKKBKTWKWKWTKBK", "....KTTBBBBBTTKKKKWPWKKKK.", "...KTTBBBBBBBTTTKRRRRRK...", "...KTTTBBBBBTTTTTKRYRK....", "...KGTTTTTTTTTWWWK........", "....KGGWWWWWWWWWK.........", ".....KKKKKKKKKKK..........", ".......KTK.KTK............", ".......KTKKTK.............", ".......KWKKW.............."),
        "sit0" to arrayOf("..........................", "..........................", "..........................", "..........................", ".........KKKKKKK..........", "........KTTTWTTTK.........", "......KKTTTTWTTTTKK.......", ".....KBBKTTTWTTTKBBK......", ".....KBBKTEHWEHTKBBK......", ".....KBBKTEEWEETKBBK......", ".....KBBKTWWNWWTKBBK......", "......KBKTWKWKWTKBK.......", ".......KKKKWWWKKKK........", ".........KRRRRRK..........", "...KK...KTKRYRKTK.........", "...KTK.KTTTWWWTTTK........", "....KTKKTTWWWWWTTK........", "....KTKKBTWWWWWTBK........", ".....KTKBTTWWWTTBK........", ".......KBTKTTTKTBK........", ".......KBTKTKTKTBK........", "........KKWK.KWKK........."),
        "sit1" to arrayOf("..........................", "..........................", "..........................", "..........................", ".........KKKKKKK..........", "........KTTTWTTTK.........", "......KKTTTTWTTTTKK.......", ".....KBBKTTTWTTTKBBK......", ".....KBBKTEHWEHTKBBK......", ".....KBBKTEEWEETKBBK......", ".....KBBKTWWNWWTKBBK......", "......KBKTWKWKWTKBK.......", ".......KKKKWWWKKKK........", ".........KRRRRRK..........", ".....KK.KTKRYRKTK.........", "....KTKKTTTWWWTTTK........", "....KTKKTTWWWWWTTK........", "....KTKKBTWWWWWTBK........", ".....KTKBTTWWWTTBK........", ".......KBTKTTTKTBK........", ".......KBTKTKTKTBK........", "........KKWK.KWKK........."),
        "sit_blink" to arrayOf("..........................", "..........................", "..........................", "..........................", ".........KKKKKKK..........", "........KTTTWTTTK.........", "......KKTTTTWTTTTKK.......", ".....KBBKTTTWTTTKBBK......", ".....KBBKTTTWTTTKBBK......", ".....KBBKTKKWKKTKBBK......", ".....KBBKTWWNWWTKBBK......", "......KBKTWKWKWTKBK.......", ".......KKKKWWWKKKK........", ".........KRRRRRK..........", "...KK...KTKRYRKTK.........", "...KTK.KTTTWWWTTTK........", "....KTKKTTWWWWWTTK........", "....KTKKBTWWWWWTBK........", ".....KTKBTTWWWTTBK........", ".......KBTKTTTKTBK........", ".......KBTKTKTKTBK........", "........KKWK.KWKK........."),
        "sit_happy" to arrayOf("..........................", "..........................", "..........................", "..........................", ".........KKKKKKK..........", "........KTTTWTTTK.........", "......KKTTTTWTTTTKK.......", ".....KBBKTTTWTTTKBBK......", ".....KBBKTKKWKKTKBBK......", ".....KBBKTTTWTTTKBBK......", ".....KBBKTWWNWWTKBBK......", "......KBKTWKWKWTKBK.......", ".......KKKKPPPKKKK........", ".........KRRRRRK..........", ".....KK.KTKRYRKTK.........", "....KTKKTTTWWWTTTK........", "....KTKKTTWWWWWTTK........", "....KTKKBTWWWWWTBK........", ".....KTKBTTWWWTTBK........", ".......KBTKTTTKTBK........", ".......KBTKTKTKTBK........", "........KKWK.KWKK........."),
        "sit_tongue" to arrayOf("..........................", "..........................", "..........................", "..........................", ".........KKKKKKK..........", "........KTTTWTTTK.........", "......KKTTTTWTTTTKK.......", ".....KBBKTTTWTTTKBBK......", ".....KBBKTEHWEHTKBBK......", ".....KBBKTEEWEETKBBK......", ".....KBBKTWWNWWTKBBK......", "......KBKTWKWKWTKBK.......", ".......KKKKWPWKKKK........", ".........KRRRRRK..........", "...KK...KTKRYRKTK.........", "...KTK.KTTTWWWTTTK........", "....KTKKTTWWWWWTTK........", "....KTKKBTWWWWWTBK........", ".....KTKBTTWWWTTBK........", ".......KBTKTTTKTBK........", ".......KBTKTKTKTBK........", "........KKWK.KWKK........."),
        "sit_up" to arrayOf("..........................", "..........................", "..........................", "..........................", ".........KKKKKKK..........", "........KTTTWTTTK.........", "......KKTTTTWTTTTKK.......", ".....KBBKTEHWEHTKBBK......", ".....KBBKTEEWEETKBBK......", ".....KBBKTTTWTTTKBBK......", ".....KBBKTWWNWWTKBBK......", "......KBKTWKWKWTKBK.......", ".......KKKKWWWKKKK........", ".........KRRRRRK..........", "...KK...KTKRYRKTK.........", "...KTK.KTTTWWWTTTK........", "....KTKKTTWWWWWTTK........", "....KTKKBTWWWWWTBK........", ".....KTKBTTWWWTTBK........", ".......KBTKTTTKTBK........", ".......KBTKTKTKTBK........", "........KKWK.KWKK........."),
        "bark0" to arrayOf("..........................", "..........................", "..........................", "..........................", ".........KKKKKKK..........", "........KTTTWTTTK.........", "......KKTTTTWTTTTKK.......", ".....KBBKTTTWTTTKBBK......", ".....KBBKTEHWEHTKBBK......", ".....KBBKTEEWEETKBBK......", ".....KBBKTWWNWWTKBBK......", "......KBKTWKKKWTKBK.......", ".......KKKKKPKKKKK........", ".........KRRRRRK..........", "...KK...KTKRYRKTK.........", "...KTK.KTTTWWWTTTK........", "....KTKKTTWWWWWTTK........", "....KTKKBTWWWWWTBK........", ".....KTKBTTWWWTTBK........", ".......KBTKTTTKTBK........", ".......KBTKTKTKTBK........", "........KKWK.KWKK........."),
        "bark1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", ".K..........KBBKTTTWTTTKBB", ".KK.........KBBKTEHWEHTKBB", ".KTK........KBBKTEEWEETKBB", "..KTK.......KBBKTWWNWWTKBB", "...KTKKKKKKKKKBKTWKKKWTKBK", "....KTTBBBBBTTKKKKKPKKKKK.", "...KTTBBBBBBBTTTKRRRRRK...", "...KTTTBBBBBTTTTTKRYRK....", "...KGTTTTTTTTTWWWK........", "....KGGWWWWWWWWWK.........", ".....KKKKKKKKKKK..........", ".....KTK.....KTK..........", ".....KTK.....KTK..........", "....KKWK....KKWK.........."),
        "scratch0" to arrayOf("..........................", "..........................", "..........................", "..........................", ".........KKKKKKK..........", "........KTTTWTTTK.........", "......KKTTTTWTTTTKK.......", ".....KBBKTTTWTTTKBBK......", ".....KBBKTKKWKKTKBBK......", ".....KBBKTTTWTTTKBBK......", ".....KBBKTWWNWWKTKBK......", "......KBKTWKWKWKTKK.......", ".......KKKKPPPKKWK........", ".........KRRRRRK..........", "...KK...KTKRYRKTK.........", "...KTK.KTTTWWWTTTK........", "....KTKKTTWWWWWTTK........", "....KTKKBTWWWWWTBK........", ".....KTKBTTWWWTTBK........", ".......KBTKTTTKTBK........", ".......KBTKTKTKTBK........", "........KKWK.KWKK........."),
        "scratch1" to arrayOf("..........................", "..........................", "..........................", "..........................", ".........KKKKKKK..........", "........KTTTWTTTK.........", "......KKTTTTWTTTTKK.......", ".....KBBKTTTWTTTKBBK......", ".....KBBKTKKWKKTKBBK......", ".....KBBKTTTWTTTKBBK......", ".....KBBKTWWNWWTKBBK......", "......KBKTWKWKWKTKK.......", ".......KKKKPPPKKTK........", ".........KRRRRRKKW........", "...KK...KTKRYRKTK.........", "...KTK.KTTTWWWTTTK........", "....KTKKTTWWWWWTTK........", "....KTKKBTWWWWWTBK........", ".....KTKBTTWWWTTBK........", ".......KBTKTTTKTBK........", ".......KBTKTKTKTBK........", "........KKWK.KWKK........."),
        "lie0" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", "............KBBKTTTWTTTKBB", "............KBBKTTTWTTTKBB", "............KBBKTKKWKKTKBB", "............KBBKTWWNWWTKBB", "...KKKKKKKKKKKBKTWKWKWTKBK", "..KKTTBBBBBBTTKKKKWWWKKKK.", ".KKTTBBBBBBBBTTTTTKKK.....", "KTKGTTTTTTTTTTTKTTK.KTTK..", "...KKKKKKKKKKKKKWWK.KWWK.."),
        "lie1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", "............KBBKTTTWTTTKBB", "............KBBKTTTWTTTKBB", "............KBBKTKKWKKTKBB", "...KKKKKKKKKKBBKTWWNWWTKBB", "..KKTTBBBBBBTKBKTWKWKWTKBK", ".KKTTBBBBBBBBTKKKKWWWKKKK.", "KTKGTTTTTTTTTTTTWWKKK.....", "...KKKKKKKKKKKKKTTK.KTTK..", "...............KWWK.KWWK.."),
        "lie_awake" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", "............KBBKTTTWTTTKBB", "............KBBKTEHWEHTKBB", "............KBBKTEEWEETKBB", "............KBBKTWWNWWTKBB", "...KKKKKKKKKKKBKTWKWKWTKBK", "..KKTTBBBBBBTTKKKKWWWKKKK.", ".KKTTBBBBBBBBTTTTTKKK.....", "KTKGTTTTTTTTTTTKTTK.KTTK..", "...KKKKKKKKKKKKKWWK.KWWK.."),
        "belly0" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", "............KBBKTTTWTTTKBB", "....KTK..KTKKBBKTKKWKKTKBB", "....KTK..KTKKBBKTTTWTTTKBB", "...KKKKKKKKKKBBKTWWNWWTKBB", "..KTWWWWWWWWWKBKTWKWKWTKBK", ".KTKTTTTTTTTTTKKKKPPPKKKK.", "KTKKKKKKKKKKKKKKK.KKK....."),
        "belly1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", "............KBBKTTTWTTTKBB", "....KKTK.KKTKBBKTKKWKKTKBB", "....KTK..KTKKBBKTTTWTTTKBB", "...KKKKKKKKKKBBKTWWNWWTKBB", "..KTWWWWWWWWWKBKTWKWKWTKBK", ".KTKTTTTTTTTTTKKKKPPPKKKK.", "KTKKKKKKKKKKKKKKK.KKK....."),
        "dig0" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", ".K........................", ".KK.......................", ".KTK......................", "..KTK.....................", "...KT.....................", ".....KKKKKKKKKKK..........", "....KTTBBBBBTTTTK.........", "...KTTBBBBBBBTTTTK........", "...KTTTBBBBBTTTTTK........", "...KGTTTTTTTTTWWKKKKK.....", "....KGGWWWWWWWWKTTTTTKK...", ".....KKKKKKKKKKBBTTEHTTK..", ".....KTK.....KKBBTTEETTTK.", "......KTK..KTKKBBKTTTTTTTK", "......KKW.KKW..KBKTTTWWWWN", "................KK.KKKKKKK"),
        "dig1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", ".K........................", ".KK.......................", ".KTK......................", "..KTK.....................", "...KT.....................", ".....KKKKKKKKKKK..........", "....KTTBBBBBTTTTK.........", "...KTTBBBBBBBTTTTK........", "...KTTTBBBBBTTTTTK........", "...KGTTTTTTTTTWWKKKKK.....", "....KGGWWWWWWWWKTTTTTKK...", ".....KKKKKKKKKKBBTTEHTTK..", ".....KTK......KBBTTEETTTK.", "....KTK.......KBBKTTTTTTTK", "...KKW.........KBKTTTWWWWN", "................KK.KKKKKKK"),
        "sniff0" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "...KK.....................", "..KTK.....................", "..KTK.....................", "..KTK.....................", "...KTKKKKKKKKKKK..........", "....KTTBBBBBTTTTK.........", "...KTTBBBBBBBTTTTK........", "...KTTTBBBBBTTTTKKKKK.....", "...KGTTTTTTTTTWKTTTTTKK...", "....KGGWWWWWWWKBBTTEHTTK..", ".....KKKKKKKKKKBBTTEETTTK.", ".....KTK.....KKBBKTTTTTTTK", ".....KTK.....KTKBKTTTWWWWN", "....KKWK....KKWKKK.KKKKKKK"),
        "sniff1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..........................", ".KK.......................", ".KTK......................", "..KTK.....................", "..KTK.....................", "...KTKKKKKKKKKKK..........", "....KTTBBBBBTTTTK.........", "...KTTBBBBBBBTTTTK........", "...KTTTBBBBBTTTTTK........", "...KGTTTTTTTTTWWKKKKK.....", "....KGGWWWWWWWWKTTTTTKK...", ".....KKKKKKKKKKBBTTEHTTK..", ".....KTK.....KKBBTTEETTTK.", ".....KTK.....KKBBKTTTTTTTK", "....KKWK....KKWKBKTTTWWWWN"),
        "bow0" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "..KK......................", ".KTK......................", ".KTK......................", ".KTK......................", "..KT......................", ".....KKKKKKKKKKKKKKKKKK...", "....KTTBBBBBTTTKTTTWTTTK..", "...KTTBBBBBBBKKTTTTWTTTTKK", "...KTTTBBBBBKBBKTTTWTTTKBB", "...KGTTTTTTTKBBKTEHWEHTKBB", "....KGGWWWWWKBBKTEEWEETKBB", ".....KKKKKKKKBBKTWWNWWTKBB", ".....KTK.....KBKTWKWKWTKBK", ".....KTK.....KKKKKWPWKKKK.", "....KKWK....KKWK..KPK.....", ".........................."),
        "bow1" to arrayOf("..........................", "..........................", "..........................", "..........................", "..........................", "..........................", "KK........................", "KTK.......................", ".KTK......................", ".KTK......................", "..KT......................", ".....KKKKKKKKKKKKKKKKKK...", "....KTTBBBBBTTTKTTTWTTTK..", "...KTTBBBBBBBKKTTTTWTTTTKK", "...KTTTBBBBBKBBKTTTWTTTKBB", "...KGTTTTTTTKBBKTEHWEHTKBB", "....KGGWWWWWKBBKTEEWEETKBB", ".....KKKKKKKKBBKTWWNWWTKBB", ".....KTK.....KBKTWKWKWTKBK", ".....KTK.....KKKKKWPWKKKK.", "....KKWK....KKWK..KPK.....", ".........................."),
        "jump" to arrayOf("..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", ".K..........KBBKTEHWEHTKBB", ".KK.........KBBKTEEWEETKBB", ".KTK........KBBKTTTWTTTKBB", "..KTK.......KBBKTWWNWWTKBB", "...KTKKKKKKKKKBKTWKWKWTKBK", "....KTTBBBBBTTKKKKWWWKKKK.", "...KTTBBBBBBBTTTKRRRRRK...", "...KTTTBBBBBTTTTTKRYRK....", "...KGTTTTTTTTTWWWK........", "....KGGWWWWWWWWWK.........", ".....KKKKKKKKKKK..........", "....KTK.......KTK.........", "...KTK.........KTK........", "...KW...........KW........", "..........................", ".........................."),
        "fall" to arrayOf("..........................", "..........................", "..........................", "................KKKKKKK...", "...............KTTTWTTTK..", ".............KKTTTTWTTTTKK", ".K..........KBBKTEHWEHTKBB", ".KK.........KBBKTEEWEETKBB", ".KTK........KBBKTTTWTTTKBB", "..KTK.......KBBKTWWNWWTKBB", "...KTKKKKKKKKKBKTWKWKWTKBK", "....KTTBBBBBTTKKKKWWWKKKK.", "...KTTBBBBBBBTTTKRRRRRK...", "...KTTTBBBBBTTTTTKRYRK....", "...KGTTTTTTTTTWWWK........", "....KGGWWWWWWWWWK.........", ".....KKKKKKKKKKK..........", "....KTK.......KTK.........", "...KTK.........KTK........", "...KW...........KW........", "..........................", ".........................."),
        "held0" to arrayOf("..........KKKKKKK.........", ".........KTTTWTTTK........", ".......KKTTTTWTTTTKK......", "......KBBKTEHWEHTKBBK.....", "......KBBKTEEWEETKBBK.....", "......KBBKTTTWTTTKBBK.....", "......KBBKTWWNWWTKBBK.....", ".......KBKTWKWKWTKBK......", "........KKKKWWWKKKK.......", ".........KKRRRRRK.........", "........KTTKRYRKTK........", "........KTBBBBBBTK........", "........KTTWWWWTTK........", "........KTTWWWWTTK........", ".........KTTTTTTK..K......", "..........KKKKKK..KT......", ".........KTK.KTK.KTK......", ".........KTK.KTKKTK.......", ".........KWK.KWK..........", "..........................", "..........................", ".........................."),
        "held1" to arrayOf("..........KKKKKKK.........", ".........KTTTWTTTK........", ".......KKTTTTWTTTTKK......", "......KBBKTTTWTTTKBBK.....", "......KBBKTTTWTTTKBBK.....", "......KBBKTKKWKKTKBBK.....", "......KBBKTWWNWWTKBBK.....", ".......KBKTWKWKWTKBK......", "........KKKKWWWKKKK.......", ".........KKRRRRRK.........", "........KTTKRYRKTK........", "........KTBBBBBBTK........", "........KTTWWWWTTK........", "........KTTWWWWTTK........", ".........KTTTTTTK..K......", "..........KKKKKK..KT......", ".........KTK.KTK.KTK......", "..........KTKTK.KTK.......", "..........KW.WK...........", "..........................", "..........................", ".........................."),
    )
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/shell/Shell.kt`

```kotlin
package com.student.amizoneplus.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.student.amizoneplus.ui.kit.CircleButton
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

enum class Route { Today, Schedule, Subjects, Lms, More }

/** The pull-down quick menu, reachable from every screen. */
class MenuController(val isOpen: Boolean, val open: () -> Unit, val close: () -> Unit) {
    fun toggle() = if (isOpen) close() else open()
}

val LocalMenu = compositionLocalOf { MenuController(false, {}, {}) }

fun Modifier.swipeDownToOpen(onOpen: () -> Unit): Modifier = pointerInput(Unit) {
    var total = 0f
    detectVerticalDragGestures(onDragStart = { total = 0f }, onDragEnd = { if (total > 36.dp.toPx()) onOpen() }) { _, dy -> total += dy }
}

/* ───────────── Small header pieces ───────────── */

/** Menu toggle shown in headers. */
@Composable
fun MenuButton(bg: Color = P.cream, fg: Color = P.onCream) {
    val menu = LocalMenu.current
    CircleButton(if (menu.isOpen) Icons.Outlined.Close else Icons.Outlined.Apps, { menu.toggle() }, 48.dp, bg, fg, Feel.Select)
}

/** Bar used by the menu overlay & sheets: wordmark + close/menu. */
@Composable
fun HeaderBar(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(L.header), verticalAlignment = Alignment.CenterVertically) {
        T("AMIZONE+", Type.headline, modifier = Modifier.weight(1f))
        MenuButton()
    }
}

@Composable
fun HeaderInk(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth()) { HeaderBar() }

@Composable
fun ErrorBanner(text: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().press(onClick).background(P.badSoft, RoundedCornerShape(22.dp)).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = P.bad)
        T("$text  $action", Type.caption, modifier = Modifier.weight(1f))
    }
}

/* ───────────── Pages ───────────── */

private val navClearance: Dp @Composable get() =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + L.nav + 36.dp

/**
 * Hero + sheet (reference "Operational Timing" / "Workers"): a colour hero at the
 * top; the content sheet with large rounded corners glides up over it on scroll
 * while the hero eases back and fades.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroPage(
    heroColor: Color,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    banner: (@Composable () -> Unit)? = null,
    scroll: ScrollState = rememberScrollState(),
    hero: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptics = LocalHaptics.current
    val d = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenH = LocalConfiguration.current.screenHeightDp.dp
    var heroH by remember { mutableFloatStateOf(0f) }
    val overlap = 28.dp

    Box(Modifier.fillMaxSize().background(P.bg)) {
        StretchRefresh(refreshing, onRefresh, Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                // Hero: scrolls at ~1/3 speed (parallax) and eases back; being part of the
                // scroll column, its buttons receive taps.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { heroH = it.height.toFloat() }
                        .graphicsLayer {
                            val k = if (heroH > 0) (scroll.value / heroH).coerceIn(0f, 1f) else 0f
                            translationY = scroll.value * 0.65f
                            alpha = 1f - k * 0.9f
                            val s = 1f - 0.04f * k
                            scaleX = s; scaleY = s
                        }
                        .background(heroColor)
                        .padding(top = statusTop + 8.dp, start = L.page, end = L.page, bottom = overlap + 16.dp),
                    content = hero,
                )
                // Sheet: overlaps the hero's bottom edge; its corners soften as it reaches the top.
                val k = if (heroH > 0) (scroll.value / heroH).coerceIn(0f, 1f) else 0f
                Column(
                    Modifier
                        .layout { m, c ->
                            val p = m.measure(c)
                            val o = overlap.roundToPx()
                            layout(p.width, p.height - o) { p.place(0, -o) }
                        }
                        .pointerInput(Unit) {} // the sheet owns touches over it (none leak to the hero beneath)
                        .fillMaxWidth()
                        .heightIn(min = screenH)
                        .background(P.sheet, RoundedCornerShape(topStart = L.sheetRadius * (1 - 0.4f * k), topEnd = L.sheetRadius * (1 - 0.4f * k)))
                        .padding(start = L.page, end = L.page, top = 18.dp, bottom = navClearance),
                    verticalArrangement = Arrangement.spacedBy(L.block),
                ) {
                    banner?.invoke()
                    content()
                }
            }
        }
    }
}

/** Plain page on the base colour (reference home). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainPage(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    banner: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptics = LocalHaptics.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    StretchRefresh(refreshing, onRefresh, Modifier.fillMaxSize().background(P.bg)) {
        Column(
            // Opaque page colour: when pulled, its top rounds off like the hero pages.
            Modifier.fillMaxSize().background(P.bg).verticalScroll(rememberScrollState())
                .padding(top = statusTop + 10.dp, start = L.page, end = L.page, bottom = navClearance),
            verticalArrangement = Arrangement.spacedBy(L.block),
        ) {
            banner?.invoke()
            content()
        }
    }
}

/** Old collapsing page, now a neutral [HeroPage] — kept for screens not yet restyled. */
@Composable
fun CollapsingPage(
    title: @Composable () -> Unit,
    extra: (@Composable () -> Unit)? = null,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    banner: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = HeroPage(P.card, refreshing, onRefresh, banner, hero = {
    Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.weight(1f)) { title() }; MenuButton() }
    extra?.invoke()
}, content = content)

/* ───────────── Bottom nav: dark icon pill, sliding sky tile ───────────── */

private val TABS = listOf(
    Route.Today to Icons.Outlined.Home,
    Route.Schedule to Icons.Outlined.Schedule,
    Route.Subjects to Icons.Outlined.AutoStories,
    Route.Lms to Icons.Outlined.FolderOpen,
    Route.More to Icons.Outlined.Settings,
)

@Composable
fun BottomNav(route: Route, onChange: (Route) -> Unit, hazeState: HazeState, modifier: Modifier = Modifier, blurAlpha: () -> Float = { 1f }) {
    val haptics = LocalHaptics.current
    val pets = com.student.amizoneplus.ui.pets.LocalPetWorld.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val screenW = LocalConfiguration.current.screenWidthDp.dp
    // One gap all round: the same distance from both sides and from the bottom edge
    // (never less than the gesture bar needs).
    val gap = navBottom.coerceIn(16.dp, 24.dp)
    val pillW = minOf(screenW - gap * 2, 520.dp)
    val pad = 6.dp
    val slot = (pillW - pad * 2) / TABS.size
    val idx = TABS.indexOfFirst { it.first == route }.coerceAtLeast(0)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val slotPx = with(density) { slot.toPx() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // Tile position (px). Follows the finger while dragging, springs to the tab otherwise.
    val tile = remember { androidx.compose.animation.core.Animatable(slotPx * idx) }
    var dragging by remember { androidx.compose.runtime.mutableStateOf(false) }
    var dragX by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    LaunchedEffect(idx, slotPx) { if (!dragging) tile.animateTo(slotPx * idx, androidx.compose.animation.core.spring(0.78f, 420f)) }
    val idxNow by androidx.compose.runtime.rememberUpdatedState(idx)
    val onChangeNow by androidx.compose.runtime.rememberUpdatedState(onChange)
    // Grows while held, but never past the screen edges; bounces back on release.
    val maxGrow = ((screenW - 8.dp) / pillW).coerceIn(1f, 1.07f)
    val grow by androidx.compose.animation.core.animateFloatAsState(
        if (dragging) maxGrow else 1f,
        if (dragging) androidx.compose.animation.core.spring(0.7f, 500f) else androidx.compose.animation.core.spring(0.42f, 380f),
        label = "navgrow",
    )
    val shown = if (dragging) (dragX / slotPx).roundToInt().coerceIn(0, TABS.lastIndex) else idx

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        // Progressive blur: strongest at the bottom, fading to nothing above the bar.
        Box(
            Modifier.fillMaxWidth().height(L.nav + navBottom + 64.dp)
                .graphicsLayer { alpha = blurAlpha() } // hidden while the quick menu blurs everything
                .hazeBlur(
                    HazeInput.Backdrop(hazeState),
                    HazeBlurStyle {
                        blurRadius(20.dp)
                        colorEffects(listOf(HazeColorEffect.tint(P.bg.copy(alpha = 0.35f))))
                        progressive(HazeProgressive.verticalGradient(startIntensity = 0f, endIntensity = 1f))
                    },
                )
                .background(Brush.verticalGradient(listOf(Color.Transparent, P.bg.copy(alpha = 0.5f)))),
        )
        Box(
            Modifier.padding(bottom = maxOf(gap, navBottom)).size(pillW, L.nav)
                .onGloballyPositioned { pets?.navTop = it.boundsInWindow().top }
                .graphicsLayer {
                    scaleX = grow; scaleY = 1f + (grow - 1f) * 1.4f
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f) // grows upward, never off the bottom
                }
                .pointerInput(slotPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true; dragX = slotPx * idxNow; haptics.press() },
                        onDragEnd = { dragging = false; scope.launch { tile.snapTo(dragX); tile.animateTo(slotPx * idxNow, androidx.compose.animation.core.spring(0.6f, 380f)) } },
                        onDragCancel = { dragging = false; scope.launch { tile.animateTo(slotPx * idxNow) } },
                    ) { ch, dx ->
                        ch.consume()
                        dragX = (dragX + dx).coerceIn(0f, slotPx * TABS.lastIndex)
                        val t = (dragX / slotPx).roundToInt().coerceIn(0, TABS.lastIndex)
                        if (t != idxNow) { haptics.tick(); onChangeNow(TABS[t].first) }
                    }
                }
                .background(P.nav.copy(alpha = 0.92f), RoundedCornerShape(L.nav / 2)).padding(pad),
        ) {
            // The tile slides between tabs.
            Box(Modifier.offset { androidx.compose.ui.unit.IntOffset(((if (dragging) dragX else tile.value) + 3.dp.toPx()).roundToInt(), 0) }
                .size(slot - 6.dp, L.nav - pad * 2).background(P.sky, RoundedCornerShape((L.nav - pad * 2) / 2)))
            Row(Modifier.fillMaxSize()) {
                TABS.forEachIndexed { i, (r, icon) ->
                    val tint by animateColorAsState(if (i == shown) P.onHero else Color(0xFFDCDCE0), label = "navtint")
                    Box(
                        Modifier.width(slot).fillMaxSize()
                            .onGloballyPositioned { c -> pets?.navTabs?.set(r, c.boundsInWindow()) }
                            .press({ if (r != route) { haptics.select(); onChange(r) } }, Feel.None, 0.9f),
                        contentAlignment = Alignment.Center,
                    ) { NavIcon(icon, tint) }
                }
            }
        }
    }
}

@Composable
private fun NavIcon(icon: ImageVector, tint: Color) = Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp))

/** A soft progressive blur under the status bar: strongest at the very top, gone below it. */
@Composable
fun StatusBlur(hazeState: HazeState, blurAlpha: () -> Float = { 1f }) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        Modifier.fillMaxWidth().height((top + 28.dp) * 0.7f)
            .graphicsLayer { alpha = blurAlpha() }
            .hazeBlur(
                HazeInput.Backdrop(hazeState),
                HazeBlurStyle {
                    blurRadius(14.dp)
                    colorEffects(listOf(HazeColorEffect.tint(P.bg.copy(alpha = 0.18f))))
                    progressive(HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f))
                },
            ),
    )
}

/** Kept so the old root compiles until it's replaced. */
@Composable
fun PillNav(route: Route, onChange: (Route) -> Unit, hazeState: HazeState, modifier: Modifier = Modifier) = BottomNav(route, onChange, hazeState, modifier)
```

---

## `app/src/main/java/com/student/amizoneplus/ui/shell/Refresh.kt`

```kotlin
package com.student.amizoneplus.ui.shell

import android.graphics.ColorMatrixColorFilter
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/*
 * Pull to refresh, after the reference reel's coral loader: the page stretches like
 * rubber as you pull, a coral panel opens behind it, and one cream word sits in the
 * middle. Each letter lives in its own clipped slot and rolls down to the next word,
 * staggered left to right; one sentence plays out word by word. The phone's motor
 * builds tension tick by tick until a firm thunk says "let go".
 */

// One sentence, a word per frame (like "DOE MEE → ONTMOET → GROEI"), then a few extras.
private val LOADING_WORDS = listOf("FETCHING", "FROM", "SHITTY", "AMIZONE", "HANG ON", "BRIBING", "SERVERS", "ALMOST")
private const val ROLL_MS = 420
private const val LETTER_STAGGER_MS = 34
private const val HOLD_MS = 560

@Composable
fun StretchRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val haptics = LocalHaptics.current
    val d = LocalDensity.current
    val scope = rememberCoroutineScope()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val threshold = with(d) { (statusTop + 78.dp).toPx() }
    val maxPull = threshold * 1.75f
    val hold = with(d) { (statusTop + 88.dp).toPx() }
    val stepFar = with(d) { 26.dp.toPx() }
    val stepNear = with(d) { 7.dp.toPx() }

    var pull by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var mine by remember { mutableStateOf(false) } // this refresh came from a pull
    var armed by remember { mutableStateOf(false) }
    var lastTick by remember { mutableFloatStateOf(0f) }
    var anim by remember { mutableStateOf<Job?>(null) }
    val refreshingNow by rememberUpdatedState(refreshing)
    val onRefreshNow by rememberUpdatedState(onRefresh)

    fun settleTo(target: Float, bouncy: Boolean = false) {
        anim?.cancel()
        anim = scope.launch {
            animate(pull, target, animationSpec = spring(dampingRatio = if (bouncy) 0.55f else 0.85f, stiffness = if (bouncy) 240f else 320f)) { v, _ -> pull = v }
        }
    }

    fun feel(p: Float) {
        val prog = (p / threshold).coerceIn(0f, 1f)
        if (!armed && p >= threshold) { armed = true; haptics.armed(); return }
        if (armed && p < threshold * 0.88f) { armed = false; haptics.disarm(); lastTick = p; return }
        if (armed) return
        val step = stepFar + (stepNear - stepFar) * prog
        if (abs(p - lastTick) >= step) { lastTick = p; haptics.tension(prog) }
    }

    val conn = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Scrolling back up while pulled: shrink the pull first.
                if (source != NestedScrollSource.UserInput || available.y >= 0f || pull <= 0f || (refreshingNow && mine)) return Offset.Zero
                anim?.cancel()
                val next = (pull + available.y).coerceAtLeast(0f)
                val used = next - pull
                pull = next; dragging = true; feel(next)
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Dragging down at the very top: the leftover becomes stretch.
                if (source != NestedScrollSource.UserInput || available.y <= 0f || (refreshingNow && mine)) return Offset.Zero
                anim?.cancel()
                val resist = (1f - pull / maxPull).coerceIn(0f, 1f)
                pull = (pull + available.y * 0.8f * resist).coerceAtMost(maxPull)
                dragging = true; feel(pull)
                return available
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!dragging) return Velocity.Zero
                dragging = false
                if (pull >= threshold && !refreshingNow) {
                    mine = true
                    haptics.launch()
                    onRefreshNow()
                    settleTo(hold)
                    scope.launch { // if the refresh never got going, don't hang around
                        delay(1500)
                        if (!refreshingNow && mine) { mine = false; armed = false; settleTo(0f, bouncy = true) }
                    }
                } else if (!(refreshingNow && mine)) {
                    armed = false
                    settleTo(0f, bouncy = true)
                }
                return available // the pull swallows the fling
            }
        }
    }

    LaunchedEffect(refreshing) {
        if (!refreshing && mine) {
            delay(650) // let "DONE" roll in first
            mine = false; armed = false
            haptics.success()
            settleTo(0f, bouncy = true)
        }
    }

    Box(modifier.fillMaxSize().nestedScroll(conn)) {
        if (pull > 0.5f) RefreshPanel(pull, pull / threshold, mine && refreshing, mine && !refreshing, statusTop)
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                translationY = pull
                val st = (pull / threshold).coerceIn(0f, 1.6f)
                scaleY = 1f + 0.055f * st // rubber stretch
                // No sideways squeeze: the page stays as wide as the coral beneath it.
                transformOrigin = TransformOrigin(0.5f, 0f)
                val r = 30.dp.toPx() * min(1f, st * 2.5f)
                shape = RoundedCornerShape(topStart = r, topEnd = r)
                clip = pull > 0.5f
            },
        ) { content() } // transparent, so the coral runs right down to the first row
    }
}

/** The coral panel revealed behind the page, with the rolling word in the middle. */
@Composable
private fun RefreshPanel(heightPx: Float, progress: Float, loading: Boolean, done: Boolean, statusTop: androidx.compose.ui.unit.Dp) {
    val d = LocalDensity.current
    var step by remember { mutableStateOf(0) }
    LaunchedEffect(loading) {
        step = 0
        if (loading) while (true) {
            delay((ROLL_MS + HOLD_MS).toLong())
            step = (step + 1) % LOADING_WORDS.size
        }
    }
    val word = when {
        done -> "DONE!"
        loading -> LOADING_WORDS[step]
        progress >= 1f -> "LET GO"
        else -> "PULL"
    }
    // The panel also fills the page's own top margin (status bar + gap) under the moved page.
    // Only as far as the page has actually moved, so no coral flashes under the status bar
    // as the page springs back; and the panel fades out over the last stretch.
    // Every page is opaque with rounded top corners, so the coral only fills the gap.
    val total = with(d) { heightPx.toDp() }
    Box(
        // A corner's depth more, hidden behind the page except where its corners round off.
        Modifier.fillMaxWidth().height(total + 32.dp)
            .graphicsLayer { alpha = (progress * 3f).coerceIn(0f, 1f) }
            .background(P.coral),
    ) {
        // The word sits in the middle of all the visible coral, below the status bar.
        Box(Modifier.fillMaxWidth().height(total).padding(top = statusTop), contentAlignment = Alignment.Center) {
            RollingWord(word, Modifier.graphicsLayer { alpha = min(1f, progress * 2f) })
        }
    }
}

private val CREAM = Color(0xFFF3E1D8)

/**
 * A word whose letters each roll down inside their own clipped slot to the next word,
 * staggered left to right. Slots resize to the new letters, so the word stays centred.
 * Letters 2 and 3 are drawn as outlines (the reference's hollow "OE").
 */
@Composable
private fun RollingWord(word: String, modifier: Modifier = Modifier) {
    // Every change re-rolls every slot, even where the letter happens to be the same.
    var gen by remember { mutableStateOf(0) }
    var shown by remember { mutableStateOf(word) }
    if (word != shown) { shown = word; gen++ }
    val slots = 10
    val style = Type.display.copy(fontSize = 44.sp, lineHeight = 44.sp)
    val stroke = Stroke(width = with(LocalDensity.current) { 1.4.dp.toPx() })
    androidx.compose.foundation.layout.Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until slots) {
            val ch = shown.getOrNull(i)?.toString() ?: ""
            val spec = tween<androidx.compose.ui.unit.IntOffset>(ROLL_MS, delayMillis = i * LETTER_STAGGER_MS, easing = com.student.amizoneplus.ui.theme.Motion.emphasized)
            androidx.compose.animation.AnimatedContent(
                targetState = gen to ch,
                transitionSpec = {
                    (androidx.compose.animation.slideInVertically(spec) { -it }) togetherWith
                        (androidx.compose.animation.slideOutVertically(spec) { it }) using
                        androidx.compose.animation.SizeTransform(clip = true) { _, _ ->
                            tween(ROLL_MS, delayMillis = i * LETTER_STAGGER_MS, easing = com.student.amizoneplus.ui.theme.Motion.emphasized)
                        }
                },
                contentAlignment = Alignment.Center,
                label = "slot$i",
            ) { (_, c) ->
                if (c.isNotEmpty()) Text(
                    c,
                    style = if (i == 1 || i == 2) style.copy(drawStyle = stroke) else style,
                    color = CREAM,
                    maxLines = 1,
                )
            }
        }
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/AppViewModel.kt`

```kotlin
package com.student.amizoneplus.ui

import android.app.Application
import android.webkit.CookieManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.student.amizoneplus.data.AmizoneData
import com.student.amizoneplus.data.AmizoneException
import com.student.amizoneplus.data.Credentials
import com.student.amizoneplus.data.Repository
import com.student.amizoneplus.data.Settings
import com.student.amizoneplus.logic.Badge
import com.student.amizoneplus.logic.earnedBadges
import com.student.amizoneplus.work.Notifier
import com.student.amizoneplus.work.RefreshWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Status { Booting, SignedOut, SignedIn }

data class UiState(
    val status: Status = Status.Booting,
    val data: AmizoneData? = null,
    val refreshing: Boolean = false,
    val error: String? = null,
    val needsWebLogin: Boolean = false,
    val webLoginOpen: Boolean = false,
    val settings: Settings = Settings(),
    val username: String? = null,
    val celebrate: Badge? = null,
)

/** One-off feedback the UI turns into haptics. */
enum class Feedback { Success, Warning, Error }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val repo = Repository.get(app)
    private val storage = repo.storage

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val _feedback = Channel<Feedback>(Channel.BUFFERED)
    val feedback = _feedback.receiveAsFlow()

    private var refreshJob: Job? = null
    private var pendingCreds: Credentials? = null

    init {
        val settings = storage.loadSettings()
        val creds = storage.loadCredentials()
        if (creds == null) {
            _state.value = UiState(status = Status.SignedOut, settings = settings)
        } else {
            _state.value = UiState(status = Status.SignedIn, data = storage.loadCache(), settings = settings, username = creds.username)
            refresh()
            RefreshWorker.schedule(app)
            com.student.amizoneplus.work.KeepAliveWorker.schedule(app)
        }
    }

    fun refresh(user: Boolean = false) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            try {
                val fresh = repo.refreshAll()
                _state.update {
                    it.copy(data = fresh, refreshing = false, needsWebLogin = false,
                        error = if (fresh.errors.isNotEmpty()) "Couldn't load: ${fresh.errors.joinToString()}" else null)
                }
                if (user) _feedback.send(if (fresh.errors.isEmpty()) Feedback.Success else Feedback.Warning)
                checkBadges(fresh)
            } catch (e: AmizoneException.Network) {
                _state.update { it.copy(refreshing = false, error = e.message) }
                if (user) _feedback.send(Feedback.Error)
            } catch (e: AmizoneException) {
                _state.update { it.copy(refreshing = false, error = e.message, needsWebLogin = true) }
                if (user) _feedback.send(Feedback.Error)
            } catch (e: Exception) {
                _state.update { it.copy(refreshing = false, error = e.message ?: "Something went wrong.") }
                if (user) _feedback.send(Feedback.Error)
            }
        }
    }

    /** Throws a user-facing message on failure; opens the website login if Amizone wants it. */
    suspend fun signIn(c: Credentials) {
        try {
            repo.client.login(c)
        } catch (e: AmizoneException.NeedsWebLogin) {
            pendingCreds = c
            _state.update { it.copy(error = e.message, webLoginOpen = true) }
            return
        }
        enterApp(c)
    }

    private fun enterApp(c: Credentials) {
        storage.saveCredentials(c)
        _state.update { it.copy(status = Status.SignedIn, username = c.username, error = null) }
        RefreshWorker.schedule(getApplication())
        com.student.amizoneplus.work.KeepAliveWorker.schedule(getApplication())
        refresh()
    }

    fun openWebLogin(c: Credentials? = null) {
        pendingCreds = c ?: storage.loadCredentials()
        _state.update { it.copy(webLoginOpen = true) }
    }

    fun webLoginFinished() {
        com.student.amizoneplus.data.Http.persistSession()
        val c = pendingCreds
        _state.update { it.copy(webLoginOpen = false, needsWebLogin = false, error = null) }
        if (_state.value.status != Status.SignedIn && c != null) enterApp(c) else refresh()
    }

    fun closeWebLogin() = _state.update { it.copy(webLoginOpen = false) }

    /** Used by in-app web pages when their session has expired. */
    suspend fun relogin(): Boolean = runCatching { repo.client.login() }.isSuccess

    fun pendingCredentials() = pendingCreds

    fun signOut() {
        RefreshWorker.cancel(getApplication())
        com.student.amizoneplus.work.KeepAliveWorker.cancel(getApplication())
        storage.clearAll()
        CookieManager.getInstance().removeAllCookies(null)
        _state.value = UiState(status = Status.SignedOut)
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        val next = transform(_state.value.settings)
        storage.saveSettings(next)
        _state.update { it.copy(settings = next) }
        _state.value.data?.let { d -> viewModelScope.launch { Notifier.process(getApplication(), d, next, storage) } }
    }

    private fun checkBadges(d: AmizoneData) {
        val earned = earnedBadges(d, _state.value.settings.threshold)
        val seen = storage.seenBadges
        val fresh = earned.filter { it.id !in seen }
        if (fresh.isNotEmpty()) {
            storage.seenBadges = seen + fresh.map { it.id }
            viewModelScope.launch {
                kotlinx.coroutines.delay(900)
                _state.update { it.copy(celebrate = fresh.first()) }
            }
        }
    }

    fun dismissCelebration() = _state.update { it.copy(celebrate = null) }
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/screens/WebScreens.kt`

```kotlin
package com.student.amizoneplus.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.widget.Toast
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.student.amizoneplus.data.Downloads
import com.student.amizoneplus.data.Http
import com.student.amizoneplus.ui.kit.CircleButton
import com.student.amizoneplus.ui.kit.Quips
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.student.amizoneplus.data.AmizoneClient
import com.student.amizoneplus.ui.kit.Ink
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.coroutines.launch

// Makes Amizone's own (responsive) layout kick in on a phone. Nothing is hidden:
// the page keeps its real header and hamburger menu, exactly like a mobile browser.
private const val VIEWPORT_JS = """
(function(){try{
  var m=document.querySelector('meta[name=viewport]');
  if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}
  m.content='width=device-width, initial-scale=1';
}catch(e){}})();"""

// What the loaded page is: the login form, Amizone's "unauthorised" stub, or a real page.
private const val STATE_JS = """
(function(){
  if(document.querySelector('input[name="_Password"]'))return 'login';
  var t=(document.body&&document.body.innerText||'').slice(0,400).toLowerCase();
  if(t.indexOf('unauthori')>=0)return 'unauth';
  return 'ok';
})()"""

/**
 * Amizone's sections are partial pages that its Home shell loads into itself
 * (opening one directly gives bare, unstyled HTML). So: open Home, then ask the
 * page to load the section the way its own menu does.
 */
private fun navJs(path: String) = """
(function(p){
  // Home pops its notice board on every visit; when a section was asked for, close it.
  function hideModals(){try{if(window.jQuery&&jQuery.fn.modal)jQuery('.modal.in,.modal.show').modal('hide');}catch(e){}}
  hideModals();setTimeout(hideModals,900);setTimeout(hideModals,2000);
  var lp=p.toLowerCase();
  var links=[].slice.call(document.querySelectorAll('a[href]'));
  var a=links.find(function(x){var h=(x.getAttribute('href')||'').toLowerCase();return h==lp||h.endsWith(lp);});
  if(a){a.click();return 'click';}
  var c=document.querySelector('#Divload,#divload,#Div_Partial,.page-content,.main-content');
  if(window.jQuery&&c){
    jQuery.ajax({url:p,headers:{'X-Requested-With':'XMLHttpRequest'}}).done(function(h){jQuery(c).html(h);window.scrollTo(0,0);});
    return 'ajax';
  }
  location.href=p;return 'nav';
})(${org.json.JSONObject.quote(path)})"""

@Composable
private fun SheetHeader(title: String, onClose: () -> Unit) {
    Ink(Modifier.padding(start = L.page, end = L.page, bottom = L.block)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(title, Type.title, ink = true, maxLines = 1, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.Close, null, tint = P.onInk, modifier = Modifier.size(26.dp).press(onClose))
        }
    }
}

@Composable
private fun WebBar(title: String, host: String, onBack: () -> Unit, onReload: () -> Unit, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = L.page, end = L.page, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircleButton(Icons.AutoMirrored.Outlined.ArrowBack, onBack, size = 44.dp)
        Column(Modifier.weight(1f)) {
            T(title, Type.title, maxLines = 1)
            T(host, Type.caption, faint = true, maxLines = 1)
        }
        CircleButton(Icons.Outlined.Refresh, onReload, size = 44.dp)
        CircleButton(Icons.Outlined.Close, onClose, size = 44.dp)
    }
}

/**
 * A signed-in Amizone page, shown the way a phone browser shows it (with Amizone's
 * own menu). Cookies and the browser identity are shared with the app's connection,
 * so it is normally already signed in; if not, the app signs in again and retries.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AuthedWebPage(url: String, title: String, relogin: suspend () -> Boolean, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val amizone = url.startsWith(AmizoneClient.BASE)
    val path = if (amizone) url.removePrefix(AmizoneClient.BASE).ifEmpty { "/" } else null
    val viaHome = path != null && !path.equals("/Home", true) && path != "/"
    val start = if (viaHome) AmizoneClient.BASE + "/Home" else url

    var cover by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0) }
    var host by remember { mutableStateOf(runCatching { android.net.Uri.parse(url).host.orEmpty() }.getOrDefault("")) }
    var web by remember { mutableStateOf<WebView?>(null) }
    var pending by remember { mutableStateOf(if (viaHome) path else null) }
    var tries by remember { mutableStateOf(0) }
    var ready by remember { mutableStateOf(!amizone) }
    val quip = remember { Quips.loading() }

    fun back() { val w = web; if (w != null && w.canGoBack()) w.goBack() else onClose() }
    BackHandler(onBack = ::back)

    // Sign in natively first, so the very first page load is already authorised.
    LaunchedEffect(Unit) {
        if (amizone) runCatching { relogin() }
        ready = true
    }

    Column(Modifier.fillMaxSize().background(P.bg).statusBarsPadding().padding(top = 10.dp)) {
        WebBar(title, host, ::back, { web?.reload() }, onClose)
        Box(Modifier.fillMaxWidth().height(2.dp)) {
            if (progress in 1..99) Box(Modifier.fillMaxWidth(progress / 100f).height(2.dp).background(P.peri))
        }
        Box(Modifier.fillMaxSize()) {
            if (ready) AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.userAgentString = Http.UA
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        setBackgroundColor(android.graphics.Color.WHITE)
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) { progress = newProgress }
                        }
                        val self = this
                        setDownloadListener { dUrl, _, disp, mime, _ ->
                            val name = URLUtil.guessFileName(dUrl, disp, mime)
                            Toast.makeText(context, "Downloading $name…", Toast.LENGTH_SHORT).show()
                            scope.launch {
                                runCatching { Downloads.open(context, dUrl, name, referer = self.url) }
                                    .onFailure { Toast.makeText(context, it.message ?: "Couldn't download that file.", Toast.LENGTH_LONG).show() }
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, u: String?, favicon: Bitmap?) {
                                u?.let { host = runCatching { android.net.Uri.parse(it).host.orEmpty() }.getOrDefault(host) }
                            }
                            override fun onPageFinished(view: WebView, u: String?) {
                                view.evaluateJavascript(VIEWPORT_JS, null)
                                view.evaluateJavascript(STATE_JS) { raw ->
                                    val state = raw.orEmpty().trim('"')
                                    if (state != "ok" && amizone && tries < 2) {
                                        // Session expired: sign in again natively, then start over.
                                        tries++
                                        cover = true
                                        scope.launch { if (relogin()) view.loadUrl(start) else cover = false }
                                        return@evaluateJavascript
                                    }
                                    if (state == "login") { cover = false; return@evaluateJavascript } // let the user sign in by hand
                                    val target = pending
                                    if (target != null && u?.startsWith(AmizoneClient.BASE) == true) {
                                        pending = null
                                        view.evaluateJavascript(navJs(target)) { how ->
                                            if (how.orEmpty().trim('"') == "nav") return@evaluateJavascript
                                            view.postDelayed({ cover = false }, 700)
                                        }
                                    } else {
                                        cover = false
                                    }
                                }
                            }
                        }
                        web = this
                        loadUrl(start, mapOf("Referer" to AmizoneClient.BASE + "/Home"))
                    }
                },
                onRelease = { it.stopLoading(); it.destroy() },
            )
            androidx.compose.animation.AnimatedVisibility(cover, enter = fadeIn(), exit = fadeOut(tween(260))) {
                Column(Modifier.fillMaxSize().background(P.bg), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = P.text)
                    T("Opening $title", Type.label, modifier = Modifier.padding(top = 16.dp))
                    T(quip, Type.caption, sub = true, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

/**
 * Fallback sign-in on the real Amizone login page (if Amizone ever rejects the
 * automatic sign-in). Pre-fills your details; closes itself once you're in.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebLoginSheet(username: String?, password: String?, onDone: () -> Unit, onClose: () -> Unit) {
    var done by remember { mutableStateOf(false) }
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().background(P.canvas).statusBarsPadding().padding(top = L.page)) {
        SheetHeader("Sign in to Amizone", onClose)
        T("Your details are filled in. Tick the Cloudflare box, tap Login, and this closes by itself.", Type.caption, sub = true, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp))
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = Http.UA
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, u: String?) {
                            val path = runCatching { android.net.Uri.parse(u).path.orEmpty() }.getOrDefault("")
                            if (!done && u?.startsWith(AmizoneClient.BASE) == true && path.isNotEmpty() && path != "/" && !path.contains("login", true)) {
                                done = true
                                CookieManager.getInstance().flush()
                                onDone()
                                return
                            }
                            if (username != null && password != null) {
                                val js = "(function(){var u=document.querySelector('[name=\"_UserName\"]'),p=document.querySelector('[name=\"_Password\"]');" +
                                    "if(u)u.value=${org.json.JSONObject.quote(username)};if(p)p.value=${org.json.JSONObject.quote(password)};})();"
                                view.evaluateJavascript(js, null)
                            }
                        }
                    }
                    loadUrl(AmizoneClient.BASE + "/")
                }
            },
        )
    }
}

@Composable
fun Spinner(modifier: Modifier = Modifier) =
    Box(modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P.ink) }
```

---

## `app/src/main/java/com/student/amizoneplus/ui/screens/LoginScreen.kt`

```kotlin
package com.student.amizoneplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.student.amizoneplus.data.AmizoneException
import com.student.amizoneplus.data.Credentials
import com.student.amizoneplus.ui.AppViewModel
import com.student.amizoneplus.ui.kit.Caps
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(vm: AppViewModel, ctxError: String?) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHaptics.current
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (user.isBlank() || pass.isEmpty()) { error = "Enter your Amizone ID and password."; haptics.warning(); return }
        busy = true; error = null
        scope.launch {
            try {
                vm.signIn(Credentials(user.trim(), pass))
            } catch (e: AmizoneException) {
                error = e.message; haptics.error()
            } catch (e: Exception) {
                error = e.message ?: "Sign in failed."; haptics.error()
            } finally {
                busy = false
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = P.hairline, focusedTextColor = P.text, unfocusedTextColor = P.text,
        cursorColor = P.text, focusedContainerColor = P.card, unfocusedContainerColor = P.card, focusedBorderColor = P.text,
    )

    Column(
        Modifier.fillMaxSize().background(P.canvas).statusBarsPadding().imePadding().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(64.dp).background(P.sun, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.School, null, tint = P.onHero, modifier = Modifier.size(30.dp))
        }
        T("WELCOME", Type.hero, modifier = Modifier.padding(top = 24.dp))
        T("a Shynxo production ✦", Type.caption, faint = true)
        T("Sign in with your Amizone account. You'll only need to do this once.", Type.body, sub = true, modifier = Modifier.padding(top = 6.dp, bottom = 32.dp))

        T("Amizone ID", Type.caption, sub = true, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            user, { user = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp), colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
        )
        T("Password", Type.caption, sub = true, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        OutlinedTextField(
            pass, { pass = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp), colors = fieldColors,
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { submit() }),
            trailingIcon = {
                Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = P.sub,
                    modifier = Modifier.press({ show = !show }, Feel.Select))
            },
        )

        (error ?: ctxError)?.let { T(it, Type.body, color = P.bad, modifier = Modifier.padding(top = 16.dp)) }

        Row(
            Modifier.padding(top = 24.dp).fillMaxWidth().height(58.dp).press(if (busy) null else ::submit, Feel.Press)
                .background(P.cream, RoundedCornerShape(50)),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            if (busy) CircularProgressIndicator(color = P.onCream, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else Caps("SIGN IN", P.onCream)
        }

        T(
            "Sign in through the Amizone website", Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = P.text,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp)
                .press({
                    if (user.isBlank() || pass.isEmpty()) error = "Enter your Amizone ID and password first so the app can remember them."
                    else vm.openWebLogin(Credentials(user.trim(), pass))
                }),
        )

        Row(Modifier.padding(top = 36.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, null, tint = P.faint, modifier = Modifier.size(14.dp))
            T("  Your password is encrypted on this phone (Android Keystore) and sent only to s.amizone.net.",
                Type.caption, faint = true)
        }
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/screens/Badges.kt`

```kotlin
package com.student.amizoneplus.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.amizoneplus.logic.ALL_BADGES
import com.student.amizoneplus.logic.Badge
import com.student.amizoneplus.ui.kit.BtnKind
import com.student.amizoneplus.ui.kit.Btn
import com.student.amizoneplus.ui.kit.Fused
import com.student.amizoneplus.ui.kit.Ink
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Five-point star badge with a short label (like the "15k Master" badge in the reference). */
@Composable
fun BadgeArt(short: String, tag: String?, size: Dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = this.size.width / 2
            val outer = c * 0.92f
            val inner = c * 0.52f
            val star = Path().apply {
                for (i in 0 until 10) {
                    val a = Math.PI * 2 / 10 * i - Math.PI / 2
                    val r = if (i % 2 == 0) outer else inner
                    val x = (c + r * cos(a)).toFloat()
                    val y = (c + r * sin(a)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(star, P.ink)
            drawPath(star, P.ink, style = Stroke(width = c * 0.12f, join = StrokeJoin.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            T(short, Type.caption.copy(fontSize = (size.value * (if (short.length > 3) 0.15f else 0.19f)).sp, fontWeight = FontWeight.SemiBold), ink = true)
            if (tag != null) T(tag, Type.caption.copy(fontSize = (size.value * 0.08f).sp), ink = true, sub = true)
        }
    }
}

/** Reward screen with a particle burst and a short haptic pattern. */
@Composable
fun Celebration(badge: Badge, onClose: () -> Unit) {
    val haptics = LocalHaptics.current
    BackHandler(onBack = onClose)
    val zoom = remember { Animatable(0.4f) }
    val burst = remember { Animatable(0f) }
    val particles = remember(badge.id) {
        List(26) { i ->
            Triple(Math.PI * 2 * i / 26 + Random.nextDouble(0.0, 0.3), 70f + Random.nextFloat() * 70f, 3f + Random.nextFloat() * 7f)
        }
    }
    LaunchedEffect(badge.id) {
        haptics.celebrate()
        launch { zoom.animateTo(1f, Motion.soft()) }
        delay(150)
        burst.animateTo(1f, tween(1100, easing = Motion.emphasized))
    }
    Column(
        Modifier.fillMaxSize().background(P.canvas).statusBarsPadding().navigationBarsPadding().padding(L.page),
    ) {
        Fused {
            Ink(padding = L.pad) { T("amizone+", Type.title, ink = true) }
            Ink { T(badge.title, Type.label, ink = true); T("New badge", Type.caption, ink = true, sub = true) }
        }
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            T("Congratulations!", Type.title.copy(fontSize = 26.sp), align = TextAlign.Center)
            T(badge.detail, Type.body, sub = true, align = TextAlign.Center, modifier = Modifier.padding(top = 8.dp).widthIn(max = 280.dp))
            Box(Modifier.padding(top = 48.dp).size(240.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val k = burst.value
                    val alpha = if (k < 0.7f) 1f else 1f - (k - 0.7f) / 0.3f
                    particles.forEachIndexed { i, (a, dist, r) ->
                        val p = Offset(
                            center.x + (cos(a) * dist * k).toFloat() * density,
                            center.y + ((sin(a) * dist * k) + 30 * k * k).toFloat() * density,
                        )
                        drawCircle(if (i % 3 == 0) P.faint else P.ink, radius = r * density * (1 - 0.4f * k) / 2, center = p, alpha = alpha * (if (k > 0f) 1f else 0f))
                    }
                }
                Box(
                    Modifier.size(120.dp).graphicsLayer { scaleX = zoom.value; scaleY = zoom.value }
                        .border(1.dp, P.line, RoundedCornerShape(L.radius)).background(P.canvas, RoundedCornerShape(L.radius)),
                    contentAlignment = Alignment.Center,
                ) { BadgeArt(badge.short, badge.tag, 84.dp) }
            }
            T("New badge", Type.label, modifier = Modifier.padding(top = 16.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Nice", BtnKind.Ink, onClick = onClose)
            Btn("Close", BtnKind.Outline, onClick = onClose)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgeGallery(earned: List<Badge>, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val locked = ALL_BADGES.filter { l -> earned.none { it.title == l.title } }
    Column(Modifier.fillMaxSize().background(P.canvas).statusBarsPadding().navigationBarsPadding().padding(L.page)) {
        Fused {
            Ink { T("Badges", Type.title, ink = true) }
            Ink { T("${earned.size}", Type.num, ink = true); T("unlocked so far", Type.caption, ink = true, sub = true) }
        }
        FlowRow(
            Modifier.padding(top = L.block).weight(1f).verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(L.block),
            verticalArrangement = Arrangement.spacedBy(L.block),
            maxItemsInEachRow = 3,
        ) {
            (earned + locked).forEachIndexed { i, b ->
                val isLocked = i >= earned.size
                Column(
                    Modifier.weight(1f).aspectRatio(0.85f).graphicsLayer { alpha = if (isLocked) 0.45f else 1f }
                        .border(1.2.dp, if (isLocked) P.hairline else P.line, RoundedCornerShape(L.radius)).padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    BadgeArt(b.short, b.tag, 58.dp)
                    T(b.title, Type.caption, align = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
                }
            }
            repeat((3 - (earned.size + locked.size) % 3) % 3) { Spacer(Modifier.weight(1f)) }
        }
        Row { Btn("Close", BtnKind.Ink, onClick = onClose) }
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/screens/TodayScreen.kt`

```kotlin
package com.student.amizoneplus.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import com.student.amizoneplus.ui.pets.perch
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.student.amizoneplus.data.ScheduledClass
import com.student.amizoneplus.logic.creditsFor
import com.student.amizoneplus.logic.earnedBadges
import com.student.amizoneplus.logic.percent
import com.student.amizoneplus.logic.presentStreak
import com.student.amizoneplus.logic.statusText
import com.student.amizoneplus.logic.currentWeek
import com.student.amizoneplus.ui.UiState
import com.student.amizoneplus.ui.kit.AreaChart
import com.student.amizoneplus.ui.kit.ArrowButton
import com.student.amizoneplus.ui.kit.Avatar
import com.student.amizoneplus.ui.kit.Card
import com.student.amizoneplus.ui.kit.Chip
import com.student.amizoneplus.ui.kit.CircleButton
import com.student.amizoneplus.ui.kit.CountUp
import com.student.amizoneplus.ui.kit.CountdownChip
import com.student.amizoneplus.ui.kit.DayCircles
import com.student.amizoneplus.ui.kit.DropdownPill
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.PairCards
import com.student.amizoneplus.ui.kit.Rise
import com.student.amizoneplus.ui.kit.SectionLabel
import com.student.amizoneplus.ui.kit.SelectorPill
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.Tone
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.shell.LocalMenu
import com.student.amizoneplus.ui.shell.PlainPage
import com.student.amizoneplus.ui.shell.Route
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.ceil

fun sameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

fun hm(ms: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))

/** Daily attendance rate (0..1) for each day with marked classes, oldest first. */
private fun dailySeries(history: List<ScheduledClass>): List<Pair<Long, Float>> =
    history.filter { it.state == "present" || it.state == "absent" }
        .groupBy { Calendar.getInstance().apply { timeInMillis = it.startMs }.get(Calendar.DAY_OF_YEAR) }
        .map { (_, cs) -> cs.first().startMs to cs.count { it.state == "present" }.toFloat() / cs.size }
        .sortedBy { it.first }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(s: UiState, go: (Route) -> Unit, onRefresh: () -> Unit, onBadges: () -> Unit, banner: (@Composable () -> Unit)?) {
    val menu = LocalMenu.current
    val data = s.data
    val t = s.settings.threshold
    val now = System.currentTimeMillis()
    val att = data?.attendance.orEmpty()
    val overall = att.sumOf { it.held }.let { h -> if (h > 0) percent(att.sumOf { it.attended }, h) else 0.0 }
    val low = att.filter { it.held > 0 && percent(it.attended, it.held) < t }
    val today = data?.schedule.orEmpty().filter { sameDay(it.startMs, now) }
    val upcoming = data?.schedule.orEmpty().firstOrNull { it.endMs > now }
    val live = upcoming != null && upcoming.startMs <= now
    val nextExam = data?.exams?.exams.orEmpty().firstOrNull { (it.timeMs ?: 0) + com.student.amizoneplus.logic.EXAM_LENGTH_MS > now }
    val name = data?.profile?.name ?: s.username ?: ""
    val first = name.substringBefore(' ')
    val badges = data?.let { earnedBadges(it, t) }.orEmpty()
    val streak = presentStreak(data?.history.orEmpty())

    var range by remember { mutableStateOf("4 weeks") }
    val series = dailySeries(data?.history.orEmpty()).let { if (range == "this week") it.takeLast(7) else it }
    val week = currentWeek(data?.history.orEmpty(), data?.schedule.orEmpty(), now)
    val todayIdx = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7
    var selDay by remember { mutableIntStateOf((Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7) }
    var i = 0

    PlainPage(s.refreshing, onRefresh, banner) {
        // Greeting row
        Rise(i++) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                var pokes by remember { mutableIntStateOf(0) }
                Box(Modifier.press({
                    android.widget.Toast.makeText(ctx, com.student.amizoneplus.ui.kit.Quips.tap(pokes++), android.widget.Toast.LENGTH_SHORT).show()
                }, Feel.Select, 0.88f)) { Avatar(name.ifEmpty { "A" }, P.sun, 52.dp) }
                T(if (first.isNotEmpty()) "Hi, $first!" else "Hi!", Type.label.copy(fontSize = 18.sp()), modifier = Modifier.weight(1f).padding(start = 14.dp), maxLines = 1)
                CircleButton(Icons.Outlined.Apps, menu.open, 52.dp)
                Spacer(Modifier.width(10.dp))
                CircleButton(Icons.Outlined.NotificationsNone, { go(if (low.isNotEmpty()) Route.Subjects else Route.Schedule) }, 52.dp,
                    badge = low.isNotEmpty() || (nextExam?.timeMs?.let { it - now < 3 * 86_400_000L } == true))
            }
        }
        Rise(i++) {
            val prof = data?.profile
            SelectorPill(Icons.Outlined.School,
                listOfNotNull(prof?.program?.ifEmpty { null }, prof?.batch?.ifEmpty { null }).joinToString(" · ").ifEmpty { "Amizone ID · ${s.username.orEmpty()}" }) { go(Route.More) }
        }

        // Alert card (reference "Stabilizing labor cost")
        Rise(i++) {
            Card(onClick = { go(if (upcoming != null || nextExam != null) Route.Schedule else Route.Subjects) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(60.dp).background(if (low.isNotEmpty() && upcoming == null) P.coral else P.sun, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(30.dp).background(P.onHero, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(when { upcoming != null -> Icons.AutoMirrored.Outlined.EventNote; nextExam != null -> Icons.Outlined.Event; low.isNotEmpty() -> Icons.Outlined.PriorityHigh; else -> Icons.Outlined.TaskAlt },
                                null, tint = P.sun, modifier = Modifier.size(18.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        when {
                            upcoming != null -> {
                                T((if (live) "Now: " else "Next: ").uppercase() + upcoming.name.uppercase(), Type.headline.copy(fontSize = 27.sp()), maxLines = 2)
                                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    T(listOfNotNull(upcoming.room.takeIf { it.isNotBlank() }?.let { "Room $it" }, hm(upcoming.startMs)).joinToString(" · ") +
                                        if (live) "  · ends in " else "  · in ", Type.body, sub = true)
                                    CountdownChip(if (live) upcoming.endMs else upcoming.startMs)
                                }
                            }
                            nextExam != null -> {
                                T("EXAM: ${nextExam.name.uppercase()}", Type.headline.copy(fontSize = 27.sp()), maxLines = 2)
                                T(listOfNotNull(nextExam.mode.ifBlank { null }, nextExam.timeMs?.let { com.student.amizoneplus.logic.examWhen(it, now) }).joinToString(" · "),
                                    Type.body, sub = true, modifier = Modifier.padding(top = 8.dp))
                            }
                            low.isNotEmpty() -> {
                                T("ATTENDANCE AT RISK", Type.headline.copy(fontSize = 27.sp()))
                                T("${low.size} subject${if (low.size > 1) "s are" else " is"} below $t%. Check the semester plan.", Type.body, sub = true, modifier = Modifier.padding(top = 8.dp))
                            }
                            else -> {
                                T("ALL CAUGHT UP", Type.headline.copy(fontSize = 27.sp()))
                                T("No classes or exams coming up. Every subject is above $t%.", Type.body, sub = true, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                    ArrowButton({ go(if (upcoming != null || nextExam != null) Route.Schedule else Route.Subjects) })
                }
            }
        }

        // Periwinkle attendance card (reference "Venue Capacity")
        Rise(i++) {
            Column(Modifier.fillMaxWidth().perch().background(P.peri, RoundedCornerShape(30.dp)).padding(20.dp).animateContentSize(tween(Motion.MORPH_MS, easing = Motion.emphasized))) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        T("ATTENDANCE", Type.display.copy(fontSize = 38f.sp(), lineHeight = 38f.sp()), color = P.onHero, maxLines = 1)
                        Row(verticalAlignment = Alignment.Bottom) {
                            CountUp(overall, 1, "%", Type.num, P.onHero)
                            T("  overall", Type.caption, color = P.onHero.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 8.dp))
                        }
                    }
                    DropdownPill(range, { range = if (range == "4 weeks") "this week" else "4 weeks" }, P.periSoft, P.onHero)
                }
                Spacer(Modifier.height(18.dp))
                DayCircles(week, selDay, { selDay = it }, todayIdx = todayIdx)
                androidx.compose.animation.AnimatedContent(selDay, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "dayline") { d ->
                    com.student.amizoneplus.ui.kit.dayLine(week[d], d == todayIdx)?.let {
                        T(it, Type.caption, color = P.onHero.copy(alpha = 0.8f), modifier = Modifier.padding(top = 12.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Spacer(Modifier.height(10.dp))
                if (series.size >= 2) {
                    val hl = series.indexOfLast { (Calendar.getInstance().apply { timeInMillis = it.first }.get(Calendar.DAY_OF_WEEK) + 5) % 7 == selDay }
                    AreaChart(series.map { it.second }, Modifier.fillMaxWidth().height(140.dp), P.coral, hl.takeIf { it >= 0 })
                } else {
                    T("Your daily attendance chart appears once a few classes are marked.", Type.caption, color = P.onHero.copy(alpha = 0.75f),
                        modifier = Modifier.padding(vertical = 12.dp))
                }
                FlowRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Target $t%", Tone.Cream, P.sun)
                    Chip("${low.size} at risk", Tone.Cream, if (low.isEmpty()) P.good else P.coral)
                    Chip("Streak $streak", Tone.Cream, P.sky)
                }
            }
        }

        Rise(i++) {
            PairCards(
                left = {
                    Card(Modifier.heightIn(min = 150.dp), onClick = { go(Route.Schedule) }) {
                        T("Classes today", Type.label)
                        T(if (today.isNotEmpty()) "${today.count { it.state == "present" }} marked present" else "Nothing scheduled", Type.caption, sub = true, modifier = Modifier.padding(top = 4.dp))
                        Spacer(Modifier.weight(1f))
                        CountUp(today.size.toDouble(), modifier = Modifier.padding(top = 14.dp))
                    }
                },
                right = {
                    Card(Modifier.heightIn(min = 150.dp), onClick = onBadges) {
                        T(if (badges.isNotEmpty()) "Badges" else "Present streak", Type.label)
                        T(if (badges.isNotEmpty()) badges.first().title else "classes in a row", Type.caption, sub = true, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                        Spacer(Modifier.weight(1f))
                        CountUp((if (badges.isNotEmpty()) badges.size else streak).toDouble(), modifier = Modifier.padding(top = 14.dp))
                    }
                },
            )
        }

        if (low.isNotEmpty()) Rise(i++) { SectionLabel("NEEDS ATTENTION") }
        low.forEach { a ->
            Rise(i++) {
                com.student.amizoneplus.ui.kit.OutlineRow(a.name.uppercase(), statusText(a.attended, a.held, t, creditsFor(a.code, data, s.settings)), { go(Route.Subjects) }) {
                    Chip("%.0f%%".format(percent(a.attended, a.held)), Tone.Bad)
                }
            }
        }

        if (today.isNotEmpty()) Rise(i++) { SectionLabel("TODAY") }
        today.forEach { c -> Rise(i++) { ClassRow(c) } }
        Rise(i++) {
            val line = remember { com.student.amizoneplus.ui.kit.Quips.footer() }
            T(line, Type.caption, faint = true, align = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        }
    }
}

private fun Float.sp() = androidx.compose.ui.unit.TextUnit(this, androidx.compose.ui.unit.TextUnitType.Sp)
private fun Int.sp() = this.toFloat().sp()

/** Timeline-style class card: time on the left, details right. */
@Composable
fun ClassRow(c: ScheduledClass, onClick: (() -> Unit)? = null) {
    val now = System.currentTimeMillis()
    val live = c.startMs <= now && now < c.endMs
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.width(64.dp).padding(top = 18.dp)) {
            T(hm(c.startMs), Type.caption, sub = !live, color = if (live) P.sky else null)
        }
        Card(Modifier.weight(1f), color = if (live) P.cardRaised else P.card, onClick = onClick, padding = 16.dp) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    T(c.name, Type.title.copy(fontSize = 21f.sp(), lineHeight = 23f.sp()), maxLines = 2)
                    if (c.faculty.isNotBlank()) T(c.faculty, Type.caption, sub = true, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                }
                when (c.state) {
                    "present" -> Chip("Present", Tone.Good)
                    "absent" -> Chip("Absent", Tone.Bad)
                    else -> if (live) Chip("Live", Tone.Sky)
                }
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (c.room.isNotBlank()) Chip("Room ${c.room}", Tone.Lav)
                if (c.code.isNotBlank()) Chip(c.code, Tone.Sage)
                Chip("${hm(c.startMs)}–${hm(c.endMs)}", Tone.Ghost)
            }
        }
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/screens/SubjectsScreen.kt`

```kotlin
package com.student.amizoneplus.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.student.amizoneplus.data.Course
import com.student.amizoneplus.data.Settings
import com.student.amizoneplus.logic.creditsFor
import com.student.amizoneplus.logic.percent
import com.student.amizoneplus.logic.semesterPlan
import com.student.amizoneplus.logic.statusText
import com.student.amizoneplus.logic.weekdayRates
import com.student.amizoneplus.ui.UiState
import com.student.amizoneplus.ui.kit.ArrowButton
import com.student.amizoneplus.ui.kit.Avatar
import com.student.amizoneplus.ui.kit.Btn
import com.student.amizoneplus.ui.kit.BtnKind
import com.student.amizoneplus.ui.kit.Card
import com.student.amizoneplus.ui.kit.Chip
import com.student.amizoneplus.ui.kit.CircleButton
import com.student.amizoneplus.ui.kit.CountUp
import com.student.amizoneplus.ui.kit.DropdownPill
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.Rise
import com.student.amizoneplus.ui.kit.SearchPill
import com.student.amizoneplus.ui.kit.StatTile
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.Tone
import com.student.amizoneplus.ui.kit.WeekBars
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.shell.HeroPage
import com.student.amizoneplus.ui.shell.MenuButton
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import com.student.amizoneplus.ui.theme.attendanceTone

private val AVATAR_COLORS = listOf(P.sun, P.sky, P.sage, P.lilac, P.coralSoft)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SubjectsScreen(
    s: UiState,
    onRefresh: () -> Unit,
    openLms: (String) -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    banner: (@Composable () -> Unit)?,
) {
    val haptics = LocalHaptics.current
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = open != null) { haptics.collapse(); open = null }
    val data = s.data
    val courses: List<Course> = data?.courses?.ifEmpty { null }
        ?: data?.attendance.orEmpty().map { Course(it.code, it.name, "", it.attended, it.held, null, null) }

    SharedTransitionLayout {
        AnimatedContent(
            open,
            transitionSpec = { fadeIn(tween(Motion.MORPH_MS, easing = Motion.emphasized)) togetherWith fadeOut(tween(Motion.MORPH_MS / 2)) },
            label = "subjects",
        ) { code ->
            val course = courses.firstOrNull { it.code == code }
            if (course == null) {
                SubjectList(s, courses, onRefresh, banner, updateSettings, this@SharedTransitionLayout, this) { haptics.expand(); open = it }
            } else {
                SubjectDetail(s, course, courses.indexOf(course), onRefresh, { haptics.collapse(); open = null }, { openLms(course.code) },
                    updateSettings, this@SharedTransitionLayout, this)
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SubjectList(
    s: UiState,
    courses: List<Course>,
    onRefresh: () -> Unit,
    banner: (@Composable () -> Unit)?,
    updateSettings: ((Settings) -> Settings) -> Unit,
    shared: SharedTransitionScope,
    anim: AnimatedVisibilityScope,
    onOpen: (String) -> Unit,
) {
    val t = s.settings.threshold
    var filter by rememberSaveable { mutableStateOf("all") }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = courses.filter {
        val p = if (it.held > 0) percent(it.attended, it.held) else 100.0
        when (filter) { "at risk" -> p < t; "on track" -> p >= t; else -> true } &&
            (query.isBlank() || it.name.contains(query, true) || it.code.contains(query, true))
    }
    val held = courses.filter { it.held > 0 }
    val atRisk = held.count { percent(it.attended, it.held) < t }

    HeroPage(
        heroColor = P.coral,
        refreshing = s.refreshing,
        onRefresh = onRefresh,
        banner = banner,
        hero = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The title shrinks a touch rather than wrapping when the filter label is long.
                androidx.compose.material3.Text(
                    "SUBJECTS", Modifier.weight(1f), color = P.onHero, maxLines = 1, softWrap = false,
                    style = Type.display, autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(
                        minFontSize = androidx.compose.ui.unit.TextUnit(28f, androidx.compose.ui.unit.TextUnitType.Sp),
                        maxFontSize = Type.display.fontSize,
                    ),
                )
                Spacer(Modifier.size(8.dp))
                FilterMenu(filter) { filter = it }
                Spacer(Modifier.size(8.dp))
                MenuButton()
            }
            // Data where the reference has mascots: every subject's attendance, with the target line.
            SubjectBars(held, t, Modifier.fillMaxWidth().height(170.dp).padding(top = 20.dp))
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${courses.size}", "subjects", P.coralSoft)
                StatTile("$atRisk", "below $t%", P.coralSoft, onClick = { filter = "at risk" })
            }
            // What you're aiming for: every "can miss" and "must attend" follows it.
            com.student.amizoneplus.ui.kit.Caps("YOUR TARGET", P.onHero.copy(alpha = 0.7f), Modifier.padding(top = 18.dp, bottom = 8.dp))
            com.student.amizoneplus.ui.kit.TargetPicker(t, { v -> updateSettings { it.copy(threshold = v) } },
                bg = P.coralSoft, fg = P.onHero, activeBg = P.onHero, activeFg = Color.White)
        },
    ) {
        SearchPill(query, { query = it }, "Search by name or code")
        T("${shown.size} subject${if (shown.size == 1) "" else "s"} found", Type.label, sub = true, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
        shown.forEachIndexed { i, c ->
            val credits = creditsFor(c.code, s.data, s.settings)
            Rise(i) {
                with(shared) {
                    Card(
                        Modifier.sharedBounds(rememberSharedContentState("card-${c.code}"), anim, clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(28.dp))),
                        onClick = { onOpen(c.code) },
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Avatar(c.code, AVATAR_COLORS[courses.indexOf(c) % AVATAR_COLORS.size], 56.dp)
                            Column(Modifier.weight(1f)) {
                                T(c.name.uppercase(), Type.title, maxLines = 2,
                                    modifier = Modifier.sharedElement(rememberSharedContentState("name-${c.code}"), anim))
                                if (c.type.isNotEmpty()) Box(Modifier.padding(top = 8.dp)) { Chip(c.type, Tone.Sun) }
                            }
                            ArrowButton({ onOpen(c.code) })
                        }
                        T(if (c.held > 0) "%.1f%% · %s".format(percent(c.attended, c.held), statusText(c.attended, c.held, t, credits)) else "No classes held yet",
                            Type.body, modifier = Modifier.padding(top = 14.dp), maxLines = 2)
                        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            T("CODE: ", Type.caption, faint = true); T(c.code, Type.caption, sub = true)
                            Box(Modifier.weight(1f))
                            if (c.held > 0) Box(Modifier.size(10.dp).background(attendanceTone(percent(c.attended, c.held), t), CircleShape))
                        }
                    }
                }
            }
        }
    }
}

/** The "all / at risk / on track" filter: a pill that opens a small menu. */
@Composable
private fun FilterMenu(value: String, onPick: (String) -> Unit) {
    val haptics = LocalHaptics.current
    var open by remember { mutableStateOf(false) }
    Box {
        DropdownPill(value, { haptics.select(); open = true }, P.coralSoft, P.onHero)
        androidx.compose.material3.DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = P.cardRaised,
            offset = androidx.compose.ui.unit.DpOffset(0.dp, 8.dp),
        ) {
            listOf("all" to "All subjects", "at risk" to "At risk", "on track" to "On track").forEach { (key, label) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { T(label, Type.label, color = if (key == value) P.text else P.sub) },
                    trailingIcon = if (key == value) ({ Icon(Icons.Outlined.Check, null, tint = P.coral, modifier = Modifier.size(18.dp)) }) else null,
                    onClick = { open = false; if (key != value) { haptics.select(); onPick(key) } },
                )
            }
        }
    }
}

/**
 * One bar per subject, scaled 50–100% so differences are visible, with a dashed
 * target line and each subject's code underneath. Bars grow in smoothly.
 */
@Composable
private fun SubjectBars(courses: List<Course>, threshold: Int, modifier: Modifier) {
    Column(modifier) {
        BarsCanvas(courses, threshold, Modifier.fillMaxWidth().weight(1f))
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            courses.forEach { c ->
                T(c.code.takeLast(3), Type.caption.copy(fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)),
                    color = P.onHero.copy(alpha = 0.7f), maxLines = 1, align = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

private const val BAR_FLOOR = 50f
private fun barFrac(p: Float) = ((p - BAR_FLOOR) / (100f - BAR_FLOOR)).coerceIn(0.04f, 1f)

@Composable
private fun BarsCanvas(courses: List<Course>, threshold: Int, modifier: Modifier) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(courses.size) { grow.snapTo(0f); grow.animateTo(1f, tween(900, easing = Motion.emphasized)) }
    Canvas(modifier) {
        if (courses.isEmpty()) return@Canvas
        val n = courses.size
        val gap = 10.dp.toPx()
        val bw = (size.width - gap * (n - 1)) / n
        courses.forEachIndexed { i, c ->
            val p = percent(c.attended, c.held).toFloat() / 100f
            val h = size.height * barFrac(p * 100) * grow.value
            val x = i * (bw + gap)
            drawRoundRect(P.onHero.copy(alpha = 0.10f), Offset(x, 0f), androidx.compose.ui.geometry.Size(bw, size.height), androidx.compose.ui.geometry.CornerRadius(bw / 2))
            drawRoundRect(if (p * 100 < threshold) P.onHero else Color.White, Offset(x, size.height - h), androidx.compose.ui.geometry.Size(bw, h),
                androidx.compose.ui.geometry.CornerRadius(bw / 2))
        }
        val ty = size.height * (1 - barFrac(threshold.toFloat()))
        drawLine(P.onHero, Offset(0f, ty), Offset(size.width, ty), 1.5.dp.toPx(), StrokeCap.Round, PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx())))
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SubjectDetail(
    s: UiState,
    c: Course,
    index: Int,
    onRefresh: () -> Unit,
    onClose: () -> Unit,
    openLms: () -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    shared: SharedTransitionScope,
    anim: AnimatedVisibilityScope,
) {
    val context = LocalContext.current
    val haptics = LocalHaptics.current
    val t = s.settings.threshold
    val basis = com.student.amizoneplus.logic.weeklyFor(c.code, s.data, s.settings)
    val credits = basis?.first
    val hours = s.data?.hours?.get(c.code.replace(" ", "").uppercase())
    val fromAmizone = hours?.weekly?.takeIf { it > 0 } ?: s.data?.credits?.get(c.code)
    val p = if (c.held > 0) percent(c.attended, c.held) else 0.0
    val plan = credits?.let { semesterPlan(c.attended, c.held, it, t) }

    fun setCredits(n: Double) {
        haptics.select()
        updateSettings { st ->
            val next = st.weeklyOverrides.toMutableMap()
            if (fromAmizone != null && n == fromAmizone) next.remove(c.code) else next[c.code] = n
            st.copy(weeklyOverrides = next)
        }
    }

    with(shared) {
        Box(Modifier.fillMaxSize().sharedBounds(rememberSharedContentState("card-${c.code}"), anim, clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(28.dp)))) {
            HeroPage(
                heroColor = P.coral,
                refreshing = s.refreshing,
                onRefresh = onRefresh,
                hero = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircleButton(Icons.AutoMirrored.Outlined.ArrowBack, onClose, 48.dp)
                        Box(Modifier.weight(1f))
                        Avatar(c.code, AVATAR_COLORS[index.coerceAtLeast(0) % AVATAR_COLORS.size], 48.dp)
                    }
                    T(c.name.uppercase(), Type.display, color = P.onHero, maxLines = 3,
                        modifier = Modifier.padding(top = 18.dp).sharedElement(rememberSharedContentState("name-${c.code}"), anim))
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(c.code, Tone.Cream)
                        if (c.type.isNotEmpty()) Chip(c.type, Tone.Cream)
                    }
                    Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("%.0f%%".format(p), "${c.attended}/${c.held} held", P.coralSoft)
                        StatTile(c.internals?.let { "%.0f".format(it.have) } ?: "—", c.internals?.let { "of %.0f internals".format(it.max) } ?: "internals", P.coralSoft)
                    }
                },
            ) {
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            T("SEMESTER PLAN", Type.title)
                            T(planLine(basis, hours, plan?.basePlanned) + (plan?.extraHeld?.takeIf { it > 0 }?.let { " · $it extra held so far" } ?: ""),
                                Type.caption, sub = true, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
                        }
                        Stepper(credits ?: 0.0, ::setCredits)
                    }
                    com.student.amizoneplus.ui.kit.Caps("TARGET", P.faint, Modifier.padding(top = 18.dp, bottom = 8.dp))
                    com.student.amizoneplus.ui.kit.TargetPicker(t, { v -> haptics.select(); updateSettings { it.copy(threshold = v) } })
                    if (plan != null) Row(Modifier.padding(top = 18.dp)) {
                        if (plan.extraHeld > 0) Stat("Extra held", plan.extraHeld.toDouble(), null, Modifier.weight(1f))
                        else Stat("Left", plan.remaining.toDouble(), null, Modifier.weight(1f))
                        Stat(if (plan.reachable) "Must attend" else "Extra needed", (if (plan.reachable) plan.need else plan.extraNeeded).toDouble(),
                            if (plan.reachable) null else P.bad, Modifier.weight(1f))
                        Stat("Can miss", plan.canMiss.toDouble(), if (plan.canMiss > 0) P.good else P.warn, Modifier.weight(1f))
                    }
                    Box(Modifier.padding(top = 16.dp)) { Chip(statusText(c.attended, c.held, t, credits), Tone.Ink) }
                }
                if (plan != null && !plan.reachable) ExtraClassesCard(c.attended, c.held, credits, t, plan)
                Card {
                    T("BY WEEKDAY", Type.title)
                    val weekly = weekdayRates(s.data?.history.orEmpty(), c.code)
                    T(if (weekly.any { it != null }) "Last 4 weeks" else "No marked classes in the last 4 weeks", Type.caption, sub = true, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                    WeekBars(weekly, bar = P.coral)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Btn("Material", BtnKind.Lav, Icons.Outlined.FolderOpen, openLms)
                    Btn("Syllabus", BtnKind.Ghost, Icons.Outlined.Description, c.syllabusUrl?.let { u -> { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) } })
                }
            }
        }
    }
}

private fun fmt(d: Double) = if (d % 1 == 0.0) d.toInt().toString() else d.toString()

/** How the planned classes were worked out, in one line. */
private fun planLine(basis: Pair<Double, com.student.amizoneplus.logic.Basis>?, hours: com.student.amizoneplus.data.CourseHours?, planned: Int?): String {
    if (basis == null || planned == null) return "Set classes per week to plan the semester"
    val (w, how) = basis
    return when (how) {
        com.student.amizoneplus.logic.Basis.Structure -> {
            val h = hours ?: com.student.amizoneplus.data.CourseHours()
            val parts = if (h.l == 0.0 && h.t > 0) listOfNotNull("T %s ÷ 2".format(fmt(h.t)), if (h.p > 0) "P " + fmt(h.p) else null)
                else listOfNotNull("L " + fmt(h.l), if (h.t > 0) "T " + fmt(h.t) else null, "P " + fmt(h.p))
            "%s = %s a week × 15 weeks = %d classes · from Programme Structure".format(parts.joinToString(" + "), fmt(w), planned)
        }
        com.student.amizoneplus.logic.Basis.Credits ->
            "%s a week × 15 weeks = %d classes · L/P not found, using credits".format(fmt(w), planned)
        com.student.amizoneplus.logic.Basis.Override ->
            "%s a week × 15 weeks = %d classes · set by you".format(fmt(w), planned)
    }
}

/**
 * Shown when even perfect attendance from here can't reach the target: how many
 * extra classes to ask for (and attend), plus how things stand for the 75% minimum.
 */
@Composable
private fun ExtraClassesCard(attended: Int, held: Int, weekly: Double, t: Int, plan: com.student.amizoneplus.logic.SemesterPlan) {
    val min = semesterPlan(attended, held, weekly, 75)
    Card(color = P.badSoft) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T("NEED ${plan.extraNeeded} EXTRA CLASS${if (plan.extraNeeded == 1) "" else "ES"}", Type.title, color = P.bad, modifier = Modifier.weight(1f))
            CountUp(plan.extraNeeded.toDouble(), style = Type.numSm, color = P.bad)
        }
        val n = "${plan.extraNeeded} extra class${if (plan.extraNeeded == 1) "" else "es"}"
        T(
            if (plan.remaining == 0) "All ${plan.basePlanned} planned classes are done and you're at %.1f%%. You need $n — and to attend every one — to reach $t%%.".format(plan.bestPct)
            else "Even if you attend all ${plan.remaining} classes left, you'd finish at %.1f%%. Get $n scheduled — and attend every one — to reach $t%%.".format(plan.bestPct),
            Type.body, modifier = Modifier.padding(top = 8.dp),
        )
        if (t > 75) T(
            if (min.reachable) "For the 75% minimum you don't need extras: attend ${min.need} of ${min.remaining} left (you can miss ${min.canMiss})."
            else "For the 75% minimum alone: ${min.extraNeeded} extra class${if (min.extraNeeded == 1) "" else "es"}.",
            Type.caption, sub = true, modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun Stat(label: String, value: Double, tone: Color?, modifier: Modifier) {
    Column(modifier) {
        T(label, Type.caption, sub = true)
        CountUp(value, style = Type.numSm, color = tone ?: P.text, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun Stepper(value: Double, onChange: (Double) -> Unit) {
    @Composable
    fun btn(icon: androidx.compose.ui.graphics.vector.ImageVector, next: Double, disabled: Boolean) {
        Box(
            Modifier.size(40.dp).press(if (disabled) null else ({ onChange(next) }), Feel.None, 0.88f)
                .background(if (disabled) P.cardRaised else P.cream, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (disabled) P.faint else P.onCream, modifier = Modifier.size(18.dp)) }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val step = if (value <= 1.0) 0.5 else 1.0
        btn(Icons.Outlined.Remove, (value - step).coerceAtLeast(0.5), value <= 0.5)
        T(if (value > 0) fmt(value) else "–", Type.numSm, modifier = Modifier.widthIn(min = 26.dp))
        btn(Icons.Outlined.Add, (value + (if (value < 1.0) 0.5 else 1.0)).coerceAtMost(16.0), value >= 16)
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/screens/ScheduleScreen.kt`

```kotlin
package com.student.amizoneplus.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.amizoneplus.ui.UiState
import com.student.amizoneplus.ui.kit.Caps
import com.student.amizoneplus.ui.kit.Card
import com.student.amizoneplus.ui.kit.Chip
import com.student.amizoneplus.ui.kit.ClockDial
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.Rise
import com.student.amizoneplus.ui.kit.Segmented
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.Tone
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.shell.HeroPage
import com.student.amizoneplus.ui.shell.MenuButton
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil

@Composable
fun ScheduleScreen(s: UiState, onRefresh: () -> Unit, banner: (@Composable () -> Unit)?, focusTab: String? = null, onFocusHandled: () -> Unit = {}) {
    var tab by remember { mutableStateOf(focusTab ?: "classes") }
    LaunchedEffect(focusTab) { if (focusTab != null) { tab = focusTab; onFocusHandled() } }
    var day by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val exams = s.data?.exams?.exams.orEmpty()
    val days = remember { List(7) { i -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, i) }.timeInMillis } }
    val classes = s.data?.schedule.orEmpty().filter { sameDay(it.startMs, day) }
    val hot = classes.firstOrNull { it.endMs > now }
    val tz = TimeZone.getDefault().let { z ->
        val off = z.getOffset(now) / 60000
        "UTC" + (if (off >= 0) "+" else "-") + "%d".format(kotlin.math.abs(off) / 60) + (if (off % 60 != 0) ":%02d".format(kotlin.math.abs(off) % 60) else "")
    }

    HeroPage(
        heroColor = P.sage,
        refreshing = s.refreshing,
        onRefresh = onRefresh,
        banner = banner,
        hero = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T(if (sameDay(day, now)) "TODAY'S\nTIMING" else SimpleDateFormat("EEEE\nd MMM", Locale.getDefault()).format(Date(day)).uppercase(),
                    Type.display, color = P.onHero, modifier = Modifier.weight(1f))
                MenuButton()
            }
            Box(Modifier.fillMaxWidth().padding(top = 18.dp), contentAlignment = Alignment.Center) {
                ClockDial(classes.map { Triple(it.startMs, it.endMs, it == hot) }, Modifier.fillMaxWidth(0.78f).aspectRatio(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(P.onHero.copy(alpha = 0.08f), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 8.dp)) { T(tz, Type.label, color = P.onHero) }
                Box(Modifier.weight(1f))
                Box(Modifier.background(P.onHero.copy(alpha = 0.08f), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                    T(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(now)), Type.label, color = P.onHero)
                }
            }
        },
    ) {
        Segmented(listOf("classes" to "Timetable", "exams" to (if (exams.isNotEmpty()) "Exams · ${exams.size}" else "Exams")), tab, { tab = it })
        AnimatedContent(tab, transitionSpec = { fadeIn(tween(280, easing = Motion.emphasized)) togetherWith fadeOut(tween(140)) }, label = "tab") { t ->
            Column(verticalArrangement = Arrangement.spacedBy(L.block)) {
                if (t == "classes") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val f = SimpleDateFormat("EEE d", Locale.getDefault())
                        days.forEachIndexed { i, d ->
                            val active = sameDay(d, day)
                            val n = s.data?.schedule.orEmpty().count { sameDay(it.startMs, d) }
                            Row(
                                Modifier.press({ day = d }, if (active) Feel.None else Feel.Select, 0.94f)
                                    .background(if (active) P.cream else Color.Transparent, RoundedCornerShape(50))
                                    .border(1.dp, if (active) P.cream else P.line, RoundedCornerShape(50))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Caps(if (i == 0) "Today" else f.format(Date(d)), if (active) P.onCream else P.text)
                                if (n > 0) Box(Modifier.padding(start = 8.dp).size(6.dp).background(if (active) P.coral else P.sub, CircleShape))
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Upcoming", Tone.Ink, P.coral)
                        Chip("Done", Tone.Ink, P.sage)
                    }
                    if (classes.isEmpty()) T("No classes on this day.", Type.body, sub = true, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 32.dp))
                    var nowShown = !sameDay(day, now)
                    classes.forEachIndexed { i, c ->
                        if (!nowShown && c.startMs > now) { nowShown = true; NowLine(now) }
                        Rise(i) { ClassRow(c) }
                    }
                    if (!nowShown && classes.isNotEmpty()) NowLine(now)
                } else {
                    s.data?.exams?.title?.let { T(it.uppercase(), Type.caption, sub = true) }
                    if (exams.isEmpty()) T("No exams scheduled right now.", Type.body, sub = true, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 32.dp))
                    exams.forEachIndexed { i, e ->
                        val past = (e.timeMs ?: Long.MAX_VALUE) + com.student.amizoneplus.logic.EXAM_LENGTH_MS < now
                        val live = e.timeMs?.let { now >= it && !past } == true
                        val left = e.timeMs?.let { com.student.amizoneplus.logic.daysUntil(it, now) }
                        Rise(i) {
                            Card(color = P.card) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column(
                                        Modifier.width(66.dp).background(if (past) P.cardRaised else P.coral, RoundedCornerShape(20.dp)).padding(vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Caps(e.timeMs?.let { SimpleDateFormat("MMM", Locale.getDefault()).format(Date(it)).uppercase() } ?: "—", if (past) P.sub else P.onHero)
                                        T(e.timeMs?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.DAY_OF_MONTH).toString() } ?: "?",
                                            Type.num.copy(fontSize = 38.sp, lineHeight = 38.sp), color = if (past) P.sub else P.onHero)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        T(e.name.uppercase(), Type.title, maxLines = 2, faint = past)
                                        T(e.code + (e.timeMs?.let { " · " + SimpleDateFormat("EEE h:mm a", Locale.getDefault()).format(Date(it)) } ?: ""),
                                            Type.caption, sub = true, modifier = Modifier.padding(top = 4.dp))
                                        if (e.location.isNotEmpty()) T(e.location, Type.caption, sub = true, modifier = Modifier.padding(top = 2.dp))
                                        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (e.mode.isNotEmpty()) Chip(e.mode, Tone.Lav)
                                            if (!past && left != null) Chip(when { live -> "Now"; left <= 0 -> "Today"; left == 1 -> "Tomorrow"; else -> "In $left days" },
                                                if (left <= 3) Tone.Bad else Tone.Sage)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The reference's "now" marker: a sky time chip with a line across. */
@Composable
private fun NowLine(now: Long) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.background(P.sky, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp)) { T(hm(now), Type.caption, color = P.onHero) }
        Box(Modifier.weight(1f).height(1.5.dp).background(P.sky.copy(alpha = 0.6f)))
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/screens/LmsScreen.kt`

```kotlin
package com.student.amizoneplus.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.amizoneplus.data.LmsCourse
import com.student.amizoneplus.data.LmsFile
import com.student.amizoneplus.data.LmsItem
import com.student.amizoneplus.data.LmsSection
import com.student.amizoneplus.data.LmsSemester
import com.student.amizoneplus.data.Lms
import com.student.amizoneplus.ui.AppViewModel
import com.student.amizoneplus.ui.UiState
import com.student.amizoneplus.ui.kit.Caps
import com.student.amizoneplus.ui.kit.Chip
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.OutlineRow
import com.student.amizoneplus.ui.kit.Rise
import com.student.amizoneplus.ui.kit.SectionLabel
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.Tone
import com.student.amizoneplus.ui.kit.UnfoldCard
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.shell.HeroPage
import com.student.amizoneplus.ui.kit.StatTile
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.coroutines.launch

private fun modIcon(m: String): ImageVector = when (m) {
    "resource" -> Icons.AutoMirrored.Outlined.InsertDriveFile
    "folder" -> Icons.Outlined.Folder
    "url" -> Icons.Outlined.Link
    "page" -> Icons.Outlined.Description
    "book" -> Icons.Outlined.Book
    "forum" -> Icons.Outlined.Forum
    "assign" -> Icons.Outlined.Assignment
    "quiz" -> Icons.Outlined.HelpOutline
    else -> Icons.Outlined.ViewInAr
}

private fun fileIcon(name: String): ImageVector {
    val e = name.substringAfterLast('.', "").lowercase()
    return when {
        e == "pdf" -> Icons.Outlined.Description
        e.startsWith("ppt") -> Icons.Outlined.Slideshow
        e.startsWith("xls") || e == "csv" -> Icons.Outlined.GridOn
        e in setOf("zip", "rar", "7z") -> Icons.Outlined.Archive
        e in setOf("png", "jpg", "jpeg", "gif") -> Icons.Outlined.Image
        e in setOf("mp4", "mkv", "mov") -> Icons.Outlined.Videocam
        else -> Icons.AutoMirrored.Outlined.InsertDriveFile
    }
}

private fun labelFor(m: String) = mapOf("resource" to "File", "url" to "Link", "page" to "Page", "book" to "Book", "forum" to "Forum",
    "assign" to "Assignment", "quiz" to "Quiz", "lesson" to "Lesson")[m] ?: m.replaceFirstChar { it.uppercase() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LmsScreen(vm: AppViewModel, s: UiState, focusCode: String?, onFocusHandled: () -> Unit, openWeb: (String, String) -> Unit) {
    val lms = vm.repo.lms
    val storage = vm.repo.storage
    val context = LocalContext.current
    val haptics = LocalHaptics.current
    val scope = rememberCoroutineScope()

    var sems by remember { mutableStateOf(listOf<LmsSemester>()) }
    var semIdx by remember { mutableIntStateOf(0) }
    var courses by remember { mutableStateOf<List<LmsCourse>?>(null) }
    var course by remember { mutableStateOf<LmsCourse?>(null) }
    var content by remember { mutableStateOf<List<LmsSection>?>(null) }
    val folders = remember { mutableStateMapOf<String, List<LmsFile>?>() } // null = loading
    var busy by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val sem = sems.getOrNull(semIdx)
    fun key(k: String) = "lms2/${sem?.label?.replace(Regex("[^A-Za-z0-9]"), "_")}_$k.json"

    LaunchedEffect(Unit) {
        try { sems = lms.semesters() } catch (e: Exception) { error = e.message ?: "Could not reach Amizone." }
    }

    suspend fun loadCourses(user: Boolean) {
        val s0 = sem ?: return
        courses = storage.readJson(key("courses"), ListSerializer(LmsCourse.serializer())) ?: courses
        loading = true
        try {
            val c = lms.courses(s0); courses = c; storage.writeJson(key("courses"), ListSerializer(LmsCourse.serializer()), c); error = null
            if (user) haptics.success()
        } catch (e: Exception) { error = e.message ?: "LMS unavailable."; if (user) haptics.error() } finally { loading = false }
    }

    suspend fun openCourse(c: LmsCourse, user: Boolean) {
        val s0 = sem ?: return
        course = c; folders.clear()
        content = storage.readJson(key("c${c.id}"), ListSerializer(LmsSection.serializer()))
        loading = true
        try {
            val x = lms.content(s0, c.id); content = x; storage.writeJson(key("c${c.id}"), ListSerializer(LmsSection.serializer()), x); error = null
            if (user) haptics.success()
        } catch (e: Exception) { error = e.message ?: "Could not load this course."; if (user) haptics.error() } finally { loading = false }
    }

    LaunchedEffect(sem?.label) { course = null; courses = null; loadCourses(false) }
    LaunchedEffect(focusCode, courses) {
        val code = focusCode ?: return@LaunchedEffect
        val match = courses?.firstOrNull { it.code.equals(code, true) } ?: return@LaunchedEffect
        openCourse(match, false); onFocusHandled()
    }
    if (course != null) BackHandler { course = null; content = null }

    fun subjectName(c: LmsCourse) = s.data?.courses?.firstOrNull { it.code.equals(c.code, true) }?.name ?: c.fullname

    fun toggleFolder(item: LmsItem) {
        val s0 = sem ?: return
        if (folders.containsKey(item.id)) { haptics.collapse(); folders.remove(item.id); return }
        haptics.expand()
        folders[item.id] = storage.readJson(key("f${item.id}"), ListSerializer(LmsFile.serializer()))
        scope.launch {
            try {
                val f = lms.folderFiles(s0, item); folders[item.id] = f; storage.writeJson(key("f${item.id}"), ListSerializer(LmsFile.serializer()), f)
            } catch (e: Exception) { haptics.error(); if (folders[item.id] == null) folders[item.id] = emptyList() }
        }
    }

    fun open(url: String, name: String, modname: String = "resource") {
        val s0 = sem ?: return
        if (busy != null) return
        haptics.press()
        if (modname in setOf("forum", "assign", "quiz", "page", "book", "lesson", "h5pactivity", "scorm")) { openWeb(url, name); return }
        busy = url
        scope.launch {
            try {
                when (val r = lms.open(context, s0, url, name)) {
                    is Lms.OpenResult.Web -> if (modname == "url") context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.url))) else openWeb(r.url, name)
                    Lms.OpenResult.Opened -> haptics.success()
                }
            } catch (e: Exception) { haptics.error(); error = e.message ?: "Couldn't open that file." } finally { busy = null }
        }
    }

    AnimatedContent(
        course,
        transitionSpec = {
            (fadeIn(tween(300, easing = Motion.emphasized)) + slideInHorizontally(tween(420, easing = Motion.emphasized)) { if (targetState != null) it / 8 else -it / 8 }) togetherWith fadeOut(tween(120))
        },
        label = "lms",
    ) { current ->
        HeroPage(
            heroColor = P.sky,
            refreshing = loading,
            onRefresh = { scope.launch { if (current != null) openCourse(current, true) else loadCourses(true) } },
            hero = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (current != null) com.student.amizoneplus.ui.kit.CircleButton(Icons.AutoMirrored.Outlined.ArrowBack, { course = null; content = null }, 48.dp)
                    T(if (current != null) "COURSE" else "LMS", Type.display, color = P.onHero, modifier = Modifier.weight(1f))
                    if (current == null && sems.size > 1) com.student.amizoneplus.ui.kit.DropdownPill(
                        sems.getOrNull(semIdx)?.label?.substringBefore(" ")?.ifEmpty { null } ?: (sems.getOrNull(semIdx)?.label ?: "semester"),
                        { semIdx = (semIdx + 1) % sems.size },
                    )
                    com.student.amizoneplus.ui.shell.MenuButton()
                }
                if (current != null) {
                    T(subjectName(current).uppercase(), Type.display, color = P.onHero, maxLines = 3, modifier = Modifier.padding(top = 16.dp))
                    Box(Modifier.padding(top = 10.dp)) { Chip(current.code.ifEmpty { current.shortname }, Tone.Cream) }
                } else {
                    T("Slides, notes and assignments from Amizone LMS — arranged the way each teacher uploaded them.", Type.body, color = P.onHero.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 14.dp))
                    Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("${courses?.size ?: 0}", "courses", P.onHero.copy(alpha = 0.08f))
                        StatTile("${s.data?.notices?.size ?: 0}", "documents", P.onHero.copy(alpha = 0.08f))
                    }
                }
            },
        ) {
            error?.let { Box(Modifier.fillMaxWidth().background(P.badSoft, RoundedCornerShape(22.dp)).padding(16.dp)) { T(it, Type.caption) } }
            if (current != null) {
                val c = content
                if (c == null) Spinner()
                else if (c.all { it.items.isEmpty() }) Empty("Nothing has been uploaded for this subject yet.")
                var idx = 0
                c?.forEachIndexed { si, sec ->
                    if (sec.items.isEmpty()) return@forEachIndexed
                    Rise(idx++) { SectionLabel(sec.title.ifEmpty { "Section ${si + 1}" }) }
                    sec.items.forEach { it ->
                        Rise(idx++) {
                            when (it.modname) {
                                "label" -> T(it.name, Type.body, sub = true, modifier = Modifier.padding(horizontal = 20.dp))
                                "folder" -> FolderBlock(it, folders.containsKey(it.id), folders[it.id], { toggleFolder(it) }, { f -> open(f.url, f.name) }, busy)
                                else -> OutlineRow(it.name, labelFor(it.modname), { open(lms.itemUrl(it), it.name, it.modname) }) {
                                    if (busy == lms.itemUrl(it)) CircularProgressIndicator(color = P.ink, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    else Icon(modIcon(it.modname), null, tint = P.text)
                                }
                            }
                        }
                    }
                }
            } else {
                val list = courses
                if (list == null) Spinner() else if (list.isEmpty()) Empty("No LMS courses for this semester.")
                list?.forEachIndexed { i, c ->
                    Rise(i) {
                        com.student.amizoneplus.ui.kit.Card(onClick = { scope.launch { openCourse(c, false) } }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                com.student.amizoneplus.ui.kit.Avatar(c.code.ifEmpty { c.fullname }, listOf(P.sky, P.sage, P.sun, P.lilac)[i % 4], 52.dp)
                                Column(Modifier.weight(1f)) {
                                    T(subjectName(c).uppercase(), Type.title, maxLines = 2)
                                    T(c.code.ifEmpty { c.shortname }, Type.caption, sub = true, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                                }
                                com.student.amizoneplus.ui.kit.ArrowButton({ scope.launch { openCourse(c, false) } })
                            }
                        }
                    }
                }
                val notices = s.data?.notices.orEmpty()
                if (notices.isNotEmpty()) {
                    Rise((list?.size ?: 0) + 1) { SectionLabel("University documents") }
                    notices.forEachIndexed { i, d ->
                        Rise((list?.size ?: 0) + 2 + i) {
                            OutlineRow(d.title, d.description, { haptics.tap(); context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(d.url))) }) {
                                Icon(Icons.Outlined.Download, null, tint = P.text)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderBlock(item: LmsItem, open: Boolean, files: List<LmsFile>?, onToggle: () -> Unit, onOpen: (LmsFile) -> Unit, busy: String?) {
    UnfoldCard(
        open = open,
        onToggle = onToggle,
        head = { onInk ->
            Icon(if (onInk) Icons.Outlined.Folder else Icons.Outlined.Folder, null, tint = if (onInk) P.lav else P.text, modifier = Modifier.size(22.dp))
            T(item.name, Type.label, ink = onInk, maxLines = 2, modifier = Modifier.weight(1f))
            if (!onInk && files != null) T("${files.size}", Type.caption, sub = true)
        },
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            when {
                files == null -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P.lav) }
                files.isEmpty() -> T("This folder is empty.", Type.body, ink = true, sub = true, modifier = Modifier.padding(vertical = 12.dp))
                else -> files.groupBy { it.path }.forEach { (path, list) ->
                    if (path.isNotEmpty()) Caps(path, P.onInkFaint, Modifier.padding(top = 12.dp, bottom = 4.dp))
                    list.forEach { f ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 60.dp).press({ onOpen(f) }, Feel.None, 0.98f).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(fileIcon(f.name), null, tint = P.onInkSub, modifier = Modifier.size(24.dp))
                            T(f.name.substringBeforeLast('.'), Type.body, ink = true, maxLines = 2, modifier = Modifier.weight(1f))
                            if (busy == f.url) CircularProgressIndicator(color = P.lav, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else f.name.substringAfterLast('.', "").takeIf { it.isNotEmpty() && it.length <= 5 }?.let { Chip(it.uppercase(), Tone.Ghost) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Empty(text: String) {
    val wink = androidx.compose.runtime.remember { com.student.amizoneplus.ui.kit.Quips.empty() }
    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        T(text, Type.body, sub = true, align = TextAlign.Center)
        T(wink, Type.caption, faint = true, align = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/screens/MoreScreen.kt`

```kotlin
﻿package com.student.amizoneplus.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.student.amizoneplus.data.Settings
import com.student.amizoneplus.ui.UiState
import com.student.amizoneplus.ui.kit.Btn
import com.student.amizoneplus.ui.kit.BtnKind
import com.student.amizoneplus.ui.kit.Caps
import com.student.amizoneplus.ui.kit.CountUp
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.Fused
import com.student.amizoneplus.ui.kit.Ink
import com.student.amizoneplus.ui.kit.PairCards
import com.student.amizoneplus.ui.kit.Rise
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.shell.HeroPage
import com.student.amizoneplus.ui.kit.StatTile
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type

@Composable
private fun SettingRow(icon: ImageVector, label: String, danger: Boolean = false, onClick: (() -> Unit)? = null, right: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).press(onClick, Feel.Tap, 0.98f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = if (danger) P.badOnInk else P.onInkSub, modifier = Modifier.size(22.dp))
        T(label, Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), ink = true, color = if (danger) P.badOnInk else null, modifier = Modifier.weight(1f))
        if (right != null) right() else if (onClick != null) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = P.onInkFaint)
    }
}

@Composable
private fun Toggle(value: Boolean, onChange: (Boolean) -> Unit) {
    val haptics = LocalHaptics.current
    Switch(
        value, { haptics.select(); onChange(it) },
        colors = SwitchDefaults.colors(checkedTrackColor = P.lav, checkedThumbColor = P.ink, uncheckedTrackColor = P.inkRaised, uncheckedThumbColor = P.onInkSub, uncheckedBorderColor = Color.Transparent),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Choice(values: List<Int>, value: Int, fmt: (Int) -> String, onChange: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { v ->
            Box(Modifier.press({ onChange(v) }, Feel.Select, 0.92f).background(if (v == value) P.lav else P.inkRaised, RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 11.dp)) {
                Caps(fmt(v), if (v == value) P.ink else P.onInkSub)
            }
        }
    }
}

@Composable
fun MoreScreen(
    s: UiState,
    onRefresh: () -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    onBadges: () -> Unit,
    openWeb: (String, String) -> Unit,
    signOut: () -> Unit,
    banner: (@Composable () -> Unit)?,
    focusResults: Boolean = false,
    onFocusHandled: () -> Unit = {},
    checkUpdates: () -> Unit = {},
) {
    val haptics = LocalHaptics.current
    var results by remember { mutableStateOf(focusResults) }
    androidx.compose.runtime.LaunchedEffect(focusResults) { if (focusResults) { results = true; onFocusHandled() } }
    var confirmOut by remember { mutableStateOf(false) }
    val p = s.data?.profile
    val sems = s.data?.results?.semesters.orEmpty()
    val latest = sems.lastOrNull()
    val st = s.settings

    HeroPage(
        heroColor = P.sun,
        refreshing = s.refreshing,
        onRefresh = onRefresh,
        banner = banner,
        hero = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.student.amizoneplus.ui.kit.Avatar(p?.name ?: s.username ?: "?", P.cream, 56.dp)
                Box(Modifier.weight(1f))
                com.student.amizoneplus.ui.shell.MenuButton()
            }
            T((p?.name ?: s.username ?: "").uppercase(), Type.display, color = P.onHero, maxLines = 2, modifier = Modifier.padding(top = 18.dp))
            T(listOfNotNull(p?.program?.ifEmpty { null }, p?.batch?.ifEmpty { null }, p?.enrollmentNo?.ifEmpty { null } ?: s.username).joinToString(" · "),
                Type.body, color = P.onHero.copy(alpha = 0.75f), modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(latest?.let { "%.2f".format(it.cgpa) } ?: "—", "CGPA", P.onHero.copy(alpha = 0.08f), onClick = { results = true })
                StatTile(latest?.let { "%.2f".format(it.sgpa) } ?: "—", "SGPA", P.onHero.copy(alpha = 0.08f), onClick = { results = true })
            }
        },
    ) {
        Rise(1) {
            Ink(padding = 20.dp) {
                SettingRow(Icons.Outlined.MilitaryTech, "Badges", onClick = onBadges)
                SettingRow(Icons.Outlined.School, "Results by subject", onClick = { results = true })
                SettingRow(Icons.Outlined.Language, "Open Amizone website", onClick = { openWeb(com.student.amizoneplus.data.AmizoneClient.BASE + "/Home", "Amizone") })
            }
        }
        Rise(2) {
            Ink {
                Caps("Feel", P.onInkFaint, Modifier.padding(bottom = 4.dp))
                SettingRow(Icons.Outlined.Vibration, "Haptic feedback", right = { Toggle(st.haptics) { v -> updateSettings { it.copy(haptics = v) } } })
                Caps("Notifications", P.onInkFaint, Modifier.padding(top = 20.dp, bottom = 4.dp))
                SettingRow(Icons.Outlined.ErrorOutline, "Low attendance alerts", right = { Toggle(st.lowAttendanceAlerts) { v -> updateSettings { it.copy(lowAttendanceAlerts = v) } } })
                SettingRow(Icons.Outlined.Alarm, "Class reminders", right = { Toggle(st.classReminders) { v -> updateSettings { it.copy(classReminders = v) } } })
                if (st.classReminders) Box(Modifier.padding(start = 38.dp, bottom = 8.dp)) {
                    Choice(listOf(5, 10, 15, 30), st.reminderMinutes, { "$it min" }) { v -> updateSettings { it.copy(reminderMinutes = v) } }
                }
                SettingRow(Icons.Outlined.Description, "Exam reminders", right = { Toggle(st.examReminders) { v -> updateSettings { it.copy(examReminders = v) } } })
                Caps("Attendance target", P.onInkFaint, Modifier.padding(top = 20.dp, bottom = 12.dp))
                Choice(com.student.amizoneplus.logic.TARGETS, st.threshold, { "$it%" }) { v -> updateSettings { it.copy(threshold = v) } }
                Caps("Pets", P.onInkFaint, Modifier.padding(top = 20.dp, bottom = 12.dp))
                val petOpts = listOf("cat", "dog", "both", "none")
                Choice(petOpts.indices.toList(), petOpts.indexOf(st.pets).coerceAtLeast(0),
                    { listOf("Cat", "Dog", "Both", "Off")[it] }) { i -> updateSettings { it.copy(pets = petOpts[i]) } }
                T("Tap to pet. Drag to pick up, throw, or drop onto anything. Hold one over a tab to take it there.",
                    Type.caption, ink = true, sub = true, modifier = Modifier.padding(top = 10.dp))
            }
        }
        Rise(3) {
            Ink(padding = 20.dp) { SettingRow(Icons.AutoMirrored.Outlined.Logout, "Sign out", danger = true, onClick = { haptics.warning(); confirmOut = true }) }
        }
        Rise(4) { AboutCard(checkUpdates) }
    }

    if (confirmOut) AlertDialog(
        onDismissRequest = { confirmOut = false },
        title = { Text("Sign out?") },
        text = { Text("This removes your saved login and cached data from this phone.") },
        confirmButton = { TextButton({ confirmOut = false; signOut() }) { Text("Sign out", color = P.bad) } },
        dismissButton = { TextButton({ confirmOut = false }) { Text("Cancel") } },
    )

    if (results) ResultsSheet(s) { results = false }
}

@Composable
private fun AboutCard(checkUpdates: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val haptics = LocalHaptics.current
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "2" }
    var taps by remember { mutableStateOf(0) }
    Ink {
        Caps("About", P.onInkFaint, Modifier.padding(bottom = 14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).background(P.sun, RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                T("A+", Type.title, color = P.onHero)
            }
            Column(Modifier.weight(1f).press({
                taps++
                haptics.select()
                val msg = when {
                    taps >= 7 -> "You are now a developer. Just kidding. Shynxo is the developer. 🛠️"
                    taps >= 4 -> "${7 - taps} more…"
                    else -> null
                }
                if (msg != null) android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
                if (taps >= 7) taps = 0
            }, Feel.Select, 0.97f)) {
                T("AMIZONE+", Type.title, ink = true)
                T("Version $version · native", Type.caption, ink = true, sub = true)
            }
        }
        T("A faster, friendlier window into Amizone. Your login and data stay on this phone; the app only ever talks to Amizone and its LMS.",
            Type.body, ink = true, sub = true, modifier = Modifier.padding(top = 16.dp))
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp).press(checkUpdates, Feel.Tap, 0.98f)
                .background(P.cardRaised, RoundedCornerShape(50)).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(androidx.compose.material.icons.Icons.Outlined.SystemUpdate, null, tint = P.text, modifier = Modifier.size(20.dp))
            T("Check for updates", Type.label, modifier = Modifier.weight(1f).padding(start = 12.dp))
            T("v$version", Type.caption, sub = true)
        }
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            T("Type: Barlow Condensed & DM Mono (SIL Open Font License)", Type.caption, ink = true, sub = true)
            T("Unofficial app · not affiliated with Amity University", Type.caption, ink = true, sub = true)
            T("No ads. No trackers. Just vibes.", Type.caption, ink = true, sub = true)
        }
        Box(Modifier.fillMaxWidth().padding(top = 22.dp).height(1.dp).background(P.line))
        T("Brought to existence by Shynxo", Type.headline.copy(fontSize = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)),
            ink = true, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 20.dp))
        T("✦", Type.caption, ink = true, sub = true, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
    }
}

@Composable
private fun ResultsSheet(s: UiState, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val sems = s.data?.results?.semesters.orEmpty()
    Column(
        Modifier.fillMaxSize().background(P.canvas).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(L.page),
        verticalArrangement = Arrangement.spacedBy(L.block),
    ) {
        Fused {
            Ink {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T("Results", Type.title, ink = true, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.Close, null, tint = P.onInk, modifier = Modifier.size(26.dp).press(onClose))
                }
            }
            Ink {
                T("Semester-wise", Type.label, ink = true)
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (sems.isEmpty()) T("No results published yet.", Type.body, ink = true, sub = true)
                    sems.forEachIndexed { i, x ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            T(x.semester.ifEmpty { "Semester ${i + 1}" }, Type.body, ink = true, sub = true, modifier = Modifier.weight(1f))
                            T("SGPA %.2f".format(x.sgpa), Type.caption, ink = true, sub = true, modifier = Modifier.widthIn(min = 82.dp))
                            T("CGPA %.2f".format(x.cgpa), Type.label, ink = true, align = TextAlign.End, modifier = Modifier.widthIn(min = 90.dp))
                        }
                    }
                }
            }
        }
        s.data?.results?.courses.orEmpty().forEachIndexed { i, c ->
            Rise(i) {
                Row(
                    Modifier.fillMaxWidth().border(1.2.dp, P.line, RoundedCornerShape(L.radius)).padding(horizontal = L.pad, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        T(c.name, Type.label, maxLines = 2)
                        T("${c.code} · ${c.credits.toInt()} credits · GP ${c.gradePoint.toInt()}", Type.caption, sub = true)
                    }
                    Box(Modifier.widthIn(min = 58.dp).background(P.ink, RoundedCornerShape(16.dp)).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        T(c.grade.ifEmpty { "—" }, Type.numSm, ink = true)
                    }
                }
            }
        }
        Row { Btn("Close", BtnKind.Ink, onClick = onClose) }
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/screens/QuickMenu.kt`

```kotlin
package com.student.amizoneplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.UnfoldCard
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.shell.HeaderInk
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlin.math.pow
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.core.tween
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

data class MenuItem(val label: String, val path: String, val icon: ImageVector)
data class MenuGroup(val title: String, val icon: ImageVector, val items: List<MenuItem>)

val MENU_GROUPS = listOf(
    MenuGroup("Academics", Icons.Outlined.School, listOf(
        MenuItem("My Faculty & Feedback", "/FacultyFeeback/FacultyFeedback", Icons.Outlined.People),
        MenuItem("Apply for OD", "/Academics/ODApplication/Apply", Icons.Outlined.Flight),
        MenuItem("Time Table", "/TimeTable/Home", Icons.Outlined.GridView),
        MenuItem("My Calendar", "/Calendar/Home", Icons.Outlined.CalendarMonth),
        MenuItem("Programme Structure", "/Academics/ProgrammeStructure", Icons.Outlined.AccountTree),
        MenuItem("My Mentors", "/FacultyFeeback/MyMentors", Icons.Outlined.SupervisorAccount),
        MenuItem("Off-campus Classes", "/TimeTable/LocationAttendance/Home", Icons.Outlined.Place),
        MenuItem("Discipline Registration", "/Electives/Specialization", Icons.Outlined.WorkspacePremium),
        MenuItem("NPTEL / Swayam", "/Electives/NPTELSwayam", Icons.Outlined.Public),
        MenuItem("Value Added Courses", "/ValueAdditionProgramme/VAP/SelectVAP", Icons.Outlined.AddCircleOutline),
    )),
    MenuGroup("Examination", Icons.Outlined.Description, listOf(
        MenuItem("Results", "/Examination/Examination", Icons.Outlined.Leaderboard),
        MenuItem("Exam Schedule", "/Examination/ExamSchedule", Icons.Outlined.Schedule),
        MenuItem("Admit Card", "/Examination/AdmitCard", Icons.Outlined.Badge),
        MenuItem("Examination Form", "/Examination/ExaminationForm", Icons.Outlined.EditNote),
        MenuItem("Apply for Rechecking", "/Examination/Rechecking", Icons.Outlined.Replay),
        MenuItem("Supplementary Exam", "/Examination/SuppExam", Icons.Outlined.Article),
        MenuItem("Back Paper Form", "/Examination/SpecialExam", Icons.Outlined.FileCopy),
        MenuItem("Improvement Form", "/Examination/Improvement", Icons.Outlined.TrendingUp),
        MenuItem("Assignment for Absence", "/Examination/StudentAssignment", Icons.Outlined.Assignment),
    )),
    MenuGroup("Campus", Icons.Outlined.Apartment, listOf(
        MenuItem("Register for Wi-Fi", "/RegisterForWifi/mac", Icons.Outlined.Wifi),
        MenuItem("ID Card / Entry Pass", "/IDCard", Icons.Outlined.Badge),
        MenuItem("Library Dues", "/Extra/LibraryDue", Icons.Outlined.LocalLibrary),
        MenuItem("Hostel", "/AmityHostel/HostelPage", Icons.Outlined.Hotel),
        MenuItem("Bus Pass", "/BusPass/BRegistration", Icons.Outlined.DirectionsBus),
        MenuItem("Cafeteria Menu", "/Extra/Cafeteria/Menu", Icons.Outlined.Restaurant),
        MenuItem("Clubs & Committees", "/ClubCommittee/ClubCommittee", Icons.Outlined.Groups),
        MenuItem("Student Support", "/SuggestionBox/SuggestionBox", Icons.Outlined.SupportAgent),
        MenuItem("Lost & Found", "/Academics/Lost_Found", Icons.Outlined.Search),
        MenuItem("Parking Application", "/Survey/ParkingApplicationform/Home", Icons.Outlined.DirectionsCar),
    )),
    MenuGroup("Fees & documents", Icons.Outlined.AccountBalanceWallet, listOf(
        MenuItem("Fee Details", "/FeeStructure/FeeStructure", Icons.Outlined.AccountBalanceWallet),
        MenuItem("Document Downloads", "/StudentDocument/StudentDocument", Icons.Outlined.Download),
        MenuItem("Apply for Transcript", "/Transcripts/Transcripts", Icons.Outlined.ReceiptLong),
        MenuItem("ABC ID", "/AcademicData/Acca/StudentABCIDDetails", Icons.Outlined.Fingerprint),
        MenuItem("Achievements", "/AcademicData/Achievements", Icons.Outlined.EmojiEvents),
    )),
    MenuGroup("Placement & internships", Icons.Outlined.Work, listOf(
        MenuItem("Placement", "/Placement/PlacementDetails", Icons.Outlined.Work),
        MenuItem("Upload Resume", "/Placement/PlacementDetails/UploadResume", Icons.Outlined.CloudUpload),
        MenuItem("Internship", "/Placement/PlacementDetails/IntrenshipIndex", Icons.Outlined.Construction),
        MenuItem("NTCC Internship", "/NTCC/NTCC/IntershipRFNABS", Icons.Outlined.Handyman),
        MenuItem("Corporate Events", "/Placement/CorporatEvent", Icons.Outlined.Event),
    )),
)

/* Timing (ms), matching the reference: rows unfurl 55 ms apart. */
const val ROW_DUR = 340
const val ROW_STAGGER = 40
val MENU_OPEN_MS = ROW_DUR + ROW_STAGGER * (MENU_GROUPS.size - 1) + 80
val MENU_CLOSE_MS = (MENU_OPEN_MS * 0.6f).toInt()

private fun easeOut(t: Float) = 1f - (1f - t).pow(4)

/** How far the screen underneath has slid up into the header (0..1), for a given menu progress. */
fun screenExit(m: Float) = (m * MENU_OPEN_MS / 300f).coerceIn(0f, 1f).let { easeOut(it) }

@Composable
fun QuickMenuOverlay(open: Boolean, fade: () -> Float, hazeState: dev.chrisbanes.haze.HazeState, onClose: () -> Unit, openPage: (String, String) -> Unit) {
    val haptics = LocalHaptics.current
    var openGroup by remember { mutableStateOf<String?>(null) }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val rowStep = 68.dp + L.block
    val density = androidx.compose.ui.platform.LocalDensity.current
    // Where the blur fades out: just below the last row — or the whole screen once a group is open.
    var rowsBottom by remember { mutableFloatStateOf(0f) }
    var screenH by remember { mutableFloatStateOf(0f) }
    val fadeLen = with(density) { 150.dp.toPx() }
    // The edge starts at the header and travels down with the rows as they unfurl.
    val headerBottom = with(density) { (statusTop + L.page + L.header).toPx() }
    // Each row has its own spring: in with a soft bounce, staggered top to bottom;
    // out quickly in reverse order with no bounce.
    val rows = remember { List(MENU_GROUPS.size) { androidx.compose.animation.core.Animatable(0f) } }
    LaunchedEffect(open) {
        rows.forEachIndexed { i, a ->
            launch {
                if (open) {
                    kotlinx.coroutines.delay(30L + i * 42L)
                    a.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.62f, stiffness = 320f))
                } else {
                    kotlinx.coroutines.delay((rows.size - 1 - i) * 22L)
                    a.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 900f))
                }
            }
        }
    }
    // The blur edge rides with the last row, so blur and options arrive together; opening
    // a group spreads it over the whole screen on a soft spring.
    val spread by androidx.compose.animation.core.animateFloatAsState(
        if (openGroup != null) 1f else 0f, androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 220f), label = "spread",
    )
    val lastRow = rows.last().value
    val restEdge = if (rowsBottom > 0f) rowsBottom + with(density) { 12.dp.toPx() } else headerBottom
    val rowsEdge = headerBottom + (restEdge - headerBottom) * lastRow.coerceAtLeast(0f)
    val edge = rowsEdge + ((screenH + fadeLen) - rowsEdge) * spread

    Box(Modifier.fillMaxSize()) {
        // The live app, blurred where the menu is: full strength down to the last row,
        // then fading out over ~150dp. Opening a group spreads it over the whole screen.
        // Blur and tint fade in and out with the menu on one smooth curve.
        Box(
            Modifier.fillMaxSize()
                .onSizeChanged { screenH = it.height.toFloat() }
                .graphicsLayer { alpha = fade() }
                .hazeBlur(
                    dev.chrisbanes.haze.HazeInput.Backdrop(hazeState),
                    HazeBlurStyle {
                        blurRadius(24.dp)
                        // A black tint (not the page colour) so the blur reads deep, not milky.
                        colorEffects(listOf(HazeColorEffect.tint(Color.Black.copy(alpha = if (P.cur.dark) 0.32f else 0.14f))))
                        progressive(dev.chrisbanes.haze.HazeProgressive.verticalGradient(
                            startY = edge, startIntensity = 1f, endY = edge + fadeLen, endIntensity = 0f,
                        ))
                    },
                )
                .drawBehind {
                    // A black gradient that follows the blur: darkest under the header,
                    // easing off down to the blur's edge, then gone with it.
                    val k = if (P.cur.dark) 1f else 0.45f
                    drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.62f * k),
                        (edge / size.height).coerceIn(0f, 1f) to Color.Black.copy(alpha = 0.38f * k),
                        ((edge + fadeLen) / size.height).coerceIn(0f, 1f) to Color.Transparent,
                    ))
                },
        )

        // Rows live in a clipped area that starts at the header's bottom edge,
        // so they appear to slide out from behind it.
        Box(Modifier.fillMaxSize().padding(top = statusTop + L.page + L.header).clipToBounds()) {
            Column(
                Modifier.fillMaxSize()
                    // Tap anywhere that isn't a card to close the menu.
                    .clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onClose() }
                    .verticalScroll(rememberScrollState())
                    .padding(top = L.joint, start = L.page, end = L.page, bottom = navBottom + 120.dp),
                verticalArrangement = Arrangement.spacedBy(L.block),
            ) {
                Column(
                    Modifier.onGloballyPositioned { rowsBottom = it.boundsInWindow().bottom },
                    verticalArrangement = Arrangement.spacedBy(L.block),
                ) {
                MENU_GROUPS.forEachIndexed { i, g ->
                    Box(Modifier.graphicsLayer {
                        val t = rows[i].value // springs past 1 a little: the bounce
                        alpha = (t * 2f).coerceIn(0f, 1f)
                        translationY = -(i + 1) * rowStep.toPx() * 0.9f * (1f - t)
                        val sc = 0.94f + 0.06f * t
                        scaleX = sc; scaleY = sc
                    }) {
                        UnfoldCard(
                            open = openGroup == g.title,
                            onToggle = {
                                if (openGroup == g.title) { haptics.collapse(); openGroup = null } else { haptics.expand(); openGroup = g.title }
                            },
                            head = { onInk ->
                                Icon(g.icon, null, tint = if (onInk) P.lav else P.text, modifier = Modifier.size(20.dp))
                                T(g.title, Type.label, ink = onInk, modifier = Modifier.weight(1f))
                                if (!onInk) T("${g.items.size}", Type.caption, sub = true)
                                Icon(if (onInk) Icons.Outlined.KeyboardArrowDown else Icons.AutoMirrored.Outlined.KeyboardArrowRight, null,
                                    tint = if (onInk) P.onInk else P.text)
                            },
                        ) {
                            Column(Modifier.padding(horizontal = L.pad, vertical = 8.dp)) {
                                g.items.forEachIndexed { j, it ->
                                    if (j > 0) HorizontalDivider(color = P.onInkFaint, thickness = 0.5.dp)
                                    Row(
                                        Modifier.fillMaxWidth().heightIn(min = 58.dp).press({ openPage(it.path, it.label) }, Feel.Tap),
                                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        Icon(it.icon, null, tint = P.lav, modifier = Modifier.size(21.dp))
                                        T(it.label, Type.body, ink = true, modifier = Modifier.weight(1f))
                                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = P.onInkFaint, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }

        // The real header card on top; swipe up on it to close.
        HeaderInk(
            Modifier.padding(top = statusTop + L.page, start = L.page, end = L.page)
                .graphicsLayer { alpha = fade().coerceIn(0f, 1f) }
                .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(onDragStart = { total = 0f }, onDragEnd = { if (total < -36.dp.toPx()) onClose() }) { _, dy -> total += dy }
            },
        )
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/screens/UpdateSheet.kt`

```kotlin
package com.student.amizoneplus.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.student.amizoneplus.data.Updater
import com.student.amizoneplus.ui.kit.Btn
import com.student.amizoneplus.ui.kit.BtnKind
import com.student.amizoneplus.ui.kit.Caps
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.coroutines.launch
import java.io.File

/**
 * "Update available" sheet: what's new, then Update → download (with progress) →
 * Android's installer. The first time, Android asks to allow installs from this app;
 * the sheet waits and carries on by itself once you come back.
 */
@Composable
fun UpdateSheet(rel: Updater.Release, onLater: () -> Unit) {
    val ctx = LocalContext.current
    val haptics = LocalHaptics.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(-1f) } // -1 idle, 0..1 downloading
    var file by remember { mutableStateOf<File?>(null) }
    var waitingPermission by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val enter = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(520, easing = Motion.emphasized)) }

    fun installNow(f: File) {
        if (!Updater.canInstall(ctx)) { waitingPermission = true; Updater.openInstallPermission(ctx); return }
        waitingPermission = false
        Updater.install(ctx, f)
    }

    fun start() {
        error = null
        file?.let { installNow(it); return }
        haptics.press()
        progress = 0f
        scope.launch {
            runCatching { Updater.download(ctx, rel) { progress = it } }
                .onSuccess { f -> file = f; progress = 1f; haptics.success(); installNow(f) }
                .onFailure { progress = -1f; error = it.message ?: "Download failed."; haptics.error() }
        }
    }

    // Back from the "allow installs" screen: carry on if it was granted.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(waitingPermission) {
        if (!waitingPermission) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (Updater.canInstall(ctx)) file?.let { installNow(it) }
        }
    }

    val busy = progress in 0f..0.999f
    BackHandler(enabled = !busy, onBack = onLater)
    val shown by animateFloatAsState(if (busy) 1f else 0f, tween(260), label = "bar")

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = enter.value }
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(remember { MutableInteractionSource() }, indication = null) { if (!busy) onLater() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .graphicsLayer { translationY = (1f - enter.value) * 220.dp.toPx() }
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .background(P.card, RoundedCornerShape(topStart = L.sheetRadius, topEnd = L.sheetRadius))
                .navigationBarsPadding()
                .padding(horizontal = L.pad, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Caps("UPDATE AVAILABLE", P.coral)
            T("V${rel.version}", Type.display)
            T("You're on v${Updater.current}" + (if (rel.sizeBytes > 0) " · %.1f MB".format(rel.sizeBytes / 1_048_576f) else ""), Type.caption, sub = true)
            if (rel.notes.isNotBlank()) Column(
                Modifier.fillMaxWidth().heightIn(max = 220.dp).padding(top = 6.dp)
                    .background(P.cardRaised, RoundedCornerShape(20.dp)).verticalScroll(rememberScrollState()).padding(16.dp),
            ) {
                Caps("WHAT'S NEW", P.faint, Modifier.padding(bottom = 8.dp))
                T(rel.notes.replace("\r", ""), Type.body)
            }
            // download progress
            Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp).graphicsLayer { alpha = shown }
                .background(P.cardRaised, RoundedCornerShape(50))) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(6.dp).background(P.coral, RoundedCornerShape(50)))
            }
            val status = when {
                error != null -> error!!
                waitingPermission -> "Allow \"Install unknown apps\" for Amizone+, then come back — it continues by itself."
                busy -> "Downloading… ${(progress * 100).toInt()}%"
                file != null -> "Downloaded. Tap Install and confirm."
                else -> "Your login and settings stay as they are."
            }
            T(status, Type.caption, color = if (error != null) P.bad else P.sub)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn("Later", BtnKind.Ghost, null, if (busy) null else onLater)
                Btn(
                    when { file != null -> "Install"; busy -> "Downloading"; else -> "Update" },
                    BtnKind.Lav, if (file != null) Icons.Outlined.InstallMobile else Icons.Outlined.Download,
                    if (busy) null else ({ start() }),
                )
            }
        }
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/ui/AppRoot.kt`

```kotlin
package com.student.amizoneplus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.student.amizoneplus.data.AmizoneClient
import com.student.amizoneplus.logic.earnedBadges
import com.student.amizoneplus.ui.screens.AuthedWebPage
import com.student.amizoneplus.ui.screens.BadgeGallery
import com.student.amizoneplus.ui.screens.Celebration
import com.student.amizoneplus.ui.screens.LmsScreen
import com.student.amizoneplus.ui.screens.LoginScreen
import com.student.amizoneplus.ui.screens.MENU_CLOSE_MS
import com.student.amizoneplus.ui.screens.MENU_OPEN_MS
import com.student.amizoneplus.ui.screens.MoreScreen
import com.student.amizoneplus.ui.screens.QuickMenuOverlay
import com.student.amizoneplus.ui.screens.ScheduleScreen
import com.student.amizoneplus.ui.screens.SubjectsScreen
import com.student.amizoneplus.ui.screens.TodayScreen
import com.student.amizoneplus.ui.screens.WebLoginSheet
import com.student.amizoneplus.ui.screens.screenExit
import com.student.amizoneplus.ui.shell.ErrorBanner
import com.student.amizoneplus.ui.shell.LocalMenu
import com.student.amizoneplus.ui.shell.MenuController
import com.student.amizoneplus.ui.shell.BottomNav
import com.student.amizoneplus.ui.shell.Route
import com.student.amizoneplus.ui.theme.Haptics
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch

@Composable
fun AppRoot(vm: AppViewModel) {
    // Light/dark follows the phone; a change recreates the activity, so this runs first.
    com.student.amizoneplus.ui.theme.P.applyTheme(androidx.compose.foundation.isSystemInDarkTheme())
    val s by vm.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    val haptics = remember { Haptics(view) { vm.state.value.settings.haptics } }

    CompositionLocalProvider(LocalHaptics provides haptics) {
        LaunchedEffect(Unit) {
            vm.feedback.collect {
                when (it) {
                    Feedback.Success -> haptics.success()
                    Feedback.Warning -> haptics.warning()
                    Feedback.Error -> haptics.error()
                }
            }
        }

        Box(Modifier.fillMaxSize().background(P.canvas)) {
            when (s.status) {
                Status.Booting -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P.ink) }
                Status.SignedOut -> LoginScreen(vm, s.error)
                Status.SignedIn -> Main(vm, s)
            }
            if (s.webLoginOpen) {
                val c = vm.pendingCredentials()
                WebLoginSheet(c?.username, c?.password, onDone = vm::webLoginFinished, onClose = vm::closeWebLogin)
            }
        }
    }
}

@Composable
private fun Main(vm: AppViewModel, s: UiState) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHaptics.current
    var route by remember { mutableStateOf(Route.Today) }
    var lmsFocus by remember { mutableStateOf<String?>(null) }
    var web by remember { mutableStateOf<Pair<String, String>?>(null) }
    var gallery by remember { mutableStateOf(false) }
    var scheduleFocus by remember { mutableStateOf<String?>(null) }
    var resultsFocus by remember { mutableStateOf(false) }
    var lastWeb by remember { mutableStateOf<Pair<String, String>?>(null) }
    val hazeState = rememberHazeState()
    // The pets live across the whole app; this is their world.
    val pets = remember { com.student.amizoneplus.ui.pets.PetWorld() }
    pets.route = route

    // Quick menu: one progress value (0 = closed, 1 = open) drives the whole choreography.
    var menuMounted by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val m = remember { Animatable(0f) }
    // The screen's ease-back and the blur run on their own smooth curve (the rows' timer is linear).
    val z = remember { Animatable(0f) }
    // The menu blurs everything below it: screens, status bar and nav (with their own blurs).
    val menuHaze = rememberHazeState()
    // Backdrop (blur + slight ease-back of the screen) on one soft, no-overshoot spring;
    // the rows bounce in on their own springs inside the overlay.
    fun openMenu() {
        if (menuOpen) return
        haptics.expand(); menuMounted = true; menuOpen = true
        scope.launch { z.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 170f)) }
    }
    fun closeMenu(after: (() -> Unit)? = null) {
        if (!menuOpen) { after?.invoke(); return }
        haptics.collapse(); menuOpen = false
        scope.launch {
            // Rows fold back first (~0.3 s), the backdrop follows and settles; then unmount.
            kotlinx.coroutines.delay(120)
            z.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 260f))
            if (!menuOpen) menuMounted = false
            after?.invoke()
        }
    }
    BackHandler(enabled = menuOpen) { closeMenu() }

    fun go(r: Route) { if (menuOpen) closeMenu(); route = r }
    val openWeb: (String, String) -> Unit = { url, title -> web = url to title }

    val banner: (@Composable () -> Unit)? = s.error?.let { err ->
        { ErrorBanner(err, if (s.needsWebLogin) "Sign in →" else "Retry") { if (s.needsWebLogin) vm.openWebLogin() else vm.refresh(true) } }
    }

    pets.switchTab = { r -> go(r) }

    // Updates from GitHub Releases: checked quietly on launch (every few hours at most),
    // or on demand from More → About.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var update by remember { mutableStateOf<com.student.amizoneplus.data.Updater.Release?>(null) }
    LaunchedEffect(Unit) {
        val u = com.student.amizoneplus.data.Updater
        if (u.enabled && u.shouldAutoCheck(ctx)) {
            kotlinx.coroutines.delay(2500) // let the first refresh go first
            runCatching { u.check() }.onSuccess { r ->
                u.markChecked(ctx)
                if (r != null && !u.snoozed(ctx, r.version)) update = r
            }
        }
    }
    val checkUpdates: () -> Unit = {
        val u = com.student.amizoneplus.data.Updater
        if (!u.enabled) android.widget.Toast.makeText(ctx, "Updates aren't set up in this build.", android.widget.Toast.LENGTH_SHORT).show()
        else scope.launch {
            runCatching { u.check() }
                .onSuccess { r ->
                    u.markChecked(ctx)
                    if (r != null) update = r
                    else android.widget.Toast.makeText(ctx, "You're on the latest version (v${u.current}).", android.widget.Toast.LENGTH_SHORT).show()
                }
                .onFailure { android.widget.Toast.makeText(ctx, it.message ?: "Couldn't check for updates.", android.widget.Toast.LENGTH_SHORT).show() }
        }
    }
    CompositionLocalProvider(
        LocalMenu provides MenuController(menuOpen, ::openMenu) { closeMenu() },
        com.student.amizoneplus.ui.pets.LocalPetWorld provides pets,
    ) {
        Box(Modifier.fillMaxSize()) {
          // Everything here (screens + status bar + nav) is what the quick menu blurs.
          Box(Modifier.fillMaxSize().hazeSource(menuHaze)) {
            // Everything under the nav is a blur source for the nav's progressive blur.
            Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
                // Swipe left / right on any free space to move between tabs. Rows that scroll
                // sideways (day chips, the Timetable/Exams toggle, the nav) keep their own swipes.
                val swipe = remember { Animatable(0f) }
                val routeNow by androidx.compose.runtime.rememberUpdatedState(route)
                val swipeEnabled = !menuMounted && web == null && !gallery
                Box(Modifier.fillMaxSize()
                    .pointerInput(swipeEnabled) {
                        if (!swipeEnabled) return@pointerInput
                        var total = 0f
                        val threshold = 72.dp.toPx()
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragCancel = { scope.launch { swipe.animateTo(0f, androidx.compose.animation.core.spring(0.8f, 320f)) } },
                            onDragEnd = {
                                val tabs = Route.entries
                                val i = tabs.indexOf(routeNow)
                                val next = when {
                                    total < -threshold && i < tabs.lastIndex -> tabs[i + 1]
                                    total > threshold && i > 0 -> tabs[i - 1]
                                    else -> null
                                }
                                if (next != null) { haptics.select(); go(next) }
                                scope.launch { swipe.animateTo(0f, androidx.compose.animation.core.spring(0.8f, 320f)) }
                            },
                        ) { ch, dx ->
                            ch.consume()
                            total += dx
                            val i = Route.entries.indexOf(routeNow)
                            val atEdge = (i == 0 && total > 0) || (i == Route.entries.lastIndex && total < 0)
                            scope.launch { swipe.snapTo(total * (if (atEdge) 0.1f else 0.28f)) }
                        }
                    }
                    .graphicsLayer {
                        val sc = 1f - 0.03f * z.value
                        scaleX = sc; scaleY = sc
                        translationX = swipe.value
                    }) {
                    AnimatedContent(
                        route,
                        transitionSpec = {
                            // Slides the way the tabs are laid out: right-hand tabs come in from the right.
                            val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                            (fadeIn(tween(240, easing = Motion.emphasized)) + slideInHorizontally(tween(420, easing = Motion.emphasized)) { dir * it / 7 }) togetherWith
                                (fadeOut(tween(160)) + slideOutHorizontally(tween(320, easing = Motion.emphasized)) { -dir * it / 10 })
                        },
                        label = "route",
                    ) { r ->
                        CompositionLocalProvider(com.student.amizoneplus.ui.pets.LocalPetRoute provides r) {
                        when (r) {
                            Route.Today -> TodayScreen(s, ::go, { vm.refresh(true) }, { gallery = true }, banner)
                            Route.Subjects -> SubjectsScreen(s, { vm.refresh(true) }, { code -> lmsFocus = code; go(Route.Lms) }, vm::updateSettings, banner)
                            Route.Lms -> LmsScreen(vm, s, lmsFocus, { lmsFocus = null }, openWeb)
                            Route.Schedule -> ScheduleScreen(s, { vm.refresh(true) }, banner, scheduleFocus) { scheduleFocus = null }
                            Route.More -> MoreScreen(s, { vm.refresh(true) }, vm::updateSettings, { gallery = true }, openWeb, vm::signOut, banner, resultsFocus, { resultsFocus = false }, checkUpdates)
                        }
                        }
                    }
                }
            }
            com.student.amizoneplus.ui.shell.StatusBlur(hazeState)
            com.student.amizoneplus.ui.shell.BottomNav(route, ::go, hazeState)
          }
            // Pets roam above the screens and the nav (so they can be carried onto a tab).
            com.student.amizoneplus.ui.pets.PetLayer(pets, s.settings.pets, active = !menuMounted && web == null && !gallery && s.celebrate == null)

            if (menuMounted) QuickMenuOverlay(menuOpen, { z.value }, menuHaze, { closeMenu() }) { path, title ->
                // Sections the app already shows natively open in the app's own UI.
                when (path.lowercase()) {
                    "/examination/examschedule" -> { scheduleFocus = "exams"; go(Route.Schedule) }
                    "/timetable/home" -> { scheduleFocus = "classes"; go(Route.Schedule) }
                    "/examination/examination" -> { resultsFocus = true; go(Route.More) }
                    else -> web = (AmizoneClient.BASE + path) to title
                }
            }

            // Status-bar icons: light over the dark home, dark over bright hero colours.
            val window = (androidx.compose.ui.platform.LocalContext.current as? android.app.Activity)?.window
            val view = androidx.compose.ui.platform.LocalView.current
            val lightBars = if (route == Route.Today) !P.cur.dark else true
            androidx.compose.runtime.SideEffect {
                window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = lightBars }
            }

            AnimatedVisibility(web != null, enter = slideInVertically(tween(380, easing = Motion.emphasized)) { it } + fadeIn(), exit = slideOutVertically(tween(260)) { it } + fadeOut()) {
                // Keep the last page drawn while it slides away.
                web?.let { lastWeb = it }
                lastWeb?.let { (url, title) -> androidx.compose.runtime.key(url) { AuthedWebPage(url, title, vm::relogin) { web = null } } }
            }
            if (gallery) BadgeGallery(s.data?.let { earnedBadges(it, s.settings.threshold) }.orEmpty()) { gallery = false }
            s.celebrate?.let { Celebration(it, vm::dismissCelebration) }
            update?.let { r ->
                com.student.amizoneplus.ui.screens.UpdateSheet(r) {
                    com.student.amizoneplus.data.Updater.snooze(ctx, r.version); update = null
                }
            }
        }
    }
}
```

---

## `app/src/main/java/com/student/amizoneplus/MainActivity.kt`

```kotlin
package com.student.amizoneplus

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.student.amizoneplus.ui.AppRoot
import com.student.amizoneplus.ui.AppViewModel

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        preferHighestRefreshRate()
        setContent { AppRoot(vm) }
    }

    /**
     * Some phones (OnePlus, Xiaomi…) run apps at 60 Hz unless they ask for more.
     * Ask for the display's fastest mode at the current resolution (e.g. 120 Hz).
     */
    @Suppress("DEPRECATION")
    private fun preferHighestRefreshRate() {
        val display = (if (Build.VERSION.SDK_INT >= 30) display else windowManager.defaultDisplay) ?: return
        val cur = display.mode
        val best = display.supportedModes
            .filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
    }

    override fun onResume() {
        super.onResume()
        // Refresh when coming back to the app if the data is a few minutes old.
        val d = vm.state.value.data
        if (d != null && System.currentTimeMillis() - d.fetchedAt > 5 * 60_000) vm.refresh()
    }
}
```

