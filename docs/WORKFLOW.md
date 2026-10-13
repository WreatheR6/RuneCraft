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
> Compile caveat: `check_compile.py` cannot see errors inside expressions whose receiver or arguments involve a Minecraft type (javac skips attributing them), e.g. `screen.button("x" + tier.points())` with a misnamed member passes offline but fails in CI. In Minecraft-side code, grep every field/method you call on our own types (field vs method, exact name, arity, static vs instance) and every import.
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
- **Session 2, round 1: bosses, transport, wearables/skulls, minigames:**
  - CI fix: `CaMenus` called `Tier.points()` (a field); `check_compile` cannot see it (see the compile caveat in the brief).
  - Demonic and tortured gorillas (prayer/style switching, boulders) in Crash Site Cavern; Deranged archaeologist; Phosani stats, max hits and drop rates; Brutus/Demonic Brutus unlocks and drops; Shellbane task-only; lair tiles from the cache.
  - Slayer ring (1)–(8) and eternal teleports; Xeric's talisman charges and Xeric's Honour (ancient tablet); POH fairy ring and spirit tree & fairy ring; Port Roberts, Deepfin Point, The Summer Shore charters with the full fare table and Sailing levels; mushtrees on their tiles; jewellery charges locked by a snapshot test.
  - Blue and dusk mystic, 36 god vestments, 12 god books, ballistae with javelin-only ammo, salve (ei) via Tarn's diary; 3 ranged-strength fixes; PvP skull system (30 min, 0/1 items kept, Emblem Trader).
  - Sorceress's Garden (4 seasons, wiki XP), Shades of Mort'ton rework (25 keys and wiki chest tables, pyre logs, temple sanctity, Urium shade), TzHaar Fight Pit.

- **Session 2, round 2: world, skilling minigames, Combat Achievements, models:**
  - Fossil Island and Ape Atoll coasts traced from the map; Kruk's Dungeon with cache-tile maniacal monkeys; Prifddinas guards, implings and animals; Konar regions KOUREND, VARLAMORE, FOSSIL_ISLAND, LITHKREN (appended to `Region`); surface spawns diffed against 4,305 cache spawns (`NpcSpawnReferenceTest`); multicombat boxes from RuneLite polygons.
  - Motherlode (57 upper level, wiki ore chart, vein depletion), GotR cells/barriers and the wiki reward table, Volcanic Mine fragments/boulder/ore shop, brewing fermentation over world time, barbarian mixes need Barbarian Training, goading potion, Wilderness course persistence, Isle of Souls and Dark chests.
  - Combat Achievements: 216 → 438 tasks tracked via fight `Metrics` and a rule language (`CombatAchievementRules`); "Like a boss", elite superior rate, hilt 2 Barrows, half-price imbues.
  - Models: ~55 creatures off the generic humanoid fallback (tormented demon, gorak, werewolf, minotaur, kalphites, aviansies, animated armour…), `ModelSpec.Variant` look-alikes, painter lighting, previews rendered from above (`docs/previews/models/`).

- **Session 2, round 3: TzHaar challenges and trekking, dungeons, Zeah geography** (towns scope still running when this was written):
  - TzHaar-Ket-Rak's Challenges (`Minigame.TZHAAR_KET_RAK`, 11 CAs tracked), Fight Pit champion skull (`rc_pit_champion` team), Temple Trekking with followers, routes, events and the wiki reward formula (`Minigame.TEMPLE_TREKKING`).
  - Waterbirth Island Dungeon (5 sublevels at cache counts, ladder to the Dagannoth Kings), Ape Atoll and Temple of Marimbo dungeons, Shade Catacombs key doors and Urium crypt (shade keys moved to `CommonItems`), Jormungand's Prison dagannoth/basilisk caves.
  - Zeah and Varlamore coasts, 24 lakes, 67 rivers and new ridges traced from the map; biomes for KOUREND, VARLAMORE, FOSSIL_ISLAND, LITHKREN; 32 Zeah multicombat zones and single-way pockets (Falador castle SW).

## Backlog (things agents reported as missing or approximate)

