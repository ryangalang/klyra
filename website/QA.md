# Website verification — November 3, 2026

The current preview preserves Ryan's landing-page edits and floating navigation.
The lyrics demo now displays its original cover, title, artist and demo album.
The card uses overflow: clip so keyboard focus cannot scroll its header away.
The motion demo remains silent, labelled original sample text; it does not claim
that this sample is Novo Amor's lyrics or synchronized commercial audio.

## Checks performed this session

- Final production Next.js build passed, including TypeScript and static pages.
- All 10 download integration tests passed against the release bundle
  (Klyra-v32-2026-11-02). ZIP integrity and corresponding app source checks
  passed; all seven binary/source asset hashes match the release checksum file.
- Logs: artifacts/website-release32-build.log and
  artifacts/website-release32-tests.log in the parent workspace.
- Browser at mobile and desktop sizes: title, artist, album and cover remain visible;
  no horizontal page overflow at 390 and 1280 pixels.
- Lyric play, pause, seek and restart controls checked against their visible state.
- Local downloads support HTTP ranges and return the actual APK/ZIP files.
- Android is 1.8-klyra.7 (version code 30); Windows is 1.8-klyra.7
  (native package 1.8.32). The primary Windows download is the Setup EXE.
- Download variant selection was checked at desktop/mobile widths. The new release
  disclosure describes playlist controls, the updater and the optional Android cover.
- Existing Android Media3 session and metadata supply automatic system media controls
  and artwork during playback. The separate immersive cover is manually opened;
  Android controls the native lock-screen layout. No physical phone was connected.

## Publication status

The site has not been deployed to Vercel. The production download manifest points
to ryangalang/klyra tag v1.8-klyra.7, published on November 3.
Local downloads use the final artifact folder configured in .env.local. The
production route checks asset availability before redirecting to GitHub. The
unauthenticated GitHub latest-release API returns v1.8-klyra.7 (not draft or
prerelease); all 12 uploaded assets have sizes and SHA-256 digests matching their
local files. Evidence: artifacts/klyra-public-release-verification.json and
artifacts/klyra-published-v7.png in the parent workspace. Redeploy the website to
Vercel to publish its updated download manifest and release details.

Responsive checks use browser viewport sizing, not physical-device testing.
Screenshots are saved locally under ignored qa/.
