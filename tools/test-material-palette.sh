#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
AAR="${MATERIAL_AAR:-$(find "$HOME/.gradle/caches/modules-2/files-2.1/com.google.android.material/material/1.13.0" -name '*.aar' -print -quit)}"
[ -f "$AAR" ] || { echo 'Resolve Material 1.13.0 first, or set MATERIAL_AAR.' >&2; exit 1; }
TEMP="$(mktemp -d)"
trap 'rm -rf "$TEMP"' EXIT
unzip -p "$AAR" classes.jar > "$TEMP/material.jar"
javac -cp "$TEMP/material.jar" -d "$TEMP/classes" \
  "$ROOT/TMessagesProj/src/main/java/com/th3nekit/finegram/appearance/material/MaterialPalette.java" \
  "$ROOT/tools/material-tests/MaterialPaletteTest.java" \
  "$ROOT/TMessagesProj/src/main/java/com/th3nekit/finegram/appearance/material/VoiceProgressColors.java" \
  "$ROOT/tools/material-tests/VoiceProgressColorsTest.java"
java -cp "$TEMP/material.jar:$TEMP/classes" MaterialPaletteTest
java -cp "$TEMP/classes" VoiceProgressColorsTest
