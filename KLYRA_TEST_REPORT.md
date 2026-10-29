# KLYRA build verification

This is a test build, not a certification of complete production readiness.
The original independent KLYRA application is preserved in the parent directory.

## Klyra capitalization and library contrast — version 1.8-klyra.4 (code 29)

- All 16 localized Android launcher labels are **Klyra**; resource XML parses.
  Desktop title/tray labels, distribution metadata and visible in-app branding
  also use Klyra. Existing package ID and account-storage paths are retained.
- Library/Favorites headings and track titles explicitly use the active theme's
  onSurface color; the empty-playlist hint uses onSurfaceVariant. Layout, blur,
  motion and the selected Rhythm logo are unchanged.
- Calculated sRGB contrast against the theme background is 17.32:1 dark and
  15.58:1 light for headings; hints are 8.45:1 dark and 5.30:1 light.
  These are palette calculations, not device screenshot or rendering tests.
- The built Android APK reports **Klyra**, version 29 / 1.8-klyra.4, unchanged
  package `app.klyra.foundation.preview`, API 26 minimum and target API 36.
  It is non-debuggable and passes APK signature v2 verification with the same
  certificate as version 28. All generated APK variants report version 29.
- ARM64 APK: **51,841,473 bytes**. SHA-256:
  `9dfed8f724c3b9c51c9c6e6bbe2d2ec0570979f19a98cd2e30e4614b7ba45e93`.
  Packaged Rhythm artwork, GPL license and updated notices match source exactly.
- `:app:assembleDevPreview :desktopApp:createDistributable` completed successfully
  on 2026-10-29 in 20m 18s (`klyra-titlecase-contrast-build-oct29.log`). The earlier
  capitalization-only build was intentionally stopped to include the reported
  library contrast fix in a fresh build. Windows `Klyra.exe` reports ProductName
  Klyra and FileDescription Klyra music player.
  No physical-device or new emulator runtime results are claimed. Earlier test
  suites were not rerun for this display-text and color-only update. The older
  embedded lint analyzer still emits Kotlin metadata compatibility diagnostics.

## Rhythm branding update — version 1.8-klyra.3 (code 28)

- On 2026-10-29, `:app:assembleDevPreview :desktopApp:createDistributable`
  completed successfully in 21m 38s (`klyra-rhythm-build-oct29.log`).
- The Android APK label is **KLYRA**, version code 28, version name 1.8-klyra.3.
  Package ID remains `app.klyra.foundation.preview`; the non-debuggable build
  retains the existing signing certificate and passes APK signature v2 verification.
  Certificate SHA-256: `f79b1e7e917220537fb3db803edff25203fcd959ef1d0bd40bff06490c55dc82`.
- ARM64 APK: **51,841,497 bytes**, minimum API 26, target API 36.
  SHA-256: `de0203913b2023b82f457b5e365cbaaf0eacd43b84f10e8d178da72dd5b5a051`.
- The selected concept 03 Rhythm was traced into native vector/raster launcher,
  in-app, notification and desktop assets. Generated SVG/XML assets parse, and
  the ICO contains 16–256 pixel sizes. Packaged Logo.svg, LogoTransparent.svg and
  the GPL asset exactly match their source files.
- The Windows distribution contains **KLYRA.exe**; its ProductName is KLYRA and
  FileDescription is KLYRA music player. Window/tray labels use KLYRA. Existing
  internal account/storage directories and Android package identity are retained.
- This update preserves UI layouts, blur, motion and playback code. The 85 tests
  below belong to version 27 and were not rerun for this asset/name update.
  No new emulator, physical-device, FPS or Windows runtime interaction results
  are claimed. Compilation and package checks do not certify smoothness.
- The older embedded lint analyzer still emits dependency Kotlin metadata
  compatibility diagnostics; completed lintVital tasks are not a claim of a
  comprehensive clean lint analysis.

## Branding/performance update — version 1.8-klyra.2 (code 27)

- `:app:assembleDevPreview :shared:jvmTest :sharedUi:jvmTest` completed successfully
  on 2026-10-28 (`klyra-final-android-oct28.log`, 29m 7s). All **85 tests passed**:
  48 shared-core and 37 shared UI/lyrics/sizing tests, zero skipped or failed.
- The APK is non-debuggable and R8 optimized. Its existing preview package ID and
  signing certificate match the previous delivery. APK Signature Scheme v2 verifies.
  ARM64 APK: **51,826,333 bytes**, Android 8/API 26 minimum, target API 36.
  SHA-256: `fa7ac5e554004644716e078d5c2460dedbe4f1d4ceb5f14249a96a2c1b9e400d`.
- Eight ARM64 native libraries are present. Both offline legal-notice assets are
  bundled; the full GPL asset exactly matches LICENSE. No OTF font assets remain.
  All 32 localized string XML files parse successfully.
- Current UI colors, blur defaults, animations and responsive layout rules were
  preserved. The new sizing tests cover phone/tablet widths, orientation and narrow
  split windows. These are sizing-invariant tests, not pixel or frame-rate tests.
