package com.runecraft.osrs.content.skilling;

/**
 * Sailing sea tables generated from the OSRS Wiki (July 2026 wikitext; snapshots in src/test/resources/osrs/sailing):
 * salvage sorting loot, sea creatures (Boat combat table and monster infoboxes), their drops and bounty tasks.
 * Tab-separated rows, parsed by {@link SailingSea}. Regenerate rather than edit.
 */
final class SailingSeaTables {
    private SailingSeaTables() {
    }

    /** salvage, section, item, quantity, rarity. */
    static final String SALVAGE = """
Small salvage	Pre-roll	Boat bottle (empty)	1	1/750
Small salvage	Pre-roll	Sailors' amulet (inert)	1	1/2000
Small salvage	Pre-roll	Rusty locket	1	1/3000
Small salvage	Shipbuilding	Bronze bar	1	200/1721
Small salvage	Shipbuilding	Bronze nails	1-5	200/1721
Small salvage	Shipbuilding	Logs	1	200/1721
Small salvage	Shipbuilding	Iron nails	1-5	100/1721
Small salvage	Shipbuilding	Plank	1	100/1721
Small salvage	Shipbuilding	Oak logs	1	100/1721
Small salvage	Shipbuilding	Sawmill coupon (wood plank)	1	100/1721
Small salvage	Shipbuilding	Iron bar	1	50/1721
Small salvage	Shipbuilding	Oak plank	1	50/1721
Small salvage	Shipbuilding	Sawmill coupon (oak plank)	1	50/1721
Small salvage	Shipbuilding	Steel bar	1	1/1721
Small salvage	Runes and ammunition	Air rune	1-10	100/1721
Small salvage	Runes and ammunition	Water rune	1-10	100/1721
Small salvage	Runes and ammunition	Bronze cannonball	2-7	100/1721
Small salvage	Runes and ammunition	Iron cannonball	2-7	50/1721
Small salvage	Runes and ammunition	Steel arrow	1-5	20/1721
Small salvage	Other	Coins	1-200	100/1721
Small salvage	Other	Bones	1	100/1721
Small salvage	Tertiary	Clue scroll (beginner)	1	1/300
Small salvage	Tertiary	Soup	1	1/800000
Fishy salvage	Pre-roll	Clue scroll (easy)	1	1/300
Fishy salvage	Pre-roll	Boat bottle (empty)	1	1/750
Fishy salvage	Pre-roll	Sailors' amulet (inert)	1	1/1500
Fishy salvage	Pre-roll	Mouldy block	1	1/3000
Fishy salvage	Shipbuilding	Oak logs	1	40/467
Fishy salvage	Shipbuilding	Oak plank	1	20/467
Fishy salvage	Shipbuilding	Sawmill coupon (oak plank)	1	20/467
Fishy salvage	Shipbuilding	Logs	1	20/467
Fishy salvage	Shipbuilding	Iron bar	1	20/467
Fishy salvage	Shipbuilding	Iron nails	1-5	20/467
Fishy salvage	Shipbuilding	Steel nails	1-5	10/467
Fishy salvage	Shipbuilding	Plank	1	10/467
Fishy salvage	Shipbuilding	Steel bar	1	1/467
Fishy salvage	Fishing	Feather	15-200	40/467
Fishy salvage	Fishing	Fishing bait	20-100	40/467
Fishy salvage	Fishing	Raw lobster	1	40/467
Fishy salvage	Fishing	Raw mackerel	1	40/467
Fishy salvage	Fishing	Raw swordfish	1	40/467
Fishy salvage	Fishing	Fishing rod	1	2/467
Fishy salvage	Fishing	Harpoon	1	2/467
Fishy salvage	Fishing	Lobster pot	1	2/467
Fishy salvage	Other	Seaweed	1	40/467
Fishy salvage	Other	Bronze cannonball	2-7	40/467
Fishy salvage	Other	Iron cannonball	1-5	20/467
Fishy salvage	Tertiary	Soup	1	1/800000
Barracuda salvage	Pre-roll	Boat bottle (empty)	1	1/500
Barracuda salvage	Pre-roll	Dull knife	1	1/3000
Barracuda salvage	Pre-roll	Sailors' amulet (inert)	1	1/1000
Barracuda salvage	Seeds	Flax seed	1-3	500/28401
Barracuda salvage	Seeds	Hemp seed	1-3	125/28401
Barracuda salvage	Shipbuilding	Swamp paste	1-10	5000/28401
Barracuda salvage	Shipbuilding	Oak logs	1	5000/28401
Barracuda salvage	Shipbuilding	Teak logs	1	3750/28401
Barracuda salvage	Shipbuilding	Steel nails	1-5	2500/28401
Barracuda salvage	Shipbuilding	Teak repair kit	1	2500/28401
Barracuda salvage	Shipbuilding	Rope	1	1250/28401
Barracuda salvage	Shipbuilding	Oak plank	1	1250/28401
Barracuda salvage	Shipbuilding	Copper ore	1	500/28401
Barracuda salvage	Shipbuilding	Teak plank	1	250/28401
Barracuda salvage	Shipbuilding	Lead ore	1	25/28401
Barracuda salvage	Fish	Raw swordfish	1	500/28401
Barracuda salvage	Fish	Raw shark	1	250/28401
Barracuda salvage	Fish	Raw marlin	1	1/28401
Barracuda salvage	Other	Rum	1	2500/28401
Barracuda salvage	Other	Banana	1	1250/28401
Barracuda salvage	Other	Steel cannonball	2-7	1250/28401
Barracuda salvage	Tertiary	Clue scroll (medium)	1	1/300
Barracuda salvage	Tertiary	Soup	1	1/800000
Large salvage	Pre-roll	Sailors' amulet (inert)	1	1/500
Large salvage	Pre-roll	Facility bottle (empty)	1	1/1000
Large salvage	Pre-roll	Broken compass	1	1/3000
Large salvage	Shipbuilding	Steel nails	1-5	200/2218
Large salvage	Shipbuilding	Teak logs	1	200/2218
Large salvage	Shipbuilding	Oak plank	1	50/2218
Large salvage	Shipbuilding	Mithril nails	1-5	30/2218
Large salvage	Seeds and frags	Elkhorn frag	1-5	10/2218
Large salvage	Seeds and frags	Hemp seed	1-5	10/2218
Large salvage	Seeds and frags	Cotton seed	1-5	5/2218
Large salvage	Seeds and frags	Pillar frag	1-5	2/2218
Large salvage	Seeds and frags	Camphor seed	1	1/2218
Large salvage	Jewellery	Gold ring	1	300/2218
Large salvage	Jewellery	Sapphire ring	1	300/2218
Large salvage	Jewellery	Emerald ring	1	300/2218
Large salvage	Jewellery	Diamond ring	1	200/2218
Large salvage	Jewellery	Oyster pearl	1	200/2218
Large salvage	Jewellery	Oyster pearls	1	200/2218
Large salvage	Jewellery	Casket	1	100/2218
Large salvage	Other	Giant seaweed	1	50/2218
Large salvage	Other	Mithril cannonball	2-5	50/2218
Large salvage	Other	Adamant cannonball	1-7	10/2218
Large salvage	Tertiary	Soup	1	1/800000
Plundered salvage	Pre-roll	Sailors' amulet (inert)	1	1/450
Plundered salvage	Pre-roll	Rusty coin	1	1/3000
Plundered salvage	Pre-roll	Dragon cannonball	2-3	1/3000
Plundered salvage	Pre-roll	Facility bottle (empty)	1	1/900
Plundered salvage	Sailing supplies	Rum	1	100/1036
Plundered salvage	Sailing supplies	Mithril cannonball	2-5	50/1036
Plundered salvage	Sailing supplies	Adamant cannonball	2-5	25/1036
Plundered salvage	Sailing supplies	Mahogany repair kit	1	25/1036
Plundered salvage	Sailing supplies	Rune cannonball	2-4	10/1036
Plundered salvage	Weapons	Mithril scimitar	1	100/1036
Plundered salvage	Weapons	Rune scimitar	1	1/1036
Plundered salvage	Jewellery	Oyster pearls	1	100/1036
Plundered salvage	Jewellery	Gold ring	1	200/1036
Plundered salvage	Jewellery	Sapphire ring	1	100/1036
Plundered salvage	Jewellery	Emerald bracelet	1	100/1036
Plundered salvage	Jewellery	Ruby bracelet	1	75/1036
Plundered salvage	Jewellery	Emerald ring	1	50/1036
Plundered salvage	Jewellery	Casket	1	50/1036
Plundered salvage	Jewellery	Diamond bracelet	1	25/1036
Plundered salvage	Jewellery	Diamond ring	1	25/1036
Plundered salvage	Tertiary	Soup	1	1/800000
Martial salvage	Pre-roll	Sailors' amulet (inert)	1	1/400
Martial salvage	Pre-roll	Clue scroll (hard)	1	1/500
Martial salvage	Pre-roll	Facility bottle (empty)	1	1/700
Martial salvage	Pre-roll	Broken sextant	1	1/3000
Martial salvage	Pre-roll	Dragon cannonball	2-3	1/3000
Martial salvage	Pre-roll	Salvor's paint	1	1/24000
Martial salvage	Weapons and armour	Mithril longsword	1	300/2440
Martial salvage	Weapons and armour	Adamant longsword	1	200/2440
Martial salvage	Weapons and armour	Adamant 2h sword	1	100/2440
Martial salvage	Weapons and armour	Green d'hide body	1	100/2440
Martial salvage	Weapons and armour	Rune longsword	1	10/2440
Martial salvage	Ammunition and materials	Adamant arrowtips	1-5	300/2440
Martial salvage	Ammunition and materials	Mithril dart tip	1-5	300/2440
Martial salvage	Ammunition and materials	Adamant dart tip	1-3	200/2440
Martial salvage	Ammunition and materials	Adamant arrow	1-3	200/2440
Martial salvage	Ammunition and materials	Adamant bolts(unf)	1-3	100/2440
Martial salvage	Ammunition and materials	Rune arrow	1-3	100/2440
Martial salvage	Ammunition and materials	Adamant cannonball	2-5	100/2440
Martial salvage	Ammunition and materials	Rune cannonball	2-4	50/2440
Martial salvage	Seeds and frags	Umbral frag	1	10/2440
Martial salvage	Seeds and frags	Camphor seed	1	10/2440
Martial salvage	Seeds and frags	Ironwood seed	1	1/2440
Martial salvage	Logs	Mahogany logs	1	100/2440
Martial salvage	Logs	Camphor logs	1	6/2440
Martial salvage	Logs	Ironwood logs	1	3/2440
Martial salvage	Other	Amulet of power	1	200/2440
Martial salvage	Other	Adamantite nails	1-3	50/2440
Martial salvage	Tertiary	Soup	1	1/800000
Fremennik salvage	Pre-roll	Sailors' amulet (inert)	1	1/350
Fremennik salvage	Pre-roll	Facility bottle (empty)	1	1/500
Fremennik salvage	Pre-roll	Smashed mirror	1	1/3000
Fremennik salvage	Fish	Raw salmon	1	400/2670
Fremennik salvage	Fish	Raw tuna	1	400/2670
Fremennik salvage	Fish	Raw lobster	1	400/2670
Fremennik salvage	Fish	Raw monkfish	1	400/2670
Fremennik salvage	Helmets	Archer helm	1	8/2670
Fremennik salvage	Helmets	Berserker helm	1	8/2670
Fremennik salvage	Helmets	Farseer helm	1	8/2670
Fremennik salvage	Helmets	Warrior helm	1	8/2670
Fremennik salvage	Helmets	Fremennik helm	1	8/2670
Fremennik salvage	Materials	Mithril ore	1	300/2670
Fremennik salvage	Materials	Arctic pine logs	1	200/2670
Fremennik salvage	Materials	Adamantite nails	1-3	100/2670
Fremennik salvage	Materials	Rune nails	1-3	50/2670
Fremennik salvage	Materials	Mahogany plank	1	50/2670
Fremennik salvage	Materials	Ironwood logs	1	6/2670
Fremennik salvage	Materials	Rosewood logs	1	3/2670
Fremennik salvage	Seeds and frags	Cotton seed	1-3	10/2670
Fremennik salvage	Seeds and frags	Umbral frag	1	5/2670
Fremennik salvage	Seeds and frags	Ironwood seed	1	5/2670
Fremennik salvage	Seeds and frags	Rosewood seed	1	1/2670
Fremennik salvage	Other	Astral rune	1-3	200/2670
Fremennik salvage	Other	Ensouled troll head	1	50/2670
Fremennik salvage	Other	Rune cannonball	2-5	50/2670
Fremennik salvage	Tertiary	Soup	1	1/800000
Opulent salvage	Pre-roll	Sailors' amulet (inert)	1	1/300
Opulent salvage	Pre-roll	Facility bottle (empty)	1	1/300
Opulent salvage	Pre-roll	Dragon nails	1-3	1/2000
Opulent salvage	Pre-roll	Mouldy doll	1	1/3000
Opulent salvage	Pre-roll	Dragon cannon barrel	1	1/20000
Opulent salvage	Sailing resources	Rune cannonball	2-5	3/424
Opulent salvage	Sailing resources	Fish offcuts	1-10	30/424
Opulent salvage	Sailing resources	Ironwood repair kit	1	10/424
Opulent salvage	Sailing resources	Dragon cannonball	2-5	1/424
Opulent salvage	Other	Uncut opal	1	30/424
Opulent salvage	Other	Uncut jade	1	30/424
Opulent salvage	Other	Uncut red topaz	1	30/424
Opulent salvage	Other	Silver ore	1	30/424
Opulent salvage	Other	Silver bar	1	30/424
Opulent salvage	Other	Tiara	1	30/424
Opulent salvage	Other	Jug of wine	1	30/424
Opulent salvage	Other	Spice	1	30/424
Opulent salvage	Other	Grey wolf fur	1	30/424
Opulent salvage	Other	Silk	1	30/424
Opulent salvage	Other	Coins	1000-5000	20/424
Opulent salvage	Other	Platinum token	1-5	20/424
Opulent salvage	Tertiary	Soup	1	1/800000
            """;

