#!/usr/bin/env bash
# Boots a real dedicated Fabric server with the built mod, generates Gielinor around a few
# towns (forcing structure placement, landmark building and NPC spawning), then stops.
# Fails if the server doesn't start or RuneCraft logs errors. Used by CI.
set -uo pipefail
cd "$(dirname "$0")/.."
prop() { grep "^$1=" gradle.properties | cut -d= -f2; }
MC=$(prop minecraft_version); LOADER=$(prop loader_version); FAPI=$(prop fabric_version)
JAR=$(ls build/libs/*.jar | grep -v -- '-sources' | head -1)
D=build/smoke
rm -rf "$D"; mkdir -p "$D/mods"
curl -sSfL -o "$D/server.jar" "https://meta.fabricmc.net/v2/versions/loader/$MC/$LOADER/1.0.1/server/jar" || exit 1
curl -sSfL -o "$D/mods/fabric-api.jar" "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$FAPI/fabric-api-$FAPI.jar" || exit 1
cp "$JAR" "$D/mods/"
echo "eula=true" > "$D/eula.txt"
printf 'online-mode=false\nspawn-protection=0\nlevel-seed=42\nsync-chunk-writes=false\n' > "$D/server.properties"
cd "$D"
LOG=server.log
: > "$LOG"

# Blocks around OSRS tiles: Lumbridge (0,0), Varrock, Falador, Al Kharid, Draynor, Camelot.
AREAS=("-80 -80 80 80" "-100 -500 60 -340" "-600 -400 -440 -240" "60 20 220 180" "-330 -110 -170 50" "-1010 -630 -850 -470")

feed() {
    for _ in $(seq 1 400); do
        grep -q 'Done (' "$LOG" && break
        grep -q 'Crash report saved\|Failed to start the minecraft server\|Exception in server tick loop' "$LOG" && return
        sleep 1
    done
    for a in "${AREAS[@]}"; do
        echo "execute in runecraft:gielinor run forceload add $a"
        sleep 45
    done
    sleep 30
    echo "save-all"
    sleep 5
    echo "stop"
    sleep 60
}
feed | timeout 1200 java -Xmx4G -jar server.jar nogui >> "$LOG" 2>&1
echo "---- RuneCraft / error lines ----"
grep -n 'runecraft\|RuneCraft\|ERROR\|Exception\|Caused by' "$LOG" | grep -v 'Loading\|- runecraft' | head -200
fail=0
grep -q 'Done (' "$LOG" || { echo "FAIL: server never finished starting"; fail=1; }
grep -q 'Crash report saved\|Exception in server tick loop' "$LOG" && { echo "FAIL: server crashed"; fail=1; }
grep -q 'Failed to place structure\|Failed to build\|Custom builder for .* failed\|could not be parsed' "$LOG" && { echo "FAIL: RuneCraft reported build errors"; fail=1; }
grep -E '\[(Server thread|main)/ERROR\]' "$LOG" | grep -qi 'runecraft' && { echo "FAIL: RuneCraft logged errors"; fail=1; }
[ $fail = 0 ] && echo "Smoke test passed"
exit $fail
