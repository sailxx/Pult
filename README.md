<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/banner-dark.svg">
  <img alt="Pult" src="assets/banner-light.svg" width="100%">
</picture>

<p>
  <b>Turn your phone into a landscape desk companion and control dock for Windows PC.</b><br/>
  Apple-grade Liquid Glass launchpad, bidirectional clipboard bridge, 60&nbsp;fps glass trackpad, stylus signature canvas, and live desktop capture over local Wi-Fi.
</p>

<p>
  <a href="https://github.com/sailxx/Pult/releases/latest/download/Pult.exe"><img src="assets/readme/cta-windows.svg" height="46" alt="Download for Windows"></a>
  &nbsp;
  <a href="https://github.com/sailxx/Pult/releases/latest/download/Pult.apk"><img src="assets/readme/cta-android.svg" height="46" alt="Download for Android"></a>
</p>

<p>
  <a href="https://github.com/sailxx/Pult/releases/latest"><img alt="Latest release" src="https://img.shields.io/badge/release-v0.3.0-orange?style=flat-square" /></a>
  <a href="https://github.com/sailxx/Pult/releases/latest"><img alt="Platforms" src="https://img.shields.io/badge/platform-Windows%20%7C%20Android-blue?style=flat-square" /></a>
  <a href="LICENSE"><img alt="MIT license" src="https://img.shields.io/badge/license-MIT-black?style=flat-square" /></a>
  <a href="https://github.com/sailxx/Pult/actions/workflows/ci.yml"><img alt="CI status" src="https://img.shields.io/badge/build-passing-brightgreen?style=flat-square" /></a>
  <img alt="Local Latency" src="https://img.shields.io/badge/latency-%3C1.5ms%20LAN-emerald?style=flat-square" />
</p>