- The initial optimized attempt did not finish. The successful build uses R8 9.1.57
  to support resolved Kotlin 2.4 dependency metadata. LintVital tasks completed,
  but AGP's older embedded lint analyzer emitted Kotlin metadata compatibility
  diagnostics; this is **not** a claim of a comprehensive clean lint analysis.
- The matching Windows distribution compiled and packaged successfully
  (`klyra-final-windows-oct28.log`, 4m 40s). Its new runtime UI was not exercised.
- Physical Android smoothness/FPS, headset controls and Bluetooth remain unverified:
  no physical Android device was attached. Emulator acceleration was available,
  but the install/launch/resize test was NOT executed: automatic approval review
  could not run after its account usage limit was reached. The rejection explicitly
  said this was a review-service failure, not a determination that the action was
  unsafe. No emulator playback, screenshots or frame-rate results are claimed.

## Verified observations

- On 2026-10-28, the corrected Android dev APK, shared test suites and Windows
  distribution completed successfully (`foundation-delivery-oct28.log`, 6m 42s).
  All 48 shared-core tests and all 34 shared UI/lyrics logic tests passed.
- The delivery ARM64 APK is labeled **KLYRA Preview**, requires Android API 26+,
  and passes APK signature v2 verification. Its manifest disallows cleartext
  traffic; the archive contains eight ARM64 native libraries and no SF Pro assets.
  SHA-256: `ee21f80ecc618d063b2370757f78af2ced1ffc93f56e02ac8180757d8b5eccd1`.

- Windows baseline compilation, shared tests and native packaging passed.
- An earlier desktop suite ran 337 tests: 335 passed and two opt-in live tests
  were skipped. This included a real Windows DPAPI round-trip and corruption test.
- The opt-in live tests were then enabled separately and both passed on
  2026-10-27. Searches for BINI/Pantropiko, BLACKPINK/Lovesick Girls, Bruno Mars/Just
  the Way You Are and Eraserheads/With A Smile each returned 20 track results.
- Pantropiko resolved through YouTube and ran through the native Windows audio
  engine with a reported duration of 225,481 ms. Playback position advanced,
  seeking to eight seconds worked, pausing held the clock and resuming advanced
  it again. The test muted output; this does not certify audible sound quality or
  uninterrupted playback of the entire song. The other three searches were not
  playback tests.
- A generated 20-second local WAV passed the same native Windows transport test.
- Android compilation and dev APK assembly passed on 2026-10-27. The ARM64 APK
  verified with APK signature scheme v2. Its dev-manifest launcher label was found
  to override the new resource name; the source was corrected to KLYRA Preview
  and its cleartext-traffic override was disabled. The delivery APK must be the
  rebuilt version, not that earlier package.
- Android process-only session-preferences test passed (one test, zero failures).
- The Supabase schema was applied successfully to the owner's configured project.
  A read-only database check returned: RLS enabled, anonymous SELECT denied,
  authenticated SELECT granted and direct authenticated INSERT denied. The SELECT
  policy limits rows to auth.uid(); writes go through the revision-checked RPC.
- The packaged Windows application started and loaded live Home content. Full
  visual interaction testing has not yet been recorded; launch alone is not a UI
  or media-controls pass.

## Still requiring verification

The subsequent request to build the Android instrumentation-test APK was not
executed: automatic approval review could not complete because of an account usage
limit. The final Windows UI launch in the same orchestration call was also not
executed. This is a tooling/review failure, not a failed app test or a determination
that the action was unsafe. No device-test pass is claimed.

- Real KLYRA email signup, confirmation/recovery and login with a tester-owned
  account, then Android-to-Windows synchronization under that same account.
- Installed Android playback, foreground notification, background continuity,
  lock-screen controls, audio focus, headset/Bluetooth and reconnect behavior.
  No physical Android device was connected during the checks above.
- Windows SMTC controls, resizing, queue/lyrics interaction and all account UI
  flows in the final package.
- A live sample of reliable word-timed lyrics, line-timed lyrics, static lyrics,
  missing lyrics, instrumental sections and translation/romanization availability.
  Unit tests of a lyric clock cannot establish provider coverage for these cases.
- Full visual refinement across every requested screen, Windows light appearance,
  accessibility review, performance profiling, account migration and public-release
  dependency/security review.

## Provider and privacy boundaries

KLYRA runs the inherited catalog/resolver/player code locally; playback does not
require using BitChord as an account backend. Upstream presence reporting and
automatic update checks are disabled, and no default BitChord party server is
configured. Credits and optional integration links retain upstream references.

Music/lyrics/artwork services receive network requests and can observe the client's
IP and requested content. The unofficial YouTube integration can stop working or
be restricted by region, account or upstream changes. Successful technical playback
does not establish official support or permission for extraction, background audio,
ad removal or offline copying. No unrestricted-download or permanent-ad-free claim
is made. See `KLYRA.md` for policy references and GPL obligations.

KLYRA account sessions use encrypted storage; public Supabase configuration is not
an administrator credential. No service-role key is bundled. This is not a full
security audit, and the build must not be described as "100% safe."
