<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/banner-dark.svg">
  <img alt="Pult" src="assets/banner-light.svg" width="100%">
</picture>

<p>
  <b>Phone as a physical remote and desk dock for your Windows PC.</b><br/>
  Hardware volume, 60 fps wireless trackpad, app launcher, active window manager, and power controls over local Wi-Fi.
</p>

<p>
  <a href="https://github.com/sailxx/Pult/releases/latest/download/Pult.exe"><img src="assets/readme/cta-windows.svg" height="48" alt="Download for Windows"></a>
  &nbsp;
  <a href="https://github.com/sailxx/Pult/releases/latest/download/Pult.apk"><img src="assets/readme/cta-android.svg" height="48" alt="Download for Android"></a>
</p>

<p>
  <a href="https://github.com/sailxx/Pult/releases/latest"><img alt="Latest release" src="https://img.shields.io/badge/release-v0.2.0-orange?style=flat-square" /></a>
  <a href="https://github.com/sailxx/Pult/releases/latest"><img alt="Platforms" src="https://img.shields.io/badge/platform-Windows%20%7C%20Android-blue?style=flat-square" /></a>
  <a href="LICENSE"><img alt="MIT license" src="https://img.shields.io/badge/license-MIT-black?style=flat-square" /></a>
  <a href="https://github.com/sailxx/Pult/actions/workflows/ci.yml"><img alt="CI status" src="https://img.shields.io/badge/build-passing-brightgreen?style=flat-square" /></a>
</p>

