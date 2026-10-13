# Accuracy workflow

How RuneCraft is being brought in line with OSRS: repeated rounds of *audit → implement → merge → push → CI release*.
This file is the shared memory of that loop. Update the round log and backlog at the end of every round.

## The environment

- **Branch:** `claude/busy-cray-anf3uw` is the integration branch. Every push to it runs CI (`.github/workflows/build.yml`), which does three things:
  - builds the jar;
  - boots a real dedicated server (`tools/smoke_server.sh`), which force-loads Gielinor, checks every station block (strict), and summons bosses and monsters;
  - runs an xvfb client walkthrough (`tools/smoke_client.sh`, continue-on-error).

  Each green run publishes the jar to the `dev-latest` release.
- **Offline checks only:** cloud sessions usually can't reach the Fabric maven, so the Minecraft layer (`com.runecraft.*`) only truly compiles in CI.
  - `tools/loop/verify.sh` runs everything that does work offline:
    - core unit tests for the pure-Java `com.runecraft.osrs`;
    - `check_visibility.py`;
    - the best-effort `check_compile.py`;
    - `gen_assets.py` validation;
    - `gen_worldgen.py`.
  - Nothing is merged unless it prints `VERIFY OK`.
  - Set `TMPDIR` to a writable scratch directory if `/tmp` is shared.
- **Checking CI** (no auth needed for a public repo): `curl -s "https://api.github.com/repos/WreatheR6/RuneCraft/actions/runs?per_page=5"`.
  - Look for your commit's `head_sha`, then `status` and `conclusion`.
  - For failures, read the failed job's log (GitHub MCP `get_job_logs`) and search for `error:`.
- **Data sources:** the OSRS wiki itself is blocked from the sandbox, but GitHub and WebSearch work. Clone sources into scratch, never into the repo.
  - `Kiyogitpy/osrs-wiki-data`: raw wikitext dump (July 2026). The best general source.
  - `weirdgloop/osrs-dps-calc`: `cdn/json/monsters.json` and `equipment.json` (monster stats, max hits, equipment bonuses).
  - `Flipping-Utilities/parsed-osrs`: items, shops, recipes, GE prices and limits, wiki Lua modules (loot calculators, skilling charts).
  - `0xNeffarion/osrsreboxed-db`: item and monster dumps with drop tables (about Oct 2024).
  - `mejrs/data_osrs`: `NPCList_OSRS.json`, cache NPC spawns with tile coordinates.
  - `runelite/runelite`: skill-calculator `*Action.java` enums, slayer `Task.java`, timetracking `Produce.java`.
  - `skretzo/shortest-path`: transport TSVs with exact tiles.
  - `Zoinkwiz/quest-helper`: quest requirements and rewards.
  - `oldschoolgg/oldschoolbot`: cross-check only; it is often out of date.
- **Snapshots in tests:** when a source is machine-readable, commit a snapshot under `src/test/resources/osrs/` and add a test that diffs the mod against it. The equipment, skill-action, slayer and transport snapshots exist already.

## One round

1. Pick 4 independent scopes (see the backlog), so that parallel agents touch different files.
2. Start one sub-agent per scope, each in its own git worktree (`isolation: "worktree"`), with the **agent brief** below plus its scope. Number the scratch prefixes so they don't collide.
3. When an agent reports, merge its branch with `tools/loop/merge_branch.sh <branch>`. It does the following:
   - Conflicts only in `src/main/resources`: takes the branch side, since those files are regenerated.
   - Code conflicts: exits 2. Resolve them by hand, keeping both sides' intent, then commit and run `tools/loop/verify.sh`.
   - Otherwise: verifies, commits the regenerated assets and pushes.
4. Wait for CI on the last commit. If it fails, read the log, fix the problem, verify and push again.
5. Update the round log and backlog here, then commit and push.

Recurring conflict spots:
- `MenuType` (append new values last; the client uses ordinals);
- `Jewellery.wornExternal` lists;
- `DungeonsContentTest` dungeon count;
- shared hooks in `*Service` classes;
- `SpawnTable`.

## Agent brief (paste into every sub-agent prompt, then add the scope)

> You are auditing and improving a Fabric 1.20.1 Minecraft mod (RuneCraft) that recreates Old School RuneScape (repo root = your working directory). Read docs/CONTENT_GUIDE.md and docs/WORKFLOW.md first. Pure-Java core lives in src/main/java/com/runecraft/osrs (unit-tested); Minecraft code in src/main/java/com/runecraft (compiles only in CI, which also boots a dedicated server + a real client — mimic existing Yarn 1.20.1 code exactly).
>
> METHOD: compare the mod's data and mechanics in your scope against OSRS as documented on the OSRS Wiki. The wiki site is not reachable; use the GitHub data sources listed in docs/WORKFLOW.md (clone read-only into your scratch dir, never into the repo) and WebSearch ("osrs wiki <topic>") to confirm numbers. Prioritise: (1) things that are wrong (level, XP, stat, formula, drop rate, mechanic), (2) missing things that matter to gameplay, (3) polish. Fix them in code/data with tests; prefer committing a snapshot of a machine-readable source plus a diff test. Keep ids stable; don't duplicate ids across modules (grep first). Mark remaining guesses `// approx`. Append new enum values last (ordinals are saved or sent to the client).
>
> Rules: stay within your scope's files where possible; small edits to shared files are OK but keep diffs minimal (other agents work in parallel). A module may not reference another module's items (move shared items into CommonItems). Classes used from another package must be public. Don't touch tools/gen_assets.py, models/textures, structures or terrain unless your scope says so. If you add items/blocks/models, run tools/loop/verify.sh (it runs gen_assets validation).
>
> Verify: `TMPDIR=<your scratch dir> bash tools/loop/verify.sh` must print VERIFY OK. Commit on your branch (in stages if large). Do not push. Report: what you checked, what was wrong and fixed (before→after for important values), what's still missing or approximate, shared files touched, and the commit hash.

