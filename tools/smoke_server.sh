#!/usr/bin/env bash
# Boots a real dedicated Fabric server with the built mod, generates Gielinor around a few
# towns (forcing structure placement, landmark building and NPC spawning), then runs gameplay
# checks from the server console:
#   - station blocks (bank booths, furnaces, altars...) exist exactly where the town structures put
#     them (positions computed from the structure data by com.runecraft.osrs.SmokeProbe),
#   - bosses and monsters can be summoned, tick for a while and be killed,
#   - a few RuneCraft console commands run.
# Fails if the server doesn't start, crashes, a station check fails, or RuneCraft logs errors or
# exceptions. Used by CI; the generated world (build/smoke/world) is reused by the client smoke test.
set -uo pipefail
cd "$(dirname "$0")/.."
prop() { grep "^$1=" gradle.properties | cut -d= -f2; }
MC=$(prop minecraft_version); LOADER=$(prop loader_version); FAPI=$(prop fabric_version)
JAR=$(ls build/libs/*.jar | grep -v -- '-sources' | head -1)
D=build/smoke
rm -rf "$D"; mkdir -p "$D/mods"

# Station positions and summoning spots, from the pure-Java structure data inside the mod jar.
java -cp "$JAR" com.runecraft.osrs.SmokeProbe > "$D/probes.txt" 2> "$D/probes.err" || { cat "$D/probes.err"; echo "FAIL: SmokeProbe failed"; exit 1; }
mapfile -t STATIONS < <(grep '^STATION ' "$D/probes.txt")
mapfile -t SPAWNS < <(grep '^SPAWN ' "$D/probes.txt")
echo "---- ${#STATIONS[@]} station checks, ${#SPAWNS[@]} spawn spots ----"
cat "$D/probes.txt"
[ "${#STATIONS[@]}" -gt 0 ] || { echo "FAIL: SmokeProbe found no station blocks to check"; exit 1; }
SPAWN_POS=$(grep '^SPAWN lumbridge ' "$D/probes.txt" | head -1 | cut -d' ' -f3-5)
[ -n "$SPAWN_POS" ] || SPAWN_POS="-62 64 1"

# Creatures with custom models: legacy bosses, data-driven bosses and common monsters.
BOSSES=(tztok_jad general_graardor king_black_dragon kreearra zulrah vorkath cerberus abyssal_sire)
MONSTERS=(man goblin guard hill_giant hobgoblin)

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
G="execute in runecraft:gielinor"

feed() {
    for _ in $(seq 1 400); do
        grep -q 'Done (' "$LOG" && break
        grep -q 'Crash report saved\|Failed to start the minecraft server\|Exception in server tick loop' "$LOG" && return
        sleep 1
    done
    for a in "${AREAS[@]}"; do
        echo "$G run forceload add $a"
        sleep 45
    done
    # Chunks holding station checks / spawn spots outside the areas above.
    for s in "${STATIONS[@]}" "${SPAWNS[@]}"; do
        read -r _ _ x _ z _ <<< "$s"
        echo "$G run forceload add $x $z"
        sleep 1
    done
    sleep 40
    # Station blocks: each check is preceded by a marker so results can be matched in the log.
    for s in "${STATIONS[@]}"; do
        read -r _ town x y z block structure <<< "$s"
        echo "say SMOKE-CHECK station $town $block at $x $y $z ($structure)"
        echo "$G if block $x $y $z $block"
        sleep 1
    done
    # Creatures: summon, let them tick, check they exist, then kill them (drops, death hooks).
    read -r sx sy sz <<< "$SPAWN_POS"
    i=0
    for e in "${BOSSES[@]}" "${MONSTERS[@]}"; do
        echo "$G run summon runecraft:$e $((sx - 4 + i % 9)) $sy $((sz - 2 + i / 9 * 4))"
        i=$((i + 1))
        sleep 1
    done
    sleep 20
    for e in "${BOSSES[@]}" "${MONSTERS[@]}"; do
        echo "say SMOKE-CHECK entity $e"
        echo "$G if entity @e[type=runecraft:$e]"
        sleep 1
    done
    # Console-safe mod commands.
    echo "rc hiscores"
    sleep 2
    echo "rc hiscores attack"
    sleep 2
    for e in "${MONSTERS[@]}" "${BOSSES[@]}"; do
        echo "$G run kill @e[type=runecraft:$e]"
        sleep 1
    done
    sleep 15
    echo "save-all"
    sleep 5
    echo "stop"
    sleep 60
}
feed | timeout 1500 java -Xmx4G -jar server.jar nogui >> "$LOG" 2>&1
echo "---- RuneCraft / error lines ----"
grep -n 'runecraft\|RuneCraft\|ERROR\|Exception\|Caused by' "$LOG" | grep -v 'Loading\|- runecraft\|SMOKE-CHECK' | head -200

# Match every SMOKE-CHECK marker with the "Test passed"/"Test failed" line that follows it.
awk '/SMOKE-CHECK/ { if (cur != "") print cur " => NO RESULT"; cur = substr($0, index($0, "SMOKE-CHECK") + 12); next }
     /Test passed/ { if (cur != "") { print cur " => PASS"; cur = "" } next }
     /Test failed/ { if (cur != "") { print cur " => FAIL"; cur = "" } next }
     END { if (cur != "") print cur " => NO RESULT" }' "$LOG" > checks.txt
echo "---- gameplay checks ----"
cat checks.txt

fail=0
grep -q 'Done (' "$LOG" || { echo "FAIL: server never finished starting"; fail=1; }
grep -q 'Crash report saved\|Exception in server tick loop' "$LOG" && { echo "FAIL: server crashed"; fail=1; }
grep -q 'Failed to place structure\|Failed to build\|Custom builder for .* failed\|could not be parsed' "$LOG" && { echo "FAIL: RuneCraft reported build errors"; fail=1; }
grep -E '\[(Server thread|main)/ERROR\]' "$LOG" | grep -qi 'runecraft' && { echo "FAIL: RuneCraft logged errors"; fail=1; }
grep -q '^\s*at com\.runecraft\.' "$LOG" && { echo "FAIL: exception thrown through RuneCraft code:"; grep -n -B3 -m5 '^\s*at com\.runecraft\.' "$LOG"; fail=1; }
stations_run=$(grep -c '^station ' checks.txt)
stations_bad=$(grep '^station ' checks.txt | grep -vc '=> PASS$')
[ "$stations_run" -eq "${#STATIONS[@]}" ] || { echo "FAIL: only $stations_run of ${#STATIONS[@]} station checks ran"; fail=1; }
[ "$stations_bad" -eq 0 ] || { echo "FAIL: $stations_bad station block check(s) failed:"; grep '^station ' checks.txt | grep -v '=> PASS$'; fail=1; }
entities_bad=$(grep '^entity ' checks.txt | grep -vc '=> PASS$')
[ "$entities_bad" -eq 0 ] || { echo "WARN: $entities_bad summoned creature(s) not found after 20s (died or despawned?):"; grep '^entity ' checks.txt | grep -v '=> PASS$'; }
[ $fail = 0 ] && echo "Smoke test passed ($stations_run station checks)"
exit $fail
