#!/usr/bin/env bash
# Run the end-to-end layer: the app's production code against the live APIs.
#
# Needs a credential in local.properties and a device that can reach the network. Never gated by
# `check`: the server is not ours and what it returns changes daily.
set -euo pipefail

cd "$(dirname "$0")/.."

E2E_ANNOTATION=com.shayan.amro.e2e.helpers.E2eTest
ANNOTATION_SOURCE="app/src/androidTest/kotlin/${E2E_ANNOTATION//.//}.kt"

# If the class cannot be loaded, the script fails.
if [[ ! -f $ANNOTATION_SOURCE ]]; then
  echo "No source at ${ANNOTATION_SOURCE}: ${E2E_ANNOTATION} has moved." >&2
  exit 1
fi

adb wait-for-device

./gradlew :app:connectedDebugAndroidTest \
  "-Pandroid.testInstrumentationRunnerArguments.annotation=${E2E_ANNOTATION}" "$@"
