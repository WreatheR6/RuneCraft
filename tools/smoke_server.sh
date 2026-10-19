#!/usr/bin/env bash
# Boots a real dedicated NeoForge server with the built mod, generates Gielinor around a few
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
NEO=$(prop neo_version)
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

# NeoForge server: the installer fetches the vanilla server and libraries and writes the launch arguments.
curl -sSfL -o "$D/installer.jar" "https://maven.neoforged.net/releases/net/neoforged/neoforge/$NEO/neoforge-$NEO-installer.jar" || exit 1
(cd "$D" && java -jar installer.jar --installServer . > installer.log 2>&1) || { tail -40 "$D/installer.log"; echo "FAIL: NeoForge installer failed"; exit 1; }
ARGS="libraries/net/neoforged/neoforge/$NEO/unix_args.txt"
[ -f "$D/$ARGS" ] || { tail -40 "$D/installer.log"; echo "FAIL: NeoForge installer wrote no $ARGS"; exit 1; }
cp "$JAR" "$D/mods/"
echo "eula=true" > "$D/eula.txt"
printf 'online-mode=false\nspawn-protection=0\nlevel-seed=42\nsync-chunk-writes=false\nmax-tick-time=-1\n' > "$D/server.properties"
cd "$D"
LOG=server.log
: > "$LOG"

# Blocks around OSRS tiles: Lumbridge (0,0), Varrock, Falador, Al Kharid, Draynor, Camelot.
AREAS=("-80 -80 80 80" "-100 -500 60 -340" "-600 -400 -440 -240" "60 20 220 180" "-330 -110 -170 50" "-1010 -630 -850 -470")
G="execute in runecraft:gielinor"

# Sleeps N seconds, or returns 1 at once when the server process has exited (marker written below).
nap() {
    for _ in $(seq 1 "$1"); do
        [ -f exited ] && return 1
        sleep 1
    done
}

feed() {
    for _ in $(seq 1 400); do
        grep -q 'Done (' "$LOG" && break
        grep -q 'Crash report saved\|crash report has been saved\|Failed to start the minecraft server\|Exception in server tick loop' "$LOG" && return
        nap 1 || return
    done
    for a in "${AREAS[@]}"; do
        echo "$G run forceload add $a"
        nap 45 || return
    done
    # Chunks holding station checks / spawn spots outside the areas above.
    for s in "${STATIONS[@]}" "${SPAWNS[@]}"; do
        read -r _ _ x _ z _ <<< "$s"
        echo "$G run forceload add $x $z"
        nap 1 || return
    done
    nap 40 || return
    # Station blocks: each check is preceded by a marker so results can be matched in the log.
    for s in "${STATIONS[@]}"; do
        read -r _ town x y z block structure <<< "$s"
        echo "say SMOKE-CHECK station $town $block at $x $y $z ($structure)"
        echo "$G if block $x $y $z $block"
        nap 1 || return
    done
    # Creatures: summon, let them tick, check they exist, then kill them (drops, death hooks).
    read -r sx sy sz <<< "$SPAWN_POS"
    i=0
    for e in "${BOSSES[@]}" "${MONSTERS[@]}"; do
        echo "$G run summon runecraft:$e $((sx - 4 + i % 9)) $sy $((sz - 2 + i / 9 * 4))"
        i=$((i + 1))
        nap 1 || return
    done
    nap 20 || return
    for e in "${BOSSES[@]}" "${MONSTERS[@]}"; do
        echo "say SMOKE-CHECK entity $e"
        echo "$G if entity @e[type=runecraft:$e]"
        nap 1 || return
    done
    # Console-safe mod commands.
    echo "rc hiscores"
    nap 2 || return
    echo "rc hiscores attack"
    nap 2 || return
    for e in "${MONSTERS[@]}" "${BOSSES[@]}"; do
        echo "$G run kill @e[type=runecraft:$e]"
        nap 1 || return
    done
    nap 15 || return
    # Second look at every station (late chunks may still have been building on the first pass),
    # plus whether the spot is plain air, to tell "not built yet/never built" from "overwritten".
    for s in "${STATIONS[@]}"; do
        read -r _ town x y z block structure <<< "$s"
        echo "say SMOKE-CHECK recheck $town $block at $x $y $z ($structure)"
        echo "$G if block $x $y $z $block"
        echo "say SMOKE-CHECK air $town at $x $y $z"
        echo "$G if block $x $y $z minecraft:air"
        nap 1 || return
    done
    # In-game self-test (com.runecraft.selftest, "/rc selftest [filter]"): menus, recipes, quests, minigames,
    # raids, bosses, teleports and save/load exercised against a fake player. Reported below; not blocking yet.
    if [ -n "${SELFTEST_FILTER:-}" ]; then echo "rc selftest $SELFTEST_FILTER"; else echo "rc selftest"; fi
    for _ in $(seq 1 "${SELFTEST_TIMEOUT:-1200}"); do
        grep -q 'SELFTEST END' "$LOG" && break
        grep -q 'Crash report saved\|crash report has been saved\|Considering it to be crashed\|Exception in server tick loop' "$LOG" && break
        nap 1 || return
    done
    nap 5 || return
    echo "save-all"
    nap 5 || return
    echo "stop"
    nap 60 || return
}
feed | { timeout 2700 java -Xmx4G @"$ARGS" nogui >> "$LOG" 2>&1; echo $? > exited; }
# The self-test provokes failures on purpose: what is logged between its BEGIN and END markers is reported
# in its own section and kept out of the blocking checks below (a crash still fails the job).
awk '/SELFTEST BEGIN/ { skip = 1 } !skip { print } /SELFTEST END/ { skip = 0 }' "$LOG" > strict.log
awk '/SELFTEST BEGIN/ { on = 1 } on { print } /SELFTEST END/ { on = 0 }' "$LOG" > selftest.log
echo "---- RuneCraft / error lines ----"
grep -n 'runecraft\|RuneCraft\|ERROR\|Exception\|Caused by' strict.log | grep -v 'Loading\|- runecraft\|SMOKE-CHECK' | head -200

