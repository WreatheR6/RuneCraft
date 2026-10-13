package com.runecraft.osrs.content.systems;

/**
 * The OSRS Combat Achievement task list (OSRS Wiki task infoboxes, synced 2026-10): one line per task,
 * "wikiId|monster|type|name|description", grouped by tier. Parsed by {@link CombatAchievements}.
 */
final class CombatAchievementData {
    private CombatAchievementData() {
    }

    static final String EASY = """
            0|Aberrant Spectre|Kill Count|Noxious Foe|Kill an Aberrant Spectre.
            24|Barrows|Kill Count|Barrows Novice|Open the Barrows chest 10 times.
            28|Barrows|Restriction|Defence? What Defence?|Kill any Barrows Brother using only magical damage.
            32|Black Dragon|Kill Count|Big, Black and Fiery|Kill a Black Dragon.
            33|Bloodveld|Kill Count|The Demonic Punching Bag|Kill a Bloodveld.
            35|Bryophyta|Kill Count|Bryophyta Novice|Open Bryophyta's chest once.
            37|Bryophyta|Mechanical|Protection from Moss|Kill Bryophyta with the Protect from Magic prayer active.
            39|Bryophyta|Perfection|Preparation Is Key|Kill Bryophyta without suffering any poison damage.
            40|Bryophyta|Restriction|A Slow Death|Kill Bryophyta with either poison or venom being the final source of damage.
            41|Bryophyta|Restriction|Fighting as Intended II|Kill Bryophyta on a free to play world.
            76|Deranged Archaeologist|Kill Count|Deranged Archaeologist Novice|Kill the Deranged Archaeologist 10 times.
            80|Fire Giant|Kill Count|The Walking Volcano|Kill a Fire Giant.
            120|Greater Demon|Kill Count|A Greater Foe|Kill a Greater Demon.
            121|Greater Demon|Restriction|Not So Great After All|Finish off a Greater Demon with a demonbane weapon.
            122|Hellhound|Kill Count|A Demon's Best Friend|Kill a Hellhound.
            129|Obor|Kill Count|Obor Novice|Open Obor's chest once.
            131|Obor|Mechanical|Sleeping Giant|Kill Obor whilst he is immobilized.
            134|Obor|Restriction|Fighting as Intended|Kill Obor on a free to play world.
            162|King Black Dragon|Kill Count|King Black Dragon Novice|Kill the King Black Dragon 10 times.
            174|Lizardman Shaman|Kill Count|A Scaley Encounter|Kill a Lizardman Shaman.
            175|Lizardman Shaman|Perfection|Shayzien Protector|Kill a Lizardman Shaman in Molch which has not dealt damage to anyone (excluding its Spawns).
            177|Giant Mole|Kill Count|Giant Mole Novice|Kill the Giant Mole 10 times.
            206|Sarachnis|Kill Count|Sarachnis Novice|Kill Sarachnis 10 times.
            279|Wintertodt|Kill Count|Wintertodt Novice|Subdue the Wintertodt 5 times.
            281|Wintertodt|Mechanical|Mummy!|Heal a pyromancer after they have fallen.
            282|Wintertodt|Mechanical|Handyman|Repair a brazier which has been destroyed by the Wintertodt.
            285|Wintertodt|Restriction|Cosy|Subdue the Wintertodt with four pieces of warm equipment equipped.
            287|Wyrm|Kill Count|A Slithery Encounter|Kill a Wyrm.
            353|Tempoross|Kill Count|Tempoross Novice|Subdue Tempoross 5 times.
            356|Tempoross|Mechanical|Master of Buckets|Extinguish at least 5 fires during a single Tempoross fight.
            357|Tempoross|Mechanical|Calm Before the Storm|Repair either a mast or a totem pole.
            360|Tempoross|Mechanical|Fire in the Hole!|Attack Tempoross from both sides by loading both cannons on both ships.
            398|Giants|Kill Count|Into the Den of Giants|Kill a Hill Giant, Moss Giant and Fire Giant in the Giant Cave within the Shayzien region.
            521|Scurrius|Kill Count|Scurrius Novice|Kill Scurrius once.
            523|Scurrius|Restriction|Sit Rat|Finish off Scurrius with a ratbane weapon in a private instance.
            591|Royal Titans|Restriction|One by one|Kill one Titan at a time, without attacking the other.
            594|Royal Titans|Restriction|Elemental Company|Kill the Royal Titans without attacking any elementals.
            597|Royal Titans|Restriction|Let them fight|Kill the Royal Titans while having the Royal Titans kill a total of 10 elementals.
            625|Shellbane gryphon|Kill Count|Shellbane Adept|Kill the Shellbane Gryphon 25 times.
            630|Shellbane gryphon|Mechanical|Dry Cleaning|Kill the Shellbane Gryphon after clearing its corrosive spit from your armour.
            632|Brutus|Kill Count|Brutus Novice|Kill Brutus.
            """;

    static final String MEDIUM = """
            25|Barrows|Kill Count|Barrows Champion|Open the Barrows chest 25 times.
            26|Barrows|Mechanical|Can't Touch Me|Kill Dharok, Verac, Torag and Guthan without letting them attack you with melee.
            27|Barrows|Perfection|Pray for Success|Kill all six Barrows Brothers and loot the Barrows chest without taking any damage from any of the brothers.
            34|Brutal Black Dragon|Kill Count|Brutal, Big, Black and Firey|Kill a Brutal Black Dragon.
            36|Bryophyta|Kill Count|Bryophyta Champion|Open Bryophyta's chest 5 times.
            38|Bryophyta|Mechanical|Quick Cutter|Kill all 3 of Bryophyta's growthlings within 3 seconds of the first one dying.
            44|Skotizo|Kill Count|Skotizo Champion|Kill Skotizo once.
            46|Skotizo|Mechanical|Demonic Weakening|Kill Skotizo with no altars active.
            48|Skotizo|Restriction|Demonbane Weaponry|Kill Skotizo with a demonbane weapon equipped.
            61|Chaos Fanatic|Kill Count|Chaos Fanatic Champion|Kill the Chaos Fanatic 10 times.
            63|Chaos Fanatic|Perfection|Sorry, What Was That?|Kill the Chaos Fanatic without anyone being hit by his explosion attack.
            70|Crazy Archaeologist|Kill Count|Crazy Archaeologist Champion|Kill the Crazy Archaeologist 10 times.
            72|Crazy Archaeologist|Mechanical|Mage of the Ruins|Kill the Crazy Archaeologist with only magical attacks.
            73|Crazy Archaeologist|Perfection|I'd Rather Not Learn|Kill the Crazy Archaeologist without anyone being hit by his "Rain of Knowledge" attack.
            77|Deranged Archaeologist|Kill Count|Deranged Archaeologist Champion|Kill the Deranged Archaeologist 25 times.
            78|Deranged Archaeologist|Mechanical|Mage of the Swamp|Kill the Deranged Archaeologist with only magical attacks.
            79|Deranged Archaeologist|Perfection|I'd Rather Be Illiterate|Kill the Deranged Archaeologist without anyone being hit by his "Learn to Read" attack.
            97|Gargoyle|Kill Count|A Smashing Time|Kill a Gargoyle.
            130|Obor|Kill Count|Obor Champion|Open Obor's chest 5 times.
            132|Obor|Mechanical|Back to the Wall|Kill Obor without being pushed back more than one square by his knockback attack.
            133|Obor|Perfection|Squashing the Giant|Kill Obor without taking any damage off prayer.
            163|King Black Dragon|Kill Count|King Black Dragon Champion|Kill the King Black Dragon 25 times.
            164|King Black Dragon|Mechanical|Claw Clipper|Kill the King Black Dragon with the Protect from Melee prayer activated.
            165|King Black Dragon|Restriction|Hide Penetration|Kill the King Black Dragon with a stab weapon.
            166|King Black Dragon|Restriction|Antifire Protection|Kill the King Black Dragon with an antifire potion active and an antidragon shield equipped.
            173|Kurask|Kill Count|Master of Broad Weaponry|Kill a Kurask.
            178|Giant Mole|Kill Count|Giant Mole Champion|Kill the Giant mole 25 times.
            182|Giant Mole|Perfection|Avoiding Those Little Arms|Kill the Giant Mole without her damaging anyone.
            197|Dagannoth Prime|Kill Count|Dagannoth Prime Champion|Kill Dagannoth Prime 10 times.
            201|Dagannoth Rex|Kill Count|Dagannoth Rex Champion|Kill Dagannoth Rex 10 times.
            204|Dagannoth Rex|Mechanical|A Frozen King|Kill Dagannoth Rex whilst he is immobilized.
            207|Sarachnis|Kill Count|Sarachnis Champion|Kill Sarachnis 25 times.
            210|Sarachnis|Restriction|Newspaper Enthusiast|Kill Sarachnis with a crush weapon.
            223|Skeletal Wyvern|Kill Count|A Frozen Foe from the Past|Kill a Skeletal Wyvern
            233|Dagannoth Supreme|Kill Count|Dagannoth Supreme Champion|Kill Dagannoth Supreme 10 times.
            280|Wintertodt|Kill Count|Wintertodt Champion|Subdue the Wintertodt 10 times.
            283|Wintertodt|Perfection|Can We Fix It?|Subdue the Wintertodt without allowing all 4 braziers to be broken at the same time.
            284|Wintertodt|Restriction|Leaving No One Behind|Subdue the Wintertodt without any of the Pyromancers falling.
            354|Tempoross|Kill Count|Tempoross Champion|Subdue Tempoross 10 times.
            359|Tempoross|Perfection|The Lone Angler|Subdue Tempoross alone without getting hit by any fires, torrents or waves.
            399|None|Mechanical|Sit Back and Relax|Deal 100 damage to creatures using undead thralls.
            522|Scurrius|Kill Count|Scurrius Champion|Kill Scurrius 10 times.
            524|Scurrius|Perfection|Perfect Scurrius|Kill Scurrius in a private instance without taking damage from the following attacks: Tail Swipe and Falling Bricks. Pray correctly against the following attacks: Flying Fur and Bolts of Electricity.
            525|Scurrius|Mechanical|Efficient Pest Control|Kill 6 Giant Rats within Scurrius' lair in 3 seconds.
            528|Moons of Peril|Kill Count|Lunar Triplet|Open the Reward Chest after defeating all three Moons.
            529|Moons of Peril|Kill Count|Perilous Novice|Open the Reward Chest 5 times.
            534|Moons of Peril|Restriction|Back to Our Roots|Defeat all three Moons in one run by only attacking with a Dragon Scimitar.
            535|Moons of Peril|Speed|Moons of Peril Speed-Trialist|Defeat all three Moons in one run in under 8 minutes.
            568|The Hueycoatl|Kill Count|Hueycoatl Champion|Kill the Hueycoatl once.
            574|The Hueycoatl|Restriction|You're a wizard|Kill the Hueycoatl using only earth spells.
            579|Amoxliatl|Kill Count|Amoxliatl Champion|Kill Amoxliatl once.
            584|Amoxliatl|Restriction|Temotli Triumph|Kill Amoxliatl using only glacial temotli as a weapon.
            589|Royal Titans|Kill Count|Royal Titan Champion|Kill the Royal Titans 10 times.
            590|Royal Titans|Kill Count|Royal Titan Adept|Kill the Royal Titans 25 times.
            598|Royal Titans|Mechanical|It takes too long|Kill both Royal Titans while they are charging up their area attack. Both titans must die during the same charging phase.
            626|Shellbane gryphon|Kill Count|Shellbane Veteran|Kill the Shellbane Gryphon 50 times.
            627|Shellbane gryphon|Perfection|Perfect Shellbane|Kill the Shellbane Gryphon without taking damage.
            628|Shellbane gryphon|Speed|Shellbane Speedrunner|Kill the Shellbane Gryphon 10 times within 12:30 minutes of entering the cave.
            633|Brutus|Kill Count|Brutus Champion|Kill Brutus 10 times.
            635|Brutus|Perfection|Beef vs Beef|Kill Brutus whilst neither equipping nor consuming anything.
            """;

