#!/usr/bin/env bash
# Compiles and tests the pure-Java OSRS core (com.runecraft.osrs) without Minecraft/Gradle.
# Useful in environments where the Fabric maven is unreachable.
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="${TMPDIR:-/tmp}/runecraft-core"
JUNIT="$OUT/junit.jar"
mkdir -p "$OUT"
[ -f "$JUNIT" ] || curl -sSfL -o "$JUNIT" https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar
rm -rf "$OUT/classes" "$OUT/test"
javac --release 17 -d "$OUT/classes" $(find src/main/java/com/runecraft/osrs -name '*.java')
javac --release 17 -cp "$OUT/classes:$JUNIT" -d "$OUT/test" $(find src/test/java -name '*.java')
java -jar "$JUNIT" execute -cp "$OUT/classes:$OUT/test" --select-package com.runecraft.osrs --details=summary
java -cp "$OUT/classes" com.runecraft.osrs.CatalogDump build/catalog.json
