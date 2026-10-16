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

- **Session 2, round 20: quest depth, raid mechanics, models part 2, world monsters:**
  - Quests: survey of 203 quests (12 hand-scripted, 128 spec quests with real objectives, 60 shallow, 3 talk-only); `osrs/FreeQuestObjectives` for the hand-scripted ones (Druidic Ritual enchanted meats, Restless Ghost skull altar, Witch's Potion rats and cauldron, cadava bushes, Vampyre Slayer stake); real puzzles/steps in Ernest the Chicken, Priest in Peril (golden/iron key bug fixed), Fight Arena (Khazard Ogre and Scorpion), Witch's House, Tree Gnome Village, Animal Magnetism, Monkey Madness I, Waterfall Quest, Dragon Slayer I; stage counts locked by `QuestDepthTest`.
  - Raids: `raid_monster_reference.tsv` (50 creatures) with many attack speed, level and defence fixes; ToA raid level damage +2%/5 levels (was +0.1%/level), invocations (attempts, time limits, Quiet/Deadly Prayers, On a Diet, Dehydration, Medic!, Stay Vigilant, Boulderdash, Upset Stomach, Not Just a Head); Tekton immunities and anvil repair, Guardian damage/HP formulas, Vespula grubs/soldiers/landing; Verzik P1 blast up to 137; CAs 529 → 532 tracked; `ItemUses.ConsumeGuard`.
  - Models (`bestiary/Common2.java`, `round20_sheet.png`): one shared TzHaar body for Ket/Xil/Mej/Hur; wingless greater/black/Catacombs demons and drake; dagannoth and one-legged pink turoth.
  - World: Troll Stronghold interior (`dng_troll_stronghold`, 3 cache planes), Kruk's Dungeon 244 → 367 monkeys (all cache clusters), Shaman Caves death wings (Karamja hard diary task now real), Clock Tower dungeon rats, Fremennik Isles snowfields and ice trolls at cache tiles, swamp crabs; 67 dungeons.
  - Self-test fixes after round 19: Gauntlet test clears a leftover fake-player stash; BA gamble test counts a coins prize. Never chain a commit after a `timeout`-limited verify: a killed verify leaves half-regenerated textures (happened in `bf3e9ea6`, restored in `32c4db9e`).

- **Session 2, round 21: shallow quests, raids part 2, skilling gaps, models part 3:**
  - Quests: 22 more quests with real objectives (Merlin's Crystal, Holy Grail, Plague City, Biohazard, The Dig Site, Recruitment Drive with Sir Leye, Children of the Sun, Song of the Elves start fixed to Edmond, Prince Ali Rescue, Underground Pass, Learning the Ropes, Fairytale I/II, Horror from the Deep, Making History, Dream Mentor, Creature of Fenkenstrain, Ghosts Ahoy, In Search of the Myreque, Fremennik Isles, The Golem, Monkey Madness I hangar); shallow 63 → 41, talk-only 0.
  - Raids: ToA raid-level accuracy/defence scaling ((250+L)/250), all boss invocations (Akkha specials, Ba-Ba, Kephri, Wardens, Overly Draining), Walk the Path's 4 random level-ups; ToB Maiden, Bloat, Sotetseg death ball, Xarpus, Verzik P1 pillar-blocked blast; CAs 532 → 539 tracked; `RaidInvocationsTest`. The server self-test slowdown (189 s → 314 s) is general, not raids.
  - Skilling: Ruins of Camdozaal as a real cave (`CamdozaalRuins`, fishing/preparation/altar, golems, barronite mace at the Sacred Forge); shooting stars persisted in `SkillingState`; seed pack snapshot (`seed_pack.tsv`); 68 dungeons.
  - Models: TzTok-Jad re-sculpted after the concept sheet and JalTok-Jad on the same body (`Flagships4`), Giant Mole, semi-upright wingless drake (`Common3`); Hespori rejected; `round21_sheet.png`.

- **Session 2, round 22: quests part 3, Sailing at sea, minigame leftovers, special attacks:**
  - Quests: shallow quests 41 → 0 (Contact!, The Giant Dwarf, King's Ransom, A Tail of Two Cats, Tears of Guthix, Sins of the Father, Kourend and Varlamore quests…; about ten only gained report-step gates); the 22 F2P hand-scripted quest NPCs stand on their OSRS tiles, far ones spawned on first chunk load (`LandmarkBuilder` queue).
  - Sailing: wiki salvage tables (`salvage_loot.tsv`) sorted at port salvaging stations or a boat facility, bronze–dragon hook facilities, per-area wreck spots; boat cannons and cannonball tiers, 21 sea creatures (`sea_creatures.tsv`), hull/keel hitpoints; 163 bounty tasks; ocean encounters (120-tick timer) and hazards with their protections; `SeaChecks` self-test.
  - Minigames: Colosseum Relentless/Myopia (weapon ranges)/Red Flag faithful, Glory unlocks, Varlamore tokens, Showboating (CAs 540); Gauntlet escape crystal and lobby reward chest, summoning stones redirect into the Gauntlet; NMZ herb box limit and scroll of redirection (8 town tablets); BA gamble tables complete (mystic lava staff, granite shield, feathers).
  - Special attacks: `spec_reference.tsv` (1,000 rows from osrs-dps-calc) all passing; godsword double flooring, per-spec defence style, nightmare staff formulas, claws cascades, dark bow minimums, always-hit specs, drains applied to the target's stats, AGS delayed Blood Sacrifice; ~15 new specs (granite hammer, soulreaper axe, dual macuahuitl, tonalztics, ursine chainmace, scorching bow…).

- **Session 2, round 23: big quest chains, landmark buildings, Sailing part 2, diaries:**
  - Quests: Recipe for Disaster subquests with their real objectives (hardleather gloves added to the chest), Desert Treasure I diamonds, DT II Ancient Guardian chapter, Lunar Diplomacy, Regicide (Tyras killed by the bomb; boss is the Tyras guard), Mourning's End I & II, Monkey Madness II (Glough boss), Dragon Slayer II key pieces, Song of the Elves (Fragment of Seren stats fixed, Essyllt boss); quest steps can require any pickaxe/axe of a minimum tier (`.needTool`); `QuestChainsDepthTest`.
  - Buildings (OSRS map floors from mejrs/layers_osrs, transports from shortest-path): Wizards' Tower (octagon, NW annex, 3 storeys, real basement), Draynor Manor (12 rooms, top floor, lever-puzzle cellar), Champions' Guild as its own structure, Edgeville Monastery cloister, Barbarian Village longhall.
  - Sailing: 10 crewmates (`SailingCrew`; privateers fire cannons, deckhands repair), 7 cargo holds, repair kits, wiki sinking and shipwright recovery, hull speeds (client velocity multiplier), chainshot/incendiary balls, shark paint, island materials gathered (nickel, rubium, cupronickel, sawmill planks).
  - Diaries 465 → 475 faithful (saltpetre, Monster Examine, Legends' totem, sawmill operator, Ent trunks…); muddy and crystal chests were never placed (fixed); Waterbirth Kings always present, two-way Lighthouse ladder, Askeladden; multicombat in Waterbirth, Lighthouse, Kruk's and Charred dungeons; implings roam.

- **Session 2, round 24: buildings part 2, slayer models, boss/raid leftovers, quest step gaps:**
  - Buildings: Grand Exchange octagon arcade, Port Sarim (L-shaped jail, Wydin's, Betty's, quay and piers), Fight Arena compound, Castle Wars lobby (roof hole so teleports land inside), Seers' Forester's Arms and courthouse, Sorcerer's Tower garden, Catherby bank; Traiborn and Mizgog on their Wizards' Tower floors (`QuestNpc.floor()`, `NpcFloor.spot`).
  - Models (`Common4`, `round24_sheet.png`): aberrant spectre, dust devil, kurask, rockslug, three-headed hydra (head count inferred, unverified), wall beast, cockatrice.
  - Bosses/raids: ToB Nylocas room with the wiki's 31 waves (`nylocas_waves.tsv`, `TobNylocas`), supports, splitting bigs, Vasilias reflect and Hard-mode Prinkipas; Verzik P1 pillars collapse; CoX Vasa teleport damage; Skotizo altars one at a time with Arclight, reanimated spawns below 33%, Dark Ankou; revenant maledictus (approx spawn divisor); CAs 540 → 544.
  - Quests: kill-count quest steps (`QuestSpec.kill(x, n)`, saved per player); big snake, crab, hell-rat, jubbly, Iorwerth warrior and mourner as real kills; Evil Chicken lvl 159; Dream Mentor's four fears and goutweed; Fareed stats and ice gloves; Ides of Milk, Night at the Theatre, Current Affairs (route fixed), My Arm's Big Adventure.
  - CI after round 23: the rebuilt Champions' Guild used the entity id `minecraft:armor_stand` as a block, which fails the smoke build check; fixed and guarded by `StructureBlockIdsTest`.

- **Session 2, round 25: buildings part 3, humanoid NPC skins, DT II quest chapters, boss mechanics:**
  - Buildings: Ardougne Castle (octagonal towers, walkable curtain, keep) and East Ardougne market with stalls on their tiles, Seers' bank T-shape, Port Sarim jail upstairs, Al Kharid palace (teleport used to land on the dome; now the open courtyard) and bank, Yanille Wizards' Guild and bank, Rellekka longhall, Brimhaven, Warriors' Guild, Burthorpe Castle (`TileMask` helper).
  - NPC skins (`tools/gen_assets.py` v2 painter, `osrs/NpcRace`, `round25_npcs_sheet.png`): faces, hair styles, beards, sleeves/belts/robes, role outfits (monks, pirates, H.A.M., bandits, barbarians), dwarf/gnome/elf/TzHaar races with render proportions.
  - Quests: DT II's four boss chapters as quest fights at dps-calc quest stats (scaled boss scripts), Mysterious Figure and Sliske's wights; Garden of Tranquillity, Secrets of the North, One Small Favour (Slagilith stats fixed) per the wiki; ToB Sotetseg maze for the arena party and Bloat turning.
  - Bosses: Zulrah's 4 rotations from the wiki, damage cap, green Jad phase; Vorkath acid/spawn/rapid-fire fixes; Grotesque Guardians' four phases; Corp's dark energy core; K'ril's special; KQ invented halving removed; Mole burrow window; Nex cough; Muspah prayer shield; Leviathan shadow stun; CAs 544 → 561.

- **Session 2, round 26: ToA fixes and supplies, slayer models part 5, buildings part 4, Combat Achievements:**
  - Raids: the backlog's invocation list was stale (done in round 21); verified all 44 invocation levels and HP scaling against the wiki/dps-calc (`toa_hp_reference.tsv`, `ToaScalingReferenceTest`). Fixed: Apmeken and Ba-Ba baboons static HP, Akkha's fixed melee→ranged→magic cycle, Mind the Gap's north and south safe sides, Medic! swarm slowdown, the free ToA lobby supplies removed. Added the Helpful Spirit supply bundles (Life/Chaos/Power) with nectar, tears, ambrosia, silk dressings, scarabs, smelling salts, liquid adrenaline and honey locusts (`ToaSupplies`).
  - Buildings: Yanille Watchtower on its real footprint (teleport lands on the open platform), Rellekka market stalls on their cache tiles, Burthorpe Games Room under the castle, Catherby bank/Caleb's range/Hickton's, Draynor bank counters and market stalls, Draynor Manor's own first floor and Count Draynor's crypt; `COVERED_WATER` emptied; `StructureBlockIdsTest` now checks every block state against `blocks_1_20_1.tsv`.
  - Combat Achievements 561 → 573. Boss script attacks (`Mechanics.hit`) now fire `CombatHooks.hurt`, so Vengeance, recoil and raid hurt listeners react to boss specials.
- **Session 2, round 26 B: slayer models part 5:**
  - Models (`Common5`, `round26_sheet.png`): hellhound (and the Catacombs one) as a deep-chested hound with a spiked spine and glowing eyes, legless hovering nechryael with bat wings and a curled tail, mutated bloodveld on the bloodveld body; jelly and warped jelly re-sculpts rejected. TzHaar NPCs (TzHaar-Hur, Fight Cave/Pit officials, TzHaar-Ket-Rak) now use the shared TzHaar caste body (`Common5.variants()`, rendered by `client/QuestNpcRenderer` for any NPC id with a model spec).

- **Session 2, round 27 (this session; the backlog audit is above it in time): self-test hardening, boss scripts, transport, items and drops, with adversarial review:**
  - Process: every agent branch went through a review workflow (compile / runtime / fidelity lenses, two skeptics per finding, a fixer that re-verifies). It caught 11 real bugs before release: Obor/Bryophyta patched from the hill/moss giant tables, a double looting-bag roll, the level-13 obelisk cutting through the Ferox Enclave, legacy stack charges lost on the move to per-player charges, obelisks ignoring Tele Block, DIQ without a house ring, untracked engine void flares at Yama, Yama's exile placed off the arena, and wrong Vardorvis/flare/Judge numbers. Worth keeping for every round (script: `review-branch` workflow).
  - Self-test: stuck quest walks now FAIL with the stage/trigger (`QuestWalk`, pure; `QuestWalkTest` plans every quest; allowlist empty); 17 `skill:*` checks through the real interaction code (woodcutting … construction); enchant 1–7, enchant bolts, Telekinetic Grab and the Lunar item spells cast and checked; state reset/restore for SysState, Slayer profile, MinigameData and quest state; Bake Pie no longer turns pie shells into pies. Client filter now also runs `skill:` and the item spells.
  - Bosses: Yama script (phases, glyphs, void flares, Judge of Yama exile, all 5 contracts), Abyssal Sire phase 1 (tentacles, disorient), Vardorvis ramp/Defence drain/heal, Amoxliatl, Maggot King, Royal Titans rewrite, Phosani sleepwalkers (`BossMechanics`, `BossMechanicsTest`); CAs 573 → 588.
  - Transport: Ardougne/Edgeville/Deserted Keep/Mage Arena levers and the Mage Bank cavern, the 6 Wilderness obelisks, dungeon fairy rings (BLP/ALR/AJQ) plus DIP/DLS/DIQ, underground teleport arrivals (`SysTeleport.arrival`, y ≥ 9000 → carved dungeon floor), teleport crystal, seed pods, basalts, amulet of the eye, Chronicle, mythical/music cape, camulet recharge, skull sceptre diary charges, ring of returning to the bed spawn, per-player ring of the elements/camulet charges.
  - Items: wiki drops for the monsters that dropped nothing, bat/wolf/wyrmling/zogre bones (`Bone` appended), vampyre dust, venom sacs, blessed bone shards, scaly blue dragonhide, crystal teleport seeds, 7 semi-precious jewellery pieces with per-player charges (4 effects wired), bracelet of ethereum, looting bag drop, GG/Araxxor tables, Fight Caves pays 8,032 tokkul once, skull timers persist.
  - CI on the merge: server 2,960 / 0, client 345 / 0.

- **Session 2, round 28: minigame depth, skilling depth, items and sources, Varlamore and the Troll Stronghold, with adversarial review:**
  - Minigames: NMZ normal/hard/endurance/practice/custom rumbles, potion barrels with 1-dose purchases (withdraw at the barrels or by Dominic Onion), hard-mode boss stats, zapper last-hit rule only where OSRS has it, practice gives no XP/points/Slayer credit (`XpService` gates, `MinigameSession.awardsXp`); Temple Trekking dangerous death (`MinigameSession.dangerous`), boat event, tomes as items; Shades of Mort'ton serums, Afflicted, Dampe coffins, flamtaer bag and bracelet, diary bonus; Sorceress's Garden elementals no longer see through hedges.
  - Skilling: GotR aether runes, binding necklace, polycatalytic stones, rift creatures; Volcanic Mine heat-proof vessel, gas holes, meteors, multiple boulders; Varlamore farming patches and the coral nursery; flax/hemp/cotton in hops patches, camphor/ironwood/rosewood hardwood trees; giant seaweed (Superglass Make works); Daeyalt essence mine; celestial ring; amulet of chemistry/bounty effects.
  - Items: shop/NPC sources for the round 27 teleport items, ring of pursuit, extended anti-venom+, Obor/Bryophyta chests from the real wiki tables, Zalcano and Lunar Chest tables, Fremennik craftsmen and Yrsa/Fremennik shops gated by The Fremennik Trials (light ballista correctly ungated).
  - World: Varlamore dungeon passages, lifts, doors, shortcuts and multicombat; moonlight cockatrices; Troll Stronghold generals, Twig/Berry, Godric/Eadgar and the secret exit; level 120–124 ice trolls (weak to fire) and Meiyerditch Vyrewatch; trek juvinates count for vampyre tasks.
  - Review caught: quest-gated shops breaking two shop self-tests, a 1-pod grand seed pod tip (now 5), ice trolls missing their fire weakness, NMZ doses unreachable in older worlds, practice dreams paying XP, the no-quest all-bosses NMZ fallback, Daeyalt rock phases colliding and its camp inside Darkmeyer, aether runes unusable in spells, celestial ring charge use, coral yields, and the flax self-test leaving a planted patch in the world save (the client run loads the server run's world, so per-position state carries over — reset it in the check).
  - CI on 4f77c23d (before the review fixes): server 2,974 / 2 (the two quest-gated shop checks), client 353 / 1 (the flax check); both fixed in the review merges.

- **Session 2, round 29: boss mechanics, Colosseum/cannon/Gauntlet, items and drop tables, ToA and CoX, with adversarial review:**
  - Bosses: Bryophyta growthlings (new `BossEntity.Script` hook), Obor's ranged attack and knockback, Vardorvis axes (max 35, awakened 96), bleed and Strangle, Yama's orb styles, magic defence, phase 3 bonuses, prayer pierce and burns (`BossDots`), Maggot King larvae eating carrion, Royal Titans walls that block movement (cleaned up across reloads via the new `BossScript` NBT hooks), Tempoross fires, totem repair, rope, water pump and supply crate with vanilla buckets (waves deal no damage); CAs 588 → 592.
  - Minigames: Sol Heredit's real attack set (spear/shield patterns, triple parry, grapple with chat-link parries, phases 90/75/50/25/10, light beams, molten sand, laser crystals, sealed sandstone gladiator wall); manticores fire 3 orbs and Mantimayhem I–III differ; totems heal every 7 ticks; Multicannon works in OSRS tiles (range was ~4), a 108-creature immunity snapshot replaces "no bosses", Nulodion sells parts singly; Gauntlet crystal tools, pestle and mortar and the grym → dust → egniol chain.
  - Items: looting bag storage (Wilderness only, open-bag pickup, bank deposit, contents drop on a Wilderness death); Barrows reward potential table; Yama, Maggot King and Mimic wiki tables; holy wrench (+2% prayer restore, ring of the gods imbue); Sanfew 4+30%; blessed bone shards offered at altars.
  - Raids: Wardens phase 2 core (4,500 HP scaled +0.1%/level) and phase 3 phantoms; Kephri's three shields, overlords, swarms, bomber/agile scarabs and eggs; Blood Thinners clouds; CA 424; CoX Scavengers and Resources rooms with 21 raid potions (the storage unit no longer hands out free supplies). CAs → 593.
  - Review caught 19 real bugs, among them a CI compile break (`SysState` duplicate local, invisible to `check_compile`), orphaned Titans wall blocks, duplicate bucket items, Obor not chasing, a Sol safespot through the fence/pillar joints, Sol one tick slow, a grapple parry exploit, a Wardens phase 2 soft-lock, Kephri dying at 1 HP, swarms spawning outside the room, overloads unmixable below 90 Herblore, and looting-bag pickup/death handling.
  - Self-test: the Konar check judged the region by the player, but the platform straddles the Misthalin/Lumbridge Swamp border (now judged where the victim stands).
  - CI on d130780e: server 2,995 / 1 (looting bag check, fixed by the review), client 357 / 0. The 21 raid potions show as skipped in `potion:*` (drunk through the raid service).

- **Session 2, round 30: quests, Sailing and islands, diaries/Agility/Thieving, combat and boss CAs, with adversarial review:**
  - Quests: a puzzle step type (`QuestPuzzle`, `QuestSpec.puzzle`, `Trigger.Kind.PUZZLE`; chat-link answers via `/questpuzzle`, per-player draws, fail-and-redraw, fight rooms); Recruitment Drive draws 5 of 7 rooms (starts at Sir Amik Varze); Murder Mystery picks a random Sinclair; Tribal Totem lock; Vale Totems; While Guthix Sleeps puzzles; Architectural Alliance, Curse of the Empty Lord (ghostly gloves/boots/cloak), In Search of Knowledge and Into the Tombs with real objectives; Restless Ghost skull and Sedridor in the Wizards' Tower basement; Cauldron of Thunder inside Taverley Dungeon; `Dt2Bosses` literals replaced by table values.
  - Sailing: special cannonballs for auto/crew fire, hull speed only for your own boat in water, eternal brazier salts, wiki lost-crate tiers/XP/loot, 8 of 10 lockboxes with keys and schematics gating boat builds, lantern harpooning (swordtip/jumbo squid), shark paint, rubium in a Charred Dungeon mine room, nickel at Ynysdail and Grimstone.
  - Diaries/Agility/Thieving: wiki shortcut snapshot (169 rows) and 38 new surface shortcuts, `shortcut:<id>` events, one-way climbing rocks and a per-use delay; underground diary visits (Crandor, Stronghold level 2); 15 shortcut tasks need the shortcut; jewellery teleport keys use the base id; 24 quest stand-ins marked approx (452 / 492 faithful); Ape Atoll stalls, Gu'Tanoth counter, cannonball stall; Dorgesh-Kaan light source and 2% fail at 99.
  - Combat: vampyre multipliers before bolt procs and limits after; specials with their own damage code follow vampyre rules; Karil's damned second hit gets hooks and XP; Justiciar minimum 1, no PvP, no typeless; rod of Ivandis charges and Retainer; Verac's beats demonic gorilla prayers. Bosses: Nightmare parasites (cure weakening, healing), Muspah shield drain sources and spikes, Araxxor eggs. CAs 593 → 598.
  - Review caught 11 real bugs: mid-quest saves soft-locked in Murder Mystery and In Search of Knowledge, Curse of the Empty Lord rewards lost mid-quest, a fight-room foe soft-lock, new quest NPCs missing in already-built places, moved shortcut ends never re-placed in old worlds (two diary tiers uncompletable), and a back-and-forth climbing-rock XP exploit. Sailing and combat reviews found only minors.
  - CI on d6258bd4 (round 30 merged): server 3,006 / 0, client 361 / 0.

## Backlog (things agents reported as missing or approximate)

Round 26 (this session) audited every line below against the code with an auditor plus a skeptic per section (109 stale claims removed). Re-check before relying on a line: other sessions may have done it since.

- **Combat Achievements:**
  - 58 tasks untracked after round 27 (73 before; Yama, Sire, Vardorvis, Amoxliatl, Maggot King, Royal Titans and Dreamland Express now tracked) (round 26 added Demonic Rebound, Mirror Image, Chitin Penetrator, Rapid Reload, three Araxxor tasks, One by one, Playing with Jads and three Barrows-run tasks). Round 29 tracked Quick Cutter, Back to the Wall, Master of Buckets, Calm Before the Storm and CA 424 (593); round 30 Terrible Parent, Crush Hour, Versatile Drainer, Space is Tight and Relaxxor (598). Still missing because the mechanic isn't modelled: Sire stuns/external tentacles, Muspah homing spikes/shockwave (CA 484), Nightmare sleepwalkers/spores, Phosani CAs 403/407, Araxyte Betrayal (566), Moons clones, Hueycoatl pillars/armour, Amoxliatl ice, Zalcano tephra golems, thralls, ratbane weapons, Giants' Den, F2P worlds, most ToB/ToA/CoX perfection tasks; Playing with Jads also needs the whole Inferno finished. 171 wearables still weigh 0 (lms_* copies could map to their base item).
  - Rewards not wired (feature missing): instance fees, cosmetic slayer helms, extra Jad/Zuk slayer kills (no Jad/Zuk tasks).
- **Minigames:**
  - Gauntlet: room contents approx (demi-boss count of 6 is sourced), bows have free arrows (deliberate), the old Hunllef summoning-stone arenas are still generated (stones only redirect), the reward chest stands at the plaza entrance place, not the lobby (3033,6124).
  - Colosseum: Solarflare/Volatility/javelin damage, wave delay and warband accuracy guessed (no wiki numbers); Sol's wind-up delay, special frequency and laser interval approx; manticore III never leads with melee; no underground lobby (pieces stand at the surface entrance); Varlamore tokens can't be spent and only the Colosseum gives them; his old summoning arena is still generated.
  - Multicannon: 8-tile range, assembly time and 32-block owner range guessed; no ornament/gold/royal parts; Nulodion's stock limit not modelled.
  - Emir's Arena: ranked duels use own gear; Fun Weapons duels can't be won (no wieldable fun weapons); blighted surge sack inert; no tournaments. LMS: competitive starts at 4 players (deliberate), no High Stakes, many layout/fog values approx.
  - Ket-Rak healer counts guessed; JalRek-Jad metamorphosis only a flag. Temple Trekking: event weights guessed.
  - Sorceress's Garden: approximate layouts. Shades of Mort'ton: Dampe's coffins and the Afflicted only appear in worlds whose Mort'ton place was built after round 28; Shade Catacombs (dungeons module) has no key chests.
  - NMZ: power-up timings guessed; the potion barrel block only exists in worlds built after round 28 (Dominic Onion serves the barrels elsewhere); rumble kills still give Slayer credit. Blighted spell sacks inert; Soul Wars cape requirement/effects missing.
- **Transport:** fairy rings to unbuilt land (BLQ, BKQ, CJQ) missing; DIP/DLS have no ring to leave from; DIQ arrives at the house portal, not its ring; KBD levers missing; obelisk timings approx (level-13 obelisk moved 4 tiles east of Ferox's wall); ring of shadows, enchanted lyre and sailors' amulet not wired; Kharedst's memoirs/book of the dead charges not modelled; giantsoul destinations, Mining Guild, Lithkren, Slepe, Dondakan and Tears of Guthix still relocated.
- **Skilling:**
  - GotR: rift creatures are invulnerable but direct damage paths (magic, cannon) still give combat XP. Volcanic Mine: lava beast rate guessed.
  - Farming: coral uses 4 harvest lives from the 30 Jul 2026 wiki snapshot (a later wiki edit may say 6); coral nurseries accept compost (OSRS forbids it); flax/hemp/cotton grow in 10-minute hops cycles (OSRS 20); the Coral Nurseries camp floats off the Great Conch; a few round 28 item values differ from the wiki.
  - Island fishing spots beyond lantern harpooning, Laguna leeches/mosquitoes/toads, Pandemonium Cave (lead), Sunbleak Cave, Buccaneers' Laboratory, Deepfin Mine; lockboxes 8 of 10 (Anglers' Retreat and Buccaneers' Haven missing; key tiles are map pins); Kragin and rubium deposits missing. Drift net fishing and shark lures missing; the celestial ring's invisible +4 doesn't apply at shooting stars. Camdozaal Vault/Ramarno/bottomless pit missing.
  - Sailing: hook success rates, creature hit chance and cannon accuracy guessed (no wiki formulas); lost-crate tier odds equal and potions come as 4-dose where the wiki gives (2)/(1); lantern harpooning only takes the lit bug lantern; shark paint only repaints to the dark oak variant; crew salvaging not modelled; the `sailing:lockbox` self-test wipes a real player's schematic unlocks.
- **Quests:** puzzle steps now exist (`QuestPuzzle`) but only Recruitment Drive, Murder Mystery, Tribal Totem, Vale Totems and While Guthix Sleeps use them; other puzzle quests still collapse into reach/talk steps; A Night at the Theatre's ToB run is a narrated reach; WGS still lacks the assassins, broav tracking, snapdragon, Dark Squall and Surok steps, and other grandmaster quests are thin; Curse of the Empty Lord has one of three ghost routes; In Search of Knowledge tomes drop from druids (OSRS: bookshelves); Cauldron of Thunder ~25 tiles off its OSRS tile; Recruitment Drive has no empty-inventory rule.
- **Raids:** Wardens have no energy siphons, beam patterns or trough choice (the phase 2 Warden is random); phantom damage/timings, Kephri egg timers and blood cloud stats guessed; CoX resource values (herb yield, growth, catch rates, scavenger count, (-)/normal revitalisation and prayer enhance) guessed, food never burns, resource rooms at fixed layout slots; Chaos bundle rare chance and honey locust count approx; most ToB/ToA/CoX perfection CAs; crystal crabs puzzle simplified.
- **Buildings:** not yet faithful: Taverley, Camelot, Canifis, Pollnivneach, Nardah, Shilo Village, Rellekka houses, most smaller towns' houses; Games Room relocated under the castle (no upstairs gallery); Watchtower Wizard not on the platform; Rellekka arrowtip chest at 2654,3666 (OSRS 2650,3659); Draynor Manor west block lacks partitions; Yanille wall one tile south; Wizards' Tower interiors simplified; Lumbridge/Varrock/Falador are out of scope unless the user asks. Wizards' Tower stairs are a pair of full-height ladders; Draynor Manor lever cellar sits under the west-wing ladder (~25 tiles off).
- **Diaries:** 16 approx tasks need mechanics that don't exist (Skavid caves, swordchick, potato cacti, Desert Phoenix feather, KQ head, two bucket tasks, menagerie pet rock, Arzinian mine, pyre ship, Karamja seaweed, Brimhaven stairs, Ape Atoll tablet, Dorgesh-Kaan grapple, dog bone, Stronghold emotes); 24 quest stand-ins marked approx (452 / 492 faithful); five shortcut tasks are visits because their shortcuts aren't built (Dwarven Mine crevice, Taverley strange floor, Zanaris jutting wall, Edgeville–Varrock Sewers pipe, Karamja rope swing). Kingdom/TBW/BA/museum constants approx.
- **Thieving and Agility:** Dorgesh-Kaan course folds Turgall's parts/spring device into the lap and has no grapple route (2% fail at 99 approx); 91 wiki shortcuts not modelled (mostly in unbuilt areas; `AgilityShortcutSnapshotTest` prints them), four skipped because structures hold their tiles, new shortcut tiles approx by a tile or two; stale blocks remain at moved shortcut ends in old worlds; Ape Atoll stalls and the Gu'Tanoth counter don't check their quest requirements; no Port Roberts stall variants; Dark Chest on the surface (Crumbling Tower basement not built).
- **World:**
  - Dungeon huts a few tiles off where another module's building holds the tile; Waterbirth doors/supports let one player through per use, Kings only appear after 90 s on a fresh server, Bardur missing; `MultiCombat.AROUND` still has a Waterbirth surface entry; stale javadocs in `WaterbirthDungeon`/`CharredDungeon`.
  - Ape Atoll dungeons are boxes; Zooknock on the surface; no hot-rock cave or locked doors. Kalphite Cave task-only rule can be bypassed by damage paths that skip `PlayerAttackModifier`.
  - Stand-ins: Braindeath, Mudskipper Point (underwater Mogre Camp), Fortis guards. Zeah multicombat polygons from 2018; the Varlamore surface has none (its dungeons do since round 28).
- **Models:** most common creatures are still blocky approximations (still open: wyrm (no reliable reference for its OSRS shape), jelly and its warped/vitreous variants (round 26 attempt rejected), greater nechryael shares the nechryael model).
- **Bosses:** Yama's sigil pieces/acquisition contracts missing; Obor knockback chance/distance, Strangle rate/spores, carrion lifetime and Tempoross lightning/fire values approx; burns and bleeds stop when the boss is gone; awakened Vardorvis axes still bleed (the wiki says they don't); Tempoross torrents and spirit anglers missing, totem repair gives no Construction XP; Phosani's last sleepwalker wave at 10% approx; awakened DT2 mechanics guessed; DT2 tablets don't teleport and quartzes have no use; Yama/Doom/Maggot King/Royal Titans lairs approx; gorilla boulder/reset timers guessed, Verac's doesn't bypass their prayer.
- **Items:**
  - Barrows quantities within a band spread evenly (approx); Mimic later-attempt rates missing; Yama rates assume full contribution; new Yama/Maggot King item values approx. Non-boss collection log pages not diffed.
  - Missing items/effects: ancient crystal (no wiki rate), aether catalyst/shark lure/sun-kissed bones/Mokhaiotl waystone (Doom table); bone shards use any altar and plain jugs of wine (no Teomat/libation bowl); the Ferox Enclave doesn't count as Wilderness for the looting bag; charged-jewellery "(n)" drop lines and poisoned weapon drops skipped.
  - Uncut zenyte is a gem-cutting recipe; Seers ring 0.2%/0.5% magic damage.
- **Combat:** Justiciar's never-named-style fallback approx; `CombatHooks.typelessNamed` treats every `nameAttack(name, null)` as typeless, so Justiciar may skip some styled boss attacks; Elysian + Justiciar applied one after the other (wiki: additive); Muspah spikes can't be dodged, Corruption drains the shield on every attack (50% chance ignored); Phosani parasites reuse Nightmare numbers; rod of Ivandis has no rod dust.
- **Monsters missing:** lobstrosity (Fossil Island underwater), armoured zombies; Varlamore dungeon teleporters and three off-tile huts not rechecked after round 28's passages/lifts/doors/shortcuts/multicombat; stalker bleed and nagua two-hit simplified; Karuulm hot floor only applies by Slayer category. Last Light, Wyrmscraig, Great Conch and Isle of Souls are still ellipses.
- **Self-test gaps:** the 13 hand-scripted quests are only checked through the stage API; some quest kill targets can't be spawned by id (fired directly); PvP minigames need a two-player harness; the client screenshot tour skips everything added in rounds 20–27; Bake Pie/String Jewellery charge one cast for the whole inventory; Superglass Make has a fixed 1.3× yield.
- **Untested in game:** much of the Minecraft-side code has only passed CI's boot test and the self-test. A real play-test pass is still the most valuable thing nobody has done.
