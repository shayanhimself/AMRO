#!/usr/bin/env bash
# Build the release variant, sign it with the local debug keystore, and install it on the connected
# device. For judging how the app actually runs: a debug build is unoptimized and runs Compose
# with its debug checks on, so it drops frames a release build does not.
#
#   scripts/installRelease.sh                     the only attached device
#   scripts/installRelease.sh -s emulator-5554    that device
#
# The debug keystore is what every debug build is already signed with, so the release build
# installs over a debug one without an uninstall and keeps the app's data. The install carries the
# baseline profile, so the device compiles the app against it as a store install would.
set -euo pipefail

cd "$(dirname "$0")/.."

DEBUG_KEYSTORE="${ANDROID_USER_HOME:-$HOME/.android}/debug.keystore"

if [[ ${1-} == -s ]]; then
  if [[ -z ${2-} ]]; then
    echo "-s needs a device serial." >&2
    exit 1
  fi
  # Gradle's install task reads the device from here, and adb does too.
  export ANDROID_SERIAL=$2
fi

adb wait-for-device

# The Android Gradle plugin writes the debug keystore the first time a debug variant is signed.
if [[ ! -f $DEBUG_KEYSTORE ]]; then
  ./gradlew :app:validateSigningDebug
fi

# The injected signing properties are the ones Android Studio passes for a signed build. They sign
# this one invocation only, so the build file keeps no signing config for release.
./gradlew :app:installRelease \
  "-Pandroid.injected.signing.store.file=${DEBUG_KEYSTORE}" \
  "-Pandroid.injected.signing.store.password=android" \
  "-Pandroid.injected.signing.key.alias=androiddebugkey" \
  "-Pandroid.injected.signing.key.password=android"