## Round log

- **Earlier rounds** (see README "Accuracy pass"):
  - monster, equipment, item value, GE and shop data;
  - combat formulas, prayers and spells, slayer, quests, minigames;
  - raids, diaries, clues, collection log;
  - drop tables, banking (notes, 800-slot bank, deposit boxes).
- **Spawns, quests, skilling, follow-ups:**
  - Monsters placed at their cache spawn locations.
  - 203 quests, 340 QP.
  - Burn rates and gathering success curves; Blast Furnace.
  - Diary perks; ToB HM / CoX CM stats; Champions' Challenge.
- **Dungeons, regions, minigames, equipment:**
  - 7 lairs carved.
  - Konar kingdom map; level variants; Sailing islands.
  - Wintertodt, Tempoross, GotR and Motherlode rewards; Volcanic Mine; Herbiboar.
  - +75 wearables and 89 stat fixes.
- **Bosses, magic/prayer, farming, construction/sailing:**
  - 7 bosses from 2025–26.
  - All spellbooks and prayers audited.
  - Farming cycles and harvest lives.
  - POH pools, nexus and servants; Sailing ports.
- **Combat, artisan, hunter, farming follow-ups:**
  - Tormented demons; enchanted bolts; altar XP; Corp and Nightmare max hits.
  - Artisan recipes diffed against RuneLite.
  - Hunter catch curves and trap limits; resource placements.
  - Disease per patch type, Hespori, anima plants, Prifddinas patches.
- **Slayer, extras, artisan products, agility/thieving:**
  - Slayer masters and reward shop from wiki snapshots; superiors.
  - Puro-Puro; Arclight → Emberlight.
  - Brewing, gnome cooking, barbarian mixes.
  - Rooftop layouts and shortcuts; coin pouches; blackjacking.
- **Minigames, transport, combat achievements, agility follow-ups** (last round):
  - BA, Castle Wars, MTA, Trouble Brewing, NMZ, Soul Wars, Tears of Guthix.
  - All transports diffed against shortest-path (46 fairy rings, charter fare table, balloons, minecarts).
  - Combat Achievements system (646 tasks, 206 tracked) and diary trigger fixes.
  - Werewolf and Penguin courses, course rewards, dungeon shortcuts, new pickpockets and chests.

## Backlog (things agents reported as missing or approximate)

- **Combat Achievements:**
  - 440 tasks untracked. They need boss mechanic hooks: raid HM/CM/expert, group size, awakened bosses, delve levels, streaks.
  - Rewards not wired: slayer "Like a boss", hilt 2 Barrows drain removal, imbue and instance cost changes.
- **Minigames:**
  - Not built: Last Man Standing, Fight Pits, Emir's Arena, Temple Trekking, Sorceress's Garden.
  - Shades of Mort'ton keys and chests are unchecked.
  - Unfinished features: BA role levels and gambles; NMZ potions and power-ups; Soul Wars spoils.
- **Transport:**
  - No slayer ring teleports.
  - Missing: Xeric's Honour, the house fairy ring, fairy rings in realms that aren't built, and the ports Deepfin Point, Port Roberts and The Summer Shore.
  - Two mushtrees sit away from their tiles.
- **Skilling:**
  - GotR cells and barriers.
  - Motherlode upper floor.
  - Volcanic Mine ore shop and full eruption model.
  - Brewing finishes instantly (real vats ferment for hours).
  - Barbarian mixes ignore Barbarian Training.
  - Goading potion; ancient and magic essence mixes.
  - Dungeon herb, seed and Varlamore crops; coral.
  - Seed pack contents are partly guessed.
- **Thieving and Agility:** the Dorgesh-Kaan course and Isle of Souls chests are missing, and Wilderness course state is lost on logout.
- **World:**
  - Kruk's Dungeon (maniacal monkeys are on the surface).
  - Prifddinas real coordinates for spawns.
  - Region rectangles are coarse.
  - Zeah, Fossil Island and Lithkren are "Uncharted Isles" for Konar.
- **Bosses:**
  - Demonic Brutus unlock; Shellbane task-only rule.
  - Several new lair coordinates are approx.
  - Phosani max hits.
  - Demonic gorillas, Deranged archaeologist, TzHaar-Ket-Rak's challenges.
- **Items:** blue mystic hat, gloves and boots; god vestments and books; ballistae; salve (i)/(ei) via NMZ only; no skull system.
- **Untested in game:** much of the Minecraft-side code has only passed CI's boot test. A play-test pass would be the most valuable round once the data rounds slow down. Use `/run` or the client smoke screenshots.
