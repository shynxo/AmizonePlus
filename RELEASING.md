# Releasing Amizone+ updates

The app checks this repo's **GitHub Releases** for new versions and offers them in-app
(More → About → *Check for updates*, and quietly on launch every few hours).

## One-time setup

1. **Back up the signing key.** It lives *outside* this project, in
   `Q:\04_Academic_files\Amizone+\signing\` (`amizoneplus-release.jks` + `keystore.properties`).
   Copy that folder somewhere safe (Drive, USB). **Never commit it.** Lose it and every user
   has to uninstall and reinstall.
2. **Your repo is set** in `app/build.gradle.kts` (`shynxo/AmizonePlus`). To change it:
   ```kotlin
   buildConfigField("String", "UPDATE_REPO", "\"your-github-username/your-repo\"")
   ```
   Until this is set, the updater stays off.
3. **Push the project to GitHub.** `.gitignore` already keeps out `keystore.properties`,
   `*.jks`, `build/` and `local.properties`. The repo must be **public** (the app reads
   releases without logging in).
4. Build the first signed APK (below) and give it to everyone. Anyone who has an older,
   debug-signed copy must uninstall it once and install this one (they sign in again).

## Every update

1. Make your changes.
2. In `app/build.gradle.kts` bump **both**:
   ```kotlin
   versionCode = 3          // +1 every release
   versionName = "2.2.0"    // the new version
   ```
3. Build: Android Studio → Build Variants → `release`, then **Build → Build APK(s)**.
   (Or: `gradlew assembleRelease`.) The file is `app/build/outputs/apk/release/app-release.apk`.
   It's signed with your key automatically, via `keystore.properties`.
4. On GitHub: **Releases → Draft a new release**
   - Tag: `v2.2.0` (must match `versionName`, with a `v`)
   - Title: anything; **description = "What's new"** shown in the app
   - Attach `app-release.apk` (rename it to `AmizonePlus-2.2.0.apk` if you like)
   - **Publish release**

That's it — users get the update sheet next time they open the app.

## What users see

"Update available · v2.2.0" with your notes → **Update** → download → Android asks
"Install this update?" → **Install**. The first time only, Android asks to allow installs
from Amizone+. Login and settings are kept.

## If someone else clones the repo

Without `keystore.properties` the release build falls back to the debug key, so it still
builds — but those APKs can't update copies signed with *your* key (and vice versa).
