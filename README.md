# Meowcy 🐈

A small Android `VpnService` client inspired by the shape of apps such as INCY, but intentionally scoped to a conventional SOCKS5 transport for the first MVP.

## What works in this MVP

- Native Android VPN permission flow.
- Foreground VPN service with persistent notification.
- SOCKS5 endpoint, port and optional username/password.
- TUN -> SOCKS5 forwarding through `hev-socks5-tunnel-android`.
- IPv4 upstream exclusion: the proxy server itself is left outside the VPN route to avoid a routing loop.
- Dark phone-friendly UI.

## Not implemented yet

- VLESS / VMess / Trojan / WireGuard profiles.
- Subscription URLs and QR import.
- Per-app routing.
- IPv6 upstream endpoints.
- Kill switch and profile manager.

## Dependencies

The transport layer uses the public `zak20090/hev-socks5-tunnel-android` wrapper around `heiher/hev-socks5-tunnel`.
The HEV wrapper requires Android SDK, NDK 25.1+ and CMake 3.22.1+ for its native build.

## Prepare dependency

```bash
./setup-hev.sh
```

## Build

With Android SDK/NDK configured and Gradle available:

```bash
./build-debug.sh
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Suggested Android SDK pieces

- Android platform 35
- Android build-tools 35.x
- NDK 25.1.8937393 or newer
- CMake 3.22.1 or newer
- JDK 17+

## Why there are 32 IPv4 routes

Android's VPN route table must not capture the SOCKS5 server's own connection, otherwise the upstream socket can feed back into the TUN interface. `IPv4Routes` represents `0.0.0.0/0` minus one `/32` as 32 sibling CIDR prefixes. Everything except the resolved proxy IPv4 address is routed into the VPN.

## Security note

SOCKS5 by itself does not provide transport encryption. Use a SOCKS5 endpoint only when its surrounding transport is appropriate for your network/security requirements. Do not hard-code credentials into source code.
