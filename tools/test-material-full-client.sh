#!/usr/bin/env bash
# Requires a fresh disposable full debug client. Never runs on a physical/account device.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
SERIAL="${ANDROID_SERIAL:-emulator-5554}"
[[ "$SERIAL" == emulator-* ]] || { echo 'Refusing to run outside an emulator.' >&2; exit 1; }
BT="$SDK/build-tools/36.0.0"; AJ="$SDK/platforms/android-36/android.jar"; ADB="$SDK/platform-tools/adb"
D="$(mktemp -d)"; trap 'rm -rf "$D"' EXIT
mkdir -p "$D/classes" "$D/dex"
"$ADB" -s "$SERIAL" shell run-as com.th3nekit.finegram.material id >/dev/null
javac --release 17 -cp "$AJ" -d "$D/classes" "$ROOT/tools/material-tests/full-client/FullWallpaperRegression.java"
"$BT/d8" --lib "$AJ" --min-api 24 --output "$D/dex" "$D/classes/com/th3nekit/finegram/materialregression/FullWallpaperRegression.class"
"$BT/aapt" package -f -M "$ROOT/tools/material-tests/full-client/AndroidManifest.xml" -I "$AJ" -F "$D/test-unsigned.apk"
(cd "$D/dex" && zip -q -j "$D/test-unsigned.apk" classes.dex)
"$BT/zipalign" -f -p 4 "$D/test-unsigned.apk" "$D/test-aligned.apk"
export KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-finegram}" KEY_PASSWORD="${KEY_PASSWORD:-${KEYSTORE_PASSWORD:-finegram}}"
"$BT/apksigner" sign --ks "${KEYSTORE_PATH:-$ROOT/finegram-debug.jks}" --ks-key-alias "${KEY_ALIAS:-finegram}" --ks-pass env:KEYSTORE_PASSWORD --key-pass env:KEY_PASSWORD --out "$D/test.apk" "$D/test-aligned.apk"
"$ADB" -s "$SERIAL" install -r "$D/test.apk" >/dev/null
"$ADB" -s "$SERIAL" shell am instrument -w -e live_network "${FINEGRAM_LIVE_NETWORK:-false}" com.th3nekit.finegram.materialregression/com.th3nekit.finegram.materialregression.FullWallpaperRegression | tee "$D/result.log"
grep -q '^PASS:' "$D/result.log"