[⚡ Quick Start](#-quick-start) • [💡 Why Pult](#-why-pult) • [📸 Interface](#-interface-screenshots) • [🎛️ Capabilities](#️-capabilities) • [🗺️ Roadmap](#️-roadmap) • [📊 Comparison](#-comparison) • [🌐 Protocol](docs/PROTOCOL.md) • [🛠️ Build](#️-building-from-source)

</div>

<br/>

<img src="assets/hero.png" width="100%" alt="Pult - phone as a remote for Windows PC" />

<br/><br/>

## 💡 Why Pult

Remote desktop tools are built for office support: they stream your entire screen with lag, drain battery, and require cloud accounts. Dedicated remote apps push subscriptions or require heavy runtimes.

Pult is designed for real everyday moments at home:

* **Watching a movie from bed:** Pause playback, balance loud dialogue with the true master volume slider, or turn off monitors without getting under the blankets.
* **Phone on a desk stand:** One-tap dock to launch your browser, Spotify, terminal, or games without searching through taskbars.
* **Frozen fullscreen game:** When an unresponsive game locks up your mouse cursor, open Pult on your phone, head to the Tasks tab, and kill the frozen window cleanly.
* **Offline router or hotspot:** Pult communicates strictly peer-to-peer over your local Wi-Fi. No internet, cloud servers, or third-party relays are ever involved.

<br/>

<img src="assets/readme/features.svg" width="100%" alt="Pult capabilities" />

<br/><br/>

<img src="assets/readme/numbers.svg" width="100%" alt="Pult specifications" />

<br/><br/>

## 📸 Interface Screenshots

The Android companion application is built with an AMOLED black interface, instant haptic response, and five specialized functional tabs:

<br/>

<img src="assets/screenshots/screens-showcase.svg" width="100%" alt="Pult Mobile Interface Screens: Media Remote, Wireless Trackpad, Apps Launchpad, System & Tasks" />

<br/>

* **1. Media Remote:** Real-time two-way Windows Core Audio slider, quick mute badge, and tactile circular D-pad for volume and media playback.
* **2. Wireless Trackpad:** High-precision 60 fps touch surface with tap-to-click, two-finger vertical scrolling, and dedicated hardware-styled LKM/PKM thumb buttons.
* **3. Apps Launchpad:** Automatic Start Menu application index with cached icons, fuzzy search, and long-press favorite pinning.
* **4. System & Tasks:** Immediate `Win + L` workstation lock, display standby, and a live active window manager to bring windows forward or force-close hung processes.

<br/>

## 🎛️ Capabilities

### 🔊 Master Volume & Mute
* Direct integration with Windows Core Audio (`IAudioEndpointVolume`).
* Live two-way volume slider: adjust volume on your PC with physical keyboard keys, and the phone slider updates instantaneously.
* Instant Mute toggle with clear visual badge.

### 🖱️ 60 FPS Wireless Trackpad
* Smooth relative cursor motion with hardware-assisted touch delta calculation.
* Native gesture support:
  * **Single tap:** Left click
  * **Double tap:** Double click
  * **Long press:** Right click
  * **Two-finger swipe / scroll strip:** Smooth vertical page scrolling
* Dedicated physical thumb buttons for left and right click.

### ⚡ Start Menu Launchpad
* Automatically indexes applications from Windows Start Menu and Desktop shortcuts.
* Instant fuzzy search as you type.
* Extracts 96×96 crisp PNG icons directly from executables with in-memory caching.
* Long-press any app to pin it to your favorites dock.

### 🪟 Task & Window Switcher
* Inspect all top-level running application windows on your desktop.
* Bring any window to the foreground with a single tap.
* Cleanly terminate hanging or unresponsive processes (`WM_CLOSE`).

### 🔒 Power & System Deck
* Lock workstation (`Win + L`) in one touch.
* Toggle desktop (`Win + D`) and cycle applications (`Alt + Tab`).
* Power down monitors into energy-saving standby.
* Sleep, reboot, and timed shutdown with confirmation dialogs and one-click abort (`shutdown /a`).

<br/>

## 🗺️ Roadmap

<img src="assets/readme/roadmap.svg" width="100%" alt="Pult Roadmap" />

<br/>

| Milestone | Status | Key Deliverables |
|---|:---:|---|
| **v0.2 — Foundation & Zero-Lag Core** | **Shipped** | Core Audio COM volume, 60 fps Trackpad, Start Menu apps index, Window switcher & task killer, pure offline UDP discovery, 40 KB native host. |
| **v0.3 — Media & Quick Macros** | **In Progress** | GSMTC Windows media integration (live track title, artist, live album artwork thumbnail on phone), custom one-tap PowerShell/CMD quick action macros. |
| **v0.4 — Companion & Telemetry** | **Planned** | iOS Companion Client (SwiftUI), Android Lock screen media controls widget, real-time PC telemetry dock (CPU temperature, GPU load, RAM usage). |
| **v1.0 — Production Suite** | **Future** | Encrypted local TLS/WSS transport with auto-generated self-signed LAN certificates, multi-monitor display switching, bidirectional clipboard bridge. |

<br/>

## 📊 Comparison

| Dimension | Pult | Unified Remote | KDE Connect | TeamViewer / AnyDesk |
|---|:---:|:---:|:---:|:---:|
| **Accounts or sign-up** | None | Optional | None | Mandatory |
| **Network path** | 100% offline LAN | Local + Telemetry | Local | Cloud relay |
| **PC Host binary** | **40 KB** | ~45 MB | ~110 MB | ~55 MB |
| **Host dependencies** | None (Native Win32/C#) | .NET Runtimes | KDE runtime pack | Proprietary drivers |
| **Volume control** | Core Audio COM (0-100%) | Blind key simulation | Blind key simulation | None |
| **Start menu launchpad** | Automatic with cached icons | Paid addon | None | None |
| **Wireless trackpad** | 60 fps relative delta | 60 fps | 60 fps | High video stream lag |
| **Task / window manager** | Native list & force kill | None | None | Slow video feed |
| **License** | Open source (MIT) | Proprietary | Open source (GPL) | Proprietary |

<br/>

## ⚡ Quick Start

### 1. Windows Host
1. Download [`Pult.exe`](https://github.com/sailxx/Pult/releases/latest/download/Pult.exe) (single 40 KB executable, zero installer).
2. Run `Pult.exe`. The remote icon will appear in your system notification tray.
3. Right-click the tray icon and select **«Разрешить в брандмауэре»** for automatic Windows Firewall setup.
4. Left-click the tray icon to see your PC's local IP and current 6-digit security PIN.

### 2. Android Client
1. Download and install [`Pult.apk`](https://github.com/sailxx/Pult/releases/latest/download/Pult.apk).
2. Tap **«Поиск ПК»** for automatic UDP discovery over your local Wi-Fi.
3. Enter the 6-digit PIN and tap **«Подключить»**.

<br/>

## 🌐 Network Architecture

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

* **Discovery (UDP 47801):** Broadcast beacon with automatic subnet sweep fallback if router client isolation is active.
* **REST API (TCP 47800):** Lightweight HTTP server with cryptographically secure PIN authentication (`RandomNumberGenerator`) and brute-force lockout (10 failed tries = 60-second lock).

Full API endpoints and curl examples are documented in [docs/PROTOCOL.md](docs/PROTOCOL.md).

<br/>

## 🛠️ Building from Source

### Windows Host (Zero SDKs required)
Compiles directly with the native C# compiler present on every modern Windows installation:
```cmd
host\build.cmd
```

### Android Client
Built with Kotlin and Jetpack Compose:
```bash
cd android
./gradlew assembleRelease
```

<br/>

## 📄 License

MIT © 2026 [@sailxx](https://github.com/sailxx)
