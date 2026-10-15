# Flowseal transport and wallpaper fixes

The Android `TgwsRoute` / `TgwsDomains` adapter already present in Finegram follows [Flowseal/tg-ws-proxy](https://github.com/Flowseal/tg-ws-proxy), not its desktop tray UI. Reference reviewed: `307a384ab7f522e34bb1a045884d0bd90f40bee5`. The upstream MIT notice is preserved in `third-party/Flowseal-tg-ws-proxy-LICENSE.txt`.

This mode supports the existing Android WebSocket transport through the project's Cloudflare domains, then direct Telegram WebSocket. It is not a bundled copy of the Python desktop program, and does not claim feature parity with its optional HTTP/2, Worker, Fake TLS or desktop integration.

- Flowseal is the default for a fresh installation; an explicitly saved server selection is preserved.
- Flowseal mode does not silently fall back to a Finegram data relay or request its authorization credentials.
- Domain refresh uses the official Flowseal repository only; offline fallback uses the already bundled upstream-derived list. Lists previously fetched from the Finegram mirror are not reused.
- Certificate and hostname verification remain enabled. MTProto remains encrypted; the proxy cannot guarantee access on every network.
- The explicitly selected Finegram server mode and separately configured call bypass remain separate features.

## Wallpaper fixes

- Stock Finegram folder cover illustrations behind the main heading/folder tabs/stories are hidden in Material mode or while a global user wallpaper is selected. Existing folder cover preferences/files are not deleted.
- Global chat wallpapers use the same window crop as the surrounding app; legacy chat parallax/translation bounds are ignored only for these global drawables.
- Chat-specific wallpaper drawable types retain the original rendering path.

## Reproduction

Install the full debug APK with package `com.th3nekit.finegram.material` in a disposable emulator, then run:

```bash
FINEGRAM_LIVE_NETWORK=true ANDROID_SERIAL=emulator-5554 ./tools/test-material-full-client.sh
```

The runner modifies only the test package's local appearance preferences and wallpaper. Never use it on an account device. It checks the native chat `SizeNotifierFrameLayout` rendering against the same window crop through 24 transition frames, stock cover policy, per-chat drawable isolation and refusal to enter a Finegram data relay in Flowseal mode. Optional network checking requires a real TLS handshake and HTTP 101 WebSocket upgrade; a TCP ping alone is not counted as a successful proxy connection. Account authorization and chats on a blocked mobile carrier still need owner testing.

## Verified build

The full universal debug APK rebuilt successfully after these changes. On the read-only Android 15 emulator: 804 full-client regression assertions (including the real chat background component), 218 native image/preference assertions, and the existing 13-step UI smoke all passed. The Flowseal endpoint completed certificate-verified TLS and HTTP 101 WebSocket upgrade. Normal cold launch of the full client also passed. This is not a claim that account sign-in, full message delivery or every carrier block has been tested.

An initialization re-entry guard also keeps Telegram Theme from reading a half-built Material role map during a cold wallpaper lookup; that path is exercised by the full-client regression.
