# Play store assets

Branding images for the Google Play listing, and the scripts that render them.
Nothing here is needed to build or run the app.

| File | Size / format | Play requirement |
| --- | --- | --- |
| `icon-512.png` | 512×512, 32-bit PNG **with alpha**, ~14 KB | store icon; max 1024 KB |
| `feature-graphic-1024x500.png` | 1024×500, 24-bit PNG, no alpha, ~149 KB | feature graphic |
| `screenshots/play-*.png` | 1080×1920 (9:16), 24-bit PNG, no alpha | phone screenshots, min 2 |

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

`feature-graphic.html` is the source: mint hairline, the icon on the left, the
wordmark, a two-line tagline and an amber "Local-only · no account · no ads"
pill. Text stays inside a 56px margin because Play crops this banner in some
placements. The store title is `TripToExcel - Km Logger` (see the release
plan); the graphic uses the shorter wordmark, which is also the launcher label.

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

## Privacy

`screenshots/` is **gitignored**: real captures show trip addresses (home, work),
which are personal data and this repository is public. The composed PNGs are
local files that get uploaded to the Play Console; the repository only keeps the
non-personal sources (`icon.svg`, the two HTML templates and the scripts).

Play also requires the images to be free of personal data, so pick a date range
with neutral addresses (or a test drive) if you would rather not show your own.
