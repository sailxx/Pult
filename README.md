<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="assets/banner-dark.svg">
    <img alt="Pult" src="assets/banner-light.svg" width="100%">
  </picture>
</p>

<p align="center">
  <b>Phone as a physical remote and desk dock for your Windows PC.</b>
</p>

<p align="center">
  Control volume, media, app launches, active windows, and power over local Wi-Fi in milliseconds.<br/>
  Zero cloud. Zero accounts. Zero telemetry. 32 KB host. Completely offline.
</p>

<p align="center">
  <a href="https://github.com/sailxx/Pult/releases/latest"><img alt="Latest release" src="https://img.shields.io/badge/release-v0.2.0-orange?style=flat-square" /></a>
  <a href="https://github.com/sailxx/Pult/releases/latest"><img alt="Platforms" src="https://img.shields.io/badge/platform-Windows%20%7C%20Android-blue?style=flat-square" /></a>
  <a href="LICENSE"><img alt="MIT license" src="https://img.shields.io/badge/license-MIT-black?style=flat-square" /></a>
  <a href="https://github.com/sailxx/Pult/actions/workflows/ci.yml"><img alt="CI status" src="https://img.shields.io/badge/build-passing-brightgreen?style=flat-square" /></a>
</p>

<p align="center">
  <a href="#quick-start">Quick Start</a> •
  <a href="#why-pult">Why Pult</a> •
  <a href="#features">Features</a> •
  <a href="#how-it-works">How It Works</a> •
  <a href="docs/PROTOCOL.md">Protocol</a> •
  <a href="SECURITY.md">Security</a> •
  <a href="#contributing">Contribute</a>
</p>

<p align="center">
  <img src="assets/hero.png" width="100%" alt="Pult - phone as a remote for Windows PC" />
</p>

---

## What is Pult?

Pult turns your Android phone into an instant hardware remote and second-screen dock for your Windows computer.

It is designed for real everyday moments:

* **Watching a movie from bed?** Pause playback, balance loud audio with the true volume slider, or turn off monitors without getting under the blankets.
* **Phone sitting on a desk stand?** One-tap dock to launch your browser, Spotify, terminal, or games without searching through taskbars.
* **Fullscreen game froze your mouse?** Open Pult on your phone, head to the Tasks tab, and kill the frozen window instantly.
* **No internet or router offline?** Pult works strictly peer-to-peer over your local Wi-Fi router or hotspot.

---

## Why Pult over alternatives?

Remote desktop tools are bloated, stream your entire screen with lag, and require cloud logins. Dedicated remote apps push subscriptions or require heavy runtimes.

Pult does one job and does it with zero overhead:

