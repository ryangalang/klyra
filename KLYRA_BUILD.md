# Klyra 1.8-klyra.7 release

Android version code 30; Windows native version 1.8.32. Existing application ID,
Android signing certificate, Windows upgrade UUID and account storage paths are retained.
One manual install of this version is needed for users of earlier builds with no update feed.

The app checks https://api.github.com/repos/ryangalang/klyra/releases/latest at launch
and every six hours while the process is running. Only published, non-prerelease Klyra
versions are considered. Assets must use the names below and have GitHub SHA-256 digests.
Android selects the matching ABI or Universal, verifies the digest, package ID, version
code and installed signing certificate, then opens Android's system installer.
Windows downloads the verified EXE and opens Setup after the user chooses Install update.
It closes Klyra for installation; users can relaunch it normally after Setup finishes.
Windows Setup may show publisher/security prompts; no silent installation is promised.

Build with JDK 21:
```powershell
.\gradlew.bat :shared:jvmTest :app:assembleDevPreview :desktopApp:createDistributable :desktopApp:packageExe '-Pbitchord.version=1.8-klyra.7' '-Pklyra.packageOutput=compose/release32'
```
Keep the current Android keystore backed up securely; never commit it. Future releases
must increase BOTH the relevant Android versionCode and desktop native package version.

Release assets: Klyra-Android-arm64.apk, Klyra-Android-arm32.apk,
Klyra-Android-universal.apk, Klyra-Android-x64-emulator.apk,
Klyra-Windows-x64-setup.exe, Klyra-Windows-x64.zip,
Klyra-corresponding-source.zip and SHA256SUMS.txt.
After reviewing the local bundle, use scripts/publish-klyra.ps1 with ReleaseDirectory.
It creates a draft by default. Publish that draft (or pass -Publish) to make it discoverable.
No user will see a newer-version notice until the release is actually published.

Sidebar account advertising is removed; account controls remain in Settings.
Signup guides users through email confirmation; a valid session refreshes cloud records
at startup and every minute. Sync is scoped to Klyra favorites/playlists, not the provider's
separate library. A last-sync timestamp is shown only after a successful library exchange.
Playlist Save/Download controls are visible on collection headers; downloads use existing
platform queues, skip already-saved tracks and report provider failures individually.
Complete playlist retrieval fails visibly instead of saving a silently truncated list.
Offline availability remains dependent on the source, access rights and technical support.

## Android cover screen

Settings > Appearance > Immersive lock-screen player opens a separate music-only
Activity. Open it while a song is playing, then lock the phone. It connects to the
existing Media3 session (no second audio player), shows artwork, clock, seek and
transport controls, and suspends polling/motion when not resumed. Reduce animation
and Reduce dynamic blur are honored. It never dismisses the keyguard, requests
overlay/full-screen-intent privileges, wakes the screen, or launches from the
background. It is not a replacement for Android System UI or an iOS animated-art
API. Regular lock-screen media controls remain provided by Media3; layout/artwork
size depend on the OS/OEM and the user's notification settings.

References: https://developer.android.com/media/implement/surfaces/mobile and
https://developer.android.com/reference/android/app/Activity#setShowWhenLocked(boolean)

## Earlier build documentation

# Building and testing Klyra

The current display name is **Klyra**, Android version `1.8-klyra.4` (29).
The November 1 download bundle keeps those exact Android APKs and updates Windows
to `1.8-klyra.6`. To reproduce that Windows image, use JDK 21 and run:

```powershell
$env:KLYRA_WINDOW_TEST='1'
.\gradlew.bat :desktopApp:test --tests '*DesktopCaptionColorsTest' :desktopApp:createDistributable '-Pbitchord.version=1.8-klyra.6' --console=plain
Remove-Item Env:KLYRA_WINDOW_TEST
```

The caption uses the real Windows DWM title bar, preserving native window controls.
Exact caption colors depend on the Windows version; unsupported attributes fall
back to the operating system's frame. No playback engine or account migration is
part of this visual update. Settings > Appearance > Liquid Glass enables the
frosted player and navigation. Reduce dynamic blur overrides it with solid fills;
Reduce motion freezes artwork drift and removes side-panel transitions. Windows
uses Compose/Haze blur and highlights, not Android-only lens/refraction shaders.
The sidebar becomes a compact rail below 1160 dp and the queue/lyrics panel
narrows below 1200 dp. The existing minimum window remains 900 x 600 dp.
One outlined sidebar-panel button in the sidebar header toggles between the full library and
a compact icon rail. There is no extra close button. The choice is saved locally. Main destinations remain anchored while
the library list scrolls. Row hover follows the row shape.
The pinned Klyra account row opens the existing cloud account form directly;
provider sign-in remains separate and explains that distinction.

The final image was built with `-Pklyra.packageOutput=compose/account`, placing it
in `desktopApp/build/compose/account/main/app/Klyra/` alongside the running preview.
Omit that property to use the default image directory below.

The Windows image is `desktopApp/build/compose/binaries/main/app/Klyra/`, with
`Klyra.exe`. Internal preview variant/package and account storage names remain
unchanged for upgrade compatibility. Earlier delivery notes below are historical.

This directory is the GPLv3 derivative, based on BitChord desktop-build-30 at
`7c673dcdbb5a6f8cc36538eb8813c559e37c26b2`. The parent directory contains the older
independent KLYRA application. Build this directory to get the new catalog/player.

## Toolchain

- JDK 21 (development used Eclipse Temurin 21.0.12.1+1).
- Gradle wrapper 8.11.1; Kotlin 2.3.20; Android Gradle Plugin 8.10.1.
- Android SDK platform `platforms;android-37.0`, build tools selected by Gradle,
  NDK 27.0.12077973, CMake 3.22.1. Android minimum API 26, target API 36.
