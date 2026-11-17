# Servo Browser for Android

A full Android browser UI built around the [Servo](https://github.com/servo/servo) engine. This
repository is the Android application layer: a native Kotlin / Jetpack Compose browser that
renders pages through Servo's JNI surface, plus the CI pipeline that builds the engine from
source and publishes release APKs.

> **Status:** early release (0.1.0). The application, engine bridge and build pipeline are in
> place. Web compatibility is bounded by Servo itself, which is not a drop-in Chromium
> replacement. See [Limitations](#limitations).

---

## What is here

| Path | Purpose |
| --- | --- |
| `app/src/main/java/org/servo/servoview/JNIServo.kt` | JNI declarations matching upstream `Java_org_servo_servoview_JNIServo_*` symbols |
| `app/src/main/java/com/servodroid/browser/engine/` | Engine abstraction: native Servo driver and a WebView development fallback |
| `app/src/main/java/com/servodroid/browser/ui/` | The whole Compose browser UI (chrome, new tab, tabs, history, bookmarks, settings) |
| `app/src/main/java/com/servodroid/browser/data/` | SharedPreferences-backed history / bookmarks / settings |
| `.github/workflows/android.yml` | Builds `libservoshell.so` from servo/servo, assembles the APK, publishes releases |

### Engine integration

Servo does not publish a stable Android AAR, so the engine is built from source in CI:

```text
servo/servo  (components/servo → ports/servoshell)
        │  ./mach build --android --target aarch64-linux-android
        ▼
libservoshell.so
        │  copied to app/src/main/jniLibs/arm64-v8a/
        ▼
JNIServo.kt  ── single-threaded GL dispatcher ──▶  ServoEngine.kt
        │
        ▼
Compose SurfaceView  ── ANativeWindow ──▶  Servo rendering context
```

The JNI surface used here is the same one upstream Servo exposes from
`ports/servoshell/egl/android/mod.rs`: `init`, `addPlatformWindow`, `resize`, `loadUri`,
`reload`, `stop`, `goBack`, `goForward`, `touchDown/Move/Up/Cancel`, `doFrame`,
`pausePainting`, and the `Callbacks` interface (`onLoadStarted`, `onLoadEnded`,
`onTitleChanged`, `onUrlChanged`, `onHistoryChanged`, …). All native calls are funnelled onto
one thread, as Servo requires.

When `libservoshell.so` is absent, `EngineFactory` selects a WebView-backed fallback so the UI
remains testable. The settings screen reports which engine is active.

---

## Features

- Address bar with URL/search resolution, secure indicator and animated load progress
- Search suggestions drawn from local history
- Multiple tabs with a private-tab group
- Back / forward / home / bookmark / share bottom bar
- History, bookmarks and downloads screens
- Find in page, desktop-site toggle, share sheet
- Settings: theme (system/dark/light), search engine, clear browsing data, engine status
- Dark-first design with a restrained light theme
- Keyboard, focus and content-description support; 48dp touch targets

---

## Design direction

The interface was designed with `ui-styling`, `ui-ux-pro-max` and the `unslop` methodology.
The direction derived from the product:

- **Product** — an independent Android browser for people who want an alternative web engine.
- **Character** — technical, composed, independent.
- **World** — the engine room of the web: instrument panels and terminals, not consumer gloss.
- **Moments** — the rendered web page is the single bright element; the chrome stays quiet.
- **Signature** — a thin load-progress line under the toolbar and a gear mark in About.

Concretely: one tinted graphite ramp (`#0B0E11` → `#232C33`), three text roles, one jade accent
(`#5FC79A`), a quiet violet reserved for private mode, IBM Plex Sans for interface text and
JetBrains Mono for addresses and metadata. All spacing sits on a 4dp grid; icons are one family
and one size. No gradients, glows, emoji icons or decorative motion.

---

## Building

You do not need to run Gradle or the NDK locally — CI does it. To build locally anyway:

```bash
# Requirements: JDK 17, Android SDK 35, NDK r29, Rust with aarch64-linux-android target.
git clone https://github.com/HakaI963/servo-android-browser.git
cd servo-android-browser

# Optional: fetch a prebuilt engine instead of building Servo yourself.
# Put the URL in gradle.properties as: servo.engine.url=https://.../servo.aar

./gradlew :app:assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

To build the engine from source locally (the slow path):

```bash
git clone --recurse-submodules https://github.com/servo/servo.git servo
cd servo && ./mach bootstrap --yes --skip-lints --skip-nextest
export ANDROID_NDK_ROOT=$ANDROID_SDK_ROOT/ndk/29.x
./mach build --android --target aarch64-linux-android --profile checked-release
mkdir -p ../app/src/main/jniLibs/arm64-v8a
cp target/aarch64-linux-android/checked-release/libservoshell.so ../app/src/main/jniLibs/arm64-v8a/
```

Releases are cut by pushing a `v*` tag; the `release` job in the Android workflow attaches the
built APK to the GitHub Release.

---

## Limitations

- Servo's web compatibility is incomplete; complex JS sites, DRM, WebRTC and some auth flows
  may not work. This is an engine limitation, not an app bug.
- The JNI shell supports a single window and has no multiprocess mode on Android.
- One engine instance is reused across tabs: switching tabs reloads the active URL rather than
  preserving per-tab scroll/form state.
- Downloads are shown as an empty screen until a download manager is wired to Servo.
- Release APKs produced by CI are signed with the Android debug key. Supply a real keystore for
  distribution.
- ABI coverage in CI is ARM64. Upstream can also build `armv7-linux-androideabi` and
  `x86_64-linux-android`.

---

## Credits and licensing

- Browser engine: [Servo](https://github.com/servo/servo), © Servo Project contributors,
  licensed under the [Mozilla Public License 2.0](LICENSE).
- UI direction follows [unslop](https://github.com/heydamir/unslop) by Damir Sokolovsky,
  licensed CC BY 4.0.
- Design system via `ui-ux-pro-max` and styling patterns via `ui-styling`.
- Fonts: IBM Plex Sans and JetBrains Mono, licensed under the SIL Open Font License 1.1.

See [NOTICE](NOTICE) for attribution details.
