# Klyra website

Next.js App Router, React, TypeScript and Tailwind CSS v4. A monochrome, responsive
download website with original Klyra interface illustrations. Developed by Ryan.
The installed Android/Windows application is not modified by this website.

## Run locally

Use Node.js 22 or later. From this folder:

```powershell
npm ci
npm run dev
```

Open http://127.0.0.1:3000. For local downloads, set this in ignored `.env.local`:

```dotenv
KLYRA_LOCAL_RELEASE_DIR=C:/app projects/music/artifacts/Klyra-v29-2026-10-29
```

The download handler streams only the seven allowlisted release files and supports
HEAD and byte-range requests. Local file delivery is disabled on Vercel. Never
copy app `local.properties`, keys or signing credentials into this site.

## Deploy on Vercel

1. Commit and push the `website/` folder to your GitHub code repository.
2. Import the repository in Vercel. Set **Root Directory: website**.
3. Select **Next.js**, Node.js **22.x**, Install Command `npm ci`, Build Command
   `npm run build`. Leave Output Directory at its framework default.
4. Set `KLYRA_DOWNLOAD_REPOSITORY` to the **public** GitHub repository holding
   the release files, such as `ryangalang/klyra` or a separately created downloads
   repository. This does not have to be the private development repository.
5. Optionally set `NEXT_PUBLIC_SITE_URL` to the final HTTPS domain. Otherwise the
   site's social metadata uses Vercel's production domain automatically.
6. Deploy. Do not set `KLYRA_LOCAL_RELEASE_DIR` on Vercel.

The site is prepared for Vercel but has not been deployed by this task.

## Make the direct downloads public

A website visitor clicks **Download APK** or **Download for Windows** and receives
the actual file. No GitHub repository page is shown in the normal download flow.
GitHub Releases stores the large binaries; Vercel serves the small website and
redirects the browser directly to the selected release asset.

Create a public GitHub Release with the exact tag **v1.8-klyra.4**, then upload:

- Klyra-Android-arm64.apk
- Klyra-Android-universal.apk
- Klyra-Android-arm32.apk
- Klyra-Android-x64-emulator.apk
- Klyra-Windows-x64.zip
- Klyra-corresponding-source.zip
- SHA256SUMS.txt

These verified files are in `../../artifacts/Klyra-v29-2026-10-29/` in Ryan's local
workspace. Also include LICENSE and the supplied verification/build notes in the
release. Do not upload binaries into the normal Git source history.

The code repo may remain private, but public downloads need a publicly readable
release host. Hiding a repository link on the landing page does not change GitHub
asset permissions. No repository visibility was changed by this task. The current
public availability of `ryangalang/klyra` and its release has not been confirmed.
Until a public release is published, deployed buttons show an honest availability
page instead of silently redirecting to a missing file. GitHub responses are
cached for up to five minutes. A private GitHub token is deliberately not shipped
to visitors or used to expose private repository files.

The only personal GitHub social link shown on the main page is Ryan's profile.
The GPL app-source download and upstream attribution are retained separately.

## Validation

```powershell
npm run build
npm run typecheck
# With the local server running and the real release directory configured:
npm test
```

The integration tests check download headers, byte ranges, invalid assets,
checksum-manifest integrity, and the complete Android APK's SHA-256 over HTTP.
See `QA.md` for actual results and any remaining limitations.

## Updating a release

Update `lib/release.ts` with the exact version/tag, file names, sizes and hashes.
Upload matching binaries and corresponding app source before pointing the site
at the new tag. Rebuild/redeploy the website. Keep previous releases available.

## Design and licensing

- Original code-native interface and album-art illustrations; not device screenshots
  or a claim that illustrative tracks are available in the provider catalog.
- System fonts only; no redistributed Apple fonts. Reduced-motion preference honored.
- No analytics, signup form, ad pixels or marketing cookies on this website.
- App provenance: a modified BitChord GPLv3 derivative. Matching app source is a
  separate download. Website modifications in this repository are under GPLv3;
  retain LICENSE and upstream notices where applicable.
