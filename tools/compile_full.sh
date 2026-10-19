#!/usr/bin/env bash
# Real compile of src/main/java against the NeoForge compile classpath (CI's exportClasspath output, also
# published as neoforge-classpath.zip on the dev-latest release). Usage:
#   tools/compile_full.sh [classpath-dir]   (default: $RUNECRAFT_CLASSPATH or build/classpath)
# Prints javac's errors and an "N error(s)" summary line; exits non-zero when there are errors.
set -uo pipefail
cd "$(dirname "$0")/.."
CP_DIR="${1:-${RUNECRAFT_CLASSPATH:-build/classpath}}"
if ! ls "$CP_DIR"/*.jar > /dev/null 2>&1; then
    echo "No jars in $CP_DIR: run ./gradlew exportClasspath, or unzip neoforge-classpath.zip from the dev-latest release there."
    exit 2
fi
OUT=$(mktemp -d)
CP=$(ls "$CP_DIR"/*.jar | tr '\n' ':')
find src/main/java -name '*.java' > "$OUT/sources.txt"
JAVA_TOOL_OPTIONS="" javac --release 21 -proc:none -nowarn -Xmaxerrs 100000 -encoding UTF-8 -cp "$CP" -d "$OUT/classes" @"$OUT/sources.txt" > "$OUT/javac.log" 2>&1
code=$?
cat "$OUT/javac.log"
n=$(grep -c ': error:' "$OUT/javac.log")
echo "$n error(s)"
rm -rf "$OUT"
[ "$code" = 0 ]
