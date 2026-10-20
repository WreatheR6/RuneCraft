#!/usr/bin/env bash
# Clones (or refreshes) the read-only OSRS data sources listed in docs/WORKFLOW.md into one shared directory, so
# parallel agents read the same copy instead of each cloning their own.
#   tools/loop/fetch_sources.sh <dir>      (default: $RUNECRAFT_SOURCES)
# Shallow clones; RuneLite is a sparse checkout of the plugin folders the snapshots use.
set -uo pipefail
DIR="${1:-${RUNECRAFT_SOURCES:?pass a directory or set RUNECRAFT_SOURCES}}"
mkdir -p "$DIR"
fetch() {
  local repo="$1" name="${1#*/}"
  if [ -d "$DIR/$name/.git" ]; then
    git -C "$DIR/$name" pull -q --depth 1 2>/dev/null || echo "warn: could not refresh $repo"
  else
    git clone -q --depth 1 "https://github.com/$repo.git" "$DIR/$name" || echo "warn: could not clone $repo"
  fi
}
for repo in Kiyogitpy/osrs-wiki-data weirdgloop/osrs-dps-calc Flipping-Utilities/parsed-osrs 0xNeffarion/osrsreboxed-db \
            mejrs/data_osrs skretzo/shortest-path Zoinkwiz/quest-helper oldschoolgg/oldschoolbot; do
  fetch "$repo"
done
if [ ! -d "$DIR/runelite/.git" ]; then
  git clone -q --depth 1 --filter=blob:none --sparse https://github.com/runelite/runelite.git "$DIR/runelite" \
    && git -C "$DIR/runelite" sparse-checkout set \
      runelite-client/src/main/java/net/runelite/client/plugins/skillcalculator \
      runelite-client/src/main/java/net/runelite/client/plugins/slayer \
      runelite-client/src/main/java/net/runelite/client/plugins/timetracking \
      runelite-client/src/main/resources/net/runelite/client/plugins/skillcalculator \
    || echo "warn: could not clone runelite"
else
  git -C "$DIR/runelite" pull -q --depth 1 2>/dev/null || echo "warn: could not refresh runelite"
fi
du -sh "$DIR"/* 2>/dev/null