    static final String HARD = """
            1|Abyssal Sire|Kill Count|Abyssal Adept|Kill the Abyssal Sire 20 times.
            3|Abyssal Sire|Mechanical|They Grow Up Too Fast|Kill the Abyssal Sire without letting any Scion mature.
            5|Abyssal Sire|Mechanical|Don't Whip Me|Kill the Abyssal Sire without being hit by any external tentacles.
            7|Abyssal Sire|Perfection|Don't Stop Moving|Kill the Abyssal Sire without taking damage from any miasma pools.
            9|Kree'arra|Kill Count|Kree'arra Adept|Kill Kree'arra 50 times.
            12|Kree'arra|Mechanical|Airborne Showdown|Finish off Kree'arra whilst all of his bodyguards are dead.
            16|General Graardor|Kill Count|General Graardor Adept|Kill General Graardor 50 times.
            18|General Graardor|Mechanical|Ourg Freezer|Kill General Graardor whilst he is immobilized.
            20|General Graardor|Mechanical|General Showdown|Finish off General Graardor whilst all of his bodyguards are dead.
            29|Barrows|Restriction|Just Like That|Kill Karil using only damage dealt by special attacks.
            30|Barrows|Restriction|Faithless Crypt Run|Kill all six Barrows Brothers and loot the Barrows chest without ever having more than 0 prayer points.
            42|Callisto|Kill Count|Callisto Adept|Kill Callisto 10 times.
            45|Skotizo|Kill Count|Skotizo Adept|Kill Skotizo 5 times.
            57|Chaos Elemental|Kill Count|Chaos Elemental Adept|Kill the Chaos Elemental 10 times.
            59|Chaos Elemental|Mechanical|Hoarder|Kill the Chaos Elemental without it unequipping any of your items.
            60|Chaos Elemental|Perfection|The Flincher|Kill the Chaos Elemental without taking any damage from its attacks.
            62|Chaos Fanatic|Kill Count|Chaos Fanatic Adept|Kill the Chaos Fanatic 25 times.
            64|Chaos Fanatic|Restriction|Praying to the Gods|Kill the Chaos Fanatic 10 times without drinking any potion which restores prayer or leaving the Wilderness.
            71|Crazy Archaeologist|Kill Count|Crazy Archaeologist Adept|Kill the Crazy Archaeologist 25 times.
            82|Grotesque Guardians|Kill Count|Grotesque Guardians Adept|Kill the Grotesque Guardians 25 times.
            84|Grotesque Guardians|Mechanical|Don't Look at the Eclipse|Kill the Grotesque Guardians without taking damage from Dusk's blinding attack.
            85|Grotesque Guardians|Mechanical|Prison Break|Kill the Grotesque Guardians without taking damage from Dusk's prison attack.
            86|Grotesque Guardians|Mechanical|Granite Footwork|Kill the Grotesque Guardians without taking damage from Dawn's rockfall attack.
            87|Grotesque Guardians|Mechanical|Heal No More|Kill the Grotesque Guardians without letting Dawn receive any healing from her orbs.
            88|Grotesque Guardians|Mechanical|Static Awareness|Kill the Grotesque Guardians without being hit by any lightning attacks.
            123|Hespori|Kill Count|Hespori Adept|Kill Hespori 5 times.
            124|Hespori|Mechanical|Hesporisn't|Finish off Hespori with a special attack.
            125|Hespori|Mechanical|Weed Whacker|Kill all of Hesporis flowers within 5 seconds.
            157|Kalphite Queen|Kill Count|Kalphite Queen Adept|Kill the Kalphite Queen 25 times.
            159|Kalphite Queen|Mechanical|Chitin Penetrator|Kill the Kalphite Queen while her defence was last lowered by you.
            167|King Black Dragon|Stamina|Who Is the King Now?|Kill The King Black Dragon 10 times in a private instance without leaving the instance.
            168|Kraken|Kill Count|Kraken Adept|Kill the Kraken 20 times.
            169|Kraken|Mechanical|Unnecessary Optimisation|Kill the Kraken after killing all four tentacles.
            170|Kraken|Stamina|Krakan't Hurt Me|Kill the Kraken 25 times in a private instance without leaving the room.
            179|Giant Mole|Mechanical|Why Are You Running?|Kill the Giant Mole without her burrowing more than 2 times.
            181|Giant Mole|Mechanical|Whack-a-Mole|Kill the Giant Mole within 10 seconds of her resurfacing.
            183|The Nightmare|Kill Count|Nightmare Adept|Kill The Nightmare once.
            198|Dagannoth Prime|Kill Count|Dagannoth Prime Adept|Kill Dagannoth Prime 25 times.
            202|Dagannoth Rex|Kill Count|Dagannoth Rex Adept|Kill Dagannoth Rex 25 times.
            208|Sarachnis|Mechanical|Ready to Pounce|Kill Sarachnis without her using her range attack twice in a row.
            209|Sarachnis|Perfection|Inspect Repellent|Kill Sarachnis without her dealing damage to anyone.
            211|Commander Zilyana|Kill Count|Commander Zilyana Adept|Kill Commander Zilyana 50 times.
            213|Commander Zilyana|Mechanical|Commander Showdown|Finish off Commander Zilyana while all of her bodyguards are dead.
            218|Scorpia|Kill Count|Scorpia Adept|Kill Scorpia 10 times.
            220|Scorpia|Perfection|I Can't Reach That|Kill Scorpia without taking any damage from her.
            221|Scorpia|Restriction|Guardians No More|Kill Scorpia without killing her guardians.
            224|Zulrah|Kill Count|Zulrah Adept|Kill Zulrah 25 times.
            234|Dagannoth Supreme|Kill Count|Dagannoth Supreme Adept|Kill Dagannoth Supreme 25 times.
            264|Venenatis|Kill Count|Venenatis Adept|Kill Venenatis 10 times.
            266|Vet'ion|Kill Count|Vet'ion Adept|Kill Vet'ion 10 times.
            286|Wintertodt|Stamina|Why Fletch?|Subdue the Wintertodt after earning 3000 or more points.
            332|K'ril Tsutsaroth|Kill Count|K'ril Tsutsaroth Adept|Kill K'ril Tsutsaroth 50 times.
            334|K'ril Tsutsaroth|Mechanical|Yarr No More|Receive kill-credit for K'ril Tsutsaroth without him using his special attack.
            335|K'ril Tsutsaroth|Mechanical|Demonic Showdown|Finish off K'ril Tsutsaroth whilst all of his bodyguards are dead.
            339|K'ril Tsutsaroth|Restriction|Demonbane Weaponry II|Finish off K'ril Tsutsaroth with a demonbane weapon.
            355|Tempoross|Restriction|Dress Like You Mean It|Subdue Tempoross while wearing any variation of the angler outfit.
            358|Tempoross|Mechanical|Why Cook?|Subdue Tempoross, getting rewarded with 10 reward permits from a single Tempoross fight.
            397|Theatre of Blood: Entry Mode|Kill Count|Theatre of Blood: SM Adept|Complete the Theatre of Blood: Entry Mode 1 time.
            461|Tombs of Amascut: Entry Mode|Kill Count|Novice Tomb Explorer|Complete the Tombs of Amascut in Entry mode (or above) once.
            462|Tombs of Amascut: Entry Mode|Kill Count|Novice Tomb Looter|Complete the Tombs of Amascut in Entry mode (or above) 25 times.
            464|Tombs of Amascut: Entry Mode|Restriction|Movin' on up|Complete a Tombs of Amascut raid at level 50 or above.
            465|Tombs of Amascut: Entry Mode|Restriction|Confident Raider|Complete a Tombs of Amascut raid at level 100 or above.
            478|Phantom Muspah|Kill Count|Phantom Muspah Adept|Kill the Phantom Muspah.
            526|Moons of Peril|Mechanical|The Clone Zone|Defeat the Eclipse moon by only attacking its clones.
            530|Moons of Peril|Kill Count|Perilous Champion|Open the Reward Chest 25 times.
            531|Moons of Peril|Perfection|Perilous Dancer|Defeat all the Moons in one run while only taking damage from regular attacks.
            532|Moons of Peril|Restriction|Fortified|Defeat a Moon without consuming any supplies.
            533|Moons of Peril|Restriction|Betrayal|Defeat a Moon using its associated weapon drop.
            536|Moons of Peril|Speed|Moons of Peril Speed-Chaser|Defeat all three Moons in one run in under 6 minutes.
            537|Moons of Peril|Stamina|Fat of the Land|Defeat 30 Moons of Peril bosses without leaving the dungeon.
            569|The Hueycoatl|Kill Count|Hueycoatl Adept|Kill the Hueycoatl 10 times.
            572|The Hueycoatl|Mechanical|Pillar Lover|Kill the Hueycoatl whilst it is vulnerable.
            575|The Hueycoatl|Restriction|I'm your son|Kill the Hueycoatl whilst wearing two pieces of Hueycoatl armour.
            580|Amoxliatl|Kill Count|Amoxliatl Adept|Kill Amoxliatl 20 times.
            581|Amoxliatl|Speed|Amoxliatl Speed-Trialist|Kill Amoxliatl in less than 1 minute.
            583|Amoxliatl|Perfection|Nagua Negation|Kill Amoxliatl without taking any damage.
            586|Amoxliatl|Stamina|Kemo Makti|Kill Amoxliatl 10 times without leaving her chamber.
            587|Amoxliatl|Mechanical|Totally Shattered|Kill Amoxliatl without any of her unstable ice shattering.
            588|Royal Titans|Perfection|Perfect Royal Titans|Kill the Royal Titans without getting hit by any avoidable damage. This includes: Melee attacks, Explosions from the ice or fire elemental, Icicle or fire spawn damage, and Ice or fire pulse attacks.
            592|Royal Titans|Speed|Royal Titan Speed-Runner|Kill the Royal Titans in less than 1:30 minutes.
            593|Royal Titans|Stamina|Titan Killer|Kill the Royal Titans 15 times without anyone leaving the instance. If a player joins the fight, the current streak will be reset to 0. If a player leaves the fight, the task will be failed and a new instance will need to be created.
            595|Royal Titans|Restriction|I need room|Kill the Royal Titans while extinguishing all fires and melting all icicles before they dissipate naturally.
            629|Shellbane gryphon|Stamina|Shellbane Survivor|Kill the Shellbane Gryphon 25 times without leaving the cave.
            631|Shellbane gryphon|Restriction|Featherweight Fighter|Kill the Shellbane Gryphon 5 times without leaving the cave, while wearing gear weighing less than 40kg.
            636|Brutus|Mechanical|Smarter than a Cow|Kill Brutus without being attacked.
            637|Maggot King|Kill Count|Maggot Squasher|Kill the Maggot King.
            """;

