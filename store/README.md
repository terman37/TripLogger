# Play store assets

Branding images for the Google Play listing, and the scripts that render them.
Nothing here is needed to build or run the app.

| File | Size / format | Play requirement |
| --- | --- | --- |
| `icon-512.png` | 512×512, 32-bit PNG **with alpha**, ~14 KB | store icon — carries no text, so it is shared by both listings |
| `feature-graphic-fr-1024x500.png` | 1024×500, 24-bit PNG, no alpha, ~149 KB | French banner (default listing) |
| `feature-graphic-en-1024x500.png` | 1024×500, 24-bit PNG, no alpha, ~147 KB | English banner (`en-US` listing) |
| `screenshots/play-*-fr.png`, `…-en.png` | 1080×1920 (9:16), 24-bit PNG, no alpha | phone screenshots, four per language (min 2) |

## Regenerating

```bash
./store/render-assets.sh                      # icon + feature graphic
./store/render-screenshots.sh home settings   # capture the current device screen
```

Both scripts only need `google-chrome` (headless) — no ImageMagick, no Python
imaging library. They print each output's dimensions and PNG colour type so a
missing alpha channel (icon) or an unwanted one (screenshots) is caught here
rather than by the Play Console upload form.

### Icon

`icon.svg` is the source, kept deliberately in sync with the app's adaptive-icon
layers (`drawable/ic_launcher_background.xml` + `ic_launcher_foreground.xml`):
dark tile, mint S-shaped road with a dark centre stripe, amber car. The artwork
is scaled 1.25× about the centre so it fills the tile. `icon.html` renders it at
508×508 inside a 512×512 transparent page — the 2px inset is what makes Chrome
keep the alpha channel, because a fully opaque page is written as a 24-bit RGB
PNG, which Play rejects. Play adds the rounded corners and drop shadow itself, so
neither is drawn here.

### Feature graphic

One banner per listing language, same layout: `feature-graphic-fr.html` and
`feature-graphic-en.html` — mint hairline, the icon on the left, the wordmark, a
two-line tagline and an amber pill ("Données locales · ni compte · ni pub" /
"Local-only · no account · no ads"). Text stays inside a 56px margin because Play
crops this banner in some placements. The French banner uses a slightly smaller
icon card and tagline because the French line is longer. The store title is
`TripToExcel - Km Logger` in both languages (see the release plan); the banner
uses the shorter wordmark, which is also the launcher label.

### Screenshots

`render-screenshots.sh` captures the *current* device screen with `adb
screencap`, samples the app's background colour from that capture
(`sample-color.html`, rendered by headless Chrome) and composes an opaque
1080×1920 PNG (`compose-screenshot.html`). Recomposing is necessary because the
phone shoots 1080×2424 (~20:9) and Play accepts only 16:9…9:16 with the long side
at most 2× the short side. Because the letterbox uses the sampled background
colour — measured the same in the bars and the app content — the padding is not
visible.

Navigate the phone to the screen before running it per name; the script does not
drive the UI.

Suggested set (Play: minimum 2, maximum 8 per device type):

| Name | Screen to open first |
| --- | --- |
| `home` | Home tab, monitoring on ("Waiting for …" or "Recording") |
| `report` | Report tab with a date range that contains trips |
| `settings` | Settings tab (registered device, grace slider, About row) |
| `about` | Settings tab with the About dialog open |

## Data shown in the screenshots

The current sets use **seeded sample data**, not the owner's trips:
`app/src/androidTest/.../tools/SampleDataSeederTest.kt` inserts a Paris → Orléans
→ Limoges → Toulouse drive in three legs plus a local Toulouse hop, a fake
registered device and monitoring on. Public addresses only, no personal data.
See the "How to reproduce" bullet in release_guide.md §Phase L4 for the exact
commands (and the three traps: `connectedDebugAndroidTest` uninstalls the app
afterwards, `SharedPreferences.apply()` needs a forced commit, and the four
permissions must be granted or monitoring switches itself off).

## Privacy

`screenshots/` is **gitignored** even so: captures can contain trip addresses,
which are personal data and this repository is public. The composed PNGs are local
files that get uploaded to the Play Console; the repository only keeps the
non-personal sources (`icon.svg`, the HTML templates, the sample-data seeder and
the scripts).