- Windows x64: MinGW g++, CMake and Ninja on PATH. Development used w64devkit
  2.10.1. The native SMTC and analysis DLLs must be present in the packaged app.
- Internet access to Maven Central, Google Maven, JitPack and the upstream
  dependency repositories listed in settings/build files.

On an 8 GB computer, run one Gradle build at a time. The checked-in worker limit
and in-process Kotlin compiler prevent several competing compiler heaps.

Create an ignored `local.properties` in this directory:

```properties
sdk.dir=C:/Users/YOUR_NAME/AppData/Local/Android/Sdk
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_ANON_KEY=YOUR_PUBLIC_PUBLISHABLE_OR_ANON_KEY
```

Use only a public project key. Never put a service-role key, database password,
personal access token or user session here. Public configuration is intentionally
embedded in each client; security comes from the database's RLS and authenticated
RPC checks. Apply `supabase/migrations/001_library.sql` to your own project.

```powershell
.\gradlew.bat :app:assembleDevDebug :app:testDevDebugUnitTest --console=plain
.\gradlew.bat :shared:jvmTest :sharedUi:jvmTest :desktopApp:test --console=plain
.\gradlew.bat :desktopApp:createDistributable --console=plain
```

The dev APK has package `app.klyra.foundation.preview` and installs separately from
the old KLYRA and upstream BitChord. Debug builds are signed for local testing;
they are not Play Store releases. Production signing belongs to the project owner.

Windows output: `desktopApp/build/compose/binaries/main/app/KLYRA Preview/`.
Keep the whole folder together and launch `KLYRA Preview.exe`; do not copy only
the executable. A Java runtime is included.

## Real provider and device checks

Live tests use the actual provider and native decoder. They are opt-in because
network, regional and upstream changes can make them fail:

```powershell
$env:KLYRA_LIVE_TEST='1'
.\gradlew.bat :desktopApp:test --tests '*LivePlaybackTest' --console=plain
Remove-Item Env:KLYRA_LIVE_TEST
```

Connect an emulator or Android device with debugging authorized, then:

```powershell
.\gradlew.bat :app:connectedDevDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.music.bitchord.KlyraPlaybackInstrumentedTest' --console=plain
```

That test uses a generated silent WAV through the real Media3 service, checking
seek, pause, background position, controller reconnection and notification posting.
It does not verify Bluetooth hardware, audible output or YouTube playback on Android.

## Accounts and sync

KLYRA email/password accounts are separate from the optional music-provider login.
Sessions use Windows DPAPI or Android encrypted preferences. If Android secure
storage is unavailable, the session lasts only for the current process.

Favorites and KLYRA playlists use owner-isolated local files plus a persisted cloud
outbox. Conflicting edits require an explicit choice in the account card. Local
file paths and local-only favorites are not uploaded. Signing in does not silently
import a guest library into the account.

For signup/recovery, the account card can redeem the original Supabase email link
inside either application. Copy the link before opening it, enter the same email,
and use **Verify email link**. Recovery then exposes the new-password field. This
manual flow avoids requiring a Windows URI-handler installation. Email delivery
depends on the project's SMTP/provider configuration and quotas. Template details:
https://supabase.com/docs/guides/auth/auth-email-templates.

To verify cross-device sync, sign into the same KLYRA account on both apps, create
a playlist and favorite a track on one, then press Sync library on the other.
This requires a real account owned by the tester; unit tests do not establish that
email delivery or a two-device account flow has passed.

## Distribution

### Rhythm branding update (2026-10-29)

Version `1.8-klyra.3` / code 28 displays **KLYRA** and uses the approved 03 Rhythm
icon. Build Android with `:app:assembleDevPreview`; the internal variant/package
names remain compatible with earlier installations. Build Windows with
`:desktopApp:createDistributable`; the application image is now under
`desktopApp/build/compose/binaries/main/app/KLYRA`, launched with `KLYRA.exe`.
The existing internal Windows data folders remain unchanged. See `branding/`
for the selected artwork and reproducible platform asset generation.

### Optimized personal preview (2026-10-28 branding update)

Run `gradlew.bat :app:assembleDevPreview` for the non-debuggable, R8-optimized
preview. Outputs are in `app/build/outputs/apk/dev/preview`. This variant keeps
`app.klyra.foundation.preview` and uses the same local Android debug signing key
as the previous dev debug APK, so it can upgrade that installation without
clearing its account or library. Version code 27 identifies this update.
The signing key is for personal testing, not a production release identity.

The current UI, blur settings, animation durations, and playback timing remain
unchanged. Artwork palette/pixel analysis runs on a worker dispatcher; derived
palette colors are remembered across animation frames. Existing reduced-blur
preferences are respected, but reduced blur is not enabled by default.

The root build overrides R8 with stable Google Maven version 9.1.57. Some resolved
dependencies contain Kotlin 2.4 metadata, whose documented minimum is R8 9.1.29;
see the [Android compiler compatibility table](https://developer.android.com/build/kotlin-support).
For this 8 GB build machine, use one build at a time with
`-Dorg.gradle.jvmargs="-Xmx3072m -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8"`.

Android Settings → About → Open-source licenses contains offline attribution
and the full GPL text. Desktop Settings → Open-source licenses includes the
derivative and dependency notices. These notices supplement, rather than
replace, the corresponding-source archive supplied with every preview.

Supply `LICENSE`, `KLYRA_CHANGES.md`, this build guide and the exact modified source
alongside binaries. Keep original copyright notices and dependency license files.
Do not package `.git`, build caches, local.properties, keystores or session data.
Platform fonts replace the removed SF Pro assets. Review dependency licenses before
public release; the upstream project license does not grant music or artwork rights.

Provider restrictions and the fork/component comparison are in `KLYRA.md`.
