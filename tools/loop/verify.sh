#!/usr/bin/env bash
# Runs every offline check a change must pass before it is committed or merged: core unit tests, visibility check,
# best-effort compile, the real NeoForge compile (when a classpath is available), asset generation + validation and
# the worldgen data.
#   tools/loop/verify.sh           full check, required before every commit and merge (prints VERIFY OK)
#   tools/loop/verify.sh --quick   while iterating: core tests and the compile only (prints VERIFY QUICK OK)
# The NeoForge classpath comes from RUNECRAFT_CLASSPATH (or build/classpath); see docs/NEOFORGE_PORT.md.
# Asset generation is skipped, but validation still runs, when nothing it reads changed since the last clean
# generation: build/catalog.json, the generator scripts and the committed src/main/resources tree, with no
# uncommitted edits under src/main/resources.
set -uo pipefail
cd "$(dirname "$0")/../.."
QUICK=0
[ "${1:-}" = "--quick" ] && QUICK=1
LOG="${TMPDIR:-/tmp}/runecraft-verify.log"
fail() { echo "VERIFY FAILED: $2"; exit "$1"; }

bash tools/run_core_tests.sh > "$LOG" 2>&1
grep -E "tests (successful|failed)" "$LOG"
if ! grep -q " 0 tests failed" "$LOG"; then
  grep -B1 -A3 "=> " "$LOG" | cut -c1-250 | head -40
  fail 3 "core tests (full log: $LOG)"
fi

CP_DIR="${RUNECRAFT_CLASSPATH:-build/classpath}"
full_compile() {
  if ls "$CP_DIR"/*.jar > /dev/null 2>&1; then
    full=$(bash tools/compile_full.sh "$CP_DIR" 2>&1); echo "$full" | tail -1
    echo "$full" | tail -1 | grep -q "^0 error" || { echo "$full" | grep -A3 ': error:' | head -60; fail 7 "full compile"; }
    return 0
  fi
  return 1
}

if [ $QUICK = 1 ]; then
  if ! full_compile; then
    out=$(python3 tools/check_compile.py); echo "$out" | tail -1
    echo "$out" | tail -1 | grep -q "^0 relevant" || { echo "$out" | head -60; fail 5 "compile"; }
  fi
  echo "VERIFY QUICK OK (run without --quick before committing)"
  exit 0
fi

out=$(python3 tools/check_visibility.py); rc=$?
echo "$out" | tail -3
[ $rc = 0 ] || fail 4 "visibility"
out=$(python3 tools/check_compile.py); echo "$out" | tail -1
echo "$out" | tail -1 | grep -q "^0 relevant" || { echo "$out" | head -60; fail 5 "compile"; }
full_compile || echo "(no NeoForge classpath in $CP_DIR: skipped the full compile; see docs/NEOFORGE_PORT.md)"

STAMP=build/.gen_assets.stamp
key=$({ sha256sum build/catalog.json tools/gen_assets.py tools/model_painter.py tools/optimize_pngs.py
        git rev-parse HEAD:src/main/resources; } 2>/dev/null | sha256sum | cut -d' ' -f1)
if [ -z "$(git status --porcelain -- src/main/resources | head -1)" ] && [ -f "$STAMP" ] && [ "$(cat "$STAMP")" = "$key" ]; then
  out=$(python3 tools/gen_assets.py --validate-only 2>&1); rc=$?
else
  out=$(python3 tools/gen_assets.py 2>&1); rc=$?
  # Only a generation that reproduced the committed resources exactly may stand in for the next one.
  if [ $rc = 0 ] && [ -z "$(git status --porcelain -- src/main/resources | head -1)" ]; then
    echo "$key" > "$STAMP"
  fi
fi
echo "$out" | tail -1
[ $rc = 0 ] || { echo "$out" | tail -40; fail 6 "gen_assets"; }
python3 tools/gen_worldgen.py | tail -1
echo "VERIFY OK"
