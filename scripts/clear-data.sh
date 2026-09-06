#!/usr/bin/env bash
# Wipe the installed app's storage on the connected device: databases, shared preferences, caches
# and files. The app is left installed and starts as if freshly installed.
#
#   scripts/clear-data.sh              the only attached device
#   scripts/clear-data.sh -s emulator-5554   that device
#
# Anything after the options goes to adb untouched.
set -euo pipefail

cd "$(dirname "$0")/.."

APPLICATION_ID=com.shayan.amro

adb "$@" wait-for-device

# `pm clear` reports its own failure on stdout as "Failed", and exits 0 either way, so the output
# is what says whether the storage is gone. The most common cause is the app not being installed
# on the device the command reached.
result=$(adb "$@" shell pm clear "$APPLICATION_ID")

if [[ $result != Success* ]]; then
  echo "Could not clear ${APPLICATION_ID}: ${result}" >&2
  exit 1
fi

echo "Cleared storage for ${APPLICATION_ID}."
