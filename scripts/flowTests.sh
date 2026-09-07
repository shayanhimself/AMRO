#!/usr/bin/env bash
# Run the flow tests: the device tests in :app that answer from a local server.
#
#   scripts/flowTests.sh                     every flow test
#   scripts/flowTests.sh 'trending screen'   the tests whose name matches
#   scripts/flowTests.sh A11yFlowTest        likewise, by class
#
# A filter is a regex matched against `package.Class#method`, and the run narrows to the modules
# whose sources it appears in: a filter matching nothing fails the task it matched nothing in.
set -euo pipefail

cd "$(dirname "$0")/.."

# A first argument that is not a flag is the filter; anything after it goes to Gradle untouched.
filter=""
if [[ ${1-} != "" && ${1-} != -* ]]; then
  filter="$1"
  shift
fi

APP_TASK=:app:connectedDebugAndroidTest
APP_TESTS=app/src/androidTest

# Skip E2E tests, they have their own script.
E2E_ANNOTATION=com.shayan.amro.e2e.E2eTest
SKIP_E2E="-Pandroid.testInstrumentationRunnerArguments.notAnnotation=${E2E_ANNOTATION}"
ANNOTATION_SOURCE="${APP_TESTS}/kotlin/${E2E_ANNOTATION//.//}.kt"

# If the class cannot be loaded, the script fails.
if [[ ! -f $ANNOTATION_SOURCE ]]; then
  echo "No source at ${ANNOTATION_SOURCE}: ${E2E_ANNOTATION} has moved." >&2
  exit 1
fi

gradle_args=("$APP_TASK" "$SKIP_E2E")
if [[ -n $filter ]]; then
  # The class or method the filter names, without the package and the `#method` suffix: those are
  # how the runner spells a test, not how the source that declares it reads.
  needle=${filter##*.}
  needle=${needle%%#*}

  # Which module a filter runs in is decided by which sources its name appears in, since the
  # filter reaches every task in the run and one that matches no test there fails that task.
  if ! grep -rqE -- "$needle" "$APP_TESTS"; then
    echo "No test source matched '${filter}'." >&2
    exit 1
  fi

  echo "Filtering on '${filter}': running ${APP_TASK}."
  gradle_args=(
    "$APP_TASK"
    "$SKIP_E2E"
    "-Pandroid.testInstrumentationRunnerArguments.tests_regex=$filter"
  )
fi

run_log=$(mktemp)
trap 'rm -f "$run_log"' EXIT

adb wait-for-device

./gradlew "${gradle_args[@]}" "$@" | tee "$run_log"

# A filter that matches nothing runs no tests and the build still succeeds, so a mistyped one
# would report a green run that tested nothing.
if [[ -n $filter ]] && grep -q "Starting 0 tests" "$run_log"; then
  echo "No test matched '${filter}'." >&2
  exit 1
fi
