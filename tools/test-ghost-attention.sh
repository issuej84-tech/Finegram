#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
CLASSES="$ROOT/TMessagesProj/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes"
[ -f "$CLASSES/com/th3nekit/finegram/privacy/FinegramGhostAttentionSync.class" ] || { echo 'Compile :TMessagesProj:compileDebugJavaWithJavac first.' >&2; exit 1; }
TEMP="$(mktemp -d)"
trap 'rm -rf "$TEMP"' EXIT
javac --release 17 -cp "$CLASSES:$SDK/platforms/android-36/android.jar" -d "$TEMP" "$ROOT/tools/material-tests/privacy/GhostAttentionCursorTest.java"
java -cp "$CLASSES:$SDK/platforms/android-36/android.jar:$TEMP" com.th3nekit.finegram.privacy.GhostAttentionCursorTest
