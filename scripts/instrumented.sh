#!/usr/bin/env bash
# Run the tests that need a real device: the flow tests in :app. Kept out of scripts/unittest.sh
# because a device is not always attached.
#
#   scripts/instrumented.sh                     every device test
#   scripts/instrumented.sh 'trending screen'   the tests whose name matches
#   scripts/instrumented.sh A11yFlowTest        likewise, by class
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

gradle_args=("$APP_TASK")
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