[⚡ Quick Start](#-quick-start) • [✨ Key Features](#-key-features) • [🖼️ Interface Showcase](#️-interface-showcase) • [🎨 Liquid Glass & Themes](#-liquid-glass--themes) • [🌐 Protocol](docs/PROTOCOL.md) • [🛠️ Build](#️-building-from-source)

</div>

<br/>

<div align="center">
  <img src="assets/screenshots/screenshot-dock.png" width="100%" alt="Pult Hero Landscape Dock with Liquid Glass Apple standard and official app icons" />
</div>

<br/><br/>

## 💡 Why Pult

Most remote control apps look like outdated TV remotes or clumsy media players. **Pult** reimagines your smartphone as an elegant, horizontal desk companion resting beside your computer:

* **Horizontal Desk Companion:** Dock your phone in landscape on a wireless charging stand. Launch frequent tools (VS Code, Brave, Chrome, Telegram, Terminal) with one tap on crisp, authentic high-resolution extracted icons.
* **Apple-Grade Liquid Glass Aesthetics:** Clean dark matte background (`#0C0E12`), floating frosted glass capsules, zero square card borders, and glowing active process indicators.
* **Instant Media & Hardware Audio:** Master Windows Core Audio control (`0–100%`) with ±5 steppers, mute badge, and tactile media keys directly on the floating Now Playing pill.
* **Two-Way Clipboard Synchronization:** Effortlessly push snippets, code blocks, or URLs from your phone into the Windows clipboard, or inspect and grab whatever is currently copied on your PC.
* **Zero Cloud & Zero Accounts:** Pult is 100% peer-to-peer over local LAN/Wi-Fi. No accounts, no telemetry, no relays, no external servers.
* **Ultra-Low Latency:** Persistent HTTP Keep-Alive connection pooling with `TCP_NODELAY` delivers **~1.04 ms/req** response times.

<br/>

---

## ✨ Key Features

| Tool | Capability | Windows Host Integration |
|---|---|---|
| **🚀 Hero Dock & Apps** | Pure floating official EXE icons, active process indicators, favorite pinning, and full app library search | `Process.Start()`, Shell indexing, Win32 window switching |
| **📋 Bidirectional Clipboard** | Live PC clipboard inspection, one-tap copy to phone, push text to PC, clear remote PC buffer | Win32 STA Clipboard API (`GetText` / `SetText`) |
| **🖱️ Precision Glass Touchpad** | Smooth 60 fps mouse navigation, tap-to-click, double tap, hold Drag & Drop mode, scroll strip | Conflated atomic delta dispatcher & `mouse_event` |
| **✍️ Stylus & Finger Signature** | Quadratic Bézier curve inking, 5-color palette, stroke width, undo, direct clipboard injection | Injected as native `Bitmap` object for instant `Ctrl+V` |
| **📸 Desktop Screen Capture** | Real-time screenshot fetch with landscape preview, save to `Pictures\Pult`, or copy to clipboard | Direct GDI+ desktop capture (`CopyFromScreen`) |
| **🪟 Windows 11 Companion** | Modern Flyouts Mica window in system tray; Volume mixer, app volume, monitor brightness, PIN and IP sharing | DWM Mica Win32 API (`DWMWA_SYSTEMBACKDROP_TYPE`) |

<br/>

---

## 🖼️ Interface Showcase

### 1. 🚀 Hero Dock & Apps Library
Prominent freely floating application logos without square cards or borders, subtle glowing indicator dots for active Windows processes, a compact floating navigation header, and a centered Liquid Glass Now Playing capsule.

<div align="center">
  <img src="assets/screenshots/screenshot-dock.png" width="92%" alt="Pult Hero Dock: Liquid Glass & Floating Icons" />
</div>

<br/>

### 2. 🖱️ 60 FPS Precision Glass Touchpad
Low-latency wireless mouse trackpad with kinetic gliding, customizable sensitivity slider from 0.5× to 3.0×, and frosted glass Left & Right click buttons.

<div align="center">
  <img src="assets/screenshots/screenshot-touchpad.png" width="92%" alt="Pult Precision Glass Touchpad" />
</div>

<br/>

### 3. 📋 Bidirectional Clipboard Bridge
Instant two-way clipboard synchronization between phone and PC with zero cloud dependency. Inspect the current Windows clipboard snippet or inject text directly into Windows memory.

<div align="center">
  <img src="assets/screenshots/screenshot-clipboard.png" width="92%" alt="Pult Bidirectional Clipboard Bridge" />
</div>

<br/>

### 4. ✍️ Digital Signature & Inking
Smooth drawing canvas powered by quadratic Bézier curves. Color and stroke thickness options with one-tap insertion directly into active PC documents (`Ctrl+V`).

<div align="center">
  <img src="assets/screenshots/screenshot-signature.png" width="92%" alt="Pult Digital Signature Canvas" />
</div>

<br/>

### 5. 📸 Desktop Screen Capture
Live Windows desktop display monitoring in real time with high-resolution landscape preview and quick actions to copy to clipboard or save to disk.

<div align="center">
  <img src="assets/screenshots/screenshot-capture.png" width="92%" alt="Pult Desktop Screen Capture" />
</div>

<br/>

---

## 🎨 Liquid Glass & Themes

By default, Pult features a clean, distraction-free **Dark Minimal** (`#0C0E12`) matte background. Users can also select from rich ambient radial glow palettes in settings:

* **⬛ Dark Minimal (Default):** Deep matte background (`#0C0E12`), translucent floating Liquid Glass panels (`Color.White.copy(0.08f)`), delicate borders, and electric sky blue accents (`#38BDF8`).
* **🌿 Aurora:** Emerald foundation (`#101719`) with soft sea-mist radial glows (`#71A78A`), deep navy (`#274268`), and fresh mint accents (`#B4EDCC`).
* **🌆 Dusk:** Twilight indigo (`#171228`) paired with amethyst radial hues (`#8B578F`) and soft lavender highlights (`#E7D4FF`).
* **🔥 Ember:** Terracotta charcoal (`#221615`) with warm copper undertones (`#A45F43`) and peach accents (`#F8DCC0`).
* **🌌 Midnight:** Deep space ultramarine (`#090E17`) with cobalt accents and icy blue highlights (`#D0E2FF`).
* **🖤 OLED Pure:** Pitch-black canvas (`#000000`) for zero power draw on AMOLED screens.

<br/>


---

## ⚡ Quick Start

### 1. Windows Host Setup
1. Download [`Pult.exe`](https://github.com/sailxx/Pult/releases/latest/download/Pult.exe) (single ~60 KB binary, no installation required).
2. Run `Pult.exe`. The companion icon will appear in your system notification tray.
3. Left-click the tray icon to open the flyout and check your 6-digit pairing PIN.
4. If connecting for the first time, open the settings menu ⚙️ and click **"Allow through Windows Firewall"**.

### 2. Android Client Setup
1. Download and install [`Pult.apk`](https://github.com/sailxx/Pult/releases/latest/download/Pult.apk).
2. Place your smartphone horizontally on a desk stand.
3. Tap **"Search"** to automatically discover your PC on the local network via UDP beacon.
4. Enter the 6-digit PIN and tap **"Connect"**.

<br/>

---

## 🌐 Architecture & Protocol

```
┌──────────────────────────────────────────────┐                 ┌─────────────────────────────────┐
│           Android Phone (Landscape)         │                 │           Windows PC            │
│          (Kotlin + Jetpack Compose)          │                 │        (Native C# Host)         │
│                                              │                 │                                 │
│  UDP Broadcast: PULT?                        │ ──────────────> │  UDP 47801 Discovery Beacon     │
│                                              │ <────────────── │  Answers: PULT!47800|DESKTOP-PC │
│                                              │                 │                                 │
│  1. Hero Dock:     POST /launch?id=...       │ ──────────────> │  Process.Start()                │
│  2. Audio Strip:   GET/POST /volume, /key    │ ──────────────> │  Core Audio COM API             │
│  3. Clipboard:     GET/POST /clipboard       │ ──────────────> │  Win32 STA Clipboard Thread     │
│  4. Touchpad:      POST /mouse?dx=..&dy=..   │ ──────────────> │  mouse_event() Win32            │
│  5. Signature:     POST /signature?action=.. │ ──────────────> │  System.Drawing Bitmap buffer   │
│  6. Capture:       GET/POST /capture         │ ──────────────> │  GDI+ CopyFromScreen            │
└──────────────────────────────────────────────┘  Local Network  └─────────────────────────────────┘
```

* **Discovery (UDP 47801):** Instant peer-to-peer broadcast beacon with subnet sweep fallback.
* **Control API (TCP 47800):** Keep-Alive HTTP server with cryptographic PIN security and brute-force lockout (10 failed attempts = 60-second cooldown).
* **Protocol Reference:** Detailed endpoints, parameter specifications, and sample payloads are available in [docs/PROTOCOL.md](docs/PROTOCOL.md).

<br/>

---

## 🛠️ Building from Source

### Windows Host (Zero SDKs Required)
Compiles directly using the Microsoft C# compiler built into every Windows installation:
```cmd
host\build.cmd
```

### Android Client
Built with Gradle and Android SDK:
```bash
cd android
./gradlew assembleRelease
```

<br/>

---

## 📄 License & Credits

Pult is released under the **MIT License**. See [LICENSE](LICENSE) for full details.

Developed with care by [@sailxx](https://github.com/sailxx). Contributions, bug reports, and suggestions are welcome on [GitHub Issues](https://github.com/sailxx/Pult/issues)!
