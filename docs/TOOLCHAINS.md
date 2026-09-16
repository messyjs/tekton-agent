# ⚔ App Toolchains — what you need to build for every target

Installer shortcut: `--toolchains "web,windows,linux,android,ios,vst,games,macos"`
(automates what is automatable; prints the exact commands for the heavy, GUI-based ones).

| Target | Toolchain | Automatable | Manual steps |
|---|---|---|---|
| **Web / HTML** | Node 20+ (core) | ✅ already installed | — |
| **Windows apps** (Tauri) | Rust + MSVC Build Tools + WebView2 | `ensure_rust` + winget VS BuildTools | none |
| **macOS apps** (Tauri) | Rust + Xcode CLT | rustup | `xcode-select --install` |
| **Linux/Ubuntu apps** (Tauri) | Rust + build-essential, libwebkit2gtk-4.1-dev, libgtk-3-dev, libayatana-appindicator3-dev, librsvg2-dev, libssl-dev, patchelf | apt one-liner | — |
| **Android apps** | Rust + android targets + JDK 17 + Android SDK/NDK | `rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android` | Install Android Studio → SDK + NDK → set `ANDROID_HOME`, `NDK_HOME` |
| **iOS apps** | Rust + iOS targets + full Xcode (macOS only) | `rustup target add aarch64-apple-ios aarch64-apple-ios-sim` | Install Xcode from App Store |
| **VST instruments / audio plugins** | C++ toolchain (MSVC / Xcode CLT / gcc) + CMake + **JUCE** | cmake via winget/brew/apt | Clone JUCE; Tekton builds the plugin skeleton |
| **Games** | Per engine: Unity Hub, Unreal, Godot (winget/brew/apt); HTML5 games = Node only | Godot via package managers | Unity/Unreal installers are interactive |
| **Cross-platform default** | `--toolchains "web"` costs nothing extra — Agent OS + dashboard are pure Node | ✅ | — |

## Platform matrix

| | Windows | macOS | Linux | Android | iOS |
|---|---|---|---|---|---|
| tekton CLI | ✅ | ✅ | ✅ | ✅ (Termux) | — (via tunnel) |
| Agent OS App | ✅ | ✅ | ✅ | ✅ | 🔜 (`tauri ios`) |
| Mobile chat | ✅ tunnel | ✅ tunnel | ✅ tunnel | ✅ browser/app | ✅ browser |

## Context home

Everything lives in **`~/.tekton/`** (`TEKTON_HOME` env to relocate):
`tekton.md` (context root) · `config.yaml` · `models.json` · `settings.json` · `auth.json` ·
`projects.json` · `engines.yaml` · `MEMORY.md` · `SOUL.md` · `USER.md` ·
`skills/` · `extensions/` · `sessions/` · `checkpoints/` · `memory/` · `cron/`.
OpenViking's recall store is separate: `~/.openviking` + server on port 1933.
