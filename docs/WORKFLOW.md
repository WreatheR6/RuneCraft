# Accuracy workflow

How RuneCraft is being brought in line with OSRS: repeated rounds of *audit → implement → merge → push → CI release*.
This file is the shared memory of that loop. Update the round log and backlog at the end of every round.

## The environment

- **Branch:** `claude/busy-cray-anf3uw` is the integration branch. Every push to it runs CI (`.github/workflows/build.yml`), which does three things:
  - builds the jar;
  - boots a real dedicated server (`tools/smoke_server.sh`), which force-loads Gielinor, checks every station block (strict), and summons bosses and monsters;
  - then runs the in-game self-test (`/rc selftest [filter]`, package `com.runecraft.selftest`) against a Fabric fake player: every menu opens, one recipe per menu is made, every quest is walked and completed, every minigame and raid runs, every boss ticks and is killed, every teleport lands on a safe spot, player data survives save/load. The job log ends with `SELFTEST n passed, m failed` and the `SELFTEST FAIL` lines (with stack frames); failures don't fail the job yet;
  - runs an xvfb client walkthrough (`tools/smoke_client.sh`, continue-on-error), which ends with `/rc selftest real minigame:,raid:,boss:` as the real, online player (the fake player is not in the player lists, so minigames, raid rooms and boss targeting skip it). Its `SELFTEST` lines and summary are printed in that step's log.

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

