# RuneCraft

Old School RuneScape for **Minecraft 1.20.1 (Fabric)**. It adds a separate dimension, **Gielinor**, laid out like the real OSRS world map (as the LOTR mod does for Middle-earth). Skills, experience and combat follow OSRS formulas.

> **Status: early alpha. The Minecraft-side code has not been compiled yet.** The pure-Java OSRS core (`com.runecraft.osrs`) is compiled and unit-tested. The Fabric layer was written against the 1.20.1 Yarn API, but the build server used to write it couldn't reach `maven.fabricmc.net`. Run `./gradlew build` and expect to fix some compile errors on the first build.

## Getting there
* Craft a **Gielinor portal** (obsidian / ender pearl / diamond / ender pearl / obsidian, in a cross). Place it and right-click it to travel to Lumbridge. Right-click a portal in Gielinor to go back.
* Ops can also use `/rc gielinor`.
* Press **K** for the RuneScape interface: Combat, Skills, Quests, Prayer, Magic, Gear and the Collection log.

## Faithful to OSRS
* **24 skills:** Attack, Hitpoints, Mining, Strength, Agility, Smithing, Defence, Herblore, Fishing, Ranged, Thieving, Cooking, Prayer, Crafting, Firemaking, Magic, Fletching, Woodcutting, Runecraft, Slayer, Farming, Construction, Hunter and Sailing.
* **Exact XP curve:** level 2 is 83 XP and level 99 is 13,034,431 XP. XP caps at 200M, virtual levels go to 126, and XP is stored in tenths (e.g. 37.5 XP per oak log).
* New characters start at **combat level 3 with 10 Hitpoints**.
* **Combat formulas:** OSRS combat-level formula, effective levels, attack and defence rolls, hit chance, melee/ranged/magic max hits, combat styles with their invisible bonuses, and 4 XP per damage (1.33 when controlled) plus 1.33 Hitpoints XP.
* **Game ticks:** everything runs on the 0.6 s OSRS tick. Weapon attack speeds are in ticks, skilling actions roll every N ticks, and eating has a 3-tick delay.
* **In Gielinor:**
  * Your max health equals your Hitpoints level, and HP regenerates at 1 per minute (Rapid Heal doubles it).
  * There is no hunger.
  * Coins go into a money pouch.
  * Death returns you to Lumbridge.
* **Equipment:** every item has OSRS stat bonuses and level requirements (stab/slash/crush/magic/ranged attack and defence, strength, prayer).
* **Prayers:** all 29 standard prayers, with drain using the drain-resistance model. Protection prayers block NPC hits. Altars recharge points, and bones offered there give 350% XP.
* **Standard spellbook:** Strike, Bolt, Blast, Wave and Surge spells (autocast with a staff), the city teleports, Home Teleport, Low and High Alchemy and Superheat. Spells use runes, and elemental staves supply their own element.

## Skills content
| Skill | Content |
|---|---|
| Woodcutting | Tree, oak, willow, teak, maple, mahogany, yew, magic and redwood trees, using the OSRS success formula and the axe multipliers. Trees deplete and respawn, and bird's nests can drop. |
| Mining | Copper through runite, plus clay, silver, gold, gems, rune/pure essence and amethyst. Pickaxe tier sets the roll speed, rocks deplete, and there is a random gem chance. |
| Fishing | Six spot types (net/bait, lure/bait, cage/harpoon, big net/harpoon, anglerfish, dark crab) with 14 fish, bait consumption and the OSRS level order. |
| Cooking | Fires, ranges and vanilla campfires/smokers, with burn chances that end at OSRS stop-burn levels. |
| Firemaking | Tinderbox and logs. The fire burns out, and you step west after lighting. |
| Smithing | Smelting at a furnace, including iron's 50% failure. Anvil smithing covers daggers through platebodies, axes, pickaxes and arrowtips, at OSRS levels and bar costs. |
| Crafting | Gem cutting, leather armour, spinning and jewellery. |
| Fletching | Arrow shafts, short and long bows from normal to magic, stringing, and arrows. |
| Herblore | Cleaning herbs and 14 potions with boost/restore effects. Locked behind Druidic Ritual, as in OSRS. |
| Runecraft | 15 altars at their real map positions, with multiple runes per essence at OSRS levels. |
| Prayer | Burying bones and offering them at altars. |
| Thieving | Pickpocketing men, warriors and guards (with stun and damage on failure) and six market stalls. |
| Agility | Gnome Stronghold plus nine rooftop courses (parkour pillars) with lap bonuses and marks of grace. |
| Farming | Herb patches (14 herbs) and allotments (8 crops), growing in roughly OSRS time. |
| Hunter | Bird snares and box traps, with the creature depending on your kingdom and level. |
| Slayer | Five masters, assigned by combat level. Each kill gives Slayer XP equal to the monster's HP. |
| Construction | Sawmill planks and furniture building at a workbench (XP only for now). |
| Sailing | **Placeholder:** XP for travelling by boat in Gielinor. |

