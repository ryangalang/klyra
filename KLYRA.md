# KLYRA foundation — experimental derivative

Every song, a little closer.

This is the isolated implementation track selected by the project owner on 2026-10-27. It is a modified GPLv3 derivative of BitChord, not an official BitChord or YouTube product. The original KLYRA Android/Windows project remains in the parent directory, including its Supabase accounts, library synchronization, local playback and Audius playback. No migration or replacement has occurred yet.

## Provenance and licensing

- Upstream: https://github.com/kushagrasinghx/BitChord
- Starting branch: `desktop-build-30`
- Starting revision: `7c673dcdbb5a6f8cc36538eb8813c559e37c26b2`
- Original copyright notices and `LICENSE` remain applicable. KLYRA modifications to this covered work are supplied under GPL version 3.
- Renaming, new artwork, or rewriting individual screens does not remove GPL obligations for the derivative.
- Before distributing binaries, provide the exact corresponding source, modifications, build scripts and dependency information, with the GPL and retained notices. Do not include secrets, signing keys, account data or cookies. Do not rely on an upstream link alone to provide source for a modified binary.
- External dependencies and assets have their own licenses. Apple SF font assets must not be redistributed in KLYRA; use platform fonts. Source archive preparation must exclude those removed assets and preserve required notices for included dependencies.

## Decision

| Approach | Benefit | Cost | Decision |
| --- | --- | --- | --- |
| Staged derivative | Retains shared catalog/resolver, Android Media3 service, desktop FFmpeg/WASAPI, queue and timed lyrics | GPL compliance, original UI, accounts migration and baseline repair | Selected |
| Copy selected components | Keeps original app shell | Coupled auth, model, resolver and queue contracts require substantial integration; GPL still applies to covered combined work | Not the initial path |
| Independent implementation | Maximum implementation control | Repeats extraction, compatibility and native player work; does not avoid provider-policy risks | Not the initial path |

## Playback limits

The inherited YouTube Music catalog uses undocumented InnerTube endpoints. Native playback resolves streams through InnerTubeX and NewPipe, with platform-specific token support. This is technically distinct from the official YouTube Data API and embedded player. It is not officially supported or licensed by Google merely because the source is open source.

YouTube's terms and developer policies restrict automated/undocumented access, extracting audio, hidden/background playback, interfering with ads and unauthorized offline copies. A successful local test does not establish permission. Provider changes, token/challenge requirements, expired URLs, region, account, age and paid-content restrictions can cause failures. No promise of permanent availability, ad-free service or unrestricted downloads is made. Native local music remains an independent fallback.

Sources reviewed: https://www.youtube.com/static?template=terms and https://developers.google.com/youtube/terms/developer-policies. Fonts: https://developer.apple.com/documentation/technologyoverviews/fonts.

## Milestones and acceptance

1. Reproduce Android and Windows builds, retain provenance, isolate package/data identity, remove incompatible redistributed assets and upstream update/service defaults.
2. Exercise catalog search, actual stream resolution and native playback, including error handling and local-source fallback. Record provider, time, platform and outcome; mocks do not establish availability.
3. Apply original KLYRA editorial surfaces, separate desktop navigation/player layout, artwork-responsive Now Playing and lyrics, and accessible motion. Retain callback contracts and one playback clock.
4. Port KLYRA Supabase account/session/library functionality without importing Google cookies or merging user libraries silently. Test ownership and conflicts before switching users to the derivative.
5. Test installed APK and packaged Windows application: queue edits, seeking, lyrics, restart, background, system controls and interruptions. Physical-device-only behavior remains unverified until hardware is available.

This document is a plan and provenance record, not a claim that these acceptance criteria have passed.
