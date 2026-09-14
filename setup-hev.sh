#!/usr/bin/env bash
set -euo pipefail
mkdir -p third_party
if [ ! -d third_party/hev-socks5-tunnel-android/.git ]; then
  git clone https://github.com/zak20090/hev-socks5-tunnel-android.git third_party/hev-socks5-tunnel-android
fi
printf '\nHEV source is ready. The first Android build also downloads/builds the native hev-socks5-tunnel core.\n'
