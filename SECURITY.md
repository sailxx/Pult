# Security Policy

## Threat Model & Principles

Pult is designed with a strict **local-first, zero-cloud** architecture:

1. **No External Network Traffic**
   - Pult never transmits data over the public internet. There are no accounts, analytics, cloud relays, or telemetry servers. All communication happens strictly over your local Wi-Fi or Ethernet network.

2. **Authentication via Dynamic Cryptographic PIN**
   - Access to control endpoints is gated by a 6-digit PIN generated using a cryptographically secure random number generator (`RandomNumberGenerator.Create()`).
   - The PIN is stored locally in `%APPDATA%\Pult\pin.txt`.
   - Every state-changing request requires the `X-Pin` HTTP header matching the host's active PIN.

3. **Brute-Force Protection**
   - The host monitors invalid authentication attempts.
   - If 10 consecutive requests fail authentication, all incoming requests are locked and rejected with HTTP 401 for 60 seconds.

4. **VPN Bypass on Android**
   - Modern mobile devices frequently route traffic through external VPN tunnels, which can inadvertently route local control requests outside the home network.
   - Pult binds its network process explicitly to the active physical Wi-Fi interface (`NetworkCapabilities.TRANSPORT_WIFI`), preventing local packets from leaking into third-party VPN tunnels.

5. **Windows Firewall Isolation**
   - Pult only requests incoming access for two dedicated local ports:
     - **TCP 47800**: HTTP API for command execution.
     - **UDP 47801**: Local network discovery.

## Reporting a Vulnerability

If you discover a security vulnerability in Pult, please report it privately via [GitHub Security Advisories](https://github.com/sailxx/pult/security/advisories/new). We will review your submission promptly and coordinate a fix.