| Feature | Pult | Unified Remote | KDE Connect | TeamViewer / AnyDesk |
|---|:---:|:---:|:---:|:---:|
| **Zero accounts or sign-up** | ✅ | ⚠️ Push notifications | ✅ | ❌ Mandatory account |
| **Zero cloud / 100% offline local Wi-Fi** | ✅ | ⚠️ Cloud telemetry | ✅ | ❌ Cloud relay |
| **PC Host binary size** | **32 KB** | ~45 MB | ~110 MB | ~55 MB |
| **Dependencies on PC** | **None** (Native Win32/C#) | .NET Runtimes | KDE runtime pack | Proprietary drivers |
| **Real two-way volume sync (0-100%)** | ✅ Core Audio API | ⚠️ Blind keys | ⚠️ Blind keys | ❌ None |
| **Start menu app scan & launchpad** | ✅ Native | ⚠️ Paid addon | ❌ | ❌ |
| **Ultra-fast 60 fps wireless trackpad** | ✅ | ✅ | ✅ | ⚠️ High video lag |
| **Kill frozen windows / Task switcher** | ✅ | ❌ | ❌ | ⚠️ Sluggish stream |
| **Open source & MIT license** | ✅ | ❌ Proprietary | ✅ GPL | ❌ Proprietary |

---

## Key Capabilities

### ⚡ 1. Launchpad & Quick Apps
* Scans shortcuts from Windows Start Menu and Desktop automatically.
* Instant fuzzy search as you type.
* Extracts 96×96 crisp PNG icons from executables with in-memory caching.
* Long-press any app to pin it to your favorites with priority positioning.

### 🔊 2. Hardware Audio Sync
* Direct integration with Windows Core Audio (`IAudioEndpointVolume`).
* Live two-way volume slider: change volume on your PC with physical keyboard keys, and the phone slider updates instantly.
* Instant Mute toggle with clear visual badge.

### 🖱️ 3. Smooth Wireless Trackpad
* 60 fps responsive touch surface with haptic feedback.
* **Gestures**:
  * Tap = Left click
  * Double tap = Double click
  * Long press = Right click
  * Two-finger swipe or scroll arrows = Smooth page scrolling
* Large physical LKM / PKM thumb buttons at the bottom.

### 🔒 4. System & Power
* Lock screen (`Win + L`) in one touch.
* Show Desktop (`Win + D`) and switch apps (`Alt + Tab`).
* Power down monitors to save electricity.
* Sleep mode, reboot, and timed shutdown with safety confirmation dialog and one-click abort (`shutdown /a`).

### 🪟 5. Task & Window Switcher
* Lists all active windows currently running on your PC.
* Bring any window to the front or kill hung processes cleanly (`WM_CLOSE`).

---

## Quick Start

### 1. Windows PC
1. Download [`Pult.exe`](https://github.com/sailxx/Pult/releases/latest) (single standalone 32 KB binary).
2. Run `Pult.exe` (sits quietly in your system notification tray).
3. Right-click the tray icon > click **«Разрешить в брандмауэре»** (one-click Windows Firewall setup).
4. Click the tray icon to see your PC's local IP and 6-digit PIN.

### 2. Android Phone
1. Download and install [`Pult.apk`](https://github.com/sailxx/Pult/releases/latest).
2. Tap **«Поиск ПК»** for instant automatic discovery over UDP broadcast.
3. Enter the 6-digit PIN and tap **«Подключить»**.

---

## Network Architecture

```
┌─────────────────────────────────┐                 ┌─────────────────────────────────┐
│       Android Phone             │                 │           Windows PC            │
│    (Kotlin + Compose M3)        │                 │        (Native C# Host)         │
│                                 │                 │                                 │
│  UDP Broadcast: PULT?           │ ──────────────> │  UDP 47801 Discovery Listener   │
│                                 │ <────────────── │  Answers: PULT!47800|DESKTOP-PC │
│                                 │                 │                                 │
│  HTTP GET /volume               │ ──────────────> │  Windows Core Audio COM API     │
│  HTTP POST /launch?id=...       │ ──────────────> │  Process.Start()                │
│  HTTP POST /mouse?dx=..&dy=..   │ ──────────────> │  mouse_event() Win32            │
│  HTTP POST /system?action=lock  │ ──────────────> │  LockWorkStation() Win32        │
└─────────────────────────────────┘  Local Network  └─────────────────────────────────┘
```

- **Discovery (UDP 47801)**: Broadcast discovery with automated fallback to parallel subnet sweep if router client isolation is active.
- **REST API (TCP 47800)**: Direct socket server with cryptographically secure PIN authentication (`RandomNumberGenerator`) and brute-force lockout (10 failed tries = 60-second lock).

Full API endpoints and curl examples are documented in [docs/PROTOCOL.md](docs/PROTOCOL.md).

---

## Building from Source

### Windows Host (0 SDKs needed)
Built with the standard C# compiler shipped in every Windows installation:
```cmd
host\build.cmd
```

### Android Client
Built with modern Kotlin and Gradle:
```bash
cd android
./gradlew assembleRelease
```

---

## Releases & Downloads

Pre-built binaries are signed and available under **[Releases](https://github.com/sailxx/Pult/releases)**:
* **Windows**: `Pult.exe` (32 KB standalone)
* **Android**: `Pult.apk` (Optimized APK)

---

## Contributing

Pull requests and bug reports are welcome! Check [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) for contribution guidelines.

---

## License

MIT © 2026 [@sailxx](https://github.com/sailxx)
