# RuneCraft

Old School RuneScape for **Minecraft 1.20.1 (Fabric)**. It adds a separate dimension, **Gielinor**, laid out like the real OSRS world map (as the LOTR mod does for Middle-earth). Skills, experience and combat follow OSRS formulas.

> **Status: early alpha.** Every push is built by GitHub Actions. The latest jar is on the [dev-latest release](https://github.com/WreatheR6/RuneCraft/releases/tag/dev-latest). Most content has not been play-tested in game yet.

## Getting there
* Craft a **Gielinor portal**: obsidian in the corners, ender pearls on the edges, a diamond in the centre. Place it and right-click it to travel to Lumbridge. Right-click a portal in Gielinor to go back.
* Ops can also use `/rc gielinor`.
* The OSRS side panel sits at the bottom-right of the screen (Skills tab by default, with Combat, Quests, Collection log, Equipment, Prayer and Magic tabs). **Hold Left Alt** to free the cursor and use it: hover a skill for its XP tooltip, click it for the skill guide. **K** shows or hides the panel, **J** opens the full-screen RuneScape interface.

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
* **Map:** Lumbridge is at (0, 0), and one OSRS tile is two blocks. Kingdom borders follow the OSRS map. The Wilderness starts at the ditch and shows its level.
* **Hand-built structures:** about 200 towns, castles, guilds, temples and ruins are built at their real OSRS tile positions, chunk by chunk as you explore. The ground under each one is levelled and blended into the surrounding terrain. This covers:
  * Misthalin: Lumbridge, Draynor and Draynor Manor, Varrock and the Grand Exchange, Edgeville, Barbarian Village, the Wizards' Tower.
  * Asgarnia: Falador and the White Knights' Castle, Port Sarim, Rimmington, Taverley, Burthorpe, the Warriors' and Heroes' guilds, the Black Knights' Fortress, Entrana.
  * Karamja: Musa Point, Brimhaven, Shilo Village, Tai Bwo Wannai, the volcano.
  * Kandarin and the west: Camelot, Seers', Catherby, both Ardougnes, Yanille, the Gnome Stronghold and Grand Tree, Rellekka, Lletya, Prifddinas, Trollheim.
  * The desert: Al Kharid, Pollnivneach, Nardah, Sophanem, Menaphos, the pyramids.
  * Morytania: Canifis, Port Phasmatys, the Barrows, Darkmeyer, Ver Sinhaza, the Slayer Tower.
  * The Wilderness: the ditch, Ferox Enclave, the Mage Arena, Rogues' Castle and ruins.
  * Great Kourend and Varlamore: Kourend Castle with the Catacombs, the five house towns, Civitas illa Fortis and the Colosseum.
  * Islands and the far west: Fossil Island, Ape Atoll, Miscellania and Etceteria, Lunar Isle, Neitiznot and Jatizso, Waterbirth, Mos Le'Harmless, Zanaris (with fairy rings), Crandor, Tutorial Island, Castle Wars, Corsair Cove, Gu'Tanoth, Zul-Andra and the Void Knights' Outpost.
* **Dungeons:** 27 dungeons are carved underground beneath their real entrances, with monsters that spawn inside. These include Taverley, Brimhaven, Edgeville, the Stronghold of Security (with its rewards), the slayer caves, the Kalphite Lair, TzHaar city, the Ancient Cavern, Dorgesh-Kaan and the Abyss.

  Previews are in `docs/previews/`.
* **NPCs and shops:** 86 shops with OSRS stock and the OSRS shop price formula, shared stock and restocking. There are bankers in every bank, about 700 NPCs with dialogue, and attackable, pickpocketable townsfolk.
* **Travel:** boats, charter ships, gnome gliders, magic carpets, canoes, the Lovakengj minecarts and the quetzal network.
* **Monsters:** over 300 monster types with OSRS stats, drop tables and OSRS aggression rules.

## Bosses and raids
* **Bosses:** 200 bosses with custom models and scripted mechanics. Examples are God Wars, Zulrah, Vorkath, the wilderness bosses, the Desert Treasure II bosses, the Moons of Peril, slayer bosses and Varlamore bosses.
* **Raids:** Chambers of Xeric, Theatre of Blood and Tombs of Amascut, with invocations and scaling.

## Quests
196 quests and miniquests, worth 326 quest points. This covers F2P through Varlamore. They have OSRS requirements, steps, NPCs and rewards; some recent quests are approximations.

## Systems and minigames
* **Systems:** slayer with every master, clue scrolls, the Grand Exchange, achievement diaries, the ancient, lunar and Arceuus spellbooks, special attacks, jewellery teleports, skill capes, and run energy.
* **Minigames:** Fight Caves, the Inferno, Pest Control, Wintertodt, Tempoross, Guardians of the Rift, Barbarian Assault, Castle Wars, the Warriors' Guild, Mage Training Arena, Pyramid Plunder, Hallowed Sepulchre and more.

## Pets, houses and Sailing
* **Pets:** boss, raid, minigame and skilling pets drop at OSRS rates, with an "All Pets" log page. Pets follow their owner.
* **Player-owned houses:** portals in all eight towns, 18 rooms and 133 furniture pieces. Altars, pools, lecterns, jewellery boxes and portals all work. Use `/house` to build.
* **Sailing:** buy boats at shipwrights and sail them on water. There are charter docks at 12 ports and voyages that give Sailing XP.

## Collection log
Pages for bosses, raids, clues, minigames, slayer, quests and other activities.

## Known limitations
* Much of the content is generated data that has not been play-tested in game yet. Expect balance and placement issues.
* Layouts of structures are recognisable approximations, not tile-exact copies of the OSRS map.
* Some mechanics are simplified. For example, the Moons of Peril drop their own sets instead of using a shared chest.
* Some recent content uses approximate stats, coordinates or quest details. These are marked `approx`/`approximate` in the code.

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
