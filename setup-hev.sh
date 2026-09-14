#!/usr/bin/env bash
set -euo pipefail

mkdir -p third_party

if [ ! -d third_party/hev-socks5-tunnel/.git ]; then
    echo "Cloning hev-socks5-tunnel..."
    git clone --recursive \
        https://github.com/heiher/hev-socks5-tunnel.git \
        third_party/hev-socks5-tunnel
fi

if [ ! -d third_party/hev-socks5-tunnel-android/.git ]; then
    echo "Cloning hev-socks5-tunnel-android..."
    git clone \
        https://github.com/zak20090/hev-socks5-tunnel-android.git \
        third_party/hev-socks5-tunnel-android
fi

echo "HEV sources are ready."
