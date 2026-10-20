# Adding content to RuneCraft

RuneCraft content lives in **content modules**. Each module has three parts:

| Part | Package | Notes |
|---|---|---|
| Pure data | `com.runecraft.osrs.content.<module>` | Pure Java, no Minecraft imports. Compiled and unit-tested without Minecraft. |
| Minecraft behaviour | `com.runecraft.content.<module>` | Entry point `<Name>Hooks implements ModuleHooks` with `init()`. |
| Client code | `com.runecraft.client.content.<module>` | Entry point `<Name>ClientHooks implements ClientModuleHooks` with `initClient()`. |

Modules are listed in `osrs/content/Modules.java`, `content/ModuleHooksRegistry.java` and `client/content/ClientModuleHooksRegistry.java`. All three lists already contain every module, so you never edit them.

## Golden rules
1. **Only edit files inside your module's three packages** (and your module's test package `src/test/java/com/runecraft/osrs/content/<module>/`). Shared files are owned by the integrator. If you need a shared change, describe it in your final report.
2. **Ids are global.** Prefix nothing, but make sure an id isn't already used:
   - `grep -r '"your_id"' src/main/java`
   - check `build/catalog.json` after `bash tools/run_core_tests.sh`
3. **References to items:** you may only reference items that:
   - you define in your own module,
   - are built-in (`osrs/ItemCatalog.java`, the enums in `osrs/data`), or
   - are listed in `osrs/content/CommonItems.java`.

   Never reference another module's items. Modules are developed in parallel and merged later.
4. **Before you finish, both must pass:**
   - `bash tools/run_core_tests.sh` (data tests, including `ContentModulesTest`)
   - `python3 tools/check_compile.py` (best-effort compile of the Minecraft code against our own classes; must say `0 relevant error(s)`)
5. **Be faithful to OSRS:** real levels, XP, stats, drop rates and mechanics. Approximations are fine where the exact value is unknown; note them in a comment.
6. **Don't touch these:**
   - `src/main/resources` (assets are generated later from `build/catalog.json`)
   - `tools/`
   - git config

## Data API (pure Java)
Implement the methods you need on your module class (`ContentModule`):

- `items(ItemSink items)`:
  - Call `items.add(id, name, ItemDef.Kind, shape, rgbColor)`, then the fluent setters `.value(gp) .stack(n) .heal(hp) .equipment(EquipmentDef) .tier("rune") .rangedStrength(n) .examine("...")`.
  - **Kinds:** SIMPLE, FOOD (needs `heal`), BONES (only for `Bone` enum ids), POTION (4 doses; effect via your hooks), GRIMY_HERB, AXE, PICKAXE, MELEE_WEAPON, ARMOUR, SHIELD, BOW, ARROW (needs `rangedStrength`), STAFF, TOOL, SEED, RUNE, COINS, SLAYER_GEM, VIAL, TRAP.
  - **Equipment:** `new EquipmentDef(slot, new EquipmentStats(stabA, slashA, crushA, magicA, rangedA, stabD, slashD, crushD, magicD, rangedD, str, rangedStr, magicDmg%, prayer), Map.of(Skill.ATTACK, 70), speedTicks, AttackType.SLASH, WeaponClass.MELEE)`.
  - **Reference bonuses:** items that exist in OSRS take their bonuses, attack speed and two-handedness from `osrs/data/EquipmentBonuses.java` (applied after every module has added its items), so fix a wrong stat there. Two-handed weapons ignore the off-hand shield; `osrs/data/AmmoRules.java` limits which arrows/bolts each bow fires; `osrs/data/Untradeables.java` blocks Grand Exchange trading.
  - **Existing texture shapes:** coins, log, plank, ore, essence, clay, bar, arrowtips, axe, pickaxe, tinderbox, knife, chisel, hammer, needle, net, rod, pot, harpoon, dibber, mould, snare, box_trap, bait, gem, mark, fish_raw, fish_cooked, fish_burnt, meat_raw, meat_cooked, meat_burnt, cake, bones, big_bones, rune, staff, staff_orb, dagger, scimitar, longsword, warhammer, battleaxe, 2h_sword, full_helm, platebody, platelegs, kiteshield, shaft, headless, string, flax, shortbow(_u), longbow(_u), arrow, hide, leather, wool, cloth, fur, spice, cowl, leather_body, chaps, boots, gem_uncut, ring, necklace, amulet, vial, vial_water, herb_grimy, herb, potion_unf, secondary, potion, seed, produce, creature, key, club, spear, crossbow, flail, cape, tokkul, pet, pot_item, skull, bead, tail, note, package, talisman, stake, beer, whip.
  - New shape names are allowed (e.g. "godsword", "trident", "crystal_bow", "dart", "blowpipe", "shard", "casket", "orb", "book"). The asset generator will get a drawer for each new shape. List new shapes in your report.