## World
* **Map:** Lumbridge is at (0, 0), and one OSRS tile is two blocks. Kingdom borders follow the OSRS map. The Wilderness starts at the ditch and shows its level. The map has 13 kingdom biomes: Misthalin, Lumbridge Swamp, Asgarnia, Wilderness, Kharidian Desert, Morytania, Kandarin, Feldip Hills, Fremennik, Troll Country, Karamja, Tirannwn and an uncharted fringe.
* **Terrain:** custom terrain gives gentle OSRS-like hills and lakes.
* **Landmarks:** about 50 landmarks are built at their real coordinates the first time their chunk loads. These are cities with banks, furnaces, anvils, ranges, shops and stalls, plus rune altars, boss lairs, agility courses and quest NPCs.
* **Monsters:** monsters use OSRS stats and are aggressive only toward players whose combat level is at most double theirs plus one. The list includes goblins, men, guards, Al-Kharid warriors, barbarians, dark wizards, hill/moss/ice giants, ghouls and lesser/greater/black demons. Vanilla mobs in Gielinor take OSRS stats.

## Bosses
Obor (needs a giant key), Bryophyta (needs a mossy key), the six **Barrows brothers** (each with their set effect), **General Graardor**, **K'ril Tsutsaroth** and **TzTok-Jad** (telegraphed attacks, so you can swap prayers). Each has a boss bar, kill counts and unique drops (Barrows gear, Bandos armour, Zamorakian spear, fire cape and more).

## Quests (22 quest points)
* Cook's Assistant
* Sheep Shearer
* Doric's Quest
* Rune Mysteries
* The Restless Ghost
* Imp Catcher
* Witch's Potion
* Romeo & Juliet
* Druidic Ritual
* Vampyre Slayer (kill Count Draynor with a stake and hammer)
* Demon Slayer (banish Delrith with Silverlight)
* The Barrows and Fight Caves miniquests

## Collection log
Boss uniques, keys, rooftop agility, hunter, gems, herbs, runes and quest rewards.

## What's not there yet
This is a large foundation, not all of OSRS. Not yet implemented:
* Most of the ~170 quests
* The Grand Exchange
* Clue scrolls
* Minigames
* Player-owned houses
* Most bosses and raids
* Dragons (no model yet)
* Diaries
* Ancient, Lunar and Arceuus spellbooks
* Special attacks
* Poison and venom
* Real Sailing content

## Building
```
./gradlew build          # needs access to maven.fabricmc.net
bash tools/run_core_tests.sh   # compiles + tests the pure-Java OSRS core without Minecraft
python3 tools/gen_assets.py    # regenerates textures/models/lang from build/catalog.json
python3 tools/gen_worldgen.py  # regenerates the Gielinor data pack
```
All OSRS data lives in `src/main/java/com/runecraft/osrs`. Adding items, monsters, recipes or bosses is mostly a matter of editing those tables.

## Commands
* **Everyone:** `/rc stats`, `/rc quests`, `/rc kc`
* **Ops:** `/rc gielinor`, `/rc leave`, `/rc tele <landmark>`, `/rc setlevel`, `/rc addxp`, `/rc completequest`, `/rc coins`

RuneScape and Old School RuneScape are trademarks of Jagex Ltd. This is an unofficial fan project.
