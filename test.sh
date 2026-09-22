#!/usr/bin/env bash
# test.sh — run all JVM unit tests for RealSR-NCNN-Android-GUI.
#
# Manual regression script for the SAF/URI directory batch-processing changes.
# Covers: SafPathHelperTest (tree-URI path resolution, display names,
# content:// detection) and ExampleUnitTest.
#
# Prerequisites:
#   - JDK 17+ (Android Gradle Plugin requires it; check: java -version)
#   - Android SDK with platform for compileSdk 36
#     (set ANDROID_HOME or create RealSR-NCNN-Android-GUI/local.properties
#      with sdk.dir=/path/to/sdk)
#
# Usage:
#   ./test.sh            # run all unit tests
#   ./test.sh --clean    # clean first, then run

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GUI_DIR="$SCRIPT_DIR/RealSR-NCNN-Android-GUI"

if [ ! -d "$GUI_DIR" ]; then
    echo "ERROR: $GUI_DIR not found" >&2
    exit 1
fi

# --- JDK sanity check (AGP needs 17+) ---------------------------------------
JAVA_VER="$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')"
if [ "${JAVA_VER:-0}" -lt 17 ] 2>/dev/null; then
    echo "ERROR: JDK 17+ required, but 'java -version' reports:" >&2
    java -version 2>&1 | sed 's/^/  /' >&2
    echo "Set JAVA_HOME to a JDK 17+ installation and retry." >&2
    exit 1
fi

cd "$GUI_DIR"

GRADLE_ARGS=(--console=plain)
if [ "${1:-}" = "--clean" ]; then
    GRADLE_ARGS+=(clean)
fi

echo "==> Running JVM unit tests (testDebugUnitTest) ..."
./gradlew "${GRADLE_ARGS[@]}" :app:testDebugUnitTest

REPORT="app/build/reports/tests/testDebugUnitTest/index.html"
echo
echo "==> Unit tests PASSED"
echo "    HTML report: $GUI_DIR/$REPORT"
echo "    XML results: $GUI_DIR/app/build/test-results/testDebugUnitTest/"