- `recipes(Consumer<Recipe> recipes)`: `Recipe.builder(id, MenuType, name).skill(Skill, level, xp).in(item, n).out(item, n).tool(item).ticks(n).success(p).burn(stopLevel, burntItem).build()`. MenuType values: see `osrs/MenuType.java`.
- `monsters()`: return `List<MonsterDef>`:
  ```java
  MonsterDef.of("abyssal_demon", "Abyssal demon").level(124).stats(150, 97, 67, 135, 1, 1)
      .speed(4).defenceBonus(20, 20, 20, 0, 20)
      .attack(AttackDef.of("melee", AttackType.STAB, 8, 2.5, 10))
      .drops(Drop.always("ashes"), Drop.of("abyssal_whip", 1, 1, 512))
      .slayer(85, "abyssal_demons").skin(0x3A1A3A, 0x2A102A, 0x2A102A, 0xA02020)
      .size(0.8f, 2.2f, 1.2f).spawn(Region.WILDERNESS, 4, 1, 2).underground()
  ```
  - **Where it spawns:** Gielinor has no natural (biome) mob spawning. A monster only appears at its OSRS spawn locations: a camp of your module (see the dungeons, zeah, islands and godwars camps) or a point in `osrs/world/SpawnTable.java` (`SpawnPoint(monster, osrsX, osrsY, floorY, count, radius, respawnTicks)`), kept stocked by `world/LocationSpawner`. `.spawn(Region, ...)` only records the kingdoms it lives in. `SpawnTableTest` fails for a hostile monster with no camp or spawn point unless it is listed in `SpawnTable.encounterOnly()` (boss minions, raids, minigames, superiors).
  - `size(width, height, scale)`: hitbox width and height in blocks, multiplied by the render/hitbox scale.
  - `model("id")`: points at a custom `ModelSpec`. A spec with the same id as the creature is picked up automatically. Without one, the creature renders as a biped with a texture generated from `skin(...)`.
- `bosses()`: return `List<BossDef>`. Same builder plus `.phase(belowPercent, message, dmgMult, summonMinions, AttackDef...)`, `.at(place)`, `.key(item)`, `.pet(item)`, `.minion(creatureId, n)`, `.arena(radius, style)`, `.raidOnly()`, `.wilderness()`, `.category("Bosses"|"Raids"|...)`.
  - Arena styles: stone, lava, ice, crypt, jungle, void, water, sand.
  - `at(place)` is a `Landmark` enum name (e.g. "EDGEVILLE") or one of your `PlaceDef` ids. An arena with a summoning stone is generated there automatically.
  - Drops with rarity ≥ 24, plus the pet, form the boss's collection log page automatically.
- `AttackDef`: `AttackDef.of(name, AttackType, maxHit, rangeBlocks, weight)`, then `.special(Special, param)`, `.telegraph(ticks, "message")`, `.particle("minecraft:flame")` and `.message("...")`.
  - **Specials:** AOE, SKY_STRIKE (dodgeable), PRAYER_PIERCING, DRAIN_PRAYER, POISON, VENOM, LIFESTEAL, SUMMON, KNOCKBACK, FREEZE, PULL, DRAGONFIRE (anti-dragon shield / antifire reduce it), HAZARD (ground pool), DRAIN_STAT, BLINK, HEAL_SELF.
  - Telegraphed attacks give players time to switch protection prayers (Jad-style).
- `places()`: `PlaceDef.town(id, name, osrsX, osrsY, Station...)` or `PlaceDef.of(id, name, osrsX, osrsY, Style, List.of(blockIds))`.
  - Uses real OSRS world coordinates (Lumbridge = 3222, 3218).
  - Built when the chunk first loads, with a plaza in the given style plus the stations and blocks.
