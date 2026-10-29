# KLYRA modification record

Base: BitChord `7c673dcdbb5a6f8cc36538eb8813c559e37c26b2` (`desktop-build-30`).

## 2026-10-29 — title-case Klyra display name

- Corrected the owner-approved spelling to **Klyra** across Android launcher,
  localized app text, Home/About/account and sharing labels, Windows title/tray,
  and desktop distribution metadata. The selected Rhythm logo is unchanged.
- Android version is `1.8-klyra.4`, code 29. Existing package ID, signing key,
  account paths, download directories, UI, blur and playback logic are preserved.
- Fixed dark-mode library heading, Favorites and song-title contrast by using
  the active theme's onSurface color; the empty-playlist hint uses onSurfaceVariant.
  This corrects text contrast on both platforms without changing layout or blur.

## 2026-10-29 — KLYRA name and selected Rhythm icon

- Changed the Android launcher label, Windows window/tray name, desktop package
  name and Settings footer to **KLYRA**, without the visible Preview suffix.
- Applied the owner's selected **03 Rhythm** K silhouette, without a dot, to
  adaptive/legacy launcher icons, notification and shared UI marks, replay/share
  assets, desktop window/tray assets and Windows executable icon. The reference
  sheet and reproducible native-vector asset generator are in `branding/`.
- Android version is `1.8-klyra.3`, code 28. Package ID and signing configuration
  stay the same; existing Windows account/profile storage paths are retained.
- Preserved existing UI layout, colors, blur, animations and playback code.
  GPL and attribution notices remain available in About and the source bundle.

## 2026-10-28 — branding and Android performance update

- Replaced the remaining ordinary app, notification, replay/share and desktop tray
  marks with original KLYRA artwork. Localized presentation strings use KLYRA;
  source package names, JNI contracts, legacy file lookups and third-party service
  identities are retained where compatibility or accurate attribution requires it.
- Android Settings → About → Open-source licenses now includes offline provenance
  and full GPL text. Desktop licenses identify the modified KLYRA derivative.
  Original source notices and the upstream repository remain in the source bundle.
- Added a non-debuggable, R8-optimized `devPreview` APK, version code 27, using the
  existing preview application ID and local test signing key for in-place updates.
- Moved artwork palette analysis and video-frame pixel checks off the UI thread;
  remembered derived colors instead of recalculating them on animation frames.
  Existing blur, animations, color choices and responsive layouts are preserved.
  Reduced-blur behavior only applies when explicitly enabled by the user.
- New exports use Music/KLYRA and Pictures/KLYRA; old Music/BitChord downloads
  remain discoverable without moving or deleting them.
- Added phone, tablet, rotated and split-window sizing invariants. These tests
  validate sizing logic, not rendered screenshots or physical-device frame rates.
- Selected stable R8 9.1.57 for dependencies carrying Kotlin 2.4 metadata.
  Build and device-verification results are recorded separately in KLYRA_TEST_REPORT.md.

## 2026-10-27

- Created an isolated GPLv3 derivative working tree; preserved the existing KLYRA project as a fallback.
- Added provenance, architecture decision, distribution requirements and provider limitations in `KLYRA.md`.
- Isolated Android package, desktop preferences, credential files, cache folders and updater identity from both the old KLYRA and BitChord.
- Disabled upstream installation-presence reporting and upstream automatic update feeds; removed the default shared party server.
- Replaced the main brand assets and UI labels with original KLYRA artwork. Retained upstream names in provenance, source packages, JNI contracts and license notices.
- Removed bundled SF Pro fonts. Both platforms use platform sans-serif fonts and neutral Material surface roles.
- Added a desktop bottom transport with real seeking, playback, queue and lyrics actions, and restored the native Windows window frame. Disabled decorative browse backgrounds and lyric particle/glow effects.
- Added KLYRA Supabase account controls, email-link verification, password update and confirmed account deletion. Preserved separate music-provider sessions.
- Added persisted favorites/playlists, playlist editing, owner-isolated local state, an optimistic revision outbox and explicit cloud conflict choices. Local-only music records survive cloud refreshes and are not uploaded.
- Added Windows DPAPI credential storage and Android process-only fallback when encrypted storage cannot be opened. Excluded account preferences from Android backup/transfer.
- Added tests for DPAPI, actual catalog/native Windows playback, local playback, Android session preferences, sync merge edge cases and Android Media3 service transport.
- Added the original KLYRA owner-protected Supabase schema and reproducible build instructions in `KLYRA_BUILD.md`.

Build and runtime outcomes are recorded separately; this change log does not certify them.
