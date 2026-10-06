#!/usr/bin/env bash
# Captures and composes Google Play phone screenshots from a connected device.
#
#   ./store/render-screenshots.sh home            # capture the current screen
#   ./store/render-screenshots.sh home settings   # one capture per name
#
# Navigate the phone to the screen you want first: the script does not drive the
# UI (Play wants real app screens, and scripted taps would be brittle).
#
# For each name it:
#   1. captures the device screen   -> store/screenshots/raw-<name>.png
#      (1080x2424 on the Pixel 9a; any 20:9 phone works the same way)
#   2. samples the app's background colour from that capture, so the letterbox
#      bars blend into the app instead of showing a seam
#   3. composes an opaque 1080x1920 (9:16) PNG -> store/screenshots/play-<name>.png
#
# Why recompose at all: Play wants 24-bit PNG/JPEG with no alpha, 320-3840px per
# side and an aspect ratio between 16:9 and 9:16. The phone shoots ~20:9
# (1080x2424), which is outside that range, so the capture is scaled to 1920px
# tall and centred on the sampled background colour.
#
# Requirements: adb on PATH (or ADB=/path/to/adb) and google-chrome (or CHROME=).
# Testing without a device: FROM=/path/to/capture.png ./store/render-screenshots.sh test
#
# NOTE: store/screenshots/ is gitignored on purpose: real captures show trip
# addresses, which are personal data and this repository is public.
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
adb="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"
chrome="${CHROME:-google-chrome}"
common=(--headless=new --disable-gpu --no-sandbox --hide-scrollbars --allow-file-access-from-files)

if [ "$#" -eq 0 ]; then
  echo "usage: $0 <name> [name...]   (e.g. $0 home settings report)" >&2
  exit 2
fi

out_dir="$here/screenshots"
mkdir -p "$out_dir"

report() {
  python3 - "$1" <<'PY'
import struct, sys, os
path = sys.argv[1]
data = open(path, "rb").read()
width, height = struct.unpack(">II", data[16:24])
ctype = {0: "gray", 2: "RGB (no alpha)", 3: "palette", 4: "gray+alpha", 6: "RGBA (alpha)"}[data[25]]
print(f"  {os.path.basename(path)}: {width}x{height}, {ctype}, {len(data)/1024:.0f} KB")
PY
}

for name in "$@"; do
  raw="$out_dir/raw-$name.png"
  play="$out_dir/play-$name.png"

  if [ -n "${FROM:-}" ]; then
    cp "$FROM" "$raw"   # test path: reuse an existing capture
  else
    # Safety net before capturing anything from a real phone: Bluetooth MAC
    # addresses are personal data, this repository is public, and the store
    # listing is public too. Refuse to capture a screen that shows one — e.g.
    # collapse the Settings device list first. Set FORCE=1 to override.
    # uiautomator cannot print to stdout: it writes a file, so dump to the
    # device, read it back, then delete it.
    remote="/sdcard/kmexpense-ui-dump.xml"
    "$adb" shell uiautomator dump "$remote" >/dev/null 2>&1 || true
    ui="$("$adb" shell cat "$remote" 2>/dev/null | tr '<' '\n' || true)"
    "$adb" shell rm -f "$remote" >/dev/null 2>&1 || true
    if [ "${FORCE:-0}" != "1" ] && printf '%s' "$ui" | grep -qE '([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}'; then
      echo "REFUSING to capture '$name': the screen shows a MAC address (personal data)." >&2
      echo "Collapse the device list / hide the sensitive row, then retry (FORCE=1 to override)." >&2
      exit 1
    fi
    "$adb" exec-out screencap -p > "$raw"
  fi

  # Sample the app background (see sample-color.html); fall back to the app's
  # dark background if the page cannot be parsed.
  bg="$( { "$chrome" "${common[@]}" --dump-dom \
            "file://$here/sample-color.html?img=file://$raw" 2>/dev/null \
            || true; } | grep -oE '#[0-9A-F]{6}' | head -1 )"
  bg="${bg:-#12151A}"
  echo "$name: background $bg"

  "$chrome" "${common[@]}" --window-size=1080,1920 --screenshot="$play" \
    "file://$here/compose-screenshot.html?img=file://$raw&bg=$(printf '%s' "$bg" | sed 's/#/%23/')" \
    >/dev/null

  report "$play"
done