    static final String ELITE = """
            2|Abyssal Sire|Kill Count|Abyssal Veteran|Kill the Abyssal Sire 50 times.
            4|Abyssal Sire|Mechanical|Respiratory Runner|Kill the Abyssal Sire after only stunning him once.
            6|Abyssal Sire|Mechanical|Demonic Rebound|Use the Vengeance spell to reflect the damage from the Abyssal Sire's explosion back to him.
            8|Abyssal Sire|Perfection|Perfect Sire|Kill the Abyssal Sire without taking damage from the external tentacles, miasma pools, explosion or damage from the Abyssal Sire without praying the appropriate protection prayer.
            10|Kree'arra|Kill Count|Kree'arra Veteran|Kill Kree'arra 100 times.
            17|General Graardor|Kill Count|General Graardor Veteran|Kill General Graardor 100 times.
            19|General Graardor|Mechanical|Ourg Freezer II|Kill General Graardor without him attacking any players.
            31|Basilisk Knight|Kill Count|Reflecting on This Encounter|Kill a Basilisk Knight.
            43|Callisto|Kill Count|Callisto Veteran|Kill Callisto 20 times.
            47|Skotizo|Perfection|Demon Evasion|Kill Skotizo without taking any damage.
            49|Skotizo|Restriction|Up for the Challenge|Kill Skotizo without equipping a demonbane weapon.
            51|Cerberus|Kill Count|Cerberus Veteran|Kill Cerberus 75 times.
            54|Cerberus|Mechanical|Ghost Buster|Kill Cerberus after successfully negating 6 or more attacks from Summoned Souls.
            55|Cerberus|Perfection|Unrequired Antifire|Kill Cerberus without taking damage from any lava pools.
            56|Cerberus|Perfection|Anti-Bite Mechanics|Kill Cerberus without taking any melee damage.
            58|Chaos Elemental|Kill Count|Chaos Elemental Veteran|Kill the Chaos Elemental 25 times.
            65|Corporeal Beast|Kill Count|Corporeal Beast Veteran|Kill the Corporeal Beast 25 times.
            67|Corporeal Beast|Perfection|Hot on Your Feet|Kill the Corporeal Beast without anyone killing the dark core or taking damage from the dark core.
            68|Corporeal Beast|Restriction|Finding the Weak Spot|Finish off the Corporeal Beast with a Halberd special attack.
            69|Corporeal Beast|Restriction|Chicken Killer|Kill the Corporeal Beast solo.
            74|Demonic Gorilla|Kill Count|If Gorillas Could Fly|Kill a Demonic Gorilla.
            75|Demonic Gorilla|Restriction|Hitting Them Where It Hurts|Finish off a Demonic Gorilla with a demonbane weapon.
            81|Galvek|Speed|Galvek Speed-Trialist|Kill Galvek in less than 3 minutes.
            83|Grotesque Guardians|Kill Count|Grotesque Guardians Veteran|Kill the Grotesque Guardians 50 times.
            89|Grotesque Guardians|Mechanical|Done before Dusk|Kill the Grotesque Guardians before Dusk uses his prison attack for a second time.
            90|Grotesque Guardians|Perfection|Perfect Grotesque Guardians|Kill the Grotesque Guardians whilst completing the "Don't look at the eclipse", "Prison Break", "Granite Footwork", "Heal no more", "Static Awareness" and "Done before dusk" tasks.
            92|Grotesque Guardians|Speed|Grotesque Guardians Speed-Trialist|Kill the Grotesque Guardians in less than 2 minutes.
            95|Grotesque Guardians|Stamina|From Dusk...|Kill the Grotesque Guardians 10 times without leaving the instance.
            98|Corrupted Hunllef|Kill Count|Corrupted Gauntlet Veteran|Complete the Corrupted Gauntlet 5 times.
            101|Corrupted Hunllef|Perfection|3, 2, 1 - Mage|Kill the Corrupted Hunllef without taking damage off prayer.
            109|Crystalline Hunllef|Kill Count|Gauntlet Veteran|Complete the Gauntlet 5 times.
            111|Crystalline Hunllef|Perfection|3, 2, 1 - Range|Kill the Crystalline Hunllef without taking damage off prayer.
            114|Crystalline Hunllef|Restriction|Egniol Diet|Kill the Crystalline Hunllef without making an egniol potion within the Gauntlet.
            115|Crystalline Hunllef|Restriction|Crystalline Warrior|Kill the Crystalline Hunllef with a full set of perfected armour equipped.
            116|Crystalline Hunllef|Restriction|Wolf Puncher|Kill the Crystalline Hunllef without making more than one attuned weapon.
            119|Glough|Speed|Glough Speed-Trialist|Kill Glough in less than 2 minutes and 30 seconds.
            126|Hespori|Restriction|Plant-Based Diet|Kill Hespori without losing any prayer points.
            127|Hespori|Speed|Hespori Speed-Trialist|Kill the Hespori in less than 48 seconds.
            135|Alchemical Hydra|Kill Count|Alchemical Veteran|Kill the Alchemical Hydra 75 times.
            147|TzTok-Jad|Kill Count|Fight Caves Veteran|Complete the Fight Caves once.
            149|TzTok-Jad|Mechanical|A Near Miss!|Complete the Fight Caves after surviving a hit from TzTok-Jad without praying.
            153|TzTok-Jad|Restriction|Facing Jad Head-on|Complete the Fight Caves with only melee.
            158|Kalphite Queen|Kill Count|Kalphite Queen Veteran|Kill the Kalphite Queen 50 times.
            160|Kalphite Queen|Mechanical|Insect Deflection|Kill the Kalphite Queen by using the Vengeance spell as the finishing blow.
            161|Kalphite Queen|Restriction|Prayer Smasher|Kill the Kalphite Queen using only the Verac's Flail as a weapon.
            171|Kraken|Stamina|Ten-tacles|Kill the Kraken 50 times in a private instance without leaving the room.
            176|The Mimic|Kill Count|Mimic Veteran|Kill the Mimic once.
            180|Giant Mole|Mechanical|Hard Hitter|Kill the Giant Mole with 4 or fewer instances of damage.
            184|The Nightmare|Kill Count|Nightmare Veteran|Kill The Nightmare 25 times.
            187|The Nightmare|Mechanical|Explosion!|Kill two Husks at the same time.
            189|The Nightmare|Restriction|Sleep Tight|Kill the Nightmare solo.
            191|The Nightmare|Speed|Nightmare (Solo) Speed-Trialist|Defeat the Nightmare (Solo) in less than 23 minutes.
            194|The Nightmare|Speed|Nightmare (5-Scale) Speed-Trialist|Defeat the Nightmare (5-scale) in less than 5 minutes.
            199|Dagannoth Prime|Mechanical|Death to the Seer King|Kill Dagannoth Prime whilst under attack by Dagannoth Supreme and Dagannoth Rex.
            200|Dagannoth Prime|Mechanical|From One King to Another|Kill Prime using a Rune Thrownaxe special attack, bounced off Dagannoth Rex.
            203|Dagannoth Rex|Mechanical|Death to the Warrior King|Kill Dagannoth Rex whilst under attack by Dagannoth Supreme and Dagannoth Prime.
            205|Dagannoth Rex|Mechanical|Toppling the Diarchy|Kill Dagannoth Rex and one other Dagannoth king at the exact same time.
            212|Commander Zilyana|Kill Count|Commander Zilyana Veteran|Kill Commander Zilyana 100 times.
            216|Commander Zilyana|Restriction|Reminisce|Kill Commander Zilyana in a private instance with melee only.
            219|Scorpia|Kill Count|Scorpia Veteran|Kill Scorpia 25 times.
            222|Fragment of Seren|Speed|Fragment of Seren Speed-Trialist|Kill The Fragment of Seren in less than 4 minutes.
            225|Zulrah|Kill Count|Zulrah Veteran|Kill Zulrah 75 times.
            227|Zulrah|Mechanical|Snake Rebound|Kill Zulrah by using the Vengeance spell as the finishing blow.
            228|Zulrah|Mechanical|Snake. Snake!? Snaaaaaake!|Kill 3 Snakelings simultaneously.
            230|Zulrah|Speed|Zulrah Speed-Trialist|Kill Zulrah in less than 1 minute 20 seconds, without a slayer task.
            235|Dagannoth Supreme|Mechanical|Death to the Archer King|Kill Dagannoth Supreme whilst under attack by Dagannoth Prime and Dagannoth Rex.
            236|Dagannoth Supreme|Mechanical|Rapid Succession|Kill all three Dagannoth Kings within 9 seconds of the first one.
            237|Theatre of Blood|Kill Count|Theatre of Blood Veteran|Complete the Theatre of Blood 25 times.
            261|Thermonuclear Smoke Devil|Kill Count|Thermonuclear Veteran|Kill the Thermonuclear Smoke Devil 20 times.
            262|Thermonuclear Smoke Devil|Perfection|Hazard Prevention|Kill the Thermonuclear Smoke Devil without it hitting anyone.
            263|Thermonuclear Smoke Devil|Restriction|Spec'd Out|Kill the Thermonuclear Smoke Devil using only special attacks.
            265|Venenatis|Kill Count|Venenatis Veteran|Kill Venenatis 20 times.
            267|Vet'ion|Kill Count|Vet'eran|Kill Vet'ion 20 times.
            268|Vorkath|Kill Count|Vorkath Veteran|Kill Vorkath 50 times.
            271|Vorkath|Restriction|Zombie Destroyer|Kill Vorkath's zombified spawn without using crumble undead.
            273|Vorkath|Restriction|Stick 'em With the Pointy End|Kill Vorkath using melee weapons only.
            292|Chambers of Xeric: Challenge Mode|Speed|Dust Seeker|Complete a Chambers of Xeric Challenge mode raid in the target time.
            299|Chambers of Xeric|Kill Count|Chambers of Xeric Veteran|Complete the Chambers of Xeric 25 times.
            302|Chambers of Xeric|Mechanical|Perfectly Balanced|Kill the Vanguards without them resetting their health.
            303|Chambers of Xeric|Mechanical|Together We'll Fall|Kill the Vanguards within 10 seconds of the first one dying.
            307|Chambers of Xeric|Mechanical|Mutta-diet|Kill the Muttadile without letting her or her baby recover hitpoints from the meat tree.
            308|Chambers of Xeric|Mechanical|Redemption Enthusiast|Kill the Abyssal Portal without forcing Vespula to land.
            311|Chambers of Xeric|Perfection|Dancing with Statues|Receive kill-credit for a Stone Guardian without taking damage from falling rocks.
            312|Chambers of Xeric|Perfection|Undying Raid Team|Complete a Chambers of Xeric raid without anyone dying.
            314|Chambers of Xeric|Perfection|Shayzien Specialist|Receive kill-credit for a Lizardman Shaman without taking damage from any shamans in the room.
            316|Chambers of Xeric|Perfection|Cryo No More|Receive kill-credit for the Ice Demon without taking any damage.
            320|Chambers of Xeric|Restriction|Blizzard Dodger|Receive kill-credit for the Ice Demon without activating the Protect from Range prayer.
            321|Chambers of Xeric|Restriction|Kill It with Fire|Finish off the Ice Demon with a fire spell.
            328|Zalcano|Kill Count|Zalcano Veteran|Kill Zalcano 25 times.
            329|Zalcano|Perfection|Perfect Zalcano|Kill Zalcano 5 times in a row without leaving or getting hit by the following: Falling rocks, rock explosions, Zalcano powering up, or standing in a red symbol.
            330|Zalcano|Mechanical|Team Player|Receive imbued tephra from a golem.
            331|Zalcano|Mechanical|The Spurned Hero|Kill Zalcano as the player who has dealt the most damage to her.
            333|K'ril Tsutsaroth|Kill Count|K'ril Tsutsaroth Veteran|Kill K'ril Tsutsaroth 100 times.
            336|K'ril Tsutsaroth|Mechanical|The Bane of Demons|Defeat K'ril Tsutsaroth in a private instance using only demonbane spells.
            337|K'ril Tsutsaroth|Perfection|Demonic Defence|Kill K'ril Tsutsaroth in a private instance without taking any of his melee hits.
            341|TzKal-Zuk|Kill Count|Half-Way There|Kill a Jal-Zek within the Inferno.
            361|TzHaar-Ket-Rak's Challenges|Kill Count|The II Jad Challenge|Complete TzHaar-Ket-Rak's second challenge.
            364|TzHaar-Ket-Rak's Challenges|Speed|TzHaar-Ket-Rak's Speed-Trialist|Complete TzHaar-Ket-Rak's first challenge in less than 45 seconds.
            367|TzHaar-Ket-Rak's Challenges|Restriction|Facing Jad Head-on III|Complete TzHaar-Ket-Rak's second challenge with only melee.
            386|Theatre of Blood: Entry Mode|Mechanical|Anticoagulants|Defeat the Maiden of Sugadinti in the Theatre of Blood: Entry Mode without letting any bloodspawn live for longer than 10 seconds.
            387|Theatre of Blood: Entry Mode|Mechanical|Appropriate Tools|Defeat the Pestilent Bloat in the Theatre of Blood: Entry Mode with everyone having a salve amulet equipped.
            388|Theatre of Blood: Entry Mode|Mechanical|They Won't Expect This|In the Theatre of Blood: Entry Mode, enter the Pestilent Bloat room from the opposite side.
            389|Theatre of Blood: Entry Mode|Mechanical|Chally Time|Defeat the Pestilent Bloat in the Theatre of Blood: Entry Mode by using a halberd special attack as your final attack.
            390|Theatre of Blood: Entry Mode|Mechanical|Nylocas, On the Rocks|In the Theatre of Blood: Entry Mode, freeze any 4 Nylocas with a single Ice Barrage spell.
            391|Theatre of Blood: Entry Mode|Mechanical|Just To Be Safe|Defeat Sotetseg in the Theatre of Blood: Entry Mode after having split the big ball with your entire team. This must be done with a group size of at least 2.
            392|Theatre of Blood: Entry Mode|Mechanical|Don't Look at Me!|Kill Xarpus in the Theatre of Blood: Entry Mode without him reflecting any damage to anyone.
            393|Theatre of Blood: Entry Mode|Mechanical|No-Pillar|Survive Verzik Vitur's pillar phase in the Theatre of Blood: Entry Mode without losing a single pillar.
            394|Theatre of Blood: Entry Mode|Mechanical|Attack, Step, Wait|Survive Verzik Vitur's second phase in the Theatre of Blood: Entry Mode without anyone getting bounced by Verzik.
            395|Theatre of Blood: Entry Mode|Mechanical|Pass It On|In the Theatre of Blood: Entry Mode, successfully pass on the green ball to a team mate.
            400|Phosani's Nightmare|Kill Count|Phosani's Veteran|Kill Phosani's Nightmare once.
            410|Nex|Kill Count|Nex Veteran|Kill Nex once.
            412|Nex|Restriction|Nex Survivors|Kill Nex without anyone dying.
            425|Tombs of Amascut|Restriction|Helpful spirit who?|Complete the Tombs of Amascut without using any supplies from the Helpful Spirit and without anyone dying. Honey locusts are included in this restriction.
            430|Tombs of Amascut|Perfection|Hardcore Raiders|Complete the Tombs of Amascut in a group of two or more without anyone dying.
            431|Tombs of Amascut|Perfection|Hardcore Tombs|Complete the Tombs of Amascut solo without dying.
            432|Tombs of Amascut|Perfection|Perfect Het|Complete the Het room without taking any damage from the light beam and orbs. You must destroy the core after one exposure.
            435|Tombs of Amascut|Perfection|Perfect Apmeken|Complete the Apmeken room in a group of two or more, without anyone allowing any dangers to trigger, standing in venom or being hit by a volatile baboon. You must complete this room in less than three minutes.
            438|Tombs of Amascut|Perfection|Perfect Crondis|Complete the Crondis room without letting a crocodile get to the tree, without anyone losing water from their container and in under one minute.
            449|Tombs of Amascut|Mechanical|Dropped the ball|Defeat Akkha without dropping any materialising orbs and without dying yourself.
            451|Tombs of Amascut|Mechanical|No skipping allowed|Defeat Ba-Ba after only attacking the non-weakened boulders in the rolling boulder phase, without dying yourself. The Boulderdash invocation must be activated.
            452|Tombs of Amascut|Mechanical|I'm in a rush|Defeat Ba-Ba after destroying four or fewer rolling boulders in total without dying yourself.
            458|Tombs of Amascut|Mechanical|Down Do Specs|Defeat the Wardens after staggering the boss a maximum of twice during phase two, without dying yourself.
            463|Tombs of Amascut: Entry Mode|Kill Count|Novice Tomb Raider|Complete the Tombs of Amascut in Entry mode (or above) 50 times.
            468|Tombs of Amascut|Kill Count|Tomb Explorer|Complete the Tombs of Amascut once.
            469|Tombs of Amascut: Expert Mode|Kill Count|Expert Tomb Explorer|Complete the Tombs of Amascut (Expert mode) once.
            473|Phantom Muspah|Restriction|Can't Escape|Kill the Phantom Muspah without running.
            475|Phantom Muspah|Speed|Phantom Muspah Speed-Trialist|Kill the Phantom Muspah in less than 3 minutes without a slayer task.
            479|Phantom Muspah|Kill Count|Phantom Muspah Veteran|Kill the Phantom Muspah 25 times.
            482|Phantom Muspah|Mechanical|Versatile Drainer|Drain the Phantom Muspah's Prayer with three different sources in one kill.
            485|Vardorvis|Speed|Vardorvis Speed-Trialist|Kill Vardorvis in less than 1:15 minutes without a slayer task.
            488|Vardorvis|Kill Count|Vardorvis Adept|Kill Vardorvis once.
            494|Whisperer|Speed|Whisperer Speed-Trialist|Kill the Whisperer in less than 3:00 without a slayer task.
            497|Whisperer|Kill Count|Whisperer Adept|Kill the Whisperer once.
            501|Whisperer|Restriction|Tentacular|Kill the Whisperer whilst only being on the Arceuus spellbook.
            503|Leviathan|Speed|Leviathan Speed-Trialist|Kill the Leviathan in less than 1:50 without a slayer task.
            506|Leviathan|Kill Count|Leviathan Adept|Kill the Leviathan once.
            512|Duke Sucellus|Speed|Duke Sucellus Speed-Trialist|Kill Duke Sucellus in less than 1:00 minutes without a slayer task.
            515|Duke Sucellus|Kill Count|Duke Sucellus Adept|Kill Duke Sucellus once.
            527|Moons of Peril|Mechanical|High Hitter|Defeat a Moon before they start their second special attack.
            542|Fortis Colosseum|Perfection|Furball|Complete Wave 4 without taking avoidable damage from a Manticore.
            545|Fortis Colosseum|Mechanical|Denied|Complete Wave 7 without the Minotaur ever healing other enemies.
            548|Fortis Colosseum|Mechanical|I was here first!|Kill a Jaguar Warrior using a Claw-type weapon special attack.
            551|Tormented Demon|Kill Count|Unending Torment|Kill a Tormented Demon.
            552|Tormented Demon|Restriction|Through Fire and Flames|Kill a Tormented Demon whilst their shield is inactive.
            553|Tormented Demon|Restriction|Two Times the Torment|Kill two Tormented Demons within 2 seconds.
            555|Tormented Demon|Mechanical|Rapid Reload|Hit three Tormented Demons within 3 seconds using a ballista or a crossbow.
            556|Araxxor|Kill Count|Araxxor Veteran|Kill Araxxor 25 times.
            558|Araxxor|Speed|Araxxor Speed-Trialist|Kill Araxxor 4 times in 10:00.
            564|Araxxor|Restriction|Relaxxor|Kill Araxxor after destroying six eggs.
            570|The Hueycoatl|Kill Count|Hueycoatl Veteran|Kill the Hueycoatl 25 times.
            571|The Hueycoatl|Perfection|Perfect Hueycoatl|Kill the Hueycoatl perfectly 5 times without leaving. To get a perfect kill, you must not take any avoidable damage from the Hueycoatl's lightning attack, tail slam attack or off-prayer projectile attacks.
            576|The Hueycoatl|Speed|Hueycoatl Speed-Trialist|Kill the Hueycoatl in 2:30
            582|Amoxliatl|Speed|Amoxliatl Speed-Chaser|Kill Amoxliatl in less than 30 seconds.
            585|Amoxliatl|Restriction|Without Ralos' Light|Kill Amoxliatl without losing any prayer points.
            596|Royal Titans|Restriction|No time to pray|Kill the Royal Titans without losing any prayer points.
            599|Yama|Kill Count|Yama Adept|Defeat Yama once.
            601|Yama|Speed|Yama Speed-Trialist|Defeat Yama in an average time of under 3:38 over your last four kills.
            607|Yama|Mechanical|Back so soon?|Defeat Yama without the judge being attacked with the wrong style and without taking more than one instance of damage each time you are exiled from the arena.
            610|Doom of Mokhaiotl|Kill Count|Doom Adept|Defeat the Doom of Mokhaiotl at delve level 3.
            613|Doom of Mokhaiotl|Mechanical|Exposed Doom|Defeat the Doom of Mokhaiotl during its Melee charge phase.
            621|Doom of Mokhaiotl|Speed|Doom Crawler|Defeat the Doom of Mokhaiotl level 1 in less than 30 seconds.
            634|Brutus|Speed|Brutus Speed-Trialist|Kill Brutus in less than 2 seconds.
            638|Maggot King|Kill Count|Maggot Exterminator|Kill the Maggot King 25 times.
            641|Maggot King|Restriction|Trying to fit in|Kill the Maggot King using only weapons that can be found in Vampyrium.
            """;

