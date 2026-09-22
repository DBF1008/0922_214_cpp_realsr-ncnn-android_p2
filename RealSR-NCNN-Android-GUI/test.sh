#!/usr/bin/env bash
#
# test.sh — run the SAF directory-batch unit tests.
#
# Default mode ("jvm"): compiles the pure-Java classes (SafPathResolver,
# DirectoryBatchValidator) and their tests with javac and runs them via
# JUnitCore.  No Android SDK or Gradle is required — only a JDK and the
# JUnit/Hamcrest jars (auto-detected in ~/.m2, or override via JUNIT_JAR /
# HAMCREST_JAR).
#
# Optional mode ("gradle"): ./test.sh --gradle
#   Runs the full unit-test suite through `./gradlew testDebugUnitTest`
#   (includes SafPathHelperTest, which needs the Android framework stubs).
#   Requires JDK 17+, ANDROID_HOME / local.properties, and network access
#   for dependency resolution.
#
# Usage:
#   ./test.sh             # pure-JVM tests (default)
#   ./test.sh --gradle    # full Gradle unit tests

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$SCRIPT_DIR/app"
MAIN_SRC="$APP_DIR/src/main/java/com/tumuyan/ncnn/realsr"
TEST_SRC="$APP_DIR/src/test/java/com/tumuyan/ncnn/realsr"
BUILD_DIR="$SCRIPT_DIR/build/saf-unit-tests"

run_gradle_tests() {
    echo "==> Running Gradle unit tests (testDebugUnitTest)..."
    cd "$SCRIPT_DIR"
    ./gradlew testDebugUnitTest
}

find_jar() {
    # $1: human name, $2: env override, $3..: candidate paths
    local name="$1" override="$2"
    shift 2
    if [ -n "$override" ] && [ -f "$override" ]; then
        echo "$override"
        return 0
    fi
    local candidate
    for candidate in "$@"; do
        if [ -f "$candidate" ]; then
            echo "$candidate"
            return 0
        fi
    done
    echo "ERROR: $name jar not found. Set the corresponding env var." >&2
    return 1
}

run_jvm_tests() {
    local junit_jar hamcrest_jar
    junit_jar="$(find_jar "JUnit" "${JUNIT_JAR:-}" \
        "$HOME/.m2/repository/junit/junit/4.13.2/junit-4.13.2.jar" \
        "$HOME/.m2/repository/junit/junit/4.12/junit-4.12.jar")"
    hamcrest_jar="$(find_jar "Hamcrest" "${HAMCREST_JAR:-}" \
        "$HOME/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar")"

    echo "==> Using JUnit:    $junit_jar"
    echo "==> Using Hamcrest: $hamcrest_jar"

    rm -rf "$BUILD_DIR"
    mkdir -p "$BUILD_DIR/classes"

    echo "==> Compiling pure-Java sources and tests..."
    javac -encoding UTF-8 -d "$BUILD_DIR/classes" \
        -cp "$junit_jar:$hamcrest_jar" \
        "$MAIN_SRC/SafPathResolver.java" \
        "$MAIN_SRC/DirectoryBatchValidator.java" \
        "$TEST_SRC/SafPathResolverTest.java" \
        "$TEST_SRC/DirectoryBatchValidatorTest.java"

    echo "==> Running tests..."
    java -cp "$BUILD_DIR/classes:$junit_jar:$hamcrest_jar" \
        org.junit.runner.JUnitCore \
        com.tumuyan.ncnn.realsr.SafPathResolverTest \
        com.tumuyan.ncnn.realsr.DirectoryBatchValidatorTest

    echo "==> All pure-JVM SAF unit tests passed."
}

case "${1:-}" in
    --gradle)
        run_gradle_tests
        ;;
    ""|jvm|--jvm)
        run_jvm_tests
        ;;
    *)
        echo "Usage: $0 [--jvm|--gradle]" >&2
        exit 2
        ;;
esac