# Match every SMOKE-CHECK marker with the "Test passed"/"Test failed" line that follows it.
awk '/SMOKE-CHECK/ { if (cur != "") print cur " => NO RESULT"; cur = substr($0, index($0, "SMOKE-CHECK") + 12); next }
     /Test passed/ { if (cur != "") { print cur " => PASS"; cur = "" } next }
     /Test failed/ { if (cur != "") { print cur " => FAIL"; cur = "" } next }
     END { if (cur != "") print cur " => NO RESULT" }' "$LOG" > checks.txt
echo "---- gameplay checks ----"
cat checks.txt

fail=0
grep -q 'Done (' "$LOG" || { echo "FAIL: server never finished starting"; fail=1; }
grep -q 'Crash report saved\|crash report has been saved\|Considering it to be crashed\|Exception in server tick loop' "$LOG" && { echo "FAIL: server crashed"; fail=1; }
grep -q 'Failed to place structure\|Failed to build\|Custom builder for .* failed\|could not be parsed' strict.log && { echo "FAIL: RuneCraft reported build errors"; fail=1; }
grep -E '\[(Server thread|main)/ERROR\]' strict.log | grep -qi 'runecraft' && { echo "FAIL: RuneCraft logged errors"; fail=1; }
grep -q '^\s*at com\.runecraft\.' strict.log && { echo "FAIL: exception thrown through RuneCraft code:"; grep -n -B3 -m5 '^\s*at com\.runecraft\.' strict.log; fail=1; }
stations_run=$(grep -c '^station ' checks.txt)
# A station passes if it is there on the first look or on the later recheck.
stations_bad=0
while read -r line; do
    key=${line#station }; key=${key% => *}
    if ! grep -qF "station $key => PASS" checks.txt && ! grep -qF "recheck $key => PASS" checks.txt; then
        stations_bad=$((stations_bad + 1))
        coords=$(sed 's/.* at \([^ ]* [^ ]* [^ ]*\).*/\1/' <<< "$key")
        echo "missing: $key ($(grep -F "air ${key%% *} at $coords =>" checks.txt | head -1 | sed 's/.*=> PASS/spot is air/;s/.*=> FAIL/spot holds another block/'))"
    fi
done < <(grep '^station ' checks.txt)
[ "$stations_run" -eq "${#STATIONS[@]}" ] || { echo "FAIL: only $stations_run of ${#STATIONS[@]} station checks ran"; fail=1; }
[ "$stations_bad" -eq 0 ] || { echo "FAIL: $stations_bad station block check(s) failed on both looks"; fail=1; }
entities_bad=$(grep '^entity ' checks.txt | grep -vc '=> PASS$')
[ "$entities_bad" -eq 0 ] || { echo "WARN: $entities_bad summoned creature(s) not found after 20s (died or despawned?):"; grep '^entity ' checks.txt | grep -v '=> PASS$'; }
# Self-test report (informational for now: failing checks don't fail the job).
echo "---- in-game self-test: every check ----"
grep -v 'SELFTEST-SUMMARY\|SELFTEST END' selftest.log | grep -o 'SELFTEST .*'
echo "---- in-game self-test: errors and warnings logged while it ran ----"
grep -n -A10 '/ERROR\]' selftest.log | grep -v 'SELFTEST' | head -300
grep 'Failed to place structure\|Failed to build\|Custom builder for .* failed\|could not be parsed' selftest.log | head -40
grep '/WARN\]' selftest.log | sed 's/^\[[^]]*\] //' | sort | uniq -c | sort -rn | head -60
echo "---- in-game self-test: summary ----"
if grep -q 'SELFTEST END' selftest.log; then
    grep -o 'SELFTEST [0-9]* passed, [0-9]* failed.*' selftest.log | tail -1
    grep -o 'SELFTEST-SUMMARY .*' selftest.log | sed 's/^SELFTEST-SUMMARY //'
elif grep -q 'SELFTEST BEGIN' selftest.log; then
    echo "WARN: the self-test did not finish; last lines:"; grep -o 'SELFTEST .*' selftest.log | tail -5
    echo "SELFTEST $(grep -c 'SELFTEST PASS' selftest.log) passed, $(grep -c 'SELFTEST FAIL' selftest.log) failed (unfinished)"
    grep -o 'SELFTEST FAIL .*' selftest.log
else
    echo "WARN: the self-test never started (is /rc selftest registered?)"
fi
[ $fail = 0 ] && echo "Smoke test passed ($stations_run station checks)"
exit $fail