- **Combat Achievements:**
  - 208 tasks untracked: awakened DT2 bosses, Doom delve levels, Yama contracts, Colosseum waves, Gauntlet prep, raid room/puzzle mechanics, heals/burrows, gear weight or value, movement tiles; missing items (Gauntlet and Hueycoatl armour, ratbane weapons).
  - Rewards not wired (feature missing): cannon capacity, instance fees, ecumenical key rate, DHCB (b)/(t), cosmetic slayer helms, bracelet recharge, extra Jad/Zuk slayer kills.
- **Minigames:**
  - Not built: Last Man Standing, Emir's Arena.
  - Fight Pit PvP is melee only. Ket-Rak healer counts guessed; JalRek-Jad metamorphosis is only a flag.
  - Temple Trekking: event weights guessed, safe instead of dangerous, no boat event, tomes give XP directly.
  - Sorceress's Garden: layouts are approximations, elementals see through hedges, Rocky pet never rolled.
  - Shades of Mort'ton: diary bonuses, serums, Dampe coffins, flamtaer bag storage; the dungeons-module Shade Catacombs has no key doors or Urium room.
  - Unfinished features: BA role levels and gambles; NMZ potions and power-ups; Soul Wars spoils.
- **Transport:**
  - Missing: pharaoh's sceptre, ring of the elements, ring of returning, giantsoul amulet and pendant of ates teleports; fairy rings in realms that aren't built; dialling DIQ for the house ring.
  - The skilling module's `SailingData.PORTS` lacks the three new ports. `ItemValues` marks slayer ring (8) untradeable (it is tradeable).
- **Skilling:**
  - GotR: Rewards Guardian block not placed, no 25-pearl search, abyssal creatures are abstract, polyelemental stones and portal talismans missing.
  - Volcanic Mine: no lava/water vessel, vents or lava beasts. Motherlode: ores rolled at deposit; upper floor has 16 of 64 veins.
  - Ancient and magic essence mixes (cross-module ingredients). Calquat kegs.
  - Dungeon herb, seed and Varlamore crops; coral.
  - Seed pack contents are partly guessed.
- **Thieving and Agility:** the Dorgesh-Kaan course is missing; Isle of Souls chests sit on the surface (their real tiles are in the dungeon/tower basement); leaving the Wilderness course doesn't forfeit the fee.
- **World:**
  - Kruk's Dungeon is only its core halls (the western plane-1 hordes would sit under open sea; needs a placement decision).
  - Waterbirth: circular doors open, no Kings' sublevel 6, no lighthouse exit; seabed shelf at y 56 over the dungeon. Ape Atoll dungeons are boxes around cache clusters.
  - New dungeon monsters are not in `MonsterStatsTable`/`MonsterDropsTable`; slayer `zombies` lacks the monkey zombie variants.
  - Remaining spawn stand-ins: Braindeath, Mudskipper Point, Jatizso ice trolls, Fortis guards.
  - Zeah multicombat polygons are from 2018; Varlamore has none.
  - Overworld implings don't roam.
- **Models:** remaining humanoid fallbacks (GWD orks/vampires/knights, Bandos/Sara/Zammy spiritual creatures, bloodveld/pyrefiend GWD variants, seasonal/titan elementals, quest giant lobster). Old single-model previews (`bst_*`, `slm_*`) were rendered from below.
- **Bosses:**
  - Yama, Doom, Maggot King and Royal Titans lairs are approx (no cache surface tile).
  - Deranged archaeologist sits at 3683,3735 because Fossil Island's coast polygon stops short of his real tile (3683,3706).
  - Gorillas: boulder chance and reset timers guessed; no overhead prayer icon; Verac's doesn't bypass. Demonic Brutus phase-2 heal unscripted.
- **Items:**
  - Skull timers are in memory only (lost on restart). Heavy ballista wield doesn't check MM2 training; uncut zenyte is a gem-cutting recipe.
  - Seers ring magic damage (0.2%/0.5%) can't be represented with whole-percent magic damage.
  - Imbue scrolls from LMS/Emir's/Bounty Hunter missing (minigames don't exist).
- **Untested in game:** much of the Minecraft-side code has only passed CI's boot test. A play-test pass would be the most valuable round once the data rounds slow down. Use `/run` or the client smoke screenshots.
