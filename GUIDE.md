# Amizone+ (native) — Android Studio guide

A native Kotlin + Jetpack Compose rewrite of Amizone+. No React Native, no C++,
so it builds anywhere on Windows (no path-length problems) and the release APK is ~2 MB.

**Every file's complete contents are in [`ALL-FILES.md`](ALL-FILES.md)**, ready to copy-paste.
This guide explains what each file does and how to use the project.

---

## 1. Open and run (fastest way)

1. Android Studio → **File → Open** → choose `Q:\amz-native` → **Trust Project**.
2. Wait for **Gradle sync** to finish (bottom status bar).
3. Connect your phone (USB debugging, or **Pair Devices Using Wi-Fi** from the device dropdown).
4. Press **Run ▶**. The app installs as **"Amizone+"** with ID `com.student.amizoneplus.native`,
   so it sits *next to* the Expo version instead of replacing it.

**Build an APK:** Build → Generate App Bundles or APKs → **Build APK(s)** →
`app\build\outputs\apk\…`. A release APK is ~2 MB.

**From a terminal** (same result):
```
gradlew assembleRelease
```

Toolchain (already on this PC): Android Gradle Plugin 9.2.1 · Kotlin 2.4.21 · Gradle 9.4.1 ·
compileSdk/targetSdk 37 · minSdk 26 (Android 8+). Gradle JDK = Android Studio's bundled **jbr**.

---

## 2. Recreating it by hand

1. **New Project → Empty Activity** (Compose). Name `AmizonePlus`, package `com.student.amizoneplus`,
   language Kotlin, min SDK 26, build configuration **Kotlin DSL**.
2. Replace the generated Gradle files with ours (section 3, "Build setup"), then **Sync Now**.
3. Create the packages under `app/src/main/java/com/student/amizoneplus/`:
   `data`, `logic`, `work`, `ui`, `ui/theme`, `ui/kit`, `ui/shell`, `ui/screens`.
4. Create each file in the order of the table below (each only depends on files above it),
   pasting the contents from `ALL-FILES.md`.
5. Add the resources (`res/values`, `res/xml`) and the launcher icons (`res/mipmap-*` —
   copy them from this project, or make your own via **New → Image Asset**).
6. Run ▶.

---

## 3. Every file and what it does

### Build setup
| File | Purpose |
|---|---|
| `settings.gradle.kts` | Repositories (Google, Maven Central) and the `:app` module |
| `build.gradle.kts` | Plugin versions: Android 9.2.1, Kotlin 2.4.21 Compose + Serialization plugins |
| `gradle.properties` | Gradle memory, AndroidX, caching |
| `gradle/wrapper/gradle-wrapper.properties` | Pins Gradle 9.4.1 (plus `gradlew`, `gradlew.bat`, `gradle-wrapper.jar` — copied from an existing project) |
| `app/build.gradle.kts` | App ID, SDK levels, R8 shrinking, Compose, all libraries |
| `app/proguard-rules.pro` | Keep-rules for WorkManager's database and serialization (prevents the crash the Expo build had) |
| `app/src/main/AndroidManifest.xml` | Permissions (internet, notifications), app/activity, reminder receiver, file provider |

### Resources
| File | Purpose |
|---|---|
| `res/values/strings.xml` | App name |
| `res/values/themes.xml`, `res/values-night/themes.xml` | Window background (light / dark) + transparent system bars |
| `res/font/*.ttf` | Barlow Condensed + DM Sans (binary — copy from the project) |
| `res/xml/file_paths.xml` | Lets PDF/PPT viewer apps open downloaded LMS files |
| `res/mipmap-*` | Launcher icons |

### Data — talking to Amizone (`data/`)
| File | Purpose |
|---|---|
| `Models.kt` | All data classes (attendance, classes, exams, courses, results, LMS items, settings) and error types |
| `Http.kt` | OkHttp client whose cookie jar **is Android's WebView CookieManager** — so in-app web pages are always signed in too; also shares the WebView's User-Agent (Cloudflare ties sessions to it) |
| `AmizoneSsl.kt` | Lets in-app web pages open Amizone on Android 8/9, whose trust store lacks Amizone's GlobalSign Root R46 (the root itself is in `res/raw`, trusted via `res/xml/network_security_config.xml`) |
| `SessionLog.kt` | Diagnostic journal of the Amizone session (cookie names + fingerprints only) in the app's external files folder |
| `Downloads.kt` | Downloads a file with the signed-in connection and opens it in the phone's viewer app (PDF, PPT, Word…) |
| `Updater.kt` | In-app updates: reads the latest GitHub Release, downloads the APK, hands it to Android's installer (see RELEASING.md) |
| `Parsers.kt` | Jsoup parsers for every Amizone page (attendance, timetable JSON, courses, exams, ID card, results, documents, credits) |
| `AmizoneClient.kt` | Sign-in (token + form POST), auto re-sign-in, and `fetchAll()` that loads every section in parallel |
| `Lms.kt` | Amizone LMS (Moodle): SSO via Amizone's LMS widget form, course list, sections, folder files, download + open in viewer |
| `Storage.kt` | Password encrypted with an **Android Keystore** AES key; JSON cache; settings |
| `Repository.kt` | `refreshAll()`: fetch → keep old values for failed sections → cache → notifications |