    static final String MASTER = """
            11|Kree'arra|Mechanical|Collateral Damage|Kill Kree'arra in a private instance without ever attacking him directly.
            13|Kree'arra|Perfection|Swoop No More|Kill Kree'arra in a private instance without taking any melee damage from the boss or his bodyguards.
            50|Skotizo|Restriction|Precise Positioning|Kill Skotizo with the final source of damage being a Chinchompa explosion.
            52|Cerberus|Kill Count|Cerberus Master|Kill Cerberus 150 times.
            53|Cerberus|Mechanical|Arooo No More|Kill Cerberus without any of the Summoned Souls being spawned.
            66|Corporeal Beast|Kill Count|Corporeal Beast Master|Kill the Corporeal Beast 50 times.
            91|Grotesque Guardians|Perfection|Perfect Grotesque Guardians II|Kill the Grotesque Guardians 5 times in a row without leaving the instance, whilst completing the Perfect Grotesque Guardians task every time.
            93|Grotesque Guardians|Speed|Grotesque Guardians Speed-Chaser|Kill the Grotesque Guardians in less than 1:40 minutes.
            96|Grotesque Guardians|Stamina|... 'til Dawn|Kill the Grotesque Guardians 20 times without leaving the instance.
            99|Corrupted Hunllef|Kill Count|Corrupted Gauntlet Master|Complete the Corrupted Gauntlet 10 times.
            102|Corrupted Hunllef|Perfection|Perfect Corrupted Hunllef|Kill the Corrupted Hunllef without taking damage from: Tornadoes, Damaging Floor or Stomp Attacks. Also, do not take damage off prayer and do not attack the Corrupted Hunllef with the wrong weapon.
            103|Corrupted Hunllef|Restriction|Defence Doesn't Matter II|Kill the Corrupted Hunllef without making any armour within the Corrupted Gauntlet.
            105|Corrupted Hunllef|Restriction|Corrupted Warrior|Kill the Corrupted Hunllef with a full set of perfected corrupted armour equipped.
            107|Corrupted Hunllef|Speed|Corrupted Gauntlet Speed-Chaser|Complete a Corrupted Gauntlet in less than 7 minutes and 5 seconds.
            110|Crystalline Hunllef|Kill Count|Gauntlet Master|Complete the Gauntlet 20 times.
            112|Crystalline Hunllef|Perfection|Perfect Crystalline Hunllef|Kill the Crystalline Hunllef without taking damage from: Tornadoes, Damaging Floor or Stomp Attacks. Also, do not take damage off prayer and do not attack the Crystalline Hunllef with the wrong weapon.
            113|Crystalline Hunllef|Restriction|Defence Doesn't Matter|Kill the Crystalline Hunllef without making any armour within the Gauntlet.
            117|Crystalline Hunllef|Speed|Gauntlet Speed-Chaser|Complete the Gauntlet in less than 4 minutes and 45 seconds.
            128|Hespori|Speed|Hespori Speed-Chaser|Kill the Hespori in less than 36 seconds.
            136|Alchemical Hydra|Kill Count|Alchemical Master|Kill the Alchemical Hydra 150 times.
            137|Alchemical Hydra|Mechanical|Unrequired Antipoisons|Kill the Alchemical Hydra without being hit by the acid pool attack.
            138|Alchemical Hydra|Mechanical|Lightning Lure|Kill the Alchemical Hydra without being hit by the lightning attack.
            139|Alchemical Hydra|Mechanical|Don't Flame Me|Kill the Alchemical Hydra without being hit by the flame wall attack.
            140|Alchemical Hydra|Mechanical|Mixing Correctly|Kill the Alchemical Hydra without empowering it.
            141|Alchemical Hydra|Mechanical|The Flame Skipper|Kill the Alchemical Hydra without letting it spawn a flame wall attack.
            142|Alchemical Hydra|Perfection|Alcleanical Hydra|Kill the Alchemical Hydra without taking any damage.
            144|Alchemical Hydra|Speed|Alchemical Speed-Chaser|Kill the Alchemical Hydra in less than 1 minute 45 seconds.
            146|Alchemical Hydra|Stamina|Working Overtime|Kill the Alchemical Hydra 15 times without leaving the room.
            148|TzTok-Jad|Kill Count|Fight Caves Master|Complete the Fight Caves 5 times.
            150|TzTok-Jad|Mechanical|Denying the Healers|Complete the Fight caves without letting any of the Yt-MejKot heal.
            152|TzTok-Jad|Mechanical|You Didn't Say Anything About a Bat|Complete the Fight Caves without being attacked by a Tz-Kih.
            155|TzTok-Jad|Speed|Fight Caves Speed-Chaser|Complete the Fight Caves in less than 30 minutes.
            172|Kraken|Stamina|One Hundred Tentacles|Kill the Kraken 100 times in a private instance without leaving the room.
            185|The Nightmare|Kill Count|Nightmare Master|Kill The Nightmare 50 times.
            188|The Nightmare|Perfection|Perfect Nightmare|Kill the Nightmare without any player taking damage from the following attacks: Nightmare rifts, an un-cured parasite explosion, Corpse flowers or the Nightmare's Surge. Also, no player can take damage off prayer or have their attacks slowed by the Nightmare spores.
            192|The Nightmare|Speed|Nightmare (Solo) Speed-Chaser|Defeat the Nightmare (Solo) in less than 19 minutes.
            195|The Nightmare|Speed|Nightmare (5-Scale) Speed-Chaser|Defeat the Nightmare (5-scale) in less than 4 minutes.
            215|Commander Zilyana|Restriction|Moving Collateral|Kill Commander Zilyana in a private instance without attacking her directly.
            226|Zulrah|Kill Count|Zulrah Master|Kill Zulrah 150 times.
            229|Zulrah|Perfection|Perfect Zulrah|Kill Zulrah whilst taking no damage from the following: Snakelings, Venom Clouds, Zulrah's Green or Crimson phase.
            231|Zulrah|Speed|Zulrah Speed-Chaser|Kill Zulrah in less than 1 minute, without a slayer task.
            238|Theatre of Blood|Kill Count|Theatre of Blood Master|Complete the Theatre of Blood 75 times.
            240|Theatre of Blood|Mechanical|Two-Down|Kill the Pestilent Bloat before he shuts down for the third time.
            241|Theatre of Blood|Mechanical|Pop It|Kill Verzik without any Nylocas being frozen and without anyone taking damage from the Nylocas.
            242|Theatre of Blood|Mechanical|A Timely Snack|Kill Sotetseg after surviving at least 3 ball attacks without sharing the damage and without anyone dying throughout the fight.
            244|Theatre of Blood|Perfection|Perfect Maiden|Kill The Maiden of Sugadinti without anyone in the team taking damage from the following sources: Blood Spawn projectiles and Blood Spawn trails. Also, without taking damage off prayer and without letting any of the Nylocas Matomenos heal The Maiden.
            245|Theatre of Blood|Perfection|Perfect Bloat|Kill the Pestilent Bloat without anyone in the team taking damage from the following sources: Pestilent flies, Falling body parts or The Pestilent Bloats stomp attack.
            246|Theatre of Blood|Perfection|Perfect Nylocas|Kill the Nylocas Vasilias without anyone in the team attacking any Nylocas with the wrong attack style, without letting a pillar collapse and without getting hit by any of the Nylocas Vasilias attacks whilst off prayer.
            247|Theatre of Blood|Perfection|Perfect Sotetseg|Kill Sotetseg without anyone in the team stepping on the wrong tile in the maze, without getting hit by the tornado and without taking any damage from Sotetseg's attacks whilst off prayer.
            248|Theatre of Blood|Perfection|Perfect Xarpus|Kill Xarpus without anyone in the team taking any damage from Xarpus' attacks and without letting an exhumed heal Xarpus more than twice.
            249|Theatre of Blood|Perfection|Perfect Verzik|Defeat Verzik Vitur without anyone in the team taking damage from Verzik Vitur's attacks other than her spider form's correctly prayed against regular magical and ranged attacks.
            250|Theatre of Blood|Restriction|Can't Drain This|Kill The Maiden of Sugadinti without anyone in the team losing any prayer points.
            251|Theatre of Blood|Restriction|Can You Dance?|Kill Xarpus without anyone in the team using a ranged or magic weapon.
            253|Theatre of Blood|Restriction|Back in My Day...|Complete the Theatre of Blood without any member of the team equipping a Scythe of Vitur.
            255|Theatre of Blood|Speed|Theatre (Trio) Speed-Chaser|Complete the Theatre of Blood (Trio) in less than 20 minutes.
            257|Theatre of Blood|Speed|Theatre (4-Scale) Speed-Chaser|Complete the Theatre of Blood (4-scale) in less than 17 minutes.
            259|Theatre of Blood|Speed|Theatre (5-Scale) Speed-Chaser|Complete the Theatre of Blood (5-scale) in less than 16 minutes.
            269|Vorkath|Kill Count|Vorkath Master|Kill Vorkath 100 times.
            270|Vorkath|Mechanical|The Walk|Hit Vorkath 12 times during the acid special without getting hit by his rapid fire or the acid pools.
            272|Vorkath|Perfection|Dodging the Dragon|Kill Vorkath 5 times without taking any damage from his special attacks and without leaving his area.
            276|Vorkath|Speed|Vorkath Speed-Chaser|Kill Vorkath in less than 1 minute and 15 seconds.
            278|Vorkath|Stamina|Extended Encounter|Kill Vorkath 10 times without leaving his area.
            288|Chambers of Xeric: Challenge Mode|Kill Count|Chambers of Xeric: CM Master|Complete the Chambers of Xeric: Challenge Mode 10 times.
            290|Chambers of Xeric: Challenge Mode|Perfection|Immortal Raid Team|Complete a Chambers of Xeric: Challenge mode raid without anyone dying.
            291|Chambers of Xeric: Challenge Mode|Perfection|Immortal Raider|Complete a Chambers of Xeric Challenge mode (Solo) raid without dying.
            293|Chambers of Xeric: Challenge Mode|Speed|Chambers of Xeric: CM (Solo) Speed-Chaser|Complete a Chambers of Xeric: Challenge Mode (Solo) in less than 45 minutes.
            295|Chambers of Xeric: Challenge Mode|Speed|Chambers of Xeric: CM (5-Scale) Speed-Chaser|Complete a Chambers of Xeric: Challenge Mode (5-scale) in less than 30 minutes.
            297|Chambers of Xeric: Challenge Mode|Speed|Chambers of Xeric: CM (Trio) Speed-Chaser|Complete a Chambers of Xeric: Challenge Mode (Trio) in less than 35 minutes.
            300|Chambers of Xeric|Kill Count|Chambers of Xeric Master|Complete the Chambers of Xeric 75 times.
            304|Chambers of Xeric|Mechanical|No Time for Death|Clear the Tightrope room without Killing any Deathly Mages or Deathly Rangers.
            305|Chambers of Xeric|Mechanical|Putting It Olm on the Line|Complete a Chambers of Xeric solo raid with more than 40,000 points.
            306|Chambers of Xeric|Mechanical|A Not So Special Lizard|Kill the Great Olm in a solo raid without letting him use any of the following special attacks in his second to last phase: Crystal Burst, Lightning Walls, Teleportation Portals or left-hand autohealing.
            309|Chambers of Xeric|Mechanical|Stop Drop and Roll|Kill Vasa Nistirio before he performs his teleport attack for the second time.
            310|Chambers of Xeric|Mechanical|Anvil No More|Kill Tekton before he returns to his anvil for a second time after the fight begins.
            313|Chambers of Xeric|Perfection|Undying Raider|Complete a Chambers of Xeric solo raid without dying.
            315|Chambers of Xeric|Perfection|Playing with Lasers|Clear the Crystal Crabs room without wasting an orb after the first crystal has been activated.
            317|Chambers of Xeric|Perfection|Perfect Olm (Solo)|Kill the Great Olm in a solo raid without taking damage from any of the following: Teleport portals, Fire Walls, Healing pools, Crystal Bombs, Crystal Burst or Prayer Orbs. You also cannot let his claws regenerate or take damage from the same acid pool back to back.
            318|Chambers of Xeric|Perfection|Perfect Olm (Trio)|Kill the Great Olm in a trio raid without any team member taking damage from any of the following: Teleport portals, Fire Walls, Healing pools, Crystal Bombs, Crystal Burst or Prayer Orbs. You also cannot let his claws regenerate or take damage from the same acid pool back to back.
            319|Chambers of Xeric|Perfection|Blind Spot|Kill Tekton without taking any damage.
            322|Chambers of Xeric|Speed|Chambers of Xeric (Solo) Speed-Chaser|Complete a Chambers of Xeric (Solo) in less than 21 minutes.
            324|Chambers of Xeric|Speed|Chambers of Xeric (5-Scale) Speed-Chaser|Complete a Chambers of Xeric (5-scale) in less than 15 minutes.
            326|Chambers of Xeric|Speed|Chambers of Xeric (Trio) Speed-Chaser|Complete a Chambers of Xeric (Trio) in less than 16 minutes and 30 seconds.
            346|TzKal-Zuk|Perfection|Nibblers, Begone!|Kill Tzkal-Zuk without letting a pillar fall before wave 67.
            362|TzHaar-Ket-Rak's Challenges|Kill Count|The IV Jad Challenge|Complete TzHaar-Ket-Rak's fourth challenge.
            365|TzHaar-Ket-Rak's Challenges|Speed|TzHaar-Ket-Rak's Speed-Chaser|Complete TzHaar-Ket-Rak's third challenge in less than 3 minutes.
            368|TzHaar-Ket-Rak's Challenges|Restriction|Facing Jad Head-on IV|Complete TzHaar-Ket-Rak's fourth challenge with only melee.
            369|TzHaar-Ket-Rak's Challenges|Perfection|Supplies? Who Needs 'em?|Complete TzHaar-Ket-Rak's third challenge without having anything in your inventory.
            371|TzHaar-Ket-Rak's Challenges|Mechanical|Multi-Style Specialist|Complete TzHaar-Ket-Rak's third challenge while using a different attack style for each JalTok-Jad.
            381|Theatre of Blood: Hard Mode|Speed|Hard Mode? Completed It|Complete the Theatre of Blood: Hard Mode within the challenge time.
            396|Theatre of Blood: Entry Mode|Speed|Theatre of Blood: SM Speed-Chaser|Complete the Theatre of Blood: Entry Mode in less than 17 minutes.
            401|Phosani's Nightmare|Kill Count|Phosani's Master|Kill Phosani's Nightmare 5 times.
            404|Phosani's Nightmare|Speed|Phosani's Speedchaser|Kill Phosani's Nightmare within 7 minutes and 30 seconds.
            406|Phosani's Nightmare|Mechanical|Crush Hour|Kill Phosani's Nightmare while killing every parasite and husk in one hit.
            407|Phosani's Nightmare|Mechanical|I Would Simply React|Kill Phosani's Nightmare without allowing your prayer to be disabled.
            408|Phosani's Nightmare|Mechanical|Dreamland Express|Kill Phosani's Nightmare without a sleepwalker reaching her during her desperation phase.
            411|Nex|Kill Count|Nex Master|Kill Nex 25 times.
            413|Nex|Mechanical|A siphon will solve this|Kill Nex without letting her heal from her Blood Siphon special attack.
            414|Nex|Mechanical|Contain this!|Kill Nex without anyone taking damage from any Ice special attack.
            415|Nex|Mechanical|There is no escape!|Kill Nex without anyone being hit by the Smoke Dash special attack.
            416|Nex|Restriction|Nex Trio|Kill Nex with three or less players at the start of the fight.
            420|Nex|Mechanical|Shadows Move...|Kill Nex without anyone being hit by the Shadow Smash attack.
            421|Tombs of Amascut|Speed|Tombs Speed Runner|Complete the Tombs of Amascut (normal) within 18 mins at any group size.
            424|Tombs of Amascut|Restriction|You are not prepared|Complete a full Tombs of Amascut raid only using supplies given inside the tomb and without anyone dying.
            426|Tombs of Amascut: Expert Mode|Restriction|Resourceful Raider|Complete the Tombs of Amascut with the "On a diet" and "Dehydration" invocations activated and without anyone dying.
            427|Tombs of Amascut: Expert Mode|Restriction|But... Damage|Complete the Tombs of Amascut without anyone in your party wearing or holding any equipment at tier 75 or above.
            428|Tombs of Amascut|Mechanical|Chompington|Defeat Zebak using only melee attacks and without dying yourself.
            429|Tombs of Amascut: Expert Mode|Restriction|Fancy feet|Complete phase three of The Wardens in a group of two or more, using only melee attacks and without dying yourself. The 'Insanity' invocation must be activated.
            433|Tombs of Amascut|Perfection|Perfect Akkha|Complete Akkha in a group of two or more, without anyone taking any damage from the following: Akkha's attacks off-prayer, Akkha's special attacks (orbs, memory, detonate), exploding shadow timers, orbs in the enrage phase or attacking Akkha with the wrong style. You must have all Akkha invocations activated.
            436|Tombs of Amascut|Perfection|Perfect Ba-Ba|Defeat Ba-Ba in a group of two or more, without anyone taking any damage from the following: Ba-Ba's Attacks off-prayer, Ba-Ba's slam, rolling boulders, rubble attack or falling rocks. No sarcophagi may be opened. You must have all Ba-Ba invocations activated.
            439|Tombs of Amascut|Perfection|Perfect Zebak|Defeat Zebak without anyone taking any damage from: poison, Zebak's basic attacks off-prayer, blood spawns and waves. You also must not push more than two jugs on the roar attack during the fight (you may destroy stationary ones). You must have all Zebak invocations activated.
            441|Tombs of Amascut|Perfection|Perfect Scabaras|Complete the Scabaras room in less than a minute without anyone taking any damage from puzzles.
            442|Tombs of Amascut|Perfection|Perfect Kephri|Defeat Kephri in a group of two or more, without anyone taking any damage from the following: egg explosions, Kephri's attacks, Exploding Scarabs, Bodyguards, dung attacks. No eggs may hatch throughout the fight.
            444|Tombs of Amascut|Perfection|Perfect Wardens|Defeat The Wardens in a group of two or more, without anyone taking avoidable damage from the following: Warden attacks, obelisk attacks, lightning attacks in phase three, skull attack in phase three, Demi god attacks in phase three. You must have all Wardens invocations activated.
            447|Tombs of Amascut: Expert Mode|Mechanical|Something of an expert myself|Complete the Tombs of Amascut raid at level 350 or above without anyone dying.
            453|Tombs of Amascut: Expert Mode|Mechanical|Ba-Bananza|Defeat Ba-Ba with all Ba-Ba invocations activated and the path levelled up to at least four, without dying yourself.
            455|Tombs of Amascut: Expert Mode|Mechanical|Rockin' around the croc|Defeat Zebak with all Zebak invocations activated and the path levelled up to at least four, without dying yourself.
            456|Tombs of Amascut: Expert Mode|Mechanical|Doesn't bug me|Defeat Kephri with all Kephri invocations activated and the path levelled up to at least four, without dying yourself.
            457|Tombs of Amascut: Expert Mode|Mechanical|All out of medics|Defeat Kephri without letting her heal above 25% after the first down. The 'Medic' invocation must be activated. You must do this without dying yourself.
            459|Tombs of Amascut|Mechanical|Better get movin'|Defeat Elidinis' Warden in phase three of the Wardens fight with 'Aerial Assault', 'Stay vigilant' and 'Insanity' invocations activated and without dying yourself.
            460|Tombs of Amascut: Expert Mode|Mechanical|Warden't you believe it|Defeat the Wardens with all Wardens invocations activated, at expert level and without dying yourself.
            466|Tombs of Amascut|Kill Count|Tomb Looter|Complete the Tombs of Amascut 25 times.
            467|Tombs of Amascut|Kill Count|Tomb Raider|Complete the Tombs of Amascut 50 times.
            470|Tombs of Amascut: Expert Mode|Kill Count|Expert Tomb Looter|Complete the Tombs of Amascut (Expert mode) 25 times.
            472|Phantom Muspah|Restriction|More than just a ranged weapon|Kill the Phantom Muspah by only dealing damage to it with a salamander.
            474|Phantom Muspah|Stamina|Essence Farmer|Kill the Phantom Muspah 10 times in one trip.
            476|Phantom Muspah|Speed|Phantom Muspah Speed-Chaser|Kill the Phantom Muspah in less than 2 minutes without a slayer task.
            480|Phantom Muspah|Kill Count|Phantom Muspah Master|Kill the Phantom Muspah 50 times.
            481|Phantom Muspah|Mechanical|Space is Tight|Kill the Phantom Muspah whilst it is surrounded by spikes.
            483|Phantom Muspah|Perfection|Walk Straight Pray True|Kill the Phantom Muspah without taking any avoidable damage.
            486|Vardorvis|Speed|Vardorvis Speed-Chaser|Kill Vardorvis in less than 1:05 without a slayer task.
            489|Vardorvis|Kill Count|Vardorvis Master|Kill Vardorvis 50 times.
            491|Vardorvis|Perfection|Perfect Vardorvis|Kill Vardorvis perfectly 5 times without leaving.
            493|Vardorvis|Restriction|Budget Cutter|Kill Vardorvis with gear worth 2m or less in total.
            495|Whisperer|Speed|Whisperer Speed-Chaser|Kill the Whisperer in less than 2:25 without a slayer task.
            498|Whisperer|Kill Count|Whisperer Master|Kill the Whisperer 50 times.
            500|Whisperer|Perfection|Perfect Whisperer|Kill the Whisperer without taking avoidable damage 5 times without leaving.
            504|Leviathan|Speed|Leviathan Speed-Chaser|Kill the Leviathan in less than 1:25 without a slayer task.
            507|Leviathan|Kill Count|Leviathan Master|Kill the Leviathan 50 times.
            509|Leviathan|Perfection|Perfect Leviathan|Kill the Leviathan perfectly 5 times without leaving.
            510|Leviathan|Mechanical|Serpentine Solo|Kill the Leviathan without stunning the boss more than once.
            513|Duke Sucellus|Speed|Duke Sucellus Speed-Chaser|Kill Duke Sucellus in less than 50 seconds without a slayer task.
            516|Duke Sucellus|Kill Count|Duke Sucellus Master|Kill Duke Sucellus 50 times.
            518|Duke Sucellus|Perfection|Perfect Duke Sucellus|Kill Duke Sucellus without taking any avoidable damage 5 times without leaving.
            519|Duke Sucellus|Restriction|Cold Feet|Kill Duke Sucellus without taking any avoidable damage, whilst also never running.
            538|Fortis Colosseum|Kill Count|Sportsmanship|Defeat Sol Heredit once.
            540|Fortis Colosseum|Restriction|I Brought Mine Too|Defeat Sol Heredit using only a Spear, Hasta or Halberd.
            544|Fortis Colosseum|Mechanical|Showboating|Defeat Sol Heredit after using Fortis Salute to the north, east, south and west of the arena while he is below 10% hitpoints.
            546|Fortis Colosseum|Mechanical|One-off|Complete Wave 11 with either 'Red Flag', 'Dynamic Duo', or 'Doom II' active.
            549|Fortis Colosseum|Speed|Colosseum Speed-Chaser|Complete the Colosseum with a total time of 28:00 or less.
            554|Tormented Demon|Restriction|Three Times the Thrashing|Kill three Tormented Demons within 3 seconds.
            557|Araxxor|Kill Count|Araxxor Master|Kill Araxxor 75 times.
            559|Araxxor|Speed|Araxxor Speed-Chaser|Kill Araxxor 5 times in 10:00.
            561|Araxxor|Perfection|Perfect Araxxor|Kill Araxxor perfectly, without taking damage from Araxxor's Mage & Range attacks, melee attack off prayer, araxyte minions damage, or damage from acid pools.
            563|Araxxor|Restriction|Let it seep in|Kill Araxxor without ever having venom or poison immunity.
            566|Araxxor|Mechanical|Araxyte Betrayal|Have an Araxyte kill three other Araxytes.
            567|Araxxor|Stamina|Arachnid Lover|Kill Araxxor 10 times without leaving.
            573|The Hueycoatl|Restriction|Is it a bird?|Kill the Hueycoatl using only dragonbane weaponry.
            577|The Hueycoatl|Speed|Hueycoatl Speed-Chaser|Kill the Hueycoatl in 2:30 with five or fewer players.
            600|Yama|Kill Count|Yama Veteran|Defeat Yama 50 times.
            602|Yama|Speed|Yama Speed-Chaser|Defeat Yama in an average time of under 3:00 over your last four kills.
            604|Yama|Restriction|No toppings, no drinks, thanks|Defeat Yama without anybody healing using anything other than plain pizza and without anybody drinking any potions.
            605|Yama|Mechanical|Shadow dancer|Defeat Yama without getting hit by shadows and without being more than one tile away from the center of his shadow crash attack.
            606|Yama|Mechanical|Fire fighter|Defeat Yama without getting hit by fire and having only killed void flares with special attacks.
            611|Doom of Mokhaiotl|Kill Count|Doom Veteran|Defeat the Doom of Mokhaiotl at delve level 8.
            614|Doom of Mokhaiotl|Mechanical|Mokhaiotl Drift|Defeat the Doom of Mokhaiotl at level 8 or above before he finishes the first burrow phase.
            615|Doom of Mokhaiotl|Mechanical|Grub Patrol|Defeat the Doom of Mokhaiotl levels 1-8 without ever letting a grub be absorbed.
            620|Doom of Mokhaiotl|Restriction|Mine's Better|Defeat the Doom of Mokhaiotl levels 1-8 whilst always wearing a shield.
            622|Doom of Mokhaiotl|Speed|Doom Chaser|Defeat the Doom of Mokhaiotl levels 1-8 in less than 10:00.
            639|Maggot King|Stamina|Camping the King|Kill the Maggot King 12 times without leaving the arena.
            640|Maggot King|Speed|Maggot King Speed Chaser|Kill the Maggot King 5 times in 9 minutes.
            642|Maggot King|Mechanical|King-sized clobbering|Kill the Maggot King without missing a single melee punish.
            643|Maggot King|Restriction|Digging in|Kill the Maggot King without moving more than 40 tiles.
            644|Maggot King|Restriction|Cordoned Off|Kill the Maggot King without moving beyond the back row of tiles.
            645|Maggot King|Perfection|Perfect Maggot King|Kill the Maggot King without taking any avoidable damage or him ever being healed.
            """;

