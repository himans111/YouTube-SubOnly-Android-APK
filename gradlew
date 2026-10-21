#!/usr/bin/env bash
# Lightweight Gradle bootstrap: downloads the official Gradle distribution when it is not cached.
set -euo pipefail
GRADLE_VERSION="8.11.1"
BASE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/channelonly-bootstrap"
GRADLE_HOME="$BASE_DIR/gradle-$GRADLE_VERSION"
if [[ ! -x "$GRADLE_HOME/bin/gradle" ]]; then
  mkdir -p "$BASE_DIR"
  ZIP="$BASE_DIR/gradle-$GRADLE_VERSION-bin.zip"
  if [[ ! -f "$ZIP" ]]; then
    if ! command -v curl >/dev/null 2>&1; then echo "curl is required to download Gradle." >&2; exit 1; fi
    curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
  fi
  if ! command -v unzip >/dev/null 2>&1; then echo "unzip is required to unpack Gradle." >&2; exit 1; fi
  unzip -q -o "$ZIP" -d "$BASE_DIR"
fi
exec "$GRADLE_HOME/bin/gradle" "$@"