- `npcs()`: `NpcDef.of(id, name, place, dx, dz, skinRGB, topRGB, bottomRGB, hatRGB)`. Talk handlers are registered on the Minecraft side.
  - The skin is painted by `tools/gen_assets.py` (`npc_v2_hints`, `v2_face`, `v2_body`): an OSRS-style face, a hair style and (for men) a beard picked from the id, a skirt for women (name keywords or `FEMALE_NAMES`), belts and cuffs, and role outfits from keywords (monk tonsure and rope belt, pirate bandana/eyepatch/stripes, H.A.M. hood and robe, desert headwrap, barbarian fur and beard). Hand-written looks go in `NAMED_NPCS` (module NPCs) or `QUEST_NPC_OUTFITS` and always win.
  - `osrs/NpcRace` decides the race from the id, name and place (Keldagrim → dwarf, Lletya/Prifddinas → elf, "gnome"/"tzhaar" in the id, plus explicit id lists). Dwarves get big beards and render short and stocky, gnomes small with big noses, elves taller with pointed ears, TzHaar as rock with glowing eyes. Only the render scale changes; the hitbox does not.
- `quests()`: `QuestDef.of(id, name).difficulty(...).points(qp).requires(Skill, lvl).requiresQuest(id).xp(Skill, xp).reward("item:count").step("journal line for stage 0").step(...)`. Stage == number of steps means complete.
- `blocks()`: `BlockDef.menu(id, name, MenuType, color, look)`, `BlockDef.scripted(id, name, color, look)` and `BlockDef.decor(...)`.
  - The `look` hint drives the texture, e.g. "altar", "chest", "portal_glow", "crate", "statue", "range", "machine", "obelisk".