- **Session 2, rounds 4–5: monster data, models, drops, DT2/Doom, skilling leftovers:**
  - Reference stats/drops for the new dungeon and trek monsters; second slayer categories (`SlayerCategories.also`); ice troll spawn aliases; slayer ring (8) tradeable.
  - Models for the remaining fallbacks (spiritual creatures per god, GWD orks/vampires/knights/priests, seasonal/titan elementals, avatars, ghasts, nail beasts) and look-alike variants; old previews re-rendered from above.
  - 10 new `Bone` values (monkey, gorilla, zombie monkey, jogre, wyrm, drake) and 21 wearables from drop tables; skipped monster drop pairs 523 → 359.
  - Awakened DT2 bosses (`BossVariant`, awakener's orbs, separate KC, blood ornament kit) and Doom of Mokhaiotl delves with per-level loot; CAs 449 → 463 tracked.
  - GotR Rewards Guardian searches, polyelemental stones, portal talismans; Volcanic Mine vents/chambers, lava beasts, noted shop ores; Motherlode ore rolled when mined, 64 upper veins, inner ring; ancient brew/mix, magic essence/mix; calquat kegs.

- **Session 2, round 6: boss mechanics, dungeon completion, Last Man Standing:**
  - Doom of Mokhaiotl beam/melee punish, grubs, shield and burrow phases, volatile earth; delve runs persisted (`runecraft_bosses_doom`); CAs 463 → 468. DT2 tablets and quartzes; Vardorvis Str/Def fixed; Demonic Brutus phase 2; gorilla prayer name tags.
  - Dagannoth Kings fought in their sublevel-6 lair (surface arena removed), circular doors with pet rock, door-supports, Lighthouse exit; deep rooms (< y 34) no longer raise a seabed shelf (`TerrainAnchors.DEEP_UNDERGROUND_BELOW`); Kruk's western hordes; Ape Atoll falling rocks and traps.
  - Last Man Standing (`Minigame.LAST_MAN_STANDING`): builds, loadouts, chest tables, fog, keys, safe deaths with stash (`runecraft_minigames_lms`), points/rank, reward shop, ring of wealth (i) and magic shortbow (i) scrolls; `CombatHandler.pvpOverride` gives Fight Pit and Castle Wars ranged/magic PvP.

- **Session 2, round 7: Emir's Arena, quest and diary snapshot audits:**
  - Emir's Arena (`Minigame.EMIRS_ARENA`): duel options/slots/presets, ranked queue, PvP Arena Rewards shop, scroll of imbuing; `lms_*` copies use base specs/ammo/2h via `EquipmentBonuses.reference`; LMS build prayer rules via `PrayerService.minigameRule`.
  - Quests: `quest_reference.tsv` (202 quests from the wiki + quest-helper) and `QuestReferenceTest`; 87 fixes (difficulty, requirements, prerequisites, XP); Learning the Ropes added; 341 QP (= OSRS).
  - Diaries: `diary_tasks.tsv` (492 wiki tasks) and `DiaryReferenceTest`; wilderness diary rework, 27 requirement fixes, 76 quest requirements enforced, `approx()` marker (407 faithful / 85 approx), place-bound spell tasks; Falador shield, Explorer's ring, sea boots, Morytania legs, Varrock armour and Rada's blessing perks fixed.

- **Session 2, rounds 8–9: in-game self-test, Sailing, diary monsters:**
  - In-game self-test (`com.runecraft.selftest`, `/rc selftest [filter]`) run by `smoke_server.sh` after the station checks: ~657 checks (save/load, every menu, one recipe per menu, every quest walked and completed, every minigame and raid ticked, every boss summoned/ticked/killed, every teleport destination). Results are `SELFTEST PASS/FAIL/SLOW` lines in the job log; failures don't fail the job. The smoke server runs with `max-tick-time=-1` and now detects 1.20.1's "This crash report has been saved" message. First full run: 649/657; the 8 failures were `Gielinor.safeSpot` landing under leaves or over air (fixed).
  - Sailing as a skill (`SailingTables`, `SailingTraining`, `SailingHooks`): 358 charting tasks, 439 courier tasks, 77 boat parts, facilities, Barracuda Trials, trawling, sail trimming; 30 courier ports; snapshots under `src/test/resources/osrs/sailing/`.
  - Vultures, ducks, terrorbirds and the Mort Myre snails at cache spawns; snelms, hastae, Guthix balance, hollow trees, Lava Maze lava eel spot; diary tasks 407 → 417 faithful.

- **Session 2, round 10: dungeons, flagship boss models, self-test items/combat:**
  - Six dungeons from cache tiles (`CacheDungeons`): Shadow, Sophanem (3 levels), Lighthouse, Kalphite Cave, Wilderness Slayer Cave, Forthos; shadow hound and locust rider diary tasks real (419 faithful).
  - Re-sculpted Corp, Kraken, Thermonuclear smoke devil, the Nightmare, Verzik P3, TzKal-Zuk, Kree'arra, Graardor, Vorkath, Cerberus (`bestiary/Flagships.java`); painter materials `crystal`, `smoke`, `spectral` and glow modes `spots`, `rim`, `sigil`; before/after previews `docs/previews/models/round10_*.png`.
  - Self-test grew to ~2,680 checks (`ItemCombatChecks`): food, potions, every wearable (requirements, bonus totals, 2h), prayers (OSRS drain formula), protection prayers, every spell, attack XP per style, slayer tasks per master, bank/GE/shops. First run 2,644 passed / 21 failed (all a test bug with level-1 requirements, fixed); it also exposed dead Jewellery ids and deposit-all on notes taking unnoted items (both fixed).

- **Session 2, round 11: remaining dungeons, flagship models part 2, diary close-out:**
  - Seven more cache dungeons: Giants' Den, Isle of Souls, Myths' Guild, Deep Wilderness, Karamja Volcano (+ Crandor tunnels), Ice Troll Caves, Ourania Cave (ZMI altar moved inside); Kalphite Cave task-only and underground multicombat (`MultiCombat.UNDERGROUND`).
  - Re-sculpted Alchemical Hydra ×4, KBD, Kalphite Queen ×2, Dusk/Dawn, Sarachnis (`bestiary/Flagships2.java`); restored Corp's green, Vorkath's frost veins, Cerberus's heads.
  - Diaries 419 → 438 faithful via `EVENT`/`WEAR` task types and `GameEvents.fireActivity` keys; spider on a stick, oomlie wrap, palm leaves, jungle spiders and oomlie birds; Bert's daily sand; non-drinkable potions; 3-dose outputs for potions made from an unfinished potion.

- **Session 2, round 12: model fixes, diary tasks:**
  - Restored the round-10 Zulrah ×3 and Abyssal Sire, veined Kalphite Queen wings, re-sculpted Phantom Muspah and the Whisperer (`bestiary/Flagships3.java`), Nex wing veins. Zilyana, K'ril, wilderness bosses, Leviathan, Vardorvis, Duke and the Wardens kept (no clear improvement).
  - Diaries 438 → 453 faithful (`DiaryExtras`): Tindel Marchant, the Elid genie, camulet, crystal chest and key halves, dwarven helmet, white 2h sword and Sir Vyvin's shop, Motherlode struts, Keldagrim Stonemason, Neitiznot shield, Warrens shop, skull sceptre, team capes, gold helmet smithing, muddy chest, god spells on players.

- **Session 2, round 13: essence mine, places, favour systems:**
  - Rune essence mine with teleports from Aubury, Sedridor, Distentor, Cromperty and Brimstail (portal returns you; pure essence from 30 Mining); Shayzien Crypts; Keldagrim stalls; Fishing Platform spots; Jatizso mine rocks; essence-mine self-test checks.
  - Managing Miscellania (coffers, workers, wiki yields, approval decay), Tai Bwo Wannai Cleanup (jungle, fence, favour, sticks, Gabooty's shop), Barbarian Assault role levels and blackboard, Varrock Museum kudos (84-question quiz snapshot, cleaning finds, Historian Minas).
  - Diaries 453 → 464 faithful of 492.

- **Session 2, round 14: combat formulas and skilling rates against reference data:**
  - `dps_reference.tsv`: 2,250 scenarios from osrs-dps-calc's real `PlayerVsNPCCalc` (generator in `src/test/resources/osrs/combat/generator/`); new pure `osrs/PlayerHitCalc` matches every row, and the Minecraft side uses it through `combat/HitCalcBridge` (melee, ranged, autocast, powered staves, spellbook spells, several specs). Fixed: magic +9 effective level, elemental spells scale with Magic, sang staff, tbow formula, missing bane/demon/dragon effects, fang, void/elite void/slayer helm/salve ordering and flooring. Before: 626/2,250 max hits wrong.
  - `success_rates.tsv` (793 wiki chart lines), `pickpocket_targets.tsv`, `stalls.tsv` with `SuccessRateSnapshotTest`: 2024 stall rebalance (gem 408 XP / 100 ticks…), pickpocket low/high and XP fixes, glove/diary/cape scaling, Rocky by source; plain-harpoon values with dragon/crystal scaling; exact per-axe tree tables; camphor/ironwood/rosewood trees.

- **Session 2, round 15: combat follow-ups, skilling gaps:**
  - Light/heavy ranged defence and elemental weakness for 324 monsters (`MonsterDefenceTable`), weapon-class defence selection; `dps_reference.tsv` 2,890 rows. Ultor/Bellator/Magus/Venator rings, Zaryte vambraces and more now wearable with stats; quest-slot gear counted in gear totals (`GearTotalsReferenceTest`); thrown weapons' ranged attack bonus fixed. NPC-vs-player via pure `NpcHitCalc` matching the calculator's `NPCVsPlayerCalc` (`npc_reference.tsv`, 1,040 rows).
  - Lead, volcanic sulphur and barronite rocks (Barronite Crusher); Shooting Stars (83 landing sites snapshot, Dusuri's shop); minnows and Kylie Minnow; aerial fishing on Lake Molch; digsite workman and Pollnivneach villager pickpockets.

- **Session 2, round 16: Sailing-era islands, monster completeness:**
  - `osrs/world/SeaIslands.java`: 52 islands and 5 lagoons traced from the map squares (Sunbleak, Drumstick, Onyx Crest, Laguna Aurorae, Buccaneers' Haven, the Pandemonium, Isle of Serpents…), `SeaIslandsGeographyTest`; Pandemonium, Buccaneers' Haven, Laguna and Onyx Crest settlements; ironwood, rosewood and lead sites; pickpocketable pirates.
  - 31 slayer-task monsters at cache spawns (`SlayerTaskMonsters`: bears, wolves, scorpions, vampyre juveniles and Vyrewatch sentinels, zogres, sorebones…); `MonsterTraits` (697 creatures: size, attributes, poison/venom immunity, wiki Slayer XP) applied after `MonsterStatsTable`; poison immunity respected; Slayer XP per kill from the wiki. Mummy, Ghoul and Zombies champions kept undead by hand (the DPS data lists no attributes for them).

- **Session 2, round 17: slayer monsters part 2, combat gear effects, missing items, teleport items:**
  - 27 slayer-task monsters (`SlayerTaskMonsters2`, `task_monsters_r17.tsv`): albino bats (Mos Le'Harmless caves), crypt rats/spiders and bloodworms in the Barrows tunnels, sulphur lizards (Karuulm room), araxytes in a new carved `dng_morytania_spider_cave`, feral vampyres, ice wolves, the Death Plateau named trolls, skogres, roosters, ancient zygomites; Varlamore wyrmlings, frost crabs, chilled jellies, earthen nagua and custodian stalkers at their dungeon entrances; custodian stalker tasks now assignable; `SlayerGear.EXEMPT` (no ice cooler for sulphur/grimy lizards, no boots of stone for wyrmlings).
  - Vampyre tiers 1–3 (`osrs/VampyreTiers`, immunities, silver/Ivandis/blisterwood/Efaritay's), amulet of avarice and Forinthry surge, revenant weapons, Elysian/Dinh's bulwark/Justiciar reductions on incoming hits (`NpcHitCalc.reduceIncoming`, `combat/DefenceEffects`), Ahrim's/Karil's damned effects, per-style defences for the 23 `Monster` constants; `dps_reference.tsv` 3,298 rows, `npc_reduction_reference.tsv` 416 rows; blisterwood sickle, wieldable rod of Ivandis.
  - Dosed potion drops (`prayer_potion(2)`, `Drop.doses()`), 37 lines rewritten and 58 added; spined/rock-shell/skeletal armour (Rellekka tanner recipes from dagannoth hide + material + coins), Fremennik boots and Yrsa's shop, edible seaweed, ugthanki kebab, stuffable Slayer heads, Yama contracts, Xerician fabric; skipped drop pairs 348 → 259 (scratch counter, not committed).
  - Pharaoh's sceptre (held item, diary charge caps, mummy recharge), ring of the elements, ring of returning, giantsoul amulet, pendant of ates, diffed in `TransportSnapshotTest`; Dorgesh-Kaan agility course (`AgilityCourse.DORGESH_KAAN`, level 70, 2,750 XP per lap, approx layout); leaving the Wilderness course forfeits the fee; the Isle of Souls chest on its dungeon tile.
  - CI: server self-test 2,805 passed / 0 failed, client 270 / 0.

- **Session 2, round 18: Varlamore dungeons, minigame rewards, boss drop tables, Combat Achievements:**
  - Six dungeons from wiki spawn-map tiles: Neypotzli, Ruins of Tapoyauik (3 floors), Tonali Cavern, Stalker Den (`dungeons/VarlamoreDungeons`), Poison Waste Dungeon (warped tortoises and terrorbirds; warped creature tasks assignable) and Charred Dungeon (lava strykewyrms); round-17 surface camps moved inside; `DungeonsContentTest` 64 dungeons.
  - Nightmare Zone overload/absorption/super ranging/super magic (dream-only), power-ups, herb box, per-boss hard points and hitpoints from the wiki; Barbarian Assault gambles (wiki tables, pet only from high gambles); Soul Wars spoils of war, ectoplasmator, blighted packs, Lil' creator; Rocky rolled in Sorceress's Garden; `MinigameRewardChecks` self-test; `CombatHooks.IncomingDamageModifier`.
  - `BossDropsTable` from 2,271 wiki rows (47 bosses, `BossDropsTableTest`), invented commodity lines removed, wilderness boss uniques 1/618 → 1/196; collection log allowlist (`collection_log_pages.tsv`, 124 pages) with new pages for the 2023–26 bosses; boss potion drops respect doses; revenant uniques and artefacts by the wiki formula (`RevenantRewards`, skull/task modifiers), revenant weapons take ether charges; topaz/opal/jade/ruby enchants (Efaritay's aid, burning amulet, dodgy necklace, passage, digsite pendant fixed).
  - Combat Achievements 468 → 510 tracked (`Mechanics.attackListener`, named strikes, tiles walked, gear weight/value/tier via `EquipmentWeights`); five "no damage from X" tasks that always passed now can fail; dragon hunter crossbow (b)/(t).
  - CI: server 2,811 / 1 (an NMZ waking check that can't work for the fake player; now deferred to the real-player run), client 275 / 0.

- **Session 2, round 19: the Gauntlet, the Fortis Colosseum, the multicannon, common-creature models:**
  - The Gauntlet and Corrupted Gauntlet (`Minigame.GAUNTLET`): per-run 7×7 maze, prep timer, gathering, demi-bosses, singing bowl (post-2026 shard costs), egniol, armour tier damage table, Hunllef drop fixes and the wiki chest tables (`gauntlet.tsv`, `GauntletTest`); items stashed crash-safe on entry.
  - The Fortis Colosseum (`Minigame.FORTIS_COLOSSEUM`): 12 wiki waves with reinforcements, nine wave monsters from dps-calc, all 14 modifiers (several simplified), Glory and ranks, per-wave wiki chest tables (`ColosseumRewards`), Minimus quiver trade, Sol Heredit only as wave 12; snapshots `colosseum_*.tsv`.
  - Dwarf multicannon (`Multicannon`, `CannonService`): set-up, loading (30/35/45/60 capacity by CA tier), 8-direction rotation matching RuneLite's double-hit tiles, accuracy from the player's attack roll, 25/35-minute decay, Nulodion, no-cannon zones, granite cannonballs; ecumenical key 1/60 → 1/40 by CA tier, Wilderness GWD only, cap 3; cannonball drops.
  - Re-sculpted TzHaar-Ket, gargoyle, bloodveld, abyssal demon, cave kraken, smoke devil, pyrefiend, ghost (`bestiary/Common1.java`); 9 attempts rejected; sheet `docs/previews/models/round19_sheet.png`.
  - Combat Achievements 510 → 529 tracked. MinigameSession gains `onClose()`.
  - CI on the merge: server 2,857 / 3, client 277 / 3 (Gauntlet stash missed players outside the player list, Hunllef and Colosseum wave monsters invisible to box queries in a not-yet-ticking instance chunk); fixed by tracking spawned mobs directly and force-loading the Colosseum arena.

## Backlog (things agents reported as missing or approximate)

- **Combat Achievements:**
  - 117 tasks untracked: remaining Gauntlet/Colosseum tasks (Showboating, prep details), most ToB/ToA/CoX room and puzzle tasks, unmodelled mechanics (Dusk prison/blind, Tempoross fires, growthlings, sleepwalkers, Yama contracts/void flares, Vengeance kills, F2P worlds, ratbane weapons, Hueycoatl armour). ~130 items have no weight (approx 0).
  - Rewards not wired (feature missing): instance fees, cosmetic slayer helms, extra Jad/Zuk slayer kills (no Jad/Zuk tasks).
- **Minigames:**
  - Gauntlet: demi-boss count and room contents approx, no crystal tools/pestle/grym steps, bows have free arrows, no escape crystal, lobby chest not built, old `the_gauntlet` summoning-stone arenas still exist.
  - Colosseum: Myopia/Relentless/Red Flag/Mantimayhem III simplified, several damage values guessed, no lobby/bank/Glory unlocks, no Varlamore tokens, Sol's gladiator wall missing.
  - Multicannon: range 8 tiles and assembly time guessed, no ornament/gold/royal variants, cannon-immune monsters not modelled, parts not sold singly.
  - Emir's Arena: ranked duels use own gear (no account builds), no fun weapons, blighted surge sack inert, no tournaments.
  - LMS: competitive starts at 4 players, no High Stakes, no per-build prayer limits, `lms_*` copies lack specs (map `lms_` → base in `SpecialAttacks.of`).
  - Ket-Rak healer counts guessed; JalRek-Jad metamorphosis is only a flag.
  - Temple Trekking: event weights guessed, safe instead of dangerous, no boat event, tomes give XP directly.
  - Sorceress's Garden: layouts are approximations, elementals see through hedges, Rocky pet never rolled.
  - Shades of Mort'ton: diary bonuses, serums, Dampe coffins, flamtaer bag storage; the dungeons-module Shade Catacombs has no key doors or Urium room.
  - NMZ: no barrels/1-dose purchases, hard bosses don't hit harder, power-up timings guessed, no endurance/practice/custom rumble, no herb box daily limit, scroll of redirection inert. BA gamble lines for feathers, mystic lava staff and granite shield missing (`BaGambles.MISSING`). Blighted spell sacks inert; Soul Wars cape effects missing.
- **Transport:**
  - Fairy rings in realms that aren't built; dialling DIQ for the house ring (needs a virtual stop in the islands fairy-ring network).
  - Teleport items: giantsoul destinations are relocated next to our lairs (approx); ring of returning always goes to Lumbridge; ring of the elements charges are per ring; Pyramid Plunder sceptres start charged; Aldarin icon step missing.
- **Skilling:**
  - GotR: binding necklace and polycatalytic stones; abyssal creatures are abstract.
  - Volcanic Mine: heat-proof vessel, gas holes, meteors, multiple boulders; lava beast spawn rate guessed.
  - Dungeon herb, seed and Varlamore crops; coral.
  - Seed pack contents are partly guessed.
- **Diaries:** 28 tasks approximated: Arzinian mine, Skavid caves, swordchick/deathwing, totem pole, emotes, and others. Kingdom/TBW/BA/museum details are approximate (see round 13). Earlier notes: 75: shadow hounds and locust riders need the Shadow Dungeon and Contact! dungeon; swordchick/deathwing have no cache spawns; Arceuus lectern tablets; Port Phasmatys has no range; NPC-talk and minigame-internal tasks.
- **Thieving and Agility:** Dorgesh-Kaan course folds Turgall's parts/spring device into the lap, no grapple route, no light-source check, fail chance 0 at 99; the Dark Chest stays on the surface (Crumbling Tower basement not built).
- **World:**
  - Missing dungeons: Myths' Guild, Isle of Souls, Giants' Den, Deep Wilderness, Crandor/Karamja, Ourania Cave interior, Stalker Den, Ice Troll Caves. New dungeon huts sit a few tiles off where another module's building occupies the tile; Kalphite Cave task-only rule and its multicombat missing.
  - Kruk's Dungeon: the western plane-1 hordes are carved at y -10 under the atoll (own shift, ladder from the lower north-west hall); the other plane-1 clusters (east of 2500, about 120 monkeys/archers) and the plane-0 clusters west of 2600 are still missing.
  - Waterbirth: the Lighthouse ladder is one-way (nothing in the Lighthouse leads down); doors/supports let one player through per use; the Kings are summoned at the stone rather than always present; the lair is not a multicombat area; Bardur and the Askeladden exit missing; the pet rock comes from a rock pile, not Askeladden. Ape Atoll dungeons are boxes around cache clusters; Zooknock still stands on the surface (quests module); no hot-rock cave or locked doors.
  - Many drop items are missing from the catalog (Fremennik/spined/berserker/warrior helms, rock-shell gloves/boots, monkey bones variants, Ape Atoll bones), so those drop lines are skipped. Trek vampyre juvinates don't count for the vampyres task (it would make Konar assign it with no spawns).
  - Remaining spawn stand-ins: Braindeath (off-map), Mudskipper Point (cache mudskippers are underground at 2950,9525), Jatizso ice trolls (cache tiles 2387-2418, 3853-3869 lie north of the traced coast), Fortis guards (snapshot has no Varlamore).
  - Zeah multicombat polygons are from 2018; Varlamore has none.
  - Overworld implings don't roam.
- **Models:** most common creatures are still blocky approximations (round 19 rejected TzHaar Xil/Mej/Hur, dagannoth, hellhound, nechryael, dust devil, aberrant spectre re-sculpts; greater/black demons untried); TzHaar-Ket now differs from the other TzHaar.
- **Bosses:**
  - Doom: grubs, shield/burrow phases, melee punish missing (CAs 613–615, 620, 624); delve runs and held loot are in memory only. Awakened mechanics are guesses; DT2 tablets/quartzes missing; normal Vardorvis stat row has Str/Def 1.
  - Yama, Doom, Maggot King and Royal Titans lairs are approx (no cache surface tile).
  - Deranged archaeologist sits at 3683,3735 because Fossil Island's coast polygon stops short of his real tile (3683,3706).
  - Gorillas: boulder chance and reset timers guessed; no overhead prayer icon; Verac's doesn't bypass. Demonic Brutus phase-2 heal unscripted.
- **Items:**
  - Boss tables: chest/activity bosses (Barrows, Jad, Hunllef, Moons, Colosseum, Zalcano, Doom) and Dusk/Dawn, Araxxor, Yama, Maggot King, Obor, Bryophyta, Mimic unpatched; non-boss collection log pages (Slayer, raids, minigames) not diffed; revenant maledictus, ancient crystal, bracelet of ethereum effect missing; ring of pursuit, amulet of bounty, expeditious bracelet, amulet of chemistry, flamtaer bracelet, necklace of faith, bracelet of slaughter missing; Fremennik armour/Yrsa's shop don't check The Fremennik Trials and are made at any tanner; poisoned weapon/ammo drops skipped; granite shield, scaly blue dragonhide, blighted supplies, crystal teleport seeds and charged-jewellery drop variants missing.
  - Skull timers are in memory only (lost on restart). Heavy ballista wield doesn't check MM2 training; uncut zenyte is a gem-cutting recipe.
  - Seers ring magic damage (0.2%/0.5%) can't be represented with whole-percent magic damage.
  - Ring of the gods imbue should need a holy wrench (nowhere modelled).
- **Combat:** no topaz ring, so Efaritay's aid can't be made; avarice amulet not on the revenant drop table; revenant weapons and the rod of Ivandis have no charges; specs with their own damage code skip vampyre transforms; Karil's second hit gives no XP; Justiciar negative-bonus and unnamed-style cases approx.
- **Skilling gaps:** island fishing spots, lockboxes and island facilities; Laguna's leeches/mosquitoes/toads; Pandemonium Cave, Sunbleak Cave, Buccaneers' Laboratory; Daeyalt essence; drift net fishing; shark lures; Camdozaal is a surface stand-in; shooting stars are in memory only; celestial ring has no effect.
- **Monsters missing:** lobstrosity (Fossil Island underwater), dungeon rat, death wing, Troll Stronghold trolls and generals, armoured zombies, undead lumberjacks, Dark Ankou, swamp crab, plain Vyrewatch tiers; Varlamore dungeon interiors approx (lifts, teleporters, shortcuts, Amoxliatl door, moonlight cockatrices missing; Stalker Den/Charred multicombat missing; Tapoyauik and Tonali huts a few tiles off; Neypotzli hut on the surface); stalker bleed and nagua two-hit simplified; Karuulm hot floor doesn't hurt in the sulphur lizard room; missing drops (bat wings, rat/wyrmling/troll bones, venom sacs, vampyre dust, blessed bone shards). Last Light, Wyrmscraig, Great Conch and Isle of Souls are still ellipses.
- **Self-test gaps:** PvP minigames need two players; utility spells needing targets/items (enchants, telegrab, bake pie) aren't cast; Guthix balance, olive oil and sacred oil have no drink effect; `TestContext.reset()` doesn't reset systems state or the Slayer profile.
- **Sailing:** boat combat, bounty tasks, ocean encounters, crew, cargo holds, cannons, hazards, salvaging hook tiers, hull speed; island materials are sold in the tool shop.
- **Untested in game:** much of the Minecraft-side code has only passed CI's boot test. A play-test pass would be the most valuable round once the data rounds slow down. Use `/run` or the client smoke screenshots.
