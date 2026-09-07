#!/usr/bin/env bash
# Run every JVM test level: unit tests, Compose UI tests on Robolectric, screenshot goldens, and
# the ktlint gate.
#
# `./gradlew test` is not equivalent. It runs the unit tests alone, leaving the screenshot
# validation and the format gate out; `check` covers all three.
set -euo pipefail

cd "$(dirname "$0")/.."

exec ./gradlew check "$@"
