#!/usr/bin/env bash
# Renders the Gielinor terrain model (com.runecraft.osrs.world.Elevation) to a PNG map.
# Usage: bash tools/render_terrain.sh [out.png] [tilesPerPixel] [minX minY maxX maxY [nolabels]]
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="${TMPDIR:-/tmp}/runecraft-terrain"
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/test"
javac --release 17 -d "$OUT/classes" $(find src/main/java/com/runecraft/osrs -name '*.java')
javac --release 17 -cp "$OUT/classes" -d "$OUT/test" src/test/java/com/runecraft/osrs/world/TerrainPreview.java
java -Djava.awt.headless=true -cp "$OUT/classes:$OUT/test" com.runecraft.osrs.world.TerrainPreview "${@:-docs/previews/terrain_map.png}"
