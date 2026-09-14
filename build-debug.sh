#!/usr/bin/env bash
set -euo pipefail
if [ ! -d third_party/hev-socks5-tunnel-android/library ]; then
  echo 'Missing HEV library. Run ./setup-hev.sh first.' >&2
  exit 1
fi
if [ -x ./gradlew ]; then
  ./gradlew :app:assembleDebug
else
  gradle :app:assembleDebug
fi
printf '\nAPK: app/build/outputs/apk/debug/app-debug.apk\n'