- `models()`: return `List<ModelSpec>`, built with `ModelSpec.builder(id).scale(s).shadow(r).part(name, parentOrNull, Role, rgb).pivot(x, y, z).rotate(pitch, yaw, roll).box(x, y, z, w, h, d).pattern("eyes|scales|plates|spots|stripes|glow|none")...build()`.
  - Units are model pixels (16 per block). Y grows downward and Y=24 is the ground, as in vanilla entity models.
  - Roles animate automatically: HEAD looks at the player; NECK sways; JAW opens on attack; LEG_* walk; LEG_EXTRA uses insect gait; ARM_* swing and attack; WING_* flap; TAIL, TENTACLE and FIN sway; SPIN rotates; FLOAT bobs; BODY breathes.
  - Use `pattern("eyes")` on heads (the generator paints eyes on the front face). Other face patterns: `eyes_animal` (small dark eyes), `face` (human face), `skull`, `visor`; `blank` stops the generator painting a face on a HEAD (slugs, insects whose eyes are separate parts).
  - `.material(...)` picks the surface painter (`rock`, `scales`, `fur`, `feathers`, `metal`, `bone`, `chitin`, `flesh`, `crystal`, `smoke`, `spectral`, `granite` (carved, cracked gargoyle stone)…) and `.glow(...)` adds full-bright detail in the accent colour: `eyes`, `maw`, `cracks` (lava veins), `veins` (branching blood/energy veins), `runes`, `spots` (glowing eyes/pores), `rim` (glowing edges), `sigil` (Corporeal Beast's spirit sigil on the front face), `full`. `spectral` parts get a glowing accent rim by themselves. `pattern("swirls")` paints pale accent-coloured swirl markings (Corp). Flagship boss re-sculpts live in `bestiary/Flagships*.java` and common-creature re-sculpts in `bestiary/Common1.java` to `Common5.java` (every TzHaar caste is built on the one body `Common2.tzhaarBody` and differs only by gear); render before/after side by side and keep a re-sculpt only when it is clearly closer to OSRS.
  - The painter also lights the whole model from above (a top-to-bottom gradient and a contact shadow at the feet), so don't darken lower parts by hand.
  - `modelVariants()`: `ModelSpec.Variant.of(id, baseModel[, scale])` (or `new ModelSpec.Variant(id, base, scale, tint)`) copies a registered model, after overrides, for creatures that look the same in OSRS (Nightmare Zone bosses, champions, God Wars ogres).
  - Keep each model under about 40 boxes.
- `collectionLog()`: extra `CollectionLogData.Page`s.

## Minecraft API (in your `Hooks.init()`)
### Bosses
- **Boss scripts:**
  - `BossScripts.register("vorkath", VorkathScript::new)`.
  - `BossScript` has `onSpawn`, `tick`, `gameTick`, `chooseAttack`, `modifyIncoming`, `onHitPlayer`, `onPhase`, `onDeath` and `isVulnerable`.
  - Use it for mechanics `AttackDef` can't express: Zulrah rotations, Vorkath's acid walk, Nightmare totems, Hydra's vents, and so on.
- **Boss helpers:**
  - `DataBossService.spawn(world, pos, bossDef, target)`
  - `DataBossService.buildArena(world, center, radius, style)`
  - `DataBossService.requirement(bossId, (player, def) -> boolean)`
- **Mechanics:** `Mechanics.perform(mob, target, attackDef, mult)`, `Mechanics.addHazard(world, pos, dmg, ticks, owner)` and `Mechanics.choose(...)`.

### Combat
- `CombatHandler.applyDamage(attacker, target, dmg)`, `CombatHandler.npcAttack(mob, player, type)` and `CombatHandler.awardXp(player, style, dmg, magic)`.
- `CombatHooks.addModifier(PlayerAttackModifier)` adjusts accuracy, max hit, rolled damage and after-hit effects (special attacks, slayer helm, salve, void).
- `CombatHooks.addHurtListener(...)` reacts to a player being hit.
- `PoisonService.poison/venom/cure`.

### Quests
- `QuestScripts.onTalk(npcId, (player, npcEntity) -> ...)` and `QuestScripts.onKill(...)`.
- `QuestApi.say/you/stage/setStage/start/complete/isComplete/takeAll/has/give`.
- Declarative quests (`QuestSpec`): `.kill("ice_troll", 5)` (or `.kill(x).times(5)`) needs N kills, counted per player in `QuestsState` and saved; `.drop(x, item, chance).times(5)` keeps dropping until the player carries 5. The self-test reports `count` kills.
- Puzzles: `.puzzle(npcOrPlace, QuestPuzzle, slot)` shows an in-world puzzle (combination lock, river crossing, riddle, deduction) answered through chat links (`/questpuzzle`). `QuestPuzzle.builder(key).room(id).variant(npc).prompt(..).options(..).solution(..)`; rooms are drawn per player (`picks`, `always`), variants picked per player, a wrong answer can send the player back and re-draw (`failTo`). Never change a quest's stage count (it is saved): put extra steps inside existing stages with `.stay().flag(..)`.

### Events
- `GameEvents.onXp/onKill/onMake/onObtain`: diaries, clue steps, points.

### World, blocks and items
- `BlockHooks.register(blockId, (player, pos, state) -> ...)` handles scripted blocks.
- `LandmarkBuilder.registerCustom(placeIdOrLandmarkName, (world, centre) -> ...)` builds extra structures (dungeon rooms, minigame arenas) once.
- `MenuService.open(player, MenuType, stationPos, range)`.
- `XpService.add(player, Skill, xp)`.
- `Inv.has/count/remove/give/stack/idOf`. Coins and tokkul live in pouches (OSRS stacks them in one slot up to 2,147,483,647, a Minecraft stack holds 64), so use `Inv` with "coins" or "tokkul".
- `Msg.game/red/action`.
- `Players.data(player)` gives `PlayerData`: skills, prayer, quest stages, kill counts, collection log, bank, coins.
- `ServerNetworking.sync(player)`.
- `ActionManager.start(player, Action)` for repeating 0.6s-tick actions. `ActionManager.gameTick()/mcTick()`.
- `Gielinor.teleport(player, Landmark)`, `Gielinor.safeSpot(world, x, z)`, `Gielinor.is(entity)`, `Gielinor.wildernessLevel(entity)`.
- `CollectionLog.obtain(player, item)`.

### NeoForge and Minecraft
- Target Minecraft 1.21.1 with Mojang names on NeoForge 21.1 (Java 21). Check every Minecraft/NeoForge call with `tools/compile_full.sh` (see docs/NEOFORGE_PORT.md, "Compiling locally"); `javap` on the NeoForge jar shows exact signatures.
- Events and networking go through `com.runecraft.platform` (and `.platform.client`): Fabric-API-shaped classes (`ServerTickEvents`, `UseBlockCallback`, `ServerPlayNetworking`, `HudRenderCallback`, ...) bridged to NeoForge. Use them rather than NeoForge's buses directly. Networking channels use ids under `runecraft:<module>/...`.
- Blocks, items and entity types are registered inside NeoForge's `RegisterEvent` (see `RuneCraft.register`); entity types use `registry.MobTypeBuilder`, which also handles default attributes and spawn rules. Never create a `Block`/`Item` outside registration.
- Persistent state: `SavedData` with `SavedData.Factory`, key `runecraft_<module>`; `save(CompoundTag, HolderLookup.Provider)`.
- Per-stack data: `util.StackData` (the `custom_data` component). Stacks saved in our own data: `ItemStack.saveOptional(provider)` (counts above 99 throw: split first) and `util.LegacyStacks.parse(provider, tag)` to read them (it upgrades stacks saved by the Fabric 1.20.1 build).

### Client (`initClient()`)
- `EntityRendererRegistry` for your own entity types.
- `HudRenderCallback` for overlays.
- `KeyBindingHelper` for keys.
- Your own `Screen`s.
- Renderers for module monsters and bosses are registered automatically: a custom model if a `ModelSpec` exists, otherwise a biped.

## Structures (`ContentModule.structures()`)

Hand-built buildings live in `osrs/content/world/*Structures.java` as `StructureDef`s
(`com.runecraft.osrs.world.StructureDef`). Each is an ordered list of block ops; the Minecraft side
(`world/StructurePlacer`) applies each structure's share chunk by chunk as chunks load, so size is
not a problem.

* Coordinates: OSRS tile (x, y) covers blocks `[bx(x), bx(x)+1] x [bz(y), bz(y)+1]`; higher OSRS y is
  further north (smaller block z). The `t*` builder methods take OSRS tiles; heights are blocks
  relative to `ground()` (default 64, sea level is 62). `y = 0` is the first block above the floor,
  `y = -1` the floor itself.
* Start every settlement with `tFlatten(...)` over its footprint: it levels the ground to
  `ground()-1` and blends an 8-block ring back into the natural terrain.
* Use `tBuilding` for ordinary houses, `tTower` for round towers, `tWallLine` for town/castle
  walls, `roof` for pitched roofs, and the raw `fill/walls/clear/set` primitives for detail.
  Block strings use command syntax: `minecraft:oak_stairs[facing=north,half=top]`,
  `runecraft:bank_booth`. Station blocks: `runecraft:bank_booth`, `furnace`, `anvil`,
  `cooking_range`, `prayer_altar`, `general_store`, `rune_shop`, `weapon_shop`, `tool_shop`,
  `archery_shop`, `spinning_wheel`, `tanning_rack`, `construction_workbench`, `herb_patch`,
  `allotment_patch`, `*_stall`, `gielinor_portal`, `slayer_master`, `apothecary_table`.
* `replaces("LUMBRIDGE")` (Landmark enum name or module place id) suppresses that place's default
  paved plaza. Its NPCs, boss arenas and any station the structure does not contain are still
  added, so put the bank booths / furnaces / altars where they really are.
* Doors and beds: give one half with full properties (`minecraft:oak_door[facing=north,half=lower]`,
  `minecraft:red_bed[facing=east,part=foot]`); the placer adds the other half.

## Terrain (`osrs/world/Elevation.java`)

Gielinor's landscape comes from a hand-authored height model of the OSRS world map, not from
Minecraft's climate noise. `GielinorGeography` holds the coastlines, islands, mountain ridges,
rivers, lakes and swamps in OSRS tile coordinates. `Elevation.height(blockX, blockZ)` gives the
surface height, and the dimension samples it through the `runecraft:gielinor_terrain` density
function. Run `bash tools/render_terrain.sh` to redraw `docs/previews/terrain_map.png`.
Zeah, Varlamore, Fossil Island, Ape Atoll and the Sailing-era sea islands (`SeaIslands`, 52 islands and islets) are traced from the map squares (coasts as polygons, lakes as
`TRACED_LAKES` polygons with their islands, streams as `RIVERS`); a place on such a coast must sit on a real land tile.

The terrain adapts to content automatically (`TerrainAnchors`):
* Every `FLATTEN` op levels its box exactly to its `ground()`. The terrain then stays within ±6
  blocks of that level for 16 blocks around the box and climbs at most 1.5 blocks per block beyond.
  A town never gets a cliff at its edge. Mountains simply rise further away.
* Places with a plaza (any style except `NONE`) and their boss arenas get a small flat terrace at
  their natural height. Style `NONE` places build nothing and leave the terrain alone.
* Rooms under y 56 (dungeons) always have at least 5 blocks of rock above them, and the surface over
  them stays at y 56 or higher, except over rooms whose top is below y 34 (`DEEP_UNDERGROUND_BELOW`):
  those only need the rock cover, so a dungeon carved that deep under the sea leaves the sea bed alone.
* Large water fills at sea level (harbours, moats, river channels) stay open water. Natural rivers stop
  at the edge of a town's levelled ground, so extend a town's own river channel to the edge of its
  `tFlatten` box.

For a structure on a mountain top, build it at a higher ground level, e.g.
`StructureDef.builder(id, name).ground(120)`; Trollheim and the Troll Stronghold do this. Avoid
building your own hills or mountains out of blocks. Add a ridge to `GielinorGeography.RIDGES`
instead. A place on a summit that should not be levelled goes in `TerrainAnchors.MOUNTAIN_PLACES`.