    static final String GRANDMASTER = """
            14|Kree'arra|Restriction|The Worst Ranged Weapon|Kill Kree'arra by only dealing damage to him with a salamander.
            15|Kree'arra|Stamina|Feather Hunter|Kill Kree'arra 30 times in a private instance without leaving the room.
            21|General Graardor|Perfection|Defence Matters|Kill General Graardor 2 times consecutively in a private instance without taking any damage from his bodyguards.
            22|General Graardor|Perfection|Keep Away|Kill General Graardor in a private instance without taking any damage from the boss or bodyguards.
            23|General Graardor|Stamina|Ourg Killer|Kill General Graardor 15 times in a private instance without leaving the room.
            94|Grotesque Guardians|Speed|Grotesque Guardians Speed-Runner|Kill the Grotesque Guardians in less than 1:20 minutes.
            100|Corrupted Hunllef|Kill Count|Corrupted Gauntlet Grandmaster|Complete the Corrupted Gauntlet 50 times.
            104|Corrupted Hunllef|Restriction|Egniol Diet II|Kill the Corrupted Hunllef without making an egniol potion within the Corrupted Gauntlet.
            106|Corrupted Hunllef|Restriction|Wolf Puncher II|Kill the Corrupted Hunllef without making more than one attuned weapon.
            108|Corrupted Hunllef|Speed|Corrupted Gauntlet Speed-Runner|Complete a Corrupted Gauntlet in less than 6 minutes and 5 seconds.
            118|Crystalline Hunllef|Speed|Gauntlet Speed-Runner|Complete the Gauntlet in less than 3 minutes and 45 seconds.
            143|Alchemical Hydra|Restriction|No Pressure|Kill the Alchemical Hydra using only Dharok's Greataxe as a weapon whilst having no more than 10 Hitpoints throughout the entire fight.
            145|Alchemical Hydra|Speed|Alchemical Speed-Runner|Kill the Alchemical Hydra in less than 1 minute 20 seconds.
            151|TzTok-Jad|Mechanical|Denying the Healers II|Complete the Fight Caves without TzTok-Jad being healed by a Yt-HurKot.
            154|TzTok-Jad|Restriction|No Time for a Drink|Complete the Fight Caves without losing any prayer points.
            156|TzTok-Jad|Speed|Fight Caves Speed-Runner|Complete the Fight Caves in less than 26 minutes and 30 seconds.
            186|The Nightmare|Mechanical|Terrible Parent|Kill the Nightmare solo without the Parasites healing the boss for more than 100 health.
            190|The Nightmare|Restriction|A Long Trip|Kill the Nightmare without any player losing any prayer points.
            193|The Nightmare|Speed|Nightmare (Solo) Speed-Runner|Defeat the Nightmare (Solo) in less than 16 minutes.
            196|The Nightmare|Speed|Nightmare (5-Scale) Speed-Runner|Defeat the Nightmare (5-scale) in less than 3:30 minutes.
            214|Commander Zilyana|Perfection|Animal Whisperer|Kill Commander Zilyana in a private instance without taking any damage from the boss or bodyguards.
            217|Commander Zilyana|Stamina|Peach Conjurer|Kill Commander Zilyana 50 times in a privately rented instance without leaving the room.
            232|Zulrah|Speed|Zulrah Speed-Runner|Kill Zulrah in less than 54 seconds, without a slayer task.
            239|Theatre of Blood|Kill Count|Theatre of Blood Grandmaster|Complete the Theatre of Blood 150 times.
            243|Theatre of Blood|Perfection|Perfect Theatre|Complete the Theatre of Blood without anyone dying through any means and whilst everyone in the team completes the following Combat Achievement tasks in a single run: "Perfect Maiden", "Perfect Bloat", "Perfect Nylocas", "Perfect Sotetseg", "Perfect Xarpus" and "Perfect Verzik".
            252|Theatre of Blood|Restriction|Morytania Only|Complete the Theatre of Blood without any member of the team equipping a non-barrows weapon (except Dawnbringer).
            254|Theatre of Blood|Speed|Theatre (Duo) Speed-Runner|Complete the Theatre of Blood (Duo) in less than 26 minutes.
            256|Theatre of Blood|Speed|Theatre (Trio) Speed-Runner|Complete the Theatre of Blood (Trio) in less than 17 minutes and 30 seconds.
            258|Theatre of Blood|Speed|Theatre (4-Scale) Speed-Runner|Complete the Theatre of Blood (4-scale) in less than 15 minutes.
            260|Theatre of Blood|Speed|Theatre (5-Scale) Speed-Runner|Complete the Theatre of Blood (5-scale) in less than 14 minutes and 15 seconds.
            274|Vorkath|Restriction|Faithless Encounter|Kill Vorkath without losing any prayer points.
            275|Vorkath|Restriction|The Fremennik Way|Kill Vorkath with only your fists.
            277|Vorkath|Speed|Vorkath Speed-Runner|Kill Vorkath in less than 54 seconds.
            289|Chambers of Xeric: Challenge Mode|Kill Count|Chambers of Xeric: CM Grandmaster|Complete the Chambers of Xeric: Challenge Mode 25 times.
            294|Chambers of Xeric: Challenge Mode|Speed|Chambers of Xeric: CM (Solo) Speed-Runner|Complete a Chambers of Xeric: Challenge Mode (Solo) in less than 38 minutes and 30 seconds.
            296|Chambers of Xeric: Challenge Mode|Speed|Chambers of Xeric: CM (5-Scale) Speed-Runner|Complete a Chambers of Xeric: Challenge Mode (5-scale) in less than 25 minutes.
            298|Chambers of Xeric: Challenge Mode|Speed|Chambers of Xeric: CM (Trio) Speed-Runner|Complete a Chambers of Xeric: Challenge Mode (Trio) in less than 27 minutes.
            301|Chambers of Xeric|Kill Count|Chambers of Xeric Grandmaster|Complete the Chambers of Xeric 150 times.
            323|Chambers of Xeric|Speed|Chambers of Xeric (Solo) Speed-Runner|Complete a Chambers of Xeric (Solo) in less than 17 minutes.
            325|Chambers of Xeric|Speed|Chambers of Xeric (5-Scale) Speed-Runner|Complete a Chambers of Xeric (5-scale) in less than 12 minutes and 30 seconds.
            327|Chambers of Xeric|Speed|Chambers of Xeric (Trio) Speed-Runner|Complete a Chambers of Xeric (Trio) in less than 14 minutes and 30 seconds.
            338|K'ril Tsutsaroth|Perfection|Demon Whisperer|Kill K'ril Tsutsaroth in a private instance without ever being hit by his bodyguards.
            340|K'ril Tsutsaroth|Stamina|Ash Collector|Kill K'ril Tsutsaroth 20 times in a private instance without leaving the room.
            342|TzKal-Zuk|Kill Count|Inferno Grandmaster|Complete the Inferno 5 times.
            343|TzKal-Zuk|Mechanical|The Floor Is Lava|Kill Tzkal-Zuk without letting Jal-ImKot dig during any wave in the Inferno.
            344|TzKal-Zuk|Mechanical|Playing with Jads|Complete wave 68 of the Inferno within 30 seconds of the first JalTok-Jad dying.
            345|TzKal-Zuk|Perfection|No Luck Required|Kill Tzkal-Zuk without being attacked by TzKal-Zuk and without taking damage from a JalTok-Jad.
            347|TzKal-Zuk|Restriction|Wasn't Even Close|Kill Tzkal-Zuk without letting your hitpoints fall below 50 during any wave in the Inferno.
            348|TzKal-Zuk|Restriction|Budget Setup|Kill Tzkal-Zuk without equipping a Twisted Bow within the Inferno.
            349|TzKal-Zuk|Restriction|Nibbler Chaser|Kill Tzkal-Zuk without using any magic spells during any wave in the Inferno.
            350|TzKal-Zuk|Restriction|Facing Jad Head-on II|Kill Tzkal-Zuk without equipping any range or mage weapons before wave 69.
            351|TzKal-Zuk|Restriction|Jad? What Are You Doing Here?|Kill Tzkal-Zuk without killing the JalTok-Jad which spawns during wave 69.
            352|TzKal-Zuk|Speed|Inferno Speed-Runner|Complete the Inferno in less than 65 minutes.
            363|TzHaar-Ket-Rak's Challenges|Kill Count|The VI Jad Challenge|Complete TzHaar-Ket-Rak's sixth challenge.
            366|TzHaar-Ket-Rak's Challenges|Speed|TzHaar-Ket-Rak's Speed-Runner|Complete TzHaar-Ket-Rak's fifth challenge in less than 5 minutes.
            370|TzHaar-Ket-Rak's Challenges|Perfection|It Wasn't a Fluke|Complete TzHaar-Ket-Rak's fifth and sixth challenges back to back without failing.
            372|Theatre of Blood: Hard Mode|Mechanical|Stop Right There!|Defeat the Maiden of Sugadinti in the Theatre of Blood: Hard Mode without letting blood spawns create more than 15 blood trails.
            373|Theatre of Blood: Hard Mode|Mechanical|Personal Space|Defeat the Pestilent Bloat in the Theatre of Blood: Hard Mode with at least 3 people in the room, without anyone in your team standing on top of each other.
            374|Theatre of Blood: Hard Mode|Mechanical|Royal Affairs|In the Theatre of Blood: Hard Mode, complete the Nylocas room without ever letting the Nylocas Prinkipas change styles.
            375|Theatre of Blood: Hard Mode|Perfection|Harder Mode I|Defeat Sotetseg in the Theatre of Blood: Hard Mode without anyone sharing the ball with anyone, without anyone dying, and without anyone taking damage from any of its other attacks or stepping on the wrong tile in the maze.
            376|Theatre of Blood: Hard Mode|Perfection|Harder Mode II|Defeat Xarpus in the Theatre of Blood: Hard Mode after letting the exhumeds heal him to full health and without anyone in the team taking any damage.
            377|Theatre of Blood: Hard Mode|Mechanical|Nylo Sniper|Defeat Verzik Vitur's in the Theatre of Blood: Hard Mode without anyone in your team causing a Nylocas to explode by getting too close.
            378|Theatre of Blood: Hard Mode|Mechanical|Team Work Makes the Dream Work|When Verzik Vitur in the Theatre of Blood: Hard Mode uses her yellow power blast attack while the tornadoes are active, have everyone get through the attack without taking damage. This cannot be completed with one player alive
            379|Theatre of Blood: Hard Mode|Restriction|Harder Mode III|Defeat Verzik Vitur in the Theatre of Blood: Hard Mode without anyone attacking her with a melee weapon during her third phase.
            380|Theatre of Blood: Hard Mode|Restriction|Pack Like a Yak|Complete the Theatre of Blood: Hard Mode within the challenge time, with no deaths and without anyone buying anything from a supply chest.
            382|Theatre of Blood: Hard Mode|Speed|Theatre: HM (Trio) Speed-Runner|Complete the Theatre of Blood: Hard Mode (Trio) with an overall time of less than 23 minutes.
            383|Theatre of Blood: Hard Mode|Speed|Theatre: HM (4-Scale) Speed-Runner|Complete the Theatre of Blood: Hard Mode (4-scale) with an overall time of less than 21 minutes.
            384|Theatre of Blood: Hard Mode|Speed|Theatre: HM (5-Scale) Speed-Runner|Complete the Theatre of Blood: Hard Mode (5-scale) with an overall time of less than 19 minutes.
            385|Theatre of Blood: Hard Mode|Kill Count|Theatre of Blood: HM Grandmaster|Complete the Theatre of Blood: Hard Mode 50 times.
            402|Phosani's Nightmare|Kill Count|Phosani's Grandmaster|Kill Phosani's Nightmare 25 times.
            403|Phosani's Nightmare|Perfection|Perfect Phosani's Nightmare|Kill Phosani's Nightmare while only taking damage from husks, power blasts and weakened Parasites. Also, without having your attacks slowed by the Nightmare Spores or letting a Sleepwalker reach Phosani's Nightmare.
            405|Phosani's Nightmare|Speed|Phosani's Speedrunner|Kill Phosani's Nightmare within 6 minutes.
            409|Phosani's Nightmare|Stamina|Can't Wake Up|Kill Phosani's Nightmare 5 times in a row without leaving Phosani's Dream.
            417|Nex|Restriction|Nex Duo|Kill Nex with two or less players at the start of the fight.
            418|Nex|Restriction|I should see a doctor|Kill Nex whilst a player is coughing.
            419|Nex|Perfection|Perfect Nex|Kill Nex whilst completing the requirements for "There is no escape", "Shadows move", "A siphon will solve this", and "Contain this!"
            422|Tombs of Amascut: Expert Mode|Speed|Tombs Speed Runner II|Complete the Tombs of Amascut (expert) within 20 mins at any group size.
            423|Tombs of Amascut: Expert Mode|Speed|Tombs Speed Runner III|Complete the Tombs of Amascut (expert) within 18 mins in a group of 8.
            434|Tombs of Amascut: Expert Mode|Perfection|Perfection of Het|Complete 'Perfect Het' and 'Perfect Akkha' in a single run of the Tombs of Amascut.
            437|Tombs of Amascut: Expert Mode|Perfection|Perfection of Apmeken|Complete 'Perfect Apmeken' and 'Perfect Ba-Ba' in a single run of the Tombs of Amascut.
            440|Tombs of Amascut: Expert Mode|Perfection|Perfection of Crondis|Complete 'Perfect Crondis' and 'Perfect Zebak' in a single run of the Tombs of Amascut.
            443|Tombs of Amascut: Expert Mode|Perfection|Perfection of Scabaras|Complete 'Perfect Scabaras' and 'Perfect Kephri' in a single run of Tombs of Amascut.
            445|Tombs of Amascut: Expert Mode|Perfection|Insanity|Complete 'Perfect Wardens' at expert or above.
            446|Tombs of Amascut: Expert Mode|Mechanical|Amascut's Remnant|Complete the Tombs of Amascut at raid level 500 or above without anyone dying.
            448|Tombs of Amascut: Expert Mode|Mechanical|Maybe I'm the boss.|Complete a Tombs of Amascut raid with every single boss invocation activated and without anyone dying.
            450|Tombs of Amascut: Expert Mode|Mechanical|Akkhan't Do it|Defeat Akkha with all Akkha invocations activated and the path levelled up to at least four, without dying yourself.
            454|Tombs of Amascut: Expert Mode|Mechanical|All Praise Zebak|Defeat Zebak without losing a single prayer point. You must also meet the conditions of the 'Rockin' Around The Croc' achievement.
            471|Tombs of Amascut: Expert Mode|Kill Count|Expert Tomb Raider|Complete the Tombs of Amascut (Expert mode) 50 times.
            477|Phantom Muspah|Speed|Phantom Muspah Speed-Runner|Kill the Phantom Muspah in less than 1 minute and 30 seconds without a slayer task.
            484|Phantom Muspah|Perfection|Phantom Muspah Manipulator|Kill the Phantom Muspah whilst completing Walk Straight Pray True, Space is Tight & Can't Escape.
            487|Vardorvis|Speed|Vardorvis Speed-Runner|Kill Vardorvis in less than 0:55 without a slayer task.
            490|Vardorvis|Kill Count|Vardorvis Sleeper|Kill Awakened Vardorvis.
            492|Vardorvis|Mechanical|Axe Enthusiast|Kill Vardorvis after surviving for 3 minutes of Vardorvis' max speed, and never leaving the centre 25 tiles.
            496|Whisperer|Speed|Whisperer Speed-Runner|Kill the Whisperer in less than 2:05 without a slayer task.
            499|Whisperer|Kill Count|Whispered|Kill the Awakened Whisperer.
            502|Whisperer|Restriction|Dark Memories|Kill the Whisperer whilst spending less than 6 seconds in the pre-enrage shadow realm.
            505|Leviathan|Speed|Leviathan Speed-Runner|Kill the Leviathan in less than 1:10 without a slayer task.
            508|Leviathan|Kill Count|Leviathan Sleeper|Kill the Awakened Leviathan.
            511|Leviathan|Restriction|Unconventional|Kill the Leviathan using only Mithril ammunition whilst having no more than 25 Hitpoints throughout the entire fight.
            514|Duke Sucellus|Speed|Duke Sucellus Speed-Runner|Kill Duke Sucellus in less than 40 seconds without a slayer task.
            517|Duke Sucellus|Kill Count|Duke Sucellus Sleeper|Kill Awakened Duke Sucellus.
            520|Duke Sucellus|Restriction|Mirror Image|Kill Duke Sucellus whilst only attacking the boss on the same tick Duke attacks you.
            539|Fortis Colosseum|Kill Count|Colosseum Grand Champion|Defeat Sol Heredit 10 times.
            541|Fortis Colosseum|Restriction|Slow Dancing in the Sand|Defeat Sol Heredit without running during the fight with him.
            543|Fortis Colosseum|Perfection|Perfect Footwork|Defeat Sol Heredit without taking any damage from his Spear, Shield, Grapple or Triple Attack.
            547|Fortis Colosseum|Mechanical|Reinforcements|Defeat Sol Heredit with "Bees II", "Quartet" and "Solarflare II" modifiers active.
            550|Fortis Colosseum|Speed|Colosseum Speed-Runner|Complete the Colosseum with a total time of 24:00 or less.
            560|Araxxor|Speed|Araxxor Speed-Runner|Kill Araxxor 6 times in 10:00.
            562|Araxxor|Perfection|Perfect Araxxor 2|Kill Araxxor perfectly, without hitting it during the enrage phase.
            565|Araxxor|Restriction|Swimming in Venom|Kill Araxxor without the boss ever moving.
            578|The Hueycoatl|Speed|Hueycoatl Speed-Runner|Kill the Hueycoatl in 2:30 with three or fewer players.
            603|Yama|Speed|Yama Speed-Runner|Defeat Yama in an average time of under 2:34 over your last four kills.
            608|Yama|Perfection|Contract Choreographer|Defeat Yama whilst completing 'Shadow Dancer', 'Fire Fighter', 'Back so soon?' and without taking any other avoidable damage.
            609|Yama|Kill Count|Contractually Unbound|Defeat Yama under the elevated conditions of one of his non-standard contracts.
            612|Doom of Mokhaiotl|Stamina|It's Dark Down Here|Defeat the Doom of Mokhaiotl at delve level 16.
            616|Doom of Mokhaiotl|Perfection|Perfect Doom|Defeat the Doom of Mokhaiotl levels 1-8 without taking any damage from the boss, letting any grubs be absorbed, or stepping in acid blood.
            617|Doom of Mokhaiotl|Restriction|The Praying Mantis|Defeat the Doom of Mokhaiotl levels 1-8 without restoring your prayer points through any method.
            618|Doom of Mokhaiotl|Restriction|Duel of Mokhaiotl|Defeat the Doom of Mokhaiotl levels 1-8 whilst only damaging the boss with one-handed melee attacks.
            619|Doom of Mokhaiotl|Restriction|Darkness Is Your Ally?|Defeat the Doom of Mokhaiotl levels 1-8 without equipping a demonbane weapon.
            623|Doom of Mokhaiotl|Speed|Doom Racer|Defeat the Doom of Mokhaiotl levels 1-8 in less than 7:15.
            624|Doom of Mokhaiotl|Mechanical|Mopping up|Clear at least 8 acid splats in a single Volatile Earth special attack.
            """;
}
