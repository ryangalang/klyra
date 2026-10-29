# Website verification — October 30, 2026

## Passed

- Next.js production build, including TypeScript and static page generation.
- All 10 download integration tests against the production server.
- All seven release files: attachment name, actual file size, and first 32 bytes
  via a resumable HTTP range request matched the files on disk.
- Full ARM64 APK downloaded over HTTP and matched the release SHA-256.
- Checksum manifest matched byte-for-byte; unknown assets returned 404 and
  unsatisfiable ranges returned 416.
- Browser widths 390, 820 and 1280: no horizontal page overflow.
- Mobile menu, section links, Android build selection, all-download disclosure,
  FAQ disclosure and checksum-copy feedback.
- No error-level browser console messages in the final production preview.
- Removed color selection; black/white design and matching social preview image.

Responsive checks use browser viewport sizing, not physical device testing.
The page uses explicitly labelled original interface illustrations.
This website task did not change or retest app playback or account synchronization.

## Publication remaining

The website has not been deployed to Vercel. Public APK/ZIP hosting is not yet
configured or verified. Local direct downloads work with `.env.local` pointing to
the verified v29 artifact directory. A deployed site requires a public GitHub
Release containing the named files and matching GPL source; see README.md.
No repository was made public, and no private token is exposed to visitors.

Screenshots from browser review are saved locally under ignored `qa/`.