    /** name, combat, hp, att, str, def, max hits by keel, speed, style, aggressive, dlight, dstandard, dheavy, x, y, location. */
    static final String CREATURES = """
Dolphin	22	45	25	10	0	2,1,0,0,0,0,0,0	4	SLASH	No	15	15	0	2710	2474	Anglerfish's Light - north-east of Charred Island
Tern	20	22	25	20	0	3,2,1,0,0,0,0,0	4	STAB	No	30	30	10	3041	3110	Bay of Sarim - south-east of Mudskipper Point
Eagle ray	44	55	70	1	0	3,2,1,0,0,0,0,0	4	SLASH	No	50	50	20	2754	2228	Anglerfish's Light - south-west of Last Light
Butterfly ray	68	83	100	35	15	4,3,2,1,0,0,0,0	4	SLASH	No	50	50	20	1334	2861	Dusk's Maw - west of Mistrock
Bull shark	65	81	80	45	20	5,4,3,2,1,0,0,0	4	SLASH	No	100	20	40	2799	3259	Around Crandor
Mogre (sea)	54	59	40	50	40	6,5,4,3,2,0,0,0	6	CRUSH	No	0	0	0	2762	2891	Kharazi Strait - south of the Kharazi Jungle
Osprey	63	72	60	50	40	6,5,4,3,2,0,0,0	4	STAB	No	30	30	10	1890	3403	Around Chinchompa Island
Stingray	92	106	130	55	25	6,5,4,3,2,0,0,0	4	SLASH	No	50	50	20	2613	2426	The Lonely Sea - west of Charred Island
Hammerhead shark	90	125	80	65	50	7,6,5,4,3,1,0,0	4	SLASH	Yes	100	20	40	2891	2792	Arrow Passage - north of Crash Island
Narwhal	111	187	95	65	50	7,6,5,4,3,1,0,0	4	SLASH	No	65	65	40	1838	4056	Everwinter Sea - west of Brittle Isle
Frigatebird	78	79	80	70	40	8,7,6,5,4,2,0,0	4	STAB	No	30	30	10	2803	2317	Anglerfish's Light - west of Last Light
Manta ray (monster)	133	194	155	75	40	8,7,6,5,4,2,0,0	4	SLASH	No	50	50	20	1927	2665	The Everdeep - south of Deepfin Point
Tiger shark	125	174	120	85	60	9,8,7,6,5,3,1,0	4	SLASH	Yes	100	20	40	2691	2356	Anglerfish's Light - south-east of Charred Island
Pygmy kraken	93	150	1	1	50	10,9,8,7,6,4,2,0	5	MAGIC	Yes	120	75	60	1175	2704	Laguna Aurorae - eastern coastline
Albatross	110	135	100	90	60	10,9,8,7,6,4,2,0	4	STAB	No	20	25	10	2929	2372	Anglerfish's Light - north-east of Last Light
Orca	205	370	165	125	75	13,12,11,10,9,7,5,3	4	SLASH	No	150	60	40	2269	3714	Fremennik Strait - south-west of Neitiznot
Spined kraken	128	177	1	1	75	14,13,12,11,10,8,6,4	5	MAGIC	Yes	120	75	60	1296	2611	Calidum Reach - south-east of Laguna Aurorae
Great white shark	175	243	155	145	70	15,14,13,12,11,9,7,5	4	SLASH	Yes	100	20	40	1116	2810	Dusk's Maw - north-west of Laguna Aurorae
Armoured kraken	180	231	1	1	150	18,17,16,15,14,12,10,8	5	MAGIC	Yes	175	125	125	2019	3261	Crystal Sea - south-east of Port Roberts
Veiled kraken	210	225	200	185	115	19,18,17,16,15,13,11,9	5	CRUSH	Yes	325	325	0	2308	2164	South of Rainbow's End
Vampyre kraken	211	311	1	1	115	22,21,20,19,18,16,14,12	5	MAGIC	Yes	120	75	60	1890	4145	Everwinter Sea - north of Brittle Isle
            """;

