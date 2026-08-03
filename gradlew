#!/bin/sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -f "$WRAPPER_JAR" ]; then
    exec java -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
fi

GRADLE_VERSION=8.1.1
CACHE_ROOT=${GRADLE_USER_HOME:-"$HOME/.gradle"}/craftandfind-bootstrap
DIST_DIR="$CACHE_ROOT/gradle-$GRADLE_VERSION"
DIST_ZIP="$CACHE_ROOT/gradle-$GRADLE_VERSION-bin.zip"
DIST_URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$DIST_DIR/bin/gradle" ]; then
    mkdir -p "$CACHE_ROOT"
    echo "Gradle wrapper JAR is absent; downloading Gradle $GRADLE_VERSION..."

    if command -v curl >/dev/null 2>&1; then
        curl -fL --retry 3 --connect-timeout 20 -o "$DIST_ZIP" "$DIST_URL"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$DIST_ZIP" "$DIST_URL"
    else
        echo "Neither curl nor wget is available. Install one of them or add gradle/wrapper/gradle-wrapper.jar." >&2
        exit 1
    fi

    if ! command -v unzip >/dev/null 2>&1; then
        echo "The unzip command is required to unpack Gradle." >&2
        exit 1
    fi

    rm -rf "$DIST_DIR"
    unzip -q "$DIST_ZIP" -d "$CACHE_ROOT"
    rm -f "$DIST_ZIP"
fi

exec "$DIST_DIR/bin/gradle" "$@"
