# KLYRA — Rhythm identity

The owner selected concept **03 Rhythm** on 2026-10-29. The approved concept
sheet is retained in `approved-concepts.png`. `rhythm-mark.svg` is its flat,
scalable K silhouette; `rhythm-icon.svg` adds the dark rounded-square tile.
There is no dot. This is a branding change, not a redesign of the application UI.

Run `python branding/generate_icons.py` with Pillow installed to regenerate the
Android vector drawables, adaptive and legacy launcher assets, shared UI mark,
notification silhouette, and Windows SVG/PNG/ICO assets. Platform sizes use the
same path; the adaptive icon respects circular-mask safe bounds.

Visible app labels are **KLYRA**. Android's existing application ID and test signing
key remain unchanged so this build can update the previous installation. Existing
Windows credential, cloud and browser-profile directories retain their internal
`KLYRA Preview` names to preserve data. These are not application display labels.

BitChord provenance, copyright notices, GPL text and corresponding-source
requirements remain intact in the legal section and source distribution.
