#!/usr/bin/env bash
# Runs every offline check a change must pass before it is committed or merged:
# core unit tests, visibility check, best-effort compile, asset generation + validation, worldgen data.
# For the real Minecraft-layer compile run tools/compile_full.sh with the NeoForge classpath (docs/NEOFORGE_PORT.md).
set -uo pipefail
cd "$(dirname "$0")/../.."
LOG="${TMPDIR:-/tmp}/runecraft-verify.log"
bash tools/run_core_tests.sh > "$LOG" 2>&1
grep -E "tests (successful|failed)" "$LOG"
if ! grep -q " 0 tests failed" "$LOG"; then
  grep -B1 -A3 "=> " "$LOG" | cut -c1-250 | head -40
  echo "VERIFY FAILED: core tests (full log: $LOG)"; exit 3
fi
python3 tools/check_visibility.py | tail -3 || { echo "VERIFY FAILED: visibility"; exit 4; }
python3 tools/check_visibility.py > /dev/null || { echo "VERIFY FAILED: visibility"; exit 4; }
out=$(python3 tools/check_compile.py); echo "$out" | tail -1
echo "$out" | tail -1 | grep -q "^0 relevant" || { echo "$out" | head -60; echo "VERIFY FAILED: compile"; exit 5; }
python3 tools/gen_assets.py 2>&1 | tail -1
python3 tools/gen_assets.py > /dev/null 2>&1 || { echo "VERIFY FAILED: gen_assets"; exit 6; }
python3 tools/gen_worldgen.py | tail -1
echo "VERIFY OK"