    /** creature, Magic level, attack bonus of its style (wiki infoboxes: mage, attbns / amagic). */
    static final String CREATURE_ACCURACY = """
            Dolphin	1	0
            Tern	1	0
            Eagle ray	1	0
            Butterfly ray	1	0
            Bull shark	1	0
            Mogre (sea)	1	22
            Osprey	1	0
            Stingray	1	0
            Hammerhead shark	1	0
            Narwhal	1	0
            Frigatebird	1	0
            Manta ray (monster)	1	0
            Tiger shark	1	0
            Pygmy kraken	90	0
            Albatross	1	0
            Orca	1	0
            Spined kraken	135	0
            Great white shark	0	0
            Armoured kraken	175	0
            Veiled kraken	100	0
            Vampyre kraken	215	0
            """;

    /** creature, section, item, quantity, rarity. */
    static final String CREATURE_DROPS = """
Dolphin	Pre-roll	Echo pearl	1	1/1200
Dolphin	100%	Bones	1	Always
Dolphin	Runes and ammunition	Water rune	50-80	15/50
Dolphin	Runes and ammunition	Iron cannonball	30-42	8/50
Dolphin	Runes and ammunition	Bronze cannonball	36-54	6/50
Dolphin	Resources	Seaweed	3-5	10/50
Dolphin	Resources	Flax seed	3-5	4/50
Dolphin	Resources	Swamp paste	40-60	4/50
Dolphin	Resources	Elkhorn frag	1	1/50
Dolphin	Other	Casket	1	1/50
Dolphin	Other	Gnomeball	1	1/50
Dolphin	Tertiary	Clue scroll (easy)	1	1/55
Tern	100%	Bones	1	Always
Tern	Seeds and frags	Flax seed	1	15/500
Tern	Seeds and frags	Hemp seed	1	9/500
Tern	Seeds and frags	Cotton seed	1	6/500
Tern	Seeds and frags	Elkhorn frag	1	6/500
Tern	Seeds and frags	Pillar frag	1	3/500
Tern	Seeds and frags	Umbral frag	1	1/500
Tern	Bird nests	Bird nest (seeds)	1	65/5000
Tern	Bird nests	Bird nest (ring)	1	32/5000
Tern	Bird nests	Bird nest (egg)#Red egg	1	1/5000
Tern	Bird nests	Bird nest (egg)#Green egg	1	1/5000
Tern	Bird nests	Bird nest (egg)#Blue egg	1	1/5000
Tern	Other	Feather	100-200	25/50
Tern	Other	Raw salmon	1	6/50
Tern	Other	Raw tuna	1	4/50
Tern	Other	Sapphire	1	4/50
Tern	Other	Sapphire ring	1	4/50
Tern	Other	Oak roots	1	2/50
Tern	Tertiary	Tern feather	1	1/3
Tern	Tertiary	Tern beak	1	1/10
Tern	Tertiary	Clue scroll (easy)	1	1/125
Eagle ray	Pre-roll	Ray barbs	1	1/50
Eagle ray	Sailing seeds and frags	Elkhorn frag	1	6/750
Eagle ray	Sailing seeds and frags	Pillar frag	1	3/750
Eagle ray	Sailing seeds and frags	Umbral frag	1	1/750
Eagle ray	Sailing seeds and frags	Flax seed	1	5/750
Eagle ray	Sailing seeds and frags	Hemp seed	1	3/750
Eagle ray	Sailing seeds and frags	Cotton seed	1	2/750
Eagle ray	Other	Bucket of sand	10-15	14/150
Eagle ray	Other	Seaweed	10-15	14/150
Eagle ray	Tertiary	Eagle ray skin	1	1/10
Eagle ray	Tertiary	Eagle ray fin	1	1/2
Eagle ray	Tertiary	Clue scroll (easy)	1	1/75
Butterfly ray	Pre-roll	Ray barbs	1	1/45
Butterfly ray	Sailing seeds and frags	Elkhorn frag	1	72/2980
Butterfly ray	Sailing seeds and frags	Pillar frag	1	36/2980
Butterfly ray	Sailing seeds and frags	Umbral frag	1	12/2980
Butterfly ray	Sailing seeds and frags	Flax seed	1	50/2980
Butterfly ray	Sailing seeds and frags	Hemp seed	1	30/2980
Butterfly ray	Sailing seeds and frags	Cotton seed	1	20/2980
Butterfly ray	Sailing seeds and frags	Camphor seed	1	15/2980
Butterfly ray	Sailing seeds and frags	Ironwood seed	1	4/2980
Butterfly ray	Sailing seeds and frags	Rosewood seed	1	1/2980
Butterfly ray	Other	Bucket of sand	10-15	6/149
Butterfly ray	Other	Seaweed	10-15	6/149
Butterfly ray	Tertiary	Butterfly ray skin	1	1/10
Butterfly ray	Tertiary	Butterfly ray fin	1	1/2
Butterfly ray	Tertiary	Clue scroll (medium)	1	1/85
Bull shark	Weapons and armour	Adamant crossbow	1	10/100
Bull shark	Weapons and armour	Adamant platelegs	1	8/100
Bull shark	Weapons and armour	Adamant mace	1	6/100
Bull shark	Weapons and armour	Adamant halberd	1	5/100
Bull shark	Weapons and armour	Snakeskin chaps	1	5/100
Bull shark	Weapons and armour	Battlestaff	1	4/100
Bull shark	Weapons and armour	Water battlestaff	1	3/100
Bull shark	Weapons and armour	Rune dagger	1	3/100
Bull shark	Frags	Elkhorn frag	1	12/1000
Bull shark	Frags	Pillar frag	1	6/1000
Bull shark	Frags	Umbral frag	1	2/1000
Bull shark	Other	Coins	3000-8000	12/100
Bull shark	Other	Emerald ring	1	12/100
Bull shark	Other	Mithril arrowtips	20-40	10/100
Bull shark	Other	Raw shark	1	8/100
Bull shark	Other	Death rune	20-30	5/100
Bull shark	Other	Steel cannonball	48-60	4/100
Bull shark	Other	Fishy salvage	1	2/100
Bull shark	Tertiary	Bull shark jaw	1	1/3
Bull shark	Tertiary	Bull shark liver	1	1/10
Bull shark	Tertiary	Clue scroll (medium)	1	1/100
Mogre (sea)	Pre-roll	Mudskipper hat	1	2/60
Mogre (sea)	Pre-roll	Flippers	1	1/60
Mogre (sea)	100%	Big bones	1	Always
Mogre (sea)	Fish	Raw swordfish	1	20/128
Mogre (sea)	Fish	Raw tuna	1	9/128
Mogre (sea)	Fish	Raw pike	1	7/128
Mogre (sea)	Fish	Raw shark	1	4/128
Mogre (sea)	Fish	Raw salmon	1	4/128
Mogre (sea)	Fish	Raw herring	1	3/128
Mogre (sea)	Fish	Raw sardine	1	3/128
Mogre (sea)	Other	Fishing bait	10-40	23/128
Mogre (sea)	Other	Seaweed	1	16/128
Mogre (sea)	Other	Oyster	1	7/128
Mogre (sea)	Other	Water rune	20-40	6/128
Mogre (sea)	Other	Giant seaweed	1	4/128
Mogre (sea)	Other	Staff of water	1	1/128
Mogre (sea)	Other	Fishbowl#Water	1	1/128
Mogre (sea)	Tertiary	Mogre mace	1	1/3
Mogre (sea)	Tertiary	Mogre head	1	1/10
Osprey	100%	Bones	1	Always
Osprey	Seeds and frags	Flax seed	1	20/500
Osprey	Seeds and frags	Hemp seed	1	12/500
Osprey	Seeds and frags	Cotton seed	1	8/500
Osprey	Seeds and frags	Elkhorn frag	1	12/500
Osprey	Seeds and frags	Pillar frag	1	6/500
Osprey	Seeds and frags	Umbral frag	1	2/500
Osprey	Bird nests	Bird nest (seeds)	1	65/2500
Osprey	Bird nests	Bird nest (ring)	1	32/2500
Osprey	Bird nests	Bird nest (egg)#Red egg	1	1/2500
Osprey	Bird nests	Bird nest (egg)#Green egg	1	1/2500
Osprey	Bird nests	Bird nest (egg)#Blue egg	1	1/2500
Osprey	Other	Feather	200-300	25/50
Osprey	Other	Raw lobster	1	2/50
Osprey	Other	Raw tuna	1	5/50
Osprey	Other	Emerald	1	4/50
Osprey	Other	Emerald ring	1	4/50
Osprey	Other	Willow roots	1	2/50
Osprey	Tertiary	Osprey feather	1	1/3
Osprey	Tertiary	Osprey beak	1	1/10
Osprey	Tertiary	Clue scroll (medium)	1	1/200
Stingray	Pre-roll	Ray barbs	1	1/30
Stingray	Sailing seeds and frags	Elkhorn frag	1-2	18/500
Stingray	Sailing seeds and frags	Pillar frag	1-2	9/500
Stingray	Sailing seeds and frags	Umbral frag	1-2	3/500
Stingray	Sailing seeds and frags	Camphor seed	1	15/400
Stingray	Sailing seeds and frags	Ironwood seed	1	4/400
Stingray	Sailing seeds and frags	Rosewood seed	1	1/400
Stingray	Other	Bucket of sand	10-15	3/100
Stingray	Other	Seaweed	10-15	3/100
Stingray	Other	Weapon poison(+)	1	2/100
Stingray	Other	Weapon poison(++)	1	2/100
Stingray	Tertiary	Stingray skin	1	1/10
Stingray	Tertiary	Stingray fin	1	1/2
Stingray	Tertiary	Clue scroll (hard)	1	1/185
Hammerhead shark	Weapons and armour	Yew shortbow	1	10/100
Hammerhead shark	Weapons and armour	Mithril platebody	1	8/100
Hammerhead shark	Weapons and armour	Adamant mace	1	6/100
Hammerhead shark	Weapons and armour	Blue d'hide vambraces	1	5/100
Hammerhead shark	Weapons and armour	Mystic boots	1	5/100
Hammerhead shark	Weapons and armour	Battlestaff	1	4/100
Hammerhead shark	Weapons and armour	Water battlestaff	1	3/100
Hammerhead shark	Weapons and armour	Adamant platebody	1	3/100
Hammerhead shark	Frags	Pillar frag	1	12/1000
Hammerhead shark	Frags	Elkhorn frag	1	6/1000
Hammerhead shark	Frags	Umbral frag	1	2/1000
Hammerhead shark	Other	Coins	3000-8000	12/100
Hammerhead shark	Other	Ruby necklace	1	12/100
Hammerhead shark	Other	Sapphire bolt tips	20-40	10/100
Hammerhead shark	Other	Raw shark	1	8/100
Hammerhead shark	Other	Chaos rune	50-80	5/100
Hammerhead shark	Other	Mithril cannonball	48-60	4/100
Hammerhead shark	Other	Barracuda salvage	1	2/100
Hammerhead shark	Tertiary	Hammerhead shark jaw	1	1/2
Hammerhead shark	Tertiary	Hammerhead shark liver	1	1/10
Hammerhead shark	Tertiary	Clue scroll (medium)	1	1/100
Narwhal	Pre-roll	Narwhal horn	1	1/20
Narwhal	Pre-roll	Dragon nails	3-7,5-10	1/75
Narwhal	100%	Big bones	1	Always
Narwhal	Weapons	Rune mace	1	1/60
Narwhal	Weapons	Rune scimitar	1	1/60
Narwhal	Weapons	Rune sword	1	1/60
Narwhal	Runes and ammunition	Water rune	300-800	15/60
Narwhal	Runes and ammunition	Adamant arrow	1	4/60
Narwhal	Runes and ammunition	Rune arrow	1	4/60
Narwhal	Runes and ammunition	Adamant cannonball	36-54	4/60
Narwhal	Runes and ammunition	Rune cannonball	30-42	2/60
Narwhal	Seeds and frags	Flax seed	2-3	5/75
Narwhal	Seeds and frags	Hemp seed	2-3	3/75
Narwhal	Seeds and frags	Cotton seed	2-3	2/75
Narwhal	Seeds and frags	Elkhorn frag	1-2	6/200
Narwhal	Seeds and frags	Pillar frag	1-2	3/200
Narwhal	Seeds and frags	Umbral frag	1-2	1/200
Narwhal	Other	Swamp paste	40-60	8/60
Narwhal	Other	Giant seaweed	1	6/60
Narwhal	Other	Casket	1	1/60
Narwhal	Other	Old boot	1	1/60
Narwhal	Tertiary	Narwhal blubber	1	1/2
Narwhal	Tertiary	Narwhal tusk	1	1/10
Narwhal	Tertiary	Clue scroll (hard)	1	1/150
Frigatebird	100%	Bones	1	Always
Frigatebird	Seeds and frags	Flax seed	1	30/500
Frigatebird	Seeds and frags	Hemp seed	1	18/500
Frigatebird	Seeds and frags	Cotton seed	1	12/500
Frigatebird	Seeds and frags	Elkhorn frag	1	18/500
Frigatebird	Seeds and frags	Pillar frag	1	9/500
Frigatebird	Seeds and frags	Umbral frag	1	3/500
Frigatebird	Other	Feather	300-500	18/50
Frigatebird	Other	Raw swordfish	1	5/50
Frigatebird	Other	Raw lobster	1	2/50
Frigatebird	Other	Ruby	1	5/50
Frigatebird	Other	Ruby ring	1	5/50
Frigatebird	Other	Maple roots	1	2/50
Frigatebird	Tertiary	Frigatebird feather	1	1/3
Frigatebird	Tertiary	Frigatebird beak	1	1/10
Frigatebird	Tertiary	Clue scroll (hard)	1	1/170
Manta ray (monster)	Pre-roll	Ray barbs	1	1/20
Manta ray (monster)	Sailing seeds and frags	Pillar frag	1	16/297
Manta ray (monster)	Sailing seeds and frags	Umbral frag	1	8/297
Manta ray (monster)	Sailing seeds and frags	Camphor seed	1	15/1980
Manta ray (monster)	Sailing seeds and frags	Ironwood seed	1	4/1980
Manta ray (monster)	Sailing seeds and frags	Rosewood seed	1	1/1980
Manta ray (monster)	Other	Raw manta ray	1	15/99
Manta ray (monster)	Other	Bucket of sand	10-15	5/99
Manta ray (monster)	Other	Seaweed	10-15	5/99
Manta ray (monster)	Tertiary	Manta ray skin	1	1/10
Manta ray (monster)	Tertiary	Clue scroll (hard)	1	1/120
Tiger shark	Weapons and armour	Adamant crossbow	1	10/100
Tiger shark	Weapons and armour	Adamant platelegs	1	8/100
Tiger shark	Weapons and armour	Adamant mace	1	6/100
Tiger shark	Weapons and armour	Adamant halberd	1	5/100
Tiger shark	Weapons and armour	Snakeskin chaps	1	5/100
Tiger shark	Weapons and armour	Battlestaff	1	4/100
Tiger shark	Weapons and armour	Water battlestaff	1	3/100
Tiger shark	Weapons and armour	Rune dagger	1	3/100
Tiger shark	Frags	Elkhorn frag	1	12/1000
Tiger shark	Frags	Pillar frag	1	6/1000
Tiger shark	Frags	Umbral frag	1	2/1000
Tiger shark	Other	Emerald ring	1	24/100
Tiger shark	Other	Mithril arrowtips	20-40	10/100
Tiger shark	Other	Raw shark	1	8/100
Tiger shark	Other	Death rune	20-30	5/100
Tiger shark	Other	Steel cannonball	48-60	4/100
Tiger shark	Other	Fishy salvage	1	2/100
Tiger shark	Tertiary	Tiger shark liver	1	1/10
Tiger shark	Tertiary	Tiger shark jaw	1	1/2
Tiger shark	Tertiary	Clue scroll (hard)	1	Rare
Pygmy kraken	Pre-roll	Inky paint	1	1/3000
Pygmy kraken	Runes and ammunition	Water rune	100-150	10/108
Pygmy kraken	Runes and ammunition	Death rune	20-25	10/108
Pygmy kraken	Runes and ammunition	Blood rune	10-15	6/108
Pygmy kraken	Runes and ammunition	Adamant cannonball	18-24	2/108
Pygmy kraken	Weapons and armour	Water battlestaff	1	3/108
Pygmy kraken	Weapons and armour	Battlestaff	1	3/108
Pygmy kraken	Weapons and armour	Mithril chainbody	1	2/108
Pygmy kraken	Coral frags	Elkhorn frag	1	6/540
Pygmy kraken	Coral frags	Pillar frag	1	3/540
Pygmy kraken	Coral frags	Umbral frag	1	1/540
Pygmy kraken	Other	Raw swordfish	1	14/108
Pygmy kraken	Other	Raw bass	1	14/108
Pygmy kraken	Other	Coins	3000-5000	8/108
Pygmy kraken	Other	Teak plank	2-4	8/108
Pygmy kraken	Other	Swamp tar	10-25	6/108
Pygmy kraken	Other	Teak repair kit	1	5/108
Pygmy kraken	Other	Large salvage	1	1/108
Pygmy kraken	Tertiary	Pygmy kraken tentacle	1	1/2
Pygmy kraken	Tertiary	Pygmy kraken ink sac	1	1/10
Pygmy kraken	Tertiary	Clue scroll (medium)	1	Rare
Albatross	Pre-roll	Swift albatross feather	1	1/35
Albatross	100%	Bones	1	Always
Albatross	Seeds and frags	Flax seed	1	40/500
Albatross	Seeds and frags	Hemp seed	1	24/500
Albatross	Seeds and frags	Cotton seed	1	16/500
Albatross	Seeds and frags	Elkhorn frag	1	24/500
Albatross	Seeds and frags	Pillar frag	1	12/500
Albatross	Seeds and frags	Umbral frag	1	4/500
Albatross	Bird nests	Bird nest (seeds)	1	65/1000
Albatross	Bird nests	Bird nest (ring)	1	32/1000
Albatross	Bird nests	Bird nest (egg)#Red egg	1	1/1000
Albatross	Bird nests	Bird nest (egg)#Green egg	1	1/1000
Albatross	Bird nests	Bird nest (egg)#Blue egg	1	1/1000
Albatross	Other	Feather	500-1000	14/50
Albatross	Other	Raw swordfish	1	5/50
Albatross	Other	Raw shark	1	2/50
Albatross	Other	Diamond	1	5/50
Albatross	Other	Diamond ring	1	5/50
Albatross	Other	Yew roots	1	2/50
Albatross	Tertiary	Albatross feather	1	1/2
Albatross	Tertiary	Albatross beak	1	1/10
Albatross	Tertiary	Clue scroll (hard)	1	1/120
Orca	Pre-roll	Echo pearl	1	1/70
Orca	Pre-roll	Dragon nails	4-8,7-11	Unknown
Orca	Pre-roll	Dragon metal sheet	1-2	Unknown
Orca	Pre-roll	Dragon keel parts	1	Unknown
Orca	Pre-roll	Large dragon keel parts	1	Unknown
Orca	100%	Big bones	1	Always
Orca	Runes and ammunition	Rune arrow	50-75	10/92
Orca	Runes and ammunition	Rune cannonball	24-42	5/92
Orca	Runes and ammunition	Blood rune	45-70	3/92
Orca	Runes and ammunition	Dragon cannonball	18-24	2/92
Orca	Weapons and armour	Rune longsword	1	8/92
Orca	Weapons and armour	Rune platelegs	1	6/92
Orca	Weapons and armour	Rune warhammer	1	6/92
Orca	Seeds and frags	Pillar frag	1	3/92
Orca	Seeds and frags	Umbral frag	1	2/92
Orca	Seeds and frags	Flax seed	1	5/460
Orca	Seeds and frags	Hemp seed	1	3/460
Orca	Seeds and frags	Cotton seed	1	2/460
Orca	Seeds and frags	Camphor seed	1	15/1840
Orca	Seeds and frags	Ironwood seed	1	4/1840
Orca	Seeds and frags	Rosewood seed	1	1/1840
Orca	Other	Coins	15000-20000	12/92
Orca	Other	Raw sea turtle	1	10/92
Orca	Other	Oyster pearls	1	8/92
Orca	Other	Camphor repair kit	1-2	6/92
Orca	Other	Fremennik salvage	1	6/92
Orca	Tertiary	Orca blubber	1	1/2
Orca	Tertiary	Orca teeth	1	1/10
Orca	Tertiary	Clue scroll (elite)	1	1/130
Spined kraken	Pre-roll	Inky paint	1	1/2500
Spined kraken	Runes and ammunition	Water rune	250-400	10/109
Spined kraken	Runes and ammunition	Death rune	35-50	10/109
Spined kraken	Runes and ammunition	Blood rune	20-30	8/109
Spined kraken	Runes and ammunition	Adamant cannonball	24-36	4/109
Spined kraken	Runes and ammunition	Rune cannonball	18-24	2/109
Spined kraken	Weapons and armour	Water battlestaff	1	5/109
Spined kraken	Weapons and armour	Mystic water staff	1	3/109
Spined kraken	Weapons and armour	Air battlestaff	1	3/109
Spined kraken	Weapons and armour	Battlestaff	1	2/109
Spined kraken	Weapons and armour	Adamant chainbody	1	2/109
Spined kraken	Seeds	Camphor seed	1	15/2180
Spined kraken	Seeds	Ironwood seed	1	4/2180
Spined kraken	Seeds	Rosewood seed	1	1/2180
Spined kraken	Other	Raw shark	1	14/109
Spined kraken	Other	Coins	8000-12000	8/109
Spined kraken	Other	Teak plank	2-4	8/109
Spined kraken	Other	Plundered salvage	1	5/109
Spined kraken	Other	Mahogany repair kit	1	1/109
Spined kraken	Tertiary	Spined kraken ink sac	1	1/10
Spined kraken	Tertiary	Spined kraken tentacle	1	1/2
Spined kraken	Tertiary	Clue scroll (hard)	1	Rare
Great white shark	Pre-roll	Broken dragon hook	1	1/500
Great white shark	Pre-roll	Large dragon keel parts	1	1/500
Great white shark	Pre-roll	Dragon keel parts	1	1/1000
Great white shark	Pre-roll	Dragon metal sheet	1-2	1/200
Great white shark	Sailing resources	Rune cannonball	24-36	4/100
Great white shark	Sailing resources	Dragon cannonball	12-18	2/100
Great white shark	Sailing resources	Fremennik salvage	1	2/100
Great white shark	Weapons and armour	Adamant platebody	1	8/100
Great white shark	Weapons and armour	Battlestaff	1	6/100
Great white shark	Weapons and armour	Rune sword	1	5/100
Great white shark	Weapons and armour	Mystic hat	1	5/100
Great white shark	Weapons and armour	Black d'hide shield	1	3/100
Great white shark	Weapons and armour	Dragon med helm	1	1/100
Great white shark	Weapons and armour	Dragon longsword	1	1/100
Great white shark	Other	Raw shark	1	17/100
Great white shark	Other	Diamond amulet	1	12/100
Great white shark	Other	Coins	15000-20000	8/100
Great white shark	Other	Rune arrow	50-65	8/100
Great white shark	Other	Runite bolts	30-50	8/100
Great white shark	Other	Onyx bolt tips	4-6	4/100
Great white shark	Other	Dragon dart tip	15-25	3/100
Great white shark	Other	Crystal key	1	2/100
Great white shark	Tertiary	Great white shark jaw	1	1/2
Great white shark	Tertiary	Great white shark liver	1	1/10
Great white shark	Tertiary	Clue scroll (elite)	1	1/200
Armoured kraken	Pre-roll	Dragon metal sheet	1-2	1/140
Armoured kraken	Pre-roll	Bottled storm	1	1/500
Armoured kraken	Pre-roll	Large dragon keel parts	1	1/600
Armoured kraken	Pre-roll	Dragon keel parts	1	1/800
Armoured kraken	Pre-roll	Inky paint	1	1/2000
Armoured kraken	Runes and ammunition	Death rune	40-65	10/119
Armoured kraken	Runes and ammunition	Water rune	400-650	10/119
Armoured kraken	Runes and ammunition	Blood rune	25-35	8/119
Armoured kraken	Runes and ammunition	Rune cannonball	24-36	4/119
Armoured kraken	Weapons and armour	Battlestaff	1	6/119
Armoured kraken	Weapons and armour	Water battlestaff	1	6/119
Armoured kraken	Weapons and armour	Earth battlestaff	1	6/119
Armoured kraken	Weapons and armour	Mystic water staff	1	5/119
Armoured kraken	Weapons and armour	Mystic earth staff	1	5/119
Armoured kraken	Weapons and armour	Adamant platebody	1	2/119
Armoured kraken	Weapons and armour	Mystic robe bottom	1	1/119
Armoured kraken	Other	Raw sea turtle	1	12/119
Armoured kraken	Other	Coins	16000-18500	10/119
Armoured kraken	Other	Teak plank	2-4	8/119
Armoured kraken	Other	Camphor repair kit	1	4/119
Armoured kraken	Other	Plundered salvage	1	2/119
Armoured kraken	Tertiary	Armoured kraken tentacle	1	1/2
Armoured kraken	Tertiary	Armoured kraken ink sac	1	1/10
Armoured kraken	Tertiary	Clue scroll (elite)	1	1/160
Veiled kraken	Pre-roll	Dragon cannon barrel	1	1/500
Veiled kraken	Pre-roll	Dragon metal sheet	1-2	4/500
Veiled kraken	Pre-roll	Dragon keel parts	1	2/500
Veiled kraken	Pre-roll	Inky paint	1	1/1900
Veiled kraken	Pre-roll	Large dragon keel parts	1	2/500
Veiled kraken	Runes and ammunition	Rune cannonball	80-100	10/272
Veiled kraken	Runes and ammunition	Earth rune	500-750	10/272
Veiled kraken	Runes and ammunition	Water rune	500-750	10/272
Veiled kraken	Runes and ammunition	Death rune	50-75	8/272
Veiled kraken	Runes and ammunition	Blood rune	30-40	5/272
Veiled kraken	Runes and ammunition	Dragon cannonball	25-35	3/272
Veiled kraken	Runes and ammunition	Onyx bolts	10-20	2/272
Veiled kraken	Weapons and armour	Earth battlestaff	1	30/272
Veiled kraken	Weapons and armour	Mystic earth staff	1	30/272
Veiled kraken	Weapons and armour	Water battlestaff	1	30/272
Veiled kraken	Weapons and armour	Mystic water staff	1	30/272
Veiled kraken	Weapons and armour	Rune full helm	1	20/272
Veiled kraken	Weapons and armour	Rune platelegs	1	20/272
Veiled kraken	Weapons and armour	Mystic hat	1	3/272
Veiled kraken	Weapons and armour	Mystic gloves	1	3/272
Veiled kraken	Weapons and armour	Mystic boots	1	3/272
Veiled kraken	Resources	Oyster pearls	1	10/272
Veiled kraken	Resources	Ironwood repair kit	1	5/272
Veiled kraken	Resources	Opulent salvage	1	5/272
Veiled kraken	Resources	Giant seaweed	1	5/272
Veiled kraken	Resources	Snake hide	1	5/272
Veiled kraken	Resources	Raw bluefin	1	5/272
Veiled kraken	Resources	Raw halibut	1	5/272
Veiled kraken	Resources	Camphor plank	1	3/272
Veiled kraken	Resources	Rosewood repair kit	1	1/272
Veiled kraken	Tertiary	Veiled kraken ink sac	1	1/10
Veiled kraken	Tertiary	Veiled kraken tentacle	1	1/3
Veiled kraken	Tertiary	Clue scroll (elite)	1	1/140
Vampyre kraken	Pre-roll	Bottled storm	1	1/300
Vampyre kraken	Pre-roll	Dragon metal sheet	1-2	1/150
Vampyre kraken	Pre-roll	Dragon keel parts	1	1/300
Vampyre kraken	Pre-roll	Large dragon keel parts	1	1/300
Vampyre kraken	Pre-roll	Inky paint	1	1/1500
Vampyre kraken	Runes and ammunition	Death rune	50-75	10/120
Vampyre kraken	Runes and ammunition	Water rune	500-750	10/120
Vampyre kraken	Runes and ammunition	Blood rune	30-40	8/120
Vampyre kraken	Runes and ammunition	Rune cannonball	48-60	4/120
Vampyre kraken	Runes and ammunition	Dragon cannonball	30-42	2/120
Vampyre kraken	Weapons and armour	Battlestaff	1	6/120
Vampyre kraken	Weapons and armour	Water battlestaff	1	6/120
Vampyre kraken	Weapons and armour	Fire battlestaff	1	6/120
Vampyre kraken	Weapons and armour	Mystic water staff	1	5/120
Vampyre kraken	Weapons and armour	Mystic fire staff	1	5/120
Vampyre kraken	Weapons and armour	Rune chainbody	1	3/120
Vampyre kraken	Weapons and armour	Mystic robe top	1	1/120
Vampyre kraken	Seeds	Camphor seed	1	15/2400
Vampyre kraken	Seeds	Ironwood seed	1	4/2400
Vampyre kraken	Seeds	Rosewood seed	1	1/2400
Vampyre kraken	Other	Raw sea turtle	1	12/120
Vampyre kraken	Other	Coins	17500-20000	10/120
Vampyre kraken	Other	Mahogany plank	3-5	8/120
Vampyre kraken	Other	Ironwood repair kit	1	4/120
Vampyre kraken	Other	Fremennik salvage	1	2/120
Vampyre kraken	Tertiary	Vampyre kraken ink sac	1	1/10
Vampyre kraken	Tertiary	Vampyre kraken tentacle	1	1/2
Vampyre kraken	Tertiary	Clue scroll (elite)	1	1/150
            """;

