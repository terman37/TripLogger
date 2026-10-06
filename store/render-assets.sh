#!/usr/bin/env bash
# Regenerates the Google Play listing images that are not screenshots.
#
# Requirements: a Chromium-based browser on PATH as `google-chrome` (or set
# CHROME=/path/to/chrome). No ImageMagick, no Python imaging library needed:
# everything is rendered by headless Chrome.
#
#   ./store/render-assets.sh
#
# Outputs (overwritten):
#   store/icon-512.png               512x512, 32-bit PNG with alpha, < 1 MB
#   store/feature-graphic-1024x500.png  1024x500, 24-bit PNG, no alpha
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
chrome="${CHROME:-google-chrome}"
common=(--headless=new --disable-gpu --no-sandbox --hide-scrollbars)

# PNG colour type lives in the IHDR chunk; report it so a wrong alpha channel is
# caught here instead of by the Play Console upload form.
report() {
  python3 - "$1" <<'PY'
import struct, sys, os
path = sys.argv[1]
data = open(path, "rb").read()
assert data[:8] == b"\x89PNG\r\n\x1a\n", f"{path}: not a PNG"
width, height = struct.unpack(">II", data[16:24])
ctype = {0: "gray", 2: "RGB (no alpha)", 3: "palette", 4: "gray+alpha", 6: "RGBA (alpha)"}[data[25]]
print(f"  {os.path.basename(path)}: {width}x{height}, {ctype}, {len(data)/1024:.0f} KB")
PY
}

# Play store icon: 32-bit PNG *with* alpha, artwork must fill the tile.
# icon.html renders icon.svg at 508x508 inside a 512x512 transparent page: the
# 2px inset is what makes Chrome keep the alpha channel (fully opaque pixels
# would be written as a 24-bit RGB PNG, which Play rejects).
"$chrome" "${common[@]}" --default-background-color=00000000 --window-size=512,512 \
  --screenshot="$here/icon-512.png" "file://$here/icon.html"

# Feature graphic: 1024x500, opaque (Play rejects transparency here).
"$chrome" "${common[@]}" --window-size=1024,500 \
  --screenshot="$here/feature-graphic-1024x500.png" "file://$here/feature-graphic.html"

report "$here/icon-512.png"
report "$here/feature-graphic-1024x500.png"
