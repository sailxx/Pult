# Contributing to Pult

Thank you for your interest in contributing to Pult!

## Philosophy

Pult is built around three core principles:
1. **Zero dependencies on Windows**: The host is a single, clean C# file compiled with `%WINDIR%\Microsoft.NET\Framework64\v4.0.30319\csc.exe`. It must not require NuGet packages or the .NET SDK.
2. **Local-first & private**: No accounts, telemetry, or external web services.
3. **Snappy & lightweight**: Sub-millisecond latency, minimal memory footprint.

---

## Development Setup

### 1. Windows Host
- Operating System: Windows 10 or 11
- To build the host binary:
  ```cmd
  host\build.cmd
  ```
- Run `host\Pult.exe` directly.

### 2. Android Client
- Prerequisites: Android Studio (Ladybug / Koala or newer), JDK 17+
- Build release APK:
  ```bash
  cd android
  ./gradlew assembleRelease
  ```
- Install to connected device:
  ```bash
  ./gradlew installDebug
  ```

---

## Submitting Pull Requests

1. Fork the repository and create your branch from `master`.
2. Ensure your changes build cleanly without warnings.
3. Test communication between phone and PC in a local Wi-Fi environment.
4. Open a pull request with a clear description of the problem solved.