    /** id, level, xp, board, monster, item, quantity, rarity, guaranteed. */
    static final String BOUNTIES = """
438	30	3465	Port Sarim	Tern	Tern feather	15	1/3	yes
439	30	3465	Port Sarim	Mogre (sea)	Mogre mace	15	1/3	
440	30	3465	Port Sarim	Bull shark	Bull shark jaw	15	1/3	
441	55	19910	Port Sarim	Tiger shark	Tiger shark jaw	25	1/2	
442	30	3465	Port Sarim	Osprey	Osprey beak	5	1/10	
443	30	3465	Port Sarim	Bull shark	Bull shark liver	5	1/10	
444	50	14575	Port Sarim	Pygmy kraken	Pygmy kraken ink sac	5	1/10	
445	30	3465	The Pandemonium	Bull shark	Bull shark jaw	15	1/3	
446	30	3465	The Pandemonium	Mogre (sea)	Mogre mace	15	1/3	
447	30	3465	The Pandemonium	Osprey	Osprey feather	15	1/3	
448	40	8800	The Pandemonium	Butterfly ray	Butterfly ray fin	15	1/3	
449	30	3465	The Pandemonium	Tern	Tern beak	5	1/10	yes
450	40	8800	The Pandemonium	Hammerhead shark	Hammerhead shark liver	5	1/10	
451	45	11825	The Pandemonium	Frigatebird	Frigatebird beak	5	1/10	
452	30	3465	Musa Point	Bull shark	Bull shark jaw	15	1/3	yes
453	30	3465	Musa Point	Mogre (sea)	Mogre mace	15	1/3	
454	30	3465	Musa Point	Tern	Tern feather	15	1/3	
455	40	8800	Musa Point	Butterfly ray	Butterfly ray fin	15	1/3	
456	30	3465	Musa Point	Eagle ray	Eagle ray skin	5	1/10	
457	30	3465	Musa Point	Bull shark	Bull shark liver	5	1/10	
458	40	8800	Musa Point	Hammerhead shark	Hammerhead shark liver	5	1/10	
459	30	3465	Catherby	Tern	Tern feather	15	1/3	
460	30	3465	Catherby	Osprey	Osprey feather	15	1/3	yes
461	30	3465	Catherby	Bull shark	Bull shark jaw	15	1/3	
462	50	14575	Catherby	Pygmy kraken	Pygmy kraken tentacle	30	1/2	
463	30	3465	Catherby	Mogre (sea)	Mogre head	5	1/10	
464	45	11825	Catherby	Frigatebird	Frigatebird beak	5	1/10	
465	50	14575	Catherby	Albatross	Albatross beak	5	1/10	
466	30	3465	Brimhaven	Osprey	Osprey feather	15	1/3	yes
467	30	3465	Brimhaven	Tern	Tern feather	15	1/3	
468	30	3465	Brimhaven	Bull shark	Bull shark jaw	15	1/3	
469	50	14575	Brimhaven	Albatross	Albatross feather	20	1/2	
470	40	8800	Brimhaven	Hammerhead shark	Hammerhead shark liver	5	1/10	
471	55	19910	Brimhaven	Tiger shark	Tiger shark liver	5	1/10	
472	30	3465	Brimhaven	Mogre (sea)	Mogre head	5	1/10	
473	30	3465	Ardougne	Mogre (sea)	Mogre mace	15	1/3	yes
474	30	3465	Ardougne	Osprey	Osprey feather	15	1/3	
475	30	3465	Ardougne	Bull shark	Bull shark jaw	15	1/3	
476	50	14575	Ardougne	Pygmy kraken	Pygmy kraken tentacle	30	1/2	
477	40	8800	Ardougne	Hammerhead shark	Hammerhead shark liver	5	1/10	
478	45	11825	Ardougne	Stingray	Stingray skin	5	1/10	
479	65	30965	Ardougne	Spined kraken	Spined kraken ink sac	5	1/10	
480	30	3465	Port Khazard	Mogre (sea)	Mogre mace	15	1/3	
481	30	3465	Port Khazard	Bull shark	Bull shark jaw	15	1/3	
482	40	8800	Port Khazard	Hammerhead shark	Hammerhead shark jaw	15	1/3	yes
483	75	40370	Port Khazard	Great white shark	Great white shark jaw	25	1/2	
484	30	3465	Port Khazard	Mogre (sea)	Mogre head	5	1/10	
485	30	3465	Port Khazard	Eagle ray	Eagle ray skin	5	1/10	
486	50	14575	Port Khazard	Albatross	Albatross beak	5	1/10	
487	40	8800	Corsair Cove	Hammerhead shark	Hammerhead shark jaw	15	1/3	
488	50	14575	Corsair Cove	Pygmy kraken	Pygmy kraken tentacle	15	1/3	
489	40	3465	Corsair Cove	Mogre (sea)	Mogre mace	15	1/3	
490	45	11825	Corsair Cove	Stingray	Stingray fin	25	1/2	yes
491	40	3465	Corsair Cove	Osprey	Osprey beak	5	1/10	
492	55	19910	Corsair Cove	Tiger shark	Tiger shark liver	5	1/10	
493	75	40370	Corsair Cove	Great white shark	Great white shark liver	5	1/10	
494	48	3465	Ruins of Unkah	Bull shark	Bull shark jaw	15	1/3	
495	48	8800	Ruins of Unkah	Hammerhead shark	Hammerhead shark jaw	25	1/2	
496	55	19910	Ruins of Unkah	Tiger shark	Tiger shark jaw	20	1/2	
497	48	8800	Ruins of Unkah	Butterfly ray	Butterfly ray fin	20	1/2	
498	48	3465	Ruins of Unkah	Eagle ray	Eagle ray skin	5	1/10	yes
499	50	14575	Ruins of Unkah	Albatross	Albatross beak	5	1/10	
500	75	40370	Ruins of Unkah	Great white shark	Great white shark liver	5	1/10	
501	50	11825	Void Knights' Outpost	Stingray	Stingray fin	20	1/2	
502	50	3465	Void Knights' Outpost	Osprey	Osprey feather	25	1/2	
503	50	11825	Void Knights' Outpost	Frigatebird	Frigatebird feather	15	1/3	
504	50	14575	Void Knights' Outpost	Pygmy kraken	Pygmy kraken tentacle	30	1/2	yes
505	50	3465	Void Knights' Outpost	Eagle ray	Eagle ray skin	5	1/10	
506	75	40370	Void Knights' Outpost	Great white shark	Great white shark liver	5	1/10	
507	60	25135	Void Knights' Outpost	Manta ray (monster)	Manta ray skin	5	1/10	
508	45	8800	The Summer Shore	Butterfly ray	Butterfly ray fin	25	1/2	
509	45	3465	The Summer Shore	Eagle ray	Eagle ray fin	25	1/2	
510	45	11825	The Summer Shore	Frigatebird	Frigatebird feather	15	1/3	
511	75	40370	The Summer Shore	Great white shark	Great white shark jaw	25	1/2	
512	50	14575	The Summer Shore	Albatross	Albatross beak	5	1/10	yes
513	55	19910	The Summer Shore	Tiger shark	Tiger shark liver	5	1/10	
514	45	11825	The Summer Shore	Stingray	Stingray skin	5	1/10	
515	55	19910	Red Rock	Tiger shark	Tiger shark jaw	20	1/2	yes
516	52	11825	Red Rock	Frigatebird	Frigatebird feather	15	1/3	
517	52	3465	Red Rock	Eagle ray	Eagle ray fin	25	1/2	
518	75	40370	Red Rock	Great white shark	Great white shark jaw	30	1/2	
519	52	11825	Red Rock	Stingray	Stingray skin	5	1/10	
520	55	19910	Red Rock	Tiger shark	Tiger shark liver	5	1/10	
521	65	30965	Red Rock	Spined kraken	Spined kraken ink sac	5	1/10	
522	30	3465	Land's End	Bull shark	Bull shark jaw	15	1/3	
523	30	3465	Land's End	Osprey	Osprey feather	15	1/3	
524	30	3465	Land's End	Tern	Tern feather	15	1/3	
525	50	14575	Land's End	Pygmy kraken	Pygmy kraken tentacle	15	1/3	
526	40	8800	Land's End	Hammerhead shark	Hammerhead shark liver	5	1/10	
527	75	40370	Land's End	Great white shark	Great white shark liver	5	1/10	
528	40	8800	Land's End	Butterfly ray	Butterfly ray skin	5	1/10	yes
529	30	3465	Port Piscarilius	Osprey	Osprey feather	15	1/3	
530	30	3465	Port Piscarilius	Bull shark	Bull shark jaw	15	1/3	
531	30	3465	Port Piscarilius	Tern	Tern feather	15	1/3	
532	65	30965	Port Piscarilius	Spined kraken	Spined kraken tentacle	20	1/2	
533	55	19910	Port Piscarilius	Tiger shark	Tiger shark liver	5	1/10	yes
534	40	8800	Port Piscarilius	Butterfly ray	Butterfly ray skin	5	1/10	
535	75	40370	Port Piscarilius	Armoured kraken	Armoured kraken ink sac	5	1/10	
536	38	3465	Civitas illa Fortis	Osprey	Osprey feather	15	1/3	
537	40	8800	Civitas illa Fortis	Hammerhead shark	Hammerhead shark jaw	20	1/2	
538	38	3465	Civitas illa Fortis	Bull shark	Bull shark jaw	20	1/2	
539	75	40370	Civitas illa Fortis	Armoured kraken	Armoured kraken tentacle	25	1/2	
540	38	3465	Civitas illa Fortis	Tern	Tern beak	5	1/10	
541	45	11825	Civitas illa Fortis	Frigatebird	Frigatebird beak	5	1/10	yes
542	38	3465	Civitas illa Fortis	Eagle ray	Eagle ray skin	5	1/10	
543	46	11825	Aldarin	Stingray	Stingray fin	20	1/2	
544	46	3465	Aldarin	Osprey	Osprey feather	20	1/2	
545	46	8800	Aldarin	Butterfly ray	Butterfly ray fin	25	1/2	
546	75	40370	Aldarin	Great white shark	Great white shark jaw	30	1/2	
547	46	8800	Aldarin	Hammerhead shark	Hammerhead shark liver	5	1/10	
548	65	30965	Aldarin	Spined kraken	Spined kraken ink sac	5	1/10	yes
549	46	11825	Aldarin	Frigatebird	Frigatebird beak	5	1/10	
550	50	8800	Port Roberts	Hammerhead shark	Hammerhead shark jaw	15	1/3	
551	50	3465	Port Roberts	Osprey	Osprey feather	15	1/3	
552	75	40370	Port Roberts	Armoured kraken	Armoured kraken tentacle	20	1/2	
553	75	40370	Port Roberts	Great white shark	Great white shark jaw	25	1/2	yes
554	50	3465	Port Roberts	Tern	Tern beak	5	1/10	
555	80	47080	Port Roberts	Orca	Orca teeth	5	1/10	
556	50	14575	Port Roberts	Albatross	Albatross beak	5	1/10	
557	67	11825	Deepfin Point	Frigatebird	Frigatebird feather	20	1/2	
558	67	3465	Deepfin Point	Eagle ray	Eagle ray fin	25	1/2	
559	67	30965	Deepfin Point	Spined kraken	Spined kraken tentacle	25	1/2	
560	67	3465	Deepfin Point	Osprey	Osprey feather	20	1/2	
561	67	11825	Deepfin Point	Stingray	Stingray skin	5	1/10	
562	67	19910	Deepfin Point	Tiger shark	Tiger shark liver	5	1/10	
563	67	8800	Deepfin Point	Hammerhead shark	Hammerhead shark liver	5	1/10	
564	66	8800	Port Tyras	Butterfly ray	Butterfly ray fin	30	1/2	
565	66	8800	Port Tyras	Hammerhead shark	Hammerhead shark jaw	30	1/2	
566	66	3465	Port Tyras	Bull shark	Bull shark jaw	25	1/2	
567	75	40370	Port Tyras	Armoured kraken	Armoured kraken tentacle	30	1/2	
568	66	14575	Port Tyras	Pygmy kraken	Pygmy kraken ink sac	5	1/10	
569	75	40370	Port Tyras	Great white shark	Great white shark liver	5	1/10	
570	66	25135	Port Tyras	Manta ray (monster)	Manta ray skin	5	1/10	yes
571	70	8800	Prifddinas	Hammerhead shark	Hammerhead shark jaw	30	1/2	
572	70	14575	Prifddinas	Pygmy kraken	Pygmy kraken tentacle	30	1/2	
573	75	40370	Prifddinas	Armoured kraken	Armoured kraken tentacle	30	1/2	yes
574	70	3465	Prifddinas	Osprey	Osprey feather	30	1/2	
575	75	40370	Prifddinas	Great white shark	Great white shark liver	5	1/10	
576	70	25135	Prifddinas	Manta ray (monster)	Manta ray skin	5	1/10	
577	70	8800	Prifddinas	Hammerhead shark	Hammerhead shark liver	5	1/10	
578	62	14575	Rellekka	Albatross	Albatross feather	25	1/2	
579	62	8800	Rellekka	Hammerhead shark	Hammerhead shark jaw	20	1/2	
580	62	19910	Rellekka	Narwhal	Narwhal blubber	30	1/2	yes
581	75	40370	Rellekka	Armoured kraken	Armoured kraken tentacle	25	1/2	
582	80	47080	Rellekka	Orca	Orca teeth	5	1/10	
583	80	47080	Rellekka	Vampyre kraken	Vampyre kraken ink sac	5	1/10	
584	65	30965	Rellekka	Spined kraken	Spined kraken ink sac	5	1/10	
585	65	14575	Etceteria	Albatross	Albatross feather	30	1/2	
586	80	47080	Etceteria	Orca	Orca blubber	30	1/2	yes
587	80	47080	Etceteria	Vampyre kraken	Vampyre kraken tentacle	20	1/2	
588	65	19910	Etceteria	Narwhal	Narwhal blubber	30	1/2	
589	65	30965	Etceteria	Spined kraken	Spined kraken ink sac	5	1/10	
590	80	47080	Etceteria	Orca	Orca teeth	5	1/10	
591	75	40370	Etceteria	Armoured kraken	Armoured kraken ink sac	5	1/10	
592	76	40370	Lunar Isle	Armoured kraken	Armoured kraken tentacle	20	1/2	
593	80	47080	Lunar Isle	Vampyre kraken	Vampyre kraken tentacle	30	1/2	yes
594	76	19910	Lunar Isle	Narwhal	Narwhal blubber	25	1/2	
595	80	47080	Lunar Isle	Orca	Orca blubber	30	1/2	
596	76	14575	Lunar Isle	Albatross	Albatross beak	5	1/10	
597	80	47080	Lunar Isle	Vampyre kraken	Vampyre kraken ink sac	5	1/10	
598	76	19910	Lunar Isle	Narwhal	Narwhal tusk	5	1/10	
605	80	47080	Port Tyras	Veiled kraken	Veiled kraken ink sac	5	1/10	
606	80	47080	Deepfin Point	Veiled kraken	Veiled kraken tentacle	15	1/3	yes
            """;

}