### Logic (`logic/`)
| File | Purpose |
|---|---|
| `Attendance.kt` | %, "can skip / must attend", and the **credits × 15** semester plan |
| `Insights.kt` | Attendance by weekday, present streak, badges |
| `When.kt` | Calendar-day countdowns for exams ("today at 10:00", "tomorrow", "in 3 days", "happening now") |
| `CourseNames.kt` | Short course names for notifications (OOPs, DSA, Maths III…), acronyms for anything else long |

### Background (`work/`)
| File | Purpose |
|---|---|
| `Notifier.kt` | Notification channels, low-attendance alerts, class & exam reminders (AlarmManager) |
| `ReminderReceiver.kt` | Shows a reminder when its alarm fires |
| `RefreshWorker.kt` | WorkManager job refreshing data about every 6 hours |

| `KeepAlive.kt` | Session keep-alive on allow-while-idle alarms every ~10 min (Amizone now needs a CAPTCHA for every new sign-in, so the session must not lapse) |
| `NotifArt.kt` | Draws reminder notifications as app-styled cards with a pixel pet (sleeping the night before an exam) |
| `AmizoneApp.kt` *(root)* | Creates notification channels at app start |

### UI
| File | Purpose |
|---|---|
| `ui/theme/Theme.kt` | Dark & light palettes, fonts & type scale, motion curves/springs, **haptics** |
| `ui/kit/Kit.kt` | Design system: fused shapes (path union − notches), Rise entrance, press feedback, Ink card, rows, chips, buttons, count-up, segmented bar, weekday bars, sliding toggle, **UnfoldCard**, countdown |
| `ui/kit/Quips.kt` | The app's playful lines (Shynxo signatures, loading quips, free-day emojis) |
| `ui/pets/PetWorld.kt` | The pets' world: perches (cards, pills, buttons, big titles they can stand on), hang points (clock hands), nav tab positions |
| `ui/pets/Pets.kt` | Cat & dog: behaviour brains, physics, roaming between tabs, tap / drag / throw / carry-to-tab, bubbles and particles |
| `ui/pets/PetSprites.kt` | **Generated** pixel art — edit `tools/pets/sprites.py` and run it (also renders preview PNGs) |
| `ui/shell/Shell.kt` | Header bar, **collapsing header** (on/off into a top-left card), floating **pill nav** over **progressive blur** (Haze) |
| `ui/shell/Refresh.kt` | **Stretch pull-to-refresh**: rubber-stretch page over a coral panel; one cream word whose letters roll in clipped slots (FETCHING → FROM → SHITTY → AMIZONE); LRA tension haptics |
| `ui/AppViewModel.kt` | App state: sign-in, refresh, web-login fallback, settings, badges, sign-out |
| `ui/screens/WebScreens.kt` | Signed-in in-app browser + website sign-in fallback |
| `ui/screens/LoginScreen.kt` | First-run sign-in |
| `ui/screens/Badges.kt` | Star badge art, celebration (particle burst), gallery |
| `ui/screens/TodayScreen.kt` | Today: greeting, attendance, next class, classes/exam pair, badges, countdown |
| `ui/screens/SubjectsScreen.kt` | Subjects with expandable cards, semester plan + credits stepper |
| `ui/screens/ScheduleScreen.kt` | 7-day timetable and exams |
| `ui/screens/LmsScreen.kt` | LMS courses, sections, folders, files |
| `ui/screens/MoreScreen.kt` | Profile, CGPA/SGPA, results, settings, sign out |
| `ui/screens/QuickMenu.kt` | Pull-down menu (Wi-Fi, faculty feedback, OD, exam forms, fees…) with the reference choreography |
| `ui/screens/UpdateSheet.kt` | "Update available" sheet: what's new, download progress, install hand-off |
| `ui/AppRoot.kt` | Navigation, screen transitions, menu animation, overlays |
| `MainActivity.kt` *(root)* | Entry point, edge-to-edge, notification permission, refresh on resume |

---

## 4. Making changes

- **Amizone changed a page?** Fix the selector in `data/Parsers.kt`.
- **Colours / fonts / spacing / motion?** `ui/theme/Theme.kt` (`P`, `Type`, `L`, `Motion`).
- **Fonts:** Barlow Condensed (titles) and DM Sans (body) live in `res/font/` and are set up as
  `Condensed` and `Mono` in `Theme.kt`. Both are OFL-licensed; their licences are in `FONTS-OFL-*.txt`.
- **Dark / light:** follows the phone. Colours for each mode are `DarkPalette` / `LightPalette` in
  `Theme.kt`; hero colours (`P.sage`, `P.coral`, `P.peri`, `P.sky`, `P.sun`) are shared by both.
- **Quick-menu items:** `MENU_GROUPS` in `ui/screens/QuickMenu.kt` (label, Amizone path, icon).
- **Replace the Expo app:** change `applicationId` in `app/build.gradle.kts` to
  `com.student.amizoneplus`, then uninstall the Expo version first (different signing key).
- **Your own signing key** (to share the app): Build → Generate Signed App Bundle / APK → create a
  keystore, and keep it safe — every future update must be signed with it.
