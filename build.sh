#!/usr/bin/env bash
# Builds the Auto Clicker mod using the bundled local Gradle (no install needed).
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"
./gradle-9.7.1/bin/gradle build "$@"
echo
echo "Jar: $DIR/build/libs/autoclicker-1.0.0.jar"
