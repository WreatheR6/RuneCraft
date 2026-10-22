package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Quest.Difficulty;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.data.Drop;
import java.util.List;

/**
 * Mahjarrat and northern storylines: Enakhra's Lament, Devious Minds, The Garden of Death, Secrets of
 * the North, Desert Treasure II - The Fallen Empire, The Curse of Arrav, My Arm's Big Adventure and
 * Making Friends with My Arm. XP-lamp rewards are real lamps the player rubs ({@link QuestLamps}).
 */
final class MahjarratQuests {
    private MahjarratQuests() {
    }

    static void register(QuestBook b) {
        b.place("quest_enakhras_temple", "Enakhra's Temple", 3104, 2932, PlaceDef.Style.RUINS);
        b.spot("quest_paterdomus_hills", "Hills north of Paterdomus", 3406, 3505);
        b.spot("quest_garden_of_death", "The Garden of Death", 1255, 3625);
        b.place("quest_the_vault", "The Vault", 3180, 2862, PlaceDef.Style.RUINS);
        b.spot("quest_troll_stronghold_roof", "Troll Stronghold rooftop", 2829, 3694);
        b.place("quest_weiss", "Weiss", 2848, 3936, PlaceDef.Style.CAMP);
        enakhrasLament(b);
        deviousMinds(b);
        gardenOfDeath(b);
        secretsOfTheNorth(b);
        desertTreasureII(b);
        curseOfArrav(b);
        myArmsBigAdventure(b);
        makingFriendsWithMyArm(b);
    }

    /**
     * Enakhra's Lament, after the wiki walkthrough and quest-helper's steps: Lazim's statue (a base from 32kg of sandstone, a
     * body from 20kg, a granite head of the player's choosing), the fall into the temple, the limbs chiselled off the statue to
     * open the four outer doors ({@link #EL_DOORS}), the sigils for the central room, the camel head moulded in soft clay for
     * the pedestal, the four rooms around the barrier ({@link #EL_ROOMS}, the braziers {@link #EL_BRAZIERS}), the first Boneguard
     * crumbled, and Akthanakos, the second Boneguard, freed by finishing the wall that hides him from Enakhra.
     */
    private static void enakhrasLament(QuestBook b) {
        b.spot("lazim_quarry", "Desert Quarry (Lazim)", 3191, 2926); // north-east of the Desert Quarry (cache tile)
        Cast.npc(b, "lazim", "Lazim", "lazim_quarry", 0, 0, Cast.BROWN, 0xC0A060);
        Cast.npc(b, "akthanakos", "Akthanakos", "quest_enakhras_temple", 3, -3, Cast.BROWN, 0x6A4A2A);
        b.item("quest_sandstone_statue", "Sandstone statue", "statue", 0xE0C890, "A statue of Enakhra, carved from sandstone.");
        b.items(s -> s.add("camulet", "Camulet", Kind.SIMPLE, "amulet", 0xC0A060).value(1).stack(1)
                .examine("An amulet that lets you talk to camels, and teleports you to Enakhra's Temple."));
        b.boss(BossDef.of("quest_boneguard", "Boneguard").level(132).stats(150, 90, 90, 120, 1, 1).speed(5)
                .defenceBonus(60, 60, 0, 60, 60)
                .attack(AttackDef.of("bone_club", AttackType.CRUSH, 16, 3, 6))
                .attack(AttackDef.of("stomp", AttackType.CRUSH, 12, 3, 2).special(Special.AOE, 3).telegraph(2, "The Boneguard raises its foot!"))
                .drops(Drop.always("big_bones")).skin(0xE8E0C8, 0xD8D0B8, 0xC8C0A8, 0x8A8070).size(1.2f, 2.6f, 1.1f)
                .at("quest_enakhras_temple").arena(9, "sand"));
        b.quest("enakhras_lament", "Enakhra's Lament", Difficulty.EXPERIENCED, 2)
                .req(Skill.CRAFTING, 50).req(Skill.FIREMAKING, 45).req(Skill.PRAYER, 43).req(Skill.MAGIC, 39)
                .xp(Skill.CRAFTING, 7000).xp(Skill.MINING, 7000).xp(Skill.FIREMAKING, 7000).xp(Skill.MAGIC, 7000).reward("camulet")
                .unlock("Access to Enakhra's Temple; talking to camels with the camulet")
                .stage("I can start this quest by speaking to Lazim just north-east of the Desert Quarry. (Requires 50 Crafting, 45 Firemaking, 43 Prayer, 39 Magic.)")
                .talk("lazim").say("I was a sculptor once, for royalty. Since my accident I can't hold a chisel. Help me raise a statue here, in this "
                        + "desolate place? Bring me 32 kilograms of sandstone from the quarry.")
                .stage("I should build Lazim's statue: 32kg of sandstone for the base, 20kg for the body and a 5kg piece of granite for "
                        + "the head, each shaped with a chisel.")
                // playability: Lazim takes the weights as three 10kg blocks and a 2kg one (OSRS: any pieces adding up to 32kg)
                .talk("lazim").stay().unlessFlag("el_base").take("sandstone_10kg:3", "sandstone_2kg").need("chisel").flag("el_base")
                .say("> I have the sandstone.", "*He binds the pieces together with a word: a 32kg block.* Chisel it into a base, and set it "
                        + "on the flat ground here.", "* You chisel a sandstone base and set it beside Lazim.",
                        "Now 20 kilograms more, for the body.")
                .talk("lazim").stay().needFlag("el_base").unlessFlag("el_body").take("sandstone_10kg:2").need("chisel").flag("el_body")
                .say("*Another block of 20kg.* The body. Try not to ruin it.", "* You chisel a sandstone body and fit it to the base.",
                        "It's missing something... a head! Granite, from the same quarry. Whose head should it bear?")
                .puzzle("lazim", EL_HEAD).stay().needFlag("el_body").unlessFlag("el_head")
                .flag("el_head").say("Fine. Carve it from a 5kg piece of granite, and set it on the statue.")
                .talk("lazim").needFlag("el_head").take("granite_5kg").need("chisel").travel("quest_enakhras_temple")
                .say("* You chisel the granite into a head and set it on the statue. The ground gives way: the statue falls into a hole...",
                        "* As you peer in, Lazim pushes you! You land in an ancient temple far below, beside the broken statue.",
                        "Everything as I planned. Treasure lies above us, friend, and the doors here want stone limbs, not keys.")
                .missing("?!el_base|Thirty-two kilograms of sandstone for the base: three 10kg blocks and a 2kg piece. And your chisel.",
                        "?el_base|?!el_body|Twenty kilograms of sandstone for the body: two 10kg blocks.",
                        "?el_body|?!el_head|Whose head should it bear? Tell me.",
                        "?el_head|A 5kg piece of granite for the head, and your chisel.")
                .stage("I'm in Enakhra's temple. I should chisel the limbs off the broken statue to open the four outer doors, take the "
                        + "Z, R, K and M sigils for the central room, then solve the rooms around the barrier upstairs.")
                .reach("quest_enakhras_temple", 10).stay().unlessFlag("el_limbs").need("chisel").flag("el_limbs")
                .say("* You chisel the broken statue's arms and legs off: four stone limbs. Lazim hands back its stone head.")
                .missing("* The statue's limbs would come away with a chisel.")
                .puzzle("quest_enakhras_temple", EL_DOORS).stay().needFlag("el_limbs").unlessFlag("el_doors").flag("el_doors")
                .say("* Behind the doors, visions of Enakhra: her temple finished for her lord, the Knights of Avarrocka frozen by her "
                        + "magic, Akthanakos come to turn her, the bones she raised. On the pedestals: the Z, R and K sigils.",
                        "* With the M sigil from your own room the consonants of Zamorak open the central room. You climb the ladder.")
                .reach("quest_enakhras_temple", 10).stay().needFlag("el_doors").unlessFlag("el_pedestal").take("soft_clay", "granite_5kg")
                .need("chisel").flag("el_pedestal")
                .say("* By the ladder a pedestal has a recess. The statue's head almost fits. You press soft clay into it: a camel's head.",
                        "* From the mould you chisel a camel head out of granite. It fits: a vision of Enakhra freezing Akthanakos.",
                        "Well done! The barrier will open when the four rooms around it are set right: their globes will light.")
                .missing("* The pedestal's recess is the shape of some head. A mould of soft clay, and 5kg of granite to chisel, would match it.")
                .puzzle("quest_enakhras_temple", EL_ROOMS).stay().needFlag("el_pedestal").unlessFlag("el_rooms")
                .reqSkill(Skill.MAGIC, 35).take("air_rune:5", "fire_rune:4", "chaos_rune:2", "bread").flag("el_rooms")
                .say("* A wind bolt clears the furnace, a fire bolt thaws the fountain, and Pentyn eats your bread and remembers his family.",
                        "* In the dark room six braziers wait to be relit.")
                .missing("* The furnace and the fountain want a wind bolt and a fire bolt (5 air, 4 fire, 2 chaos runes; 35 Magic); the "
                        + "frozen knight is starving (a loaf of bread; OSRS takes a pie, cake, pizza or potato too).")
                .puzzle("quest_enakhras_temple", EL_BRAZIERS).needFlag("el_rooms").reqSkill(Skill.FIREMAKING, 45)
                .take("logs", "oak_logs", "willow_logs", "maple_logs", "candle", "coal").need("tinderbox")
                .say("* The last brazier blazes and the fourth globe lights. The magic barrier fades.")
                .missing("?el_rooms|* The six braziers want logs, oak, willow and maple logs, a candle and some coal, and a tinderbox.")
                .stage("I must get past the Boneguard beyond the barrier. It will not let me by unless it crumbles to dust.")
                .kill("quest_boneguard")
                .say("* The Boneguard collapses. Its freed spirit thanks you. Down the ladder stands a second Boneguard, held by Enakhra's spells.")
                .stage("I should help the second Boneguard: it asks me to finish the wall that hides it from Enakhra, with stones from the "
                        + "rubble trimmed by a chisel.")
                .talk("akthanakos").stay().unlessFlag("el_wall_asked").flag("el_wall_asked")
                .say("* It swings at you, and again; under Protect from Melee you hardly flinch. It lowers its club.",
                        "Impressive. I am her slave: Enakhra tricked me into this shape. Finish my wall so she cannot see me, and I am free.",
                        "Three loads of stone from the rubble, trimmed to fit with a chisel.")
                .reach("quest_enakhras_temple", 10).needFlag("el_wall_asked").need("chisel")
                .say("* You stack three loads of sandstone from the rubble on the wall and trim each to fit. Enakhra shrieks with fury.")
                .missing("* The unfinished wall needs stones from the rubble, and a chisel to trim them.")
                .stage("The wall is finished. I should speak to the Boneguard.")
                .talk("akthanakos")
                .say("* The Boneguard crumbles: it is the Mahjarrat Akthanakos, camel-headed, free after thousands of years.",
                        "Take my amulet, friend. It speaks to camels, and brings you back here.",
                        "* Enakhra blows the wall apart. The two Mahjarrat take their skeletal forms and leave to settle their rivalry in the North.");
    }

    /**
     * Enakhra's Lament (wiki): Lazim lets the player choose the statue's head: Lazim's own, a camel's, Zamorak's or Icthlarin's.
     * The player's own head he won't allow.
     */
    static final QuestPuzzle EL_HEAD = QuestPuzzle.builder("el_head").variant(null)
            .prompt("Whose head should the statue bear?")
            .options("My own", "Lazim's", "A camel's", "Zamorak's", "Icthlarin's")
            .solution(1).solution(2).solution(3).solution(4)
            .failLines("Your head? On my statue? Certainly not. Choose again.").build();

    /**
     * Enakhra's Lament: the four outer doors open to the stone limbs (quest-helper: the right arm door to the north-east, the
     * left leg door to the north-west, the left arm door to the south-west, the right leg door to the south-east), walked
     * anticlockwise for the visions to come in order.
     */
    static final QuestPuzzle EL_DOORS = QuestPuzzle.builder("el_doors").shuffle().variant(null)
            .prompt("* Four doors ring the temple, each with a recess cut to the shape of a stone limb. Walk them anticlockwise, "
                            + "from the north-east, and the visions behind them come in order.",
                    "* North-east: the recess of a right arm. North-west: of a left leg. South-west: of a left arm. South-east: of a "
                            + "right leg.",
                    "* Fit a limb to each door in turn.")
            .options("Stone right arm", "Stone left arm", "Stone right leg", "Stone left leg")
            .solution(0, 3, 1, 2)
            .failLines("* The limb doesn't fit the recess. You take your limbs back.").build();

    /**
     * Enakhra's Lament (wiki): the four rooms around the barrier. Smoke (north-east): the furnace's grate wants a strong blast
     * of air, a wind bolt or stronger; shadow (south-east): six braziers to relight; blood (north-west): Pentyn, a frozen knight,
     * wants food; ice (south-west): a frozen fountain that yields only to a fire bolt or stronger.
     */
    static final QuestPuzzle EL_ROOMS = QuestPuzzle.builder("el_rooms").shuffle().variant(null)
            .prompt("* Four rooms surround the barrier, each with a globe on the pedestal: unlit.",
                    "* North-east: a furnace, its grate choked with fumes. North-west: an old knight, frozen in place, wounded and starving.",
                    "* South-west: a fountain frozen solid; ordinary fire and swords have not touched it. South-east: six cold braziers "
                            + "in the dark.",
                    "* What does each room need, north-east, north-west, then south-west? (The braziers come last.)")
            .options("Cast a wind bolt", "Cast a fire bolt", "Cast a water bolt", "Give him food", "Strike it with a sword",
                    "Light a tinderbox")
            .solution(0, 3, 1)
            .failLines("* Nothing happens. The globe stays dark.").build();

    /**
     * Enakhra's Lament (wiki, quest-helper): each brazier in the shadow room shows what burned there (45 Firemaking reads them
     * all): normal logs to the south-west, oak to the south, willow to the south-east, maple to the north-west, a candle to the
     * north and coal to the north-east.
     */
    static final QuestPuzzle EL_BRAZIERS = QuestPuzzle.builder("el_braziers").shuffle().variant(null)
            .prompt("* You look into each brazier for what once burned in it.",
                    "* South-west: plain, pale wood ash. South: ash and a scrap of thick, ridged bark. South-east: ash and the curl of a "
                            + "thin, drooping twig.",
                    "* North-west: ash and a scorched winged seed. North: a hardened puddle of wax and a burnt wick. North-east: black, "
                            + "glittering dust that smudges your fingers.",
                    "* Relight them in that order: south-west, south, south-east, north-west, north, north-east.")
            .options("Logs", "Oak logs", "Willow logs", "Maple logs", "A candle", "Coal")
            .solution(0, 1, 2, 3, 4, 5)
            .failLines("* The fuel smoulders and goes out. The braziers fall dark again.").build();

    private static void deviousMinds(QuestBook b) {
        Cast.npc(b, "devious_monk", "Monk", "quest_paterdomus_hills", 0, 0, Cast.FAIR, 0x6A4A2A);
        b.item("slender_blade", "Slender blade", "dagger", 0x8A9AB0, "A fine blade made from mithril.");
        b.item("bow_sword", "Bow-sword", "longbow", 0x8A9AB0, "A yew longbow with a blade fixed to it.");
        b.item("devious_package", "Package", "package", 0xA08060, "A package from the monk.");
        b.quest("devious_minds", "Devious Minds", Difficulty.EXPERIENCED, 1)
                .reqQuest("wanted", "troll_stronghold", "DORICS_QUEST", "enter_the_abyss")
                .req(Skill.SMITHING, 65).req(Skill.RUNECRAFT, 50).req(Skill.FLETCHING, 50)
                .xp(Skill.FLETCHING, 5000).xp(Skill.RUNECRAFT, 5000).xp(Skill.SMITHING, 6500)
                .unlock("Access to the Abyss via the Entrana Zamorakian portal")
                .stage("I can start this quest by speaking to the Monk in the hills north of Paterdomus. (Requires Wanted!, Troll Stronghold, Doric's Quest, Enter the Abyss, 65 Smithing, 50 Runecraft, 50 Fletching.)")
                .talk("devious_monk").say("I seek a weapon of a peculiar kind: a bow and a sword, joined. A bow-sword. Make me one, and I'll reward you.")
                .stage("I should smith a slender blade (a mithril bar) and attach it to a yew longbow to make a bow-sword.")
                .talk("devious_monk").take("mithril_bar", "yew_longbow").need("hammer").give("devious_package")
                .say("*You hammer out a slender blade and fix it to the bow.* Perfect. Now take this package to the Entrana priest... it's a gift. No questions.")
                .missing("Bring me a mithril bar and a yew longbow, and a hammer to smith the blade.")
                .stage("I should deliver the monk's package to the High Priest of Entrana. No weapons are allowed on the island.")
                .talk("high_priest_entrana").take("devious_package").say("A package? *It bursts open: a Zamorakian portal!* Doomsayers pour through and steal our holy relic!")
                .stage("The monk was a Zamorakian! I must chase the thieves into the Abyss and recover the relic.")
                .kill("abyssal_demon").say("* Among the abyssal creatures you find the relic, dropped by a fleeing doomsayer.")
                .stage("I should return the relic to the High Priest of Entrana.")
                .talk("high_priest_entrana").say("The relic is safe! You were deceived, but you made it right. The portal the Zamorakians made still leads into the Abyss, if you dare.");
    }

    private static void gardenOfDeath(QuestBook b) {
        Cast.npc(b, "kasonde", "Kasonde", "kebos_lowlands", -3, 3, Cast.BROWN, 0x5A6A3A);
        b.item("garden_tablet_fragment", "Stone tablet fragment", "rune", 0x8A8A7A, "A fragment of an ancient stone tablet.");
        b.quest("the_garden_of_death", "The Garden of Death", Difficulty.INTERMEDIATE, 1)
                .xp(Skill.FARMING, 10000) // OSRS (current quest XP rewards)
                .req(Skill.FARMING, 20)
                .unlock("Access to the Garden of Death beneath the Kebos Swamp")
                .stage("I can start this quest by speaking to Kasonde in the Kebos Lowlands. (Requires 20 Farming.)")
                .talk("kasonde").say("I'm looking for a garden that was buried long ago, beneath the swamp. The old tablets say it was tended by the dead.")
                .stage("I should search the Kebos swamp for the entrance to the Garden of Death.")
                .reach("quest_garden_of_death", 6).need("spade").give("garden_tablet_fragment:3")
                .say("* You dig through the mud into a buried garden of dead plants. Three tablet fragments lie among the roots.")
                .missing("* The ground is too muddy to search by hand. I need a spade.")
                .stage("I should bring the tablet fragments to Kasonde.")
                .talk("kasonde").take("garden_tablet_fragment:3").say("These speak of a medallion of the old empire... and of a vault in the desert. Interesting. Very interesting. Thank you.");
    }

    private static void secretsOfTheNorth(QuestBook b) {
        // OSRS walkthrough: the Carnillean guard, Evelot (the hired killer) at the Fight Arena, Hazeel, Claus's cellar,
        // Weiss (Big Fish, Snowflake), the assassin, the ritual room puzzles and the Strange Creature (the Phantom Muspah).
        b.spot("sotn_evelot_trail", "Evelot's trail", 2643, 3202); // quest-helper: where the trail of boulders and bushes ends at Evelot
        b.spot("sotn_weiss_cave", "Weiss southern cave", 2860, 3930); // approx: beneath the main building
        b.spot("sotn_weiss_ritual_room", "Weiss ritual room", 2870, 3925); // approx
        Cast.npc(b, "sotn_carnillean_guard", "Guard", "carnillean_house", 2, 3, Cast.FAIR, 0x5A5A6A);
        Cast.npc(b, "evelot", "Evelot", "fight_arena", 6, 6, Cast.FAIR, 0x6A2A5A);
        Cast.npc(b, "sotn_hazeel", "Hazeel", "hazeel_cult_hideout", -2, 3, 0x7A8A9A, 0x2A1A3A);
        Cast.npc(b, "sotn_big_fish", "Big Fish", "sotn_weiss_cave", 6, 6, Cast.TROLL, 0x5A6A7A);
        Cast.npc(b, "sotn_snowflake", "Snowflake", "sotn_weiss_cave", -6, 4, Cast.TROLL, 0xC0C8D0);
        b.item("sotn_dusty_scroll", "Dusty scroll", "scroll", 0xB0A080, "An old scroll from the Carnilleans' hidden chest. It mentions a ritual in the north.");
        b.boss(BossDef.of("quest_evelot", "Evelot").level(148).stats(160, 160, 150, 120, 80, 1).speed(4)
                .defenceBonus(70, 60, 40, 0, 40)
                .attack(AttackDef.of("mace", AttackType.CRUSH, 34, 2.5, 6))
                .attack(AttackDef.of("prayer_smash", AttackType.CRUSH, 30, 2.5, 1).special(Special.DRAIN_PRAYER, 10)
                        .telegraph(2, "Evelot raises her mace - your prayers falter!")) // approx: disables protection prayers
                .drops(Drop.always("bones")).skin(Cast.FAIR, 0x6A2A5A, 0x3A1A2A, 0x2A1A1A).size(0.6f, 1.9f, 1.0f)
                .at("sotn_evelot_trail").arena(9, "stone"));
        b.boss(BossDef.of("quest_assassin", "Assassin").level(262).stats(400, 140, 140, 140, 140, 140).speed(4)
                .defenceBonus(60, 30, 80, 200, 50)
                .attack(AttackDef.of("blade", AttackType.SLASH, 39, 2.5, 4))
                .attack(AttackDef.of("knives", AttackType.RANGED, 39, 10, 4))
                .attack(AttackDef.of("poison_vial", AttackType.RANGED, 30, 10, 1).special(Special.POISON, 6)
                        .particle("minecraft:item_slime")) // OSRS: up to 30, poison starting at 6
                .drops(Drop.always("bones")).skin(Cast.PALE, 0x1A1A1A, 0x101010, 0x2A2A2A).size(0.6f, 1.9f, 1.0f)
                .at("sotn_weiss_cave").arena(10, "ice"));
        // Wiki "Strange Creature": level 368, 550 HP, the Phantom Muspah's fight on smaller numbers (MuspahRules.STRANGE_CREATURE,
        // played by the bosses module's Muspah script): a ranged form (ice shards up to 32, speed 6, and a corrupting magic orb up
        // to 33) and a melee form (stab up to 24, speed 5) that swap as it takes damage with spikes under the player; the orb
        // barrage and homing spikes; at 20% health the shockwave (40, only spikes stop it) and a 75-point prayer shield with
        // Soul Split, then its ranged form at 38 with spikes over the whole cave.
        b.boss(BossDef.of("quest_strange_creature", "Strange Creature").level(368).stats(550, 280, 280, 200, 150, 280).speed(6)
                .defenceBonus(110, 90, 80, 280, 0) // ranged form (melee form: magic -10, ranged 160)
                .attack(AttackDef.of("ice_shard", AttackType.RANGED, 32, 14, 5).particle("minecraft:snowflake"))
                .attack(AttackDef.of("smite", AttackType.MAGIC, 33, 14, 1).particle("minecraft:soul_fire_flame")
                        .telegraph(2, "The Strange Creature raises its arms, charging purple energy!"))
                .attack(AttackDef.of("stomp", AttackType.STAB, 24, 4, 5))
                .attack(AttackDef.of("lightning_cloud", AttackType.MAGIC, 14, 64, 1).special(Special.HAZARD, 0)
                        .message("The Strange Creature vanishes and the cave dims - a barrage of orbs!").particle("minecraft:electric_spark"))
                .attack(AttackDef.of("shockwave", AttackType.MAGIC, 40, 64, 1).special(Special.AOE, 32)
                        .telegraph(5, "The Strange Creature teleports to the centre - hide behind a spike!").particle("minecraft:sonic_boom"))
                .drops(Drop.always("bones")).model("phantom_muspah").size(2.0f, 2.6f, 1.6f)
                .at("sotn_weiss_ritual_room").arena(12, "ice"));
        b.quest("secrets_of_the_north", "Secrets of the North", Difficulty.MASTER, 2)
                .reqQuest("making_friends_with_my_arm", "the_generals_shadow", "devious_minds", "hazeel_cult")
                .xp(Skill.AGILITY, 60000).xp(Skill.THIEVING, 50000).xp(Skill.HUNTER, 40000) // OSRS (current quest XP rewards)
                .req(Skill.AGILITY, 69).req(Skill.THIEVING, 64).req(Skill.HUNTER, 56)
                .unlock("The Phantom Muspah boss in the Ghorrock Dungeon")
                .stage("I can start this quest by speaking to the guard at the Carnillean Mansion in East Ardougne. (Requires Making Friends with My Arm, The General's Shadow, Devious Minds, Hazeel Cult, 69 Agility, 64 Thieving, 56 Hunter.)")
                .talk("sotn_carnillean_guard").say("Ceril Carnillean's been murdered! Upstairs, in his own home. The captain wants it kept quiet - will you take a look?")
                .stage("I should investigate Ceril's murder upstairs in the Carnillean Mansion.")
                .reach("carnillean_house", 8).stay().unlessFlag("sotn_scene").flag("sotn_scene")
                .say("* You inspect Ceril's corpse and the broken window, knock on the false wall and climb to a hidden room. Its chest has been forced.")
                .talk("sotn_carnillean_guard").needFlag("sotn_scene")
                .say("> The window was broken from the outside. Ceril was stabbed from behind. And a hidden room above us was broken into.",
                        "A professional, then. The Khazard barman at the Fight Arena hears about anyone with a blade for hire. Ask about Evelot.")
                .missing("Have you looked upstairs yet? Inspect the body, the window... the whole room.")
                .stage("I should ask the Khazard barman about Evelot and track her down.")
                .talk("khazard_barman").stay().needFlag("sotn_scene").unlessFlag("sotn_barman").flag("sotn_barman")
                .say("Evelot? Drank here, paid in old coins, left in a hurry. Try following her tracks outside.")
                .reach("sotn_evelot_trail", 6).stay().needFlag("sotn_barman").unlessFlag("sotn_tracked").flag("sotn_tracked").spawn("quest_evelot")
                .say("* Barrels by the bar, a boulder, a bush, a tree stump, another boulder, a bush... Evelot is waiting for you, mace in hand.")
                .kill("quest_evelot").stay().needFlag("sotn_tracked").unlessFlag("sotn_evelot").flag("sotn_evelot")
                .say("* Evelot drops to her knees.")
                .talk("evelot").needFlag("sotn_evelot").say("I was paid to take a scroll from that room, nothing more. The one who paid me wore Hazeel's colours.")
                .missing("* Evelot won't say a word while she can still fight.")
                .stage("I should find out who hired Evelot: Hazeel's followers in the cult hideout beneath the Ardougne sewers.")
                .talk("clivet").stay().unlessFlag("sotn_cult").flag("sotn_cult").say("The master will see you. Take the raft.")
                .talk("sotn_hazeel").stay().needFlag("sotn_cult").unlessFlag("sotn_hazeel").flag("sotn_hazeel")
                .say("I did not order a murder. But the Carnilleans hid something of mine: a scroll. Find what the killer missed.")
                .reach("carnillean_house", 8).stay().needFlag("sotn_hazeel").unless("sotn_dusty_scroll").unlessFlag("sotn_scroll").give("sotn_dusty_scroll")
                .reqSkill(Skill.THIEVING, 64).need("lockpick") // quick guide: a lockpick or a hair clip (not in RuneCraft)
                .say("* Claus the Chef lets you into the cellar. Behind the cooking shelves a button opens a passage to a locked chest.",
                        "* You pick the lock pin by pin and take a dusty scroll.")
                .missing("* The chest in the hidden passage is locked. You'll need a lockpick.")
                .talk("sotn_hazeel").needFlag("sotn_hazeel").take("sotn_dusty_scroll").flag("sotn_scroll")
                .say("A ritual... in the north. Weiss. Whoever hired your killer is going there. Follow them.")
                .missing("The scroll the Carnilleans hid. Search their cellar.")
                .stage("I should go to Weiss and find out what the killer is after.")
                .talk("sotn_big_fish").stay().unlessFlag("sotn_big_fish").flag("sotn_big_fish")
                .say("Stranger came through. Asked about the old cave under the big house. Snowflake saw more.")
                .talk("sotn_snowflake").stay().needFlag("sotn_big_fish").unlessFlag("sotn_snowflake").flag("sotn_snowflake")
                .say("Odd man. Hooded. Went down the stairs, to the south cave. Snowflake not follow. Snowflake not stupid.")
                .reach("sotn_weiss_cave", 6).stay().needFlag("sotn_snowflake").unlessFlag("sotn_cave").flag("sotn_cave").spawn("quest_assassin")
                .say("* In the southern cave, a hooded assassin turns from the ice. Smoke bombs scatter across the floor!")
                .kill("quest_assassin").needFlag("sotn_cave").flag("sotn_assassin")
                .say("* The assassin flees in a puff of smoke, wounded.")
                .stage("I should open the ritual room's sealed gate under Weiss and face whatever lies beyond.")
                .talk("sotn_hazeel").stay().needFlag("sotn_assassin").unlessFlag("sotn_hazeel_weiss").flag("sotn_hazeel_weiss")
                .say("I followed you. The gate ahead is Zarosian work. Its locks answer to old words and old fire.")
                // the Ghorrock Dungeon's locks, in the walkthrough's order (SOTN_LOCKS)
                .puzzle("sotn_weiss_ritual_room", SOTN_LOCKS.get(0)).stay().needFlag("sotn_hazeel_weiss").unlessFlag("sotn_north_gate", "sotn_puzzles")
                .flag("sotn_north_gate")
                .say("* The northern gate opens. In its chest, a strange list. You fit the lever handle from the barrel and pull the lever;",
                        "* in the room where you fought the assassin, the south-western pillar gives up a jewel shard and a strange cipher.")
                .puzzle("sotn_weiss_ritual_room", SOTN_LOCKS.get(1)).stay().needFlag("sotn_north_gate").unlessFlag("sotn_centre_gate", "sotn_puzzles")
                .flag("sotn_centre_gate")
                .say("* BLOOD. The centre gate swings open.")
                .puzzle("sotn_weiss_ritual_room", SOTN_LOCKS.get(2)).stay().needFlag("sotn_centre_gate").unlessFlag("sotn_braziers", "sotn_puzzles")
                .need("tinderbox").flag("sotn_braziers")
                .say("* The last brazier catches. The western chest unlocks.")
                .missing("* The braziers need lighting. You'll need a tinderbox (the crate in the western part of the room).")
                .puzzle("sotn_weiss_ritual_room", SOTN_LOCKS.get(3)).stay().needFlag("sotn_braziers").unlessFlag("sotn_puzzles").flag("sotn_puzzles")
                .say("* The northern chest gives up an icy chest. You join the two jewel shards into an ancient jewel; it opens the icy chest:",
                        "* inside, an icy key. The key turns in the gate south-west of the centre room. Beyond it, a crevice.")
                .reach("sotn_weiss_ritual_room", 6).stay().needFlag("sotn_puzzles").unlessFlag("sotn_creature").spawn("quest_strange_creature")
                .say("* Beyond the icy gate, a crevice. Something prismatic unfolds from the dark: a Strange Creature!")
                .kill("quest_strange_creature").stay().needFlag("sotn_puzzles").unlessFlag("sotn_creature").flag("sotn_creature")
                .say("* The Strange Creature shatters and flees deeper into the ice. Hazeel teleports you back to Ardougne.")
                .talk("sotn_carnillean_guard").needFlag("sotn_creature")
                .say("A Zarosian ritual under Weiss? And a creature like that... Well. The captain will want this kept very quiet indeed. Thank you.");
    }

    /**
     * Secrets of the North, the Ghorrock Dungeon's locks in order (wiki walkthrough "Secrets of the Dungeon"): the northern
     * gate's arrows from Tullia's letter and the ancient map (left, up, left, down), the centre gate's word from the Duke note,
     * strange list and strange cipher (BLOOD), the braziers by the numbers note (north-west, south-east, north-east,
     * south-west), then the northern chest's code from the settlements note on the ancient map (7402).
     */
    static final List<QuestPuzzle> SOTN_LOCKS = List.of(
            lock("sotn_north_gate", "* The gate stays shut. Better read Tullia's letter against the map again.",
                    new String[]{"* Tullia's letter, from the chest in the south-eastern room: five of its letters are purple - S, p, c, d, l.",
                            "* The ancient map, from the crate in the east: Senntisten, with Paddewwa to its west, Carrallanger north of",
                            "* Paddewwa, Dareeyak west of Carrallanger and Lassar south of Dareeyak.",
                            "* The northern gate's lock takes four arrows. Which do you press?"},
                    new String[]{"Left", "Right", "Up", "Down"}, 0, 2, 0, 3),
            lock("sotn_centre_gate", "* The lock doesn't budge. Better read the notes again.",
                    new String[]{"* The strange list names twelve dukes; the strange cipher matches their symbols to letters.",
                            "* Filled in from the list, the Duke note reads: Picus, Quirinius, Sucellus, Mantus, Nemesis, Cacus, Terminus, Ceres,",
                            "* Orcus, Mephitis, Maximus, Festus. Through the cipher (Orcus's symbol reads L), its marked symbols spell",
                            "* a hidden message: 'BOUND BY ...'. The centre gate's lock wants a five-letter word."}, // approx: the cipher told as letters
                    new String[]{"B", "D", "L", "N", "O", "U", "Y"}, 0, 2, 4, 4, 1),
            lock("sotn_braziers", "* The flames gutter out. The braziers must be lit in some order.",
                    new String[]{"* The numbers note, from the damaged table: 3241, with '4: NE' in its corner. The strange cipher's corner says",
                            "* '1: SW', the ancient map's '2: SE', the strange list's '3: NW'.",
                            "* Four unlit braziers stand in the corners of the room. Light them in order."},
                    new String[]{"North-west", "North-east", "South-west", "South-east"}, 0, 3, 1, 2),
            lock("sotn_north_chest", "* The lock doesn't budge. Better read the settlements note against the map again.",
                    new String[]{"* The western chest opens: a jewel shard and a settlements note. Joined on the ancient map as the note lists them,",
                            "* Kharid-et - Kharyrll - Senntisten draws a 7; the next line draws a 4, the next a closed loop, the last a",
                            "* hook and a base like a 2.", // approx: the other three lines' settlements
                            "* The northern chest's lock takes four digits."},
                    digits(), 7, 4, 0, 2));

    /**
     * Desert Treasure II, the keys of Ghorrock Prison in order (wiki walkthrough): the altar room's six candle stands
     * (candles give the digit, the banner's strokes its place: 214013), the administration chests' grid and requisition note
     * (LIES), the dormitory's seven bed chests followed by the magic lantern's arrows (up, right, left, down, right, up) and
     * the strange slider read against the library note's phrase (WRATH).
     */
    static final List<QuestPuzzle> DT2_PRISON_KEYS = List.of(
            lock("dt2_sapphire_key", "* The chest stays shut.",
                    new String[]{"* The warning letter by the altar: 'Count the light, and let the banners keep the order.'",
                            "* Six candle stands, each before a banner: north-east, 2 candles and a banner of 1 stroke; west, 1 candle and",
                            "* 2 strokes; south-west, 4 candles and 3 strokes; north-west, no candles and 4 strokes; east, 1 candle and",
                            "* 5 strokes; south, 3 candles and 6 strokes. The south-east chest wants six digits."}, // approx: only the north-east stand is on the wiki
                    digits(), 2, 1, 4, 0, 1, 3),
            lock("dt2_emerald_key", "* The chest stays shut.",
                    new String[]{"* In the administration crate: a requisition note, its requesters' capitals spelling VIAL, its identifiers 24, 31,",
                            "* 42 and 44 (row, then column); and a grid note of the chests' first letters, read facing west:",
                            "* row 1: R C V H; row 2: B F T L (Lanterns); row 3: I A M N; row 4: P E K S.", // approx: the grid's other letters
                            "* The vial chest (row 1, column 3) is locked by four letters."},
                    new String[]{"A", "E", "I", "L", "S", "T", "V"}, 3, 2, 1, 4),
            lock("dt2_ruby_key", "* The chest stays shut. Jhallan's laughter echoes down the corridor.",
                    new String[]{"* Seven bed chests name their owners. The bottom row, west to east: Atreus, Nithrak, Baelys; Phalina above",
                            "* Baelys; the top row: Grazon above Atreus, Marcus above Nithrak, Vedius at the east end.", // approx: the beds' layout
                            "* Through the code converter, the magic lantern's arrows spell A G V M N B P.",
                            "* The south-east chest takes six arrows."},
                    new String[]{"Up", "Down", "Left", "Right"}, 0, 3, 2, 1, 3, 0),
            lock("dt2_diamond_key", "* The chest stays shut.",
                    new String[]{"* The library note: find Duke Quirinius's books, and remember 'Various people emerged near us'.",
                            "* With Innocence set between Harmonious and Longing, the strange slider pairs its top letters with the bottom:",
                            "* H-K, V-W, L-O, P-R, S-D, E-A, N-T, O-S, U-H.", // approx: the slider told as pairs
                            "* The cell's locked chest wants five letters."},
                    new String[]{"A", "D", "H", "K", "O", "R", "S", "T", "W"}, 8, 5, 0, 7, 2));

    /** A one-room lock: its clues, its dial and the one sequence that opens it. */
    private static QuestPuzzle lock(String key, String failLine, String[] prompt, String[] options, int... solution) {
        return QuestPuzzle.builder(key).room("lock").variant(null).prompt(prompt).options(options).solution(solution)
                .failLines(failLine).build();
    }

    private static String[] digits() {
        return new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    }

    private static void desertTreasureII(QuestBook b) {
        Cast.npc(b, "asgarnia_smith", "Asgarnia Smith", "exam_centre", -3, 3, Cast.FAIR, 0x8A6A3A);
        Cast.npc(b, "quest_ancient_guardian_vault", "Ancient Guardian", "quest_the_vault", 0, 3, 0x9A9AA0, 0x5A3A6A);
        b.item("vardorvis_medallion_piece", "Vardorvis' medallion", "amulet", 0x8A2A2A, "A piece of the ancient medallion, taken from Vardorvis.");
        b.item("sucellus_medallion_piece", "Sucellus' medallion", "amulet", 0x2A8A8A, "A piece of the ancient medallion, taken from Duke Sucellus.");
        b.item("whisperer_medallion_piece", "Whisperer's medallion", "amulet", 0x2A2A8A, "A piece of the ancient medallion, taken from the Whisperer.");
        // OSRS: the Scar's piece is Perseriya's medallion (the Leviathan's keeper); the id is kept for saves.
        b.item("leviathan_medallion_piece", "Perseriya's medallion", "amulet", 0x2A6A2A, "A piece of the ancient medallion, belonging to Perseriya.");
        b.items(s -> s.add("ring_of_shadows", "Ring of shadows", Kind.SIMPLE, "ring", 0x3A2A4A).value(1).stack(1)
                .examine("A ring that reveals the shadow realm and teleports to the four strongholds of the old empire."));
        b.spot("dt2_digsite_east_winch", "Digsite east winch", 3384, 3418); // quest-helper: the winch
        // The Ancient Guardian under the Digsite: OSRS Wiki infobox.
        b.boss(BossDef.of("dt2_ancient_guardian", "Ancient Guardian").level(153).stats(100, 180, 110, 100, 100, 1).speed(5)
                .attack(AttackDef.of("slam", AttackType.CRUSH, 12, 2.5, 10))
                .drops(Drop.always("bones")).skin(0x9A9AA0, 0x5A3A6A, 0x3A2A4A, 0x8A8A90).size(1.0f, 2.6f, 1.2f)
                .at("dt2_digsite_east_winch").arena(8, "sand"));
        dt2Chapters(b);
        b.quest("desert_treasure_ii", "Desert Treasure II - The Fallen Empire", Difficulty.GRANDMASTER, 5)
                .reqQuest("desert_treasure_i", "secrets_of_the_north", "enakhras_lament", "temple_of_the_eye", "the_garden_of_death",
                        "below_ice_mountain", "his_faithful_servants")
                .req(Skill.FIREMAKING, 75).req(Skill.MAGIC, 75).req(Skill.THIEVING, 70).req(Skill.HERBLORE, 62)
                .req(Skill.RUNECRAFT, 60).req(Skill.CONSTRUCTION, 60)
                // OSRS: three ancient lamps of 100,000 XP each in a combat skill or Prayer (level 60+) (QuestLamps)
                .reward("ring_of_shadows")
                .unlock("Awakened versions of Vardorvis, Duke Sucellus, the Whisperer and the Leviathan; the Lassar Undercity")
                .unlock("Access to the Scar essence mine").unlock("The ability to wear ancient rings")
                .stage("I can start this quest by speaking to Asgarnia Smith at the Digsite Exam Centre. (Requires Desert Treasure I, Secrets of the North, Enakhra's Lament, Temple of the Eye, The Garden of Death, Below Ice Mountain, His Faithful Servants, 75 Firemaking, 75 Magic and more.)")
                .talk("asgarnia_smith").say("A vault of the old Zarosian empire has been uncovered in the desert! I need someone brave, and preferably expendable, to open it.")
                .stage("I should investigate the Vault in the Kharidian Desert.")
                .reach("quest_the_vault", 8).say("* The vault door bears four empty sockets, shaped like pieces of a medallion. An ancient guardian stirs.")
                // OSRS "The Ancient Guardian": Terry Balando, the east winch, Dr Banikan's rocks (pickaxe), the guardian, then the golem machine
                // charged by burst spells on the four totems.
                .stage("I should find out who built the vault: Terry Balando at the Exam Centre, then the military installation under the Digsite.")
                .talk("terry_balando").stay().unlessFlag("dt2_terry").flag("dt2_terry")
                .say("Statues of four generals and a plaque in Zarosian? Dr Banikan has found an old military installation beneath the Digsite. Take the east winch.")
                .reach("dt2_digsite_east_winch", 8).stay().needFlag("dt2_terry").unlessFlag("dt2_rocks").needTool("pickaxe").flag("dt2_rocks").spawn("dt2_ancient_guardian")
                .say("* Down the winch, Dr Banikan points at a rockfall. You mine through it... and something huge stirs in the crevice!")
                .missing("* Dr Banikan's rockfall blocks the way. You'll need a pickaxe.")
                .kill("dt2_ancient_guardian").stay().needFlag("dt2_rocks").unlessFlag("dt2_guardian").flag("dt2_guardian")
                .say("* The Ancient Guardian crumbles. Behind it lies an old golem machine.")
                .talk("quest_ancient_guardian_vault").needFlag("dt2_guardian").reqSkill(Skill.MAGIC, 75).take("death_rune:4", "chaos_rune:4")
                .missing("* The golem machine's cells are empty. Burst spells on the four totems (death and chaos runes) would charge them.")
                .say("* You charge the cells with burst spells on the totems and set the golem running. It speaks in an ancient voice:",
                        "The medallion was split among the four strongholds of the empire: the Stranglewood, Ghorrock, Lassar and the Scar. Their keepers have... changed.")
                .stage("I must recover the four medallion pieces: Vardorvis' in the Stranglewood, Perseriya's in the Scar, Sucellus' in Ghorrock Prison and the Whisperer's in the Lassar Undercity.")
                // ---- Carnifex Maximus Vardorvis (OSRS walkthrough) ----
                .talk("dt2_barus").stay().unlessFlag("dt2_v_barus").flag("dt2_v_barus")
                .say("Kasonde? He came through here talking about the Stranglewood. He left something in the house south of the Hosidius estate agent.")
                .reach("dt2_hosidius_house", 4).stay().needFlag("dt2_v_barus").unless("dt2_strange_potion").unlessFlag("dt2_v_boat").give("dt2_strange_potion")
                .say("* On the desk: a strange potion and a note. 'Drink before boarding. It keeps the strangling at bay... for a while.'")
                .reach("dt2_stranglewood_dock", 5).stay().unlessFlag("dt2_v_boat").take("dt2_strange_potion").flag("dt2_v_boat")
                .say("* You drink the strange potion and board the boat. Through the mist, a dead forest rises: the Stranglewood.")
                .missing("* The boatman won't take you without protection from the strangling. (Search the house south of the Hosidius estate agent.)")
                .talk("kasonde").stay().needFlag("dt2_v_boat").unlessFlag("dt2_v_kasonde").flag("dt2_v_kasonde").spawn("dt2_strangled")
                .say("You came! Down here, quick - the strangled are coming. Build the barricades, light the satchels, keep them off me!")
                .kill("dt2_strangled", 4).stay().needFlag("dt2_v_kasonde").unlessFlag("dt2_v_defended").flag("dt2_v_defended")
                .say("* The last of the strangled falls back. Kasonde catches his breath.",
                        "@Kasonde: A serum. Korbal herb from the outcrop with the boar, argian berries from the south-west beach. Then we can walk among them.")
                .reach("dt2_korbal_outcrop", 4).stay().needFlag("dt2_v_defended").unless("dt2_korbal_herb").unlessFlag("dt2_v_serum").give("dt2_korbal_herb")
                .say("* On a small outcrop, past a wild boar, you pick a korbal herb.")
                .reach("dt2_argian_beach", 4).stay().needFlag("dt2_v_defended").unless("dt2_argian_berries").unlessFlag("dt2_v_serum").give("dt2_argian_berries")
                .say("* By the old campfire on the south-west beach, past a lynx, you pick argian berries.")
                .talk("kasonde").stay().needFlag("dt2_v_defended").unlessFlag("dt2_v_serum").take("dt2_korbal_herb", "dt2_argian_berries").flag("dt2_v_serum")
                .say("* In the great pyramid you add the korbal herb and the argian berries to Kasonde's unfinished serum and drink it.",
                        "The strangled won't root you now. Vardorvis waits at the ritual site to the west. Bring me the temple key he guards.")
                .missing("The serum needs a korbal herb and argian berries.")
                .reach("dt2_vardorvis_square", 6).stay().needFlag("dt2_v_serum").unlessFlag("dt2_v_vardorvis").spawn("dt2_quest_vardorvis")
                .say("* At the ritual site, a hulking figure with a parasitic head drags his axe through the snow: Vardorvis!")
                .kill("dt2_quest_vardorvis").stay().needFlag("dt2_v_serum").unlessFlag("dt2_v_vardorvis").flag("dt2_v_vardorvis")
                .say("* Vardorvis collapses. You pick up the temple key he carried.")
                .talk("kasonde").stay().needFlag("dt2_v_vardorvis").unlessFlag("dt2_v_betrayal").flag("dt2_v_betrayal").spawn("dt2_kasonde")
                .say("The key! At last the medallion is... mine. Did you really think I'd share the empire's secrets?")
                .kill("dt2_kasonde").stay().needFlag("dt2_v_betrayal").unlessFlag("dt2_v_medallion").flag("dt2_v_medallion").give("vardorvis_medallion_piece")
                .say("* Kasonde yields. He tells you of the tatty page in the temple; it leads back to the stone chest in the barricaded hideout.",
                        "* In the chest you find Vardorvis' medallion.")
                // ---- Praefectus Classis Perseriya: the Scar ----
                .talk("wizard_persten").stay().unlessFlag("dt2_l_persten").flag("dt2_l_persten")
                .say("You came through the Catalytic Guardian too? Perseriya's ships lie beyond three passages. Solve them all before you leave, or the Scar resets them.")
                .reach("dt2_scar_central", 5).stay().needFlag("dt2_l_persten").unlessFlag("dt2_l_central").flag("dt2_l_central")
                .say("* In the central passage you guide the abyssal pathways until the doors seal shut behind you.")
                .reach("dt2_scar_northwest", 5).stay().needFlag("dt2_l_persten").unlessFlag("dt2_l_northwest").need("tinderbox").flag("dt2_l_northwest")
                .say("* In the north-west passage you light the braziers one by one, holding your breath against the choking smoke.")
                .missing("* The braziers here need lighting. You'll need a tinderbox.")
                .reach("dt2_scar_southeast", 5).stay().needFlag("dt2_l_persten").unlessFlag("dt2_l_southeast").flag("dt2_l_southeast")
                .say("* In the south-east passage you clear the growths from the pipes while scarred demons claw at you.")
                .reach("dt2_perseriya_ship", 6).stay().needFlag("dt2_l_central", "dt2_l_northwest", "dt2_l_southeast").unlessFlag("dt2_l_leviathan")
                .spawn("dt2_quest_leviathan")
                .say("* The passage opens onto Perseriya's sunken ship. The water heaves: the Leviathan rises from the abyss!")
                .kill("dt2_quest_leviathan").stay().needFlag("dt2_l_central").unlessFlag("dt2_l_leviathan").flag("dt2_l_leviathan")
                .say("* The Leviathan sinks back into the depths.")
                .talk("wizard_persten").stay().needFlag("dt2_l_leviathan").unlessFlag("dt2_l_medallion").flag("dt2_l_medallion").give("leviathan_medallion_piece")
                .say("You killed it? Then the debris on the little island is safe to search.", "* Among the debris you find Perseriya's medallion.")
                // ---- Duke Sucellus: Ghorrock Prison ----
                .reach("dt2_ghorrock_cell", 6).stay().unlessFlag("dt2_d_escape").flag("dt2_d_escape")
                .say("* Beneath Weiss you reach the prison door - and are thrown into a cell. A voice in the wall talks you through it:",
                        "* a prisoner's letter in the skeleton, a knife in the bed, a lockpick in the bucket... and the door swings open.",
                        "* A hooded assassin watches you leave, then vanishes.")
                // the prison's four keys, in the walkthrough's order (DT2_PRISON_KEYS)
                .puzzle("dt2_ghorrock_refugee_camp", DT2_PRISON_KEYS.get(0)).stay().needFlag("dt2_d_escape").unlessFlag("dt2_d_sapphire", "dt2_d_keys")
                .flag("dt2_d_sapphire").say("* In the chest: the sapphire key. The gate north of the main block opens.")
                .puzzle("dt2_ghorrock_refugee_camp", DT2_PRISON_KEYS.get(1)).stay().needFlag("dt2_d_sapphire").unlessFlag("dt2_d_emerald", "dt2_d_keys")
                .flag("dt2_d_emerald").say("* In the vial chest: the emerald key. As you leave the room, Jhallan begins to hunt you.")
                .puzzle("dt2_ghorrock_refugee_camp", DT2_PRISON_KEYS.get(2)).stay().needFlag("dt2_d_emerald").unlessFlag("dt2_d_ruby", "dt2_d_keys")
                .flag("dt2_d_ruby").say("* In the chest: the ruby key.")
                .puzzle("dt2_ghorrock_refugee_camp", DT2_PRISON_KEYS.get(3)).stay().needFlag("dt2_d_ruby").unlessFlag("dt2_d_keys").flag("dt2_d_keys")
                .say("* In the chest: the diamond key. With the sapphire, emerald, ruby and diamond keys every gate of the prison opens to you.")
                .talk("dt2_assassin").stay().needFlag("dt2_d_keys").unlessFlag("dt2_d_jhallan").flag("dt2_d_jhallan")
                .say("You found your gear. Good. Light the firecrackers in the refugee camp and keep Jhallan busy until I return.",
                        "* Jhallan's illusions surround you. You pray against his magic and circle the pillar until the assassin returns and Jhallan withdraws.")
                .reach("dt2_ghorrock_asylum", 6).stay().needFlag("dt2_d_jhallan").unlessFlag("dt2_d_poisons").needTool("pickaxe").need("pestle_and_mortar")
                .flag("dt2_d_poisons").spawn("dt2_quest_duke_sucellus")
                .say("* Past the extremities' gaze you grind arder and musca mushrooms, mine salt and brew two poisons in the vats.",
                        "* You pour them on the sleeping Duke. Duke Sucellus wakes!")
                .missing("* You'll need a pickaxe for the salt and a pestle and mortar for the mushrooms (there are spares by the vats).")
                .kill("dt2_quest_duke_sucellus").stay().needFlag("dt2_d_poisons").unlessFlag("dt2_d_medallion").flag("dt2_d_medallion").give("sucellus_medallion_piece")
                .say("* Duke Sucellus slumps into the ice. With his key you open the chest in the west room, where the assassin waits: Sucellus' medallion.")
                // ---- The Whisperer: the Lassar Undercity ----
                .talk("dt2_ramarno").stay().unlessFlag("dt2_w_ramarno").flag("dt2_w_ramarno")
                .say("A sinkhole opened in the north-west of the ruins. Prescott has a rope long enough, if you're mad enough to climb down.")
                .talk("dt2_prescott").stay().needFlag("dt2_w_ramarno").unless("dt2_very_long_rope").unlessFlag("dt2_w_rope").give("dt2_very_long_rope")
                .say("A very long rope, for a very deep hole. Don't tell Ramarno I lent it.")
                .reach("dt2_camdozaal_sinkhole", 4).stay().needFlag("dt2_w_ramarno").unlessFlag("dt2_w_rope").take("dt2_very_long_rope").flag("dt2_w_rope")
                .say("* You tie the very long rope to the rock and climb down into the drowned city of Lassar.")
                .talk("dt2_ketla").stay().needFlag("dt2_w_rope").unlessFlag("dt2_w_fragment").flag("dt2_w_fragment")
                .say("A living soul! Take this blackstone fragment; it pulls you out of the Shadow Realm. Bring me schematics and I'll build you what you need.")
                .reach("dt2_lassar_science", 6).stay().needFlag("dt2_w_fragment").unlessFlag("dt2_w_torches").flag("dt2_w_torches")
                .say("* With Ketla's shadow blocker, idol and torches you pick the shadow locks of the Science and Residential districts,",
                        "* burning the tentacles and gathering the green, purple, blue and white shadow keys.")
                .reach("dt2_lassar_palace", 6).stay().needFlag("dt2_w_torches").unlessFlag("dt2_w_icon").flag("dt2_w_icon")
                .say("* In the Palace you light the four braziers in the Shadow Realm and take the perfected shadow torch schematic.",
                        "* With the red key you find both halves of the icon segment and join them into a strange icon.")
                .talk("dt2_ketla").stay().needFlag("dt2_w_icon").unlessFlag("dt2_w_perfected").flag("dt2_w_perfected")
                .say("The strange icon fits the ancient pillar below the drain... There. Your perfected shadow torch. The cathedral's tentacles won't stop you now.")
                .reach("dt2_sunken_cathedral", 6).stay().needFlag("dt2_w_perfected").unlessFlag("dt2_w_whisperer").spawn("dt2_quest_whisperer")
                .say("* You burn through the tentacles and wade into the Sunken Cathedral. The water stirs, and something sings: the Whisperer!")
                .kill("dt2_quest_whisperer").stay().needFlag("dt2_w_perfected").unlessFlag("dt2_w_whisperer").flag("dt2_w_whisperer")
                .give("whisperer_medallion_piece").say("* The Whisperer dissolves into shadow, leaving the Whisperer's medallion in the water.")
                .talk("quest_ancient_guardian_vault").need("vardorvis_medallion_piece", "sucellus_medallion_piece", "whisperer_medallion_piece", "leviathan_medallion_piece")
                .say("All four pieces. Place them on the statues, and the vault will open.")
                .missing("The medallion is incomplete. Vardorvis, Perseriya, Duke Sucellus and the Whisperer each hold a piece.")
                // ---- The Desert Treasure: the Mysterious Figure and Sliske's wights ----
                .stage("I should place the four medallions on the statues of the generals and open the vault.")
                .reach("quest_the_vault", 6).stay().unlessFlag("dt2_f_trap")
                .take("vardorvis_medallion_piece", "sucellus_medallion_piece", "whisperer_medallion_piece", "leviathan_medallion_piece").flag("dt2_f_trap")
                .spawn("dt2_mysterious_figure")
                .say("* As the last medallion settles, the floor gives way - a trap. You wake in a cell, pick the lock with a hair clip from the bed",
                        "* and take your things from the chest. At the altar to the west, a mysterious figure is waiting.")
                .kill("dt2_mysterious_figure").stay().needFlag("dt2_f_trap").unlessFlag("dt2_f_figure").flag("dt2_f_figure")
                .say("* The figure staggers and vanishes in a swirl of shadow. The vault stands open.")
                .reach("quest_the_vault", 6).stay().needFlag("dt2_f_figure").unlessFlag("dt2_f_wights").flag("dt2_f_wights").spawn("dt2_forsaken_assassin")
                .say("* Inside the vault, Sliske's voice echoes: 'Did you think you were the only ones looking?' Shadowy copies of your allies rise.")
                .kill("dt2_forsaken_assassin").stay().needFlag("dt2_f_wights").unlessFlag("dt2_f_w1").flag("dt2_f_w1").spawn("dt2_ketla_the_unworthy")
                .say("* The Forsaken Assassin falls. Ketla the Unworthy steps out of the dark, her clones fanning out around her.")
                .kill("dt2_ketla_the_unworthy").stay().needFlag("dt2_f_w1").unlessFlag("dt2_f_w2").flag("dt2_f_w2").spawn("dt2_kasonde_the_craven")
                .say("* Ketla fades. Kasonde the Craven charges, hurling bottles of corruption.")
                .kill("dt2_kasonde_the_craven").stay().needFlag("dt2_f_w2").unlessFlag("dt2_f_w3").flag("dt2_f_w3").spawn("dt2_persten_the_deceitful")
                .say("* Kasonde crumbles. Persten the Deceitful tears open a portal of leeches.")
                .kill("dt2_persten_the_deceitful").needFlag("dt2_f_w3")
                .say("* The last of Sliske's wights dissolves. In the heart of the vault lies the empire's secret... and an empty space where something was taken.")
                .stage("I should tell Azzanadra in the Jaldraocht Pyramid what happened at the vault.")
                .talk("azzanadra").say("The empire stirs. You have done Zaros a great service, and earned a great warning. Keep this ring of shadows; it will guide you back to the four strongholds.");
    }

    /**
     * Desert Treasure II's chapters (OSRS walkthrough): helpers, quest items and the quest versions of the four bosses
     * (osrs-dps-calc "Quest" rows), the Mysterious Figure and Sliske's wights. The quest bosses reuse the real bosses'
     * scripts (registered by the bosses module) with lower max hits.
     */
    private static void dt2Chapters(QuestBook b) {
        // quest-helper (DesertTreasureII VardorvisSteps): Barus by the burning man in south-west Lovakengj (1459, 3782), Kasonde's
        // desk (1781, 3619), the Stranglewood boat (1227, 3470, on the river: the spot is on the bank beside it), Kasonde and his
        // hideout's entry (1191, 3404), the korbal herb (1155, 3447), the argian berry bush (1126, 3323), the temple entry
        // (1174, 3428; Kasonde turns on the player in its room) and Vardorvis at the ritual site (1129, 3419)
        b.spot("dt2_burning_man", "The Burning Man", 1459, 3782);
        b.spot("dt2_hosidius_house", "House south of the Hosidius estate agent", 1781, 3619);
        b.spot("dt2_stranglewood_dock", "Stranglewood boat dock", 1230, 3470);
        b.spot("dt2_kasonde_hideout", "Kasonde's hideout, Stranglewood", 1191, 3404);
        b.spot("dt2_korbal_outcrop", "Stranglewood korbal outcrop", 1155, 3447);
        b.spot("dt2_argian_beach", "Stranglewood south-west beach", 1126, 3323);
        b.spot("dt2_stranglewood_temple", "Stranglewood temple", 1174, 3428);
        b.spot("dt2_vardorvis_square", "Stranglewood ritual site", 1129, 3419);
        b.spot("dt2_scar_central", "The Scar, central passage", 3410, 2805); // approx
        b.spot("dt2_scar_northwest", "The Scar, north-west passage", 3395, 2820); // approx
        b.spot("dt2_scar_southeast", "The Scar, south-east passage", 3435, 2775); // approx
        b.spot("dt2_perseriya_ship", "Perseriya's ship", 3450, 2810); // approx
        b.spot("dt2_ghorrock_cell", "Ghorrock Prison cells", 2900, 3975); // approx: beneath Weiss
        b.spot("dt2_ghorrock_refugee_camp", "Ghorrock Prison refugee camp", 2925, 3990); // approx
        b.spot("dt2_ghorrock_asylum", "Ghorrock Prison asylum", 2960, 3990); // approx: behind the dragonstone gate
        b.spot("dt2_camdozaal_sinkhole", "Camdozaal sinkhole", 2980, 3510); // approx: north-west of the ruins
        b.spot("dt2_lassar_science", "Lassar Undercity Science District", 3340, 3445); // approx: beside the mod's Lassar Undercity
        b.spot("dt2_lassar_palace", "Lassar Undercity Palace", 3365, 3455); // approx
        b.spot("dt2_sunken_cathedral", "Sunken Cathedral", 3385, 3420); // approx
        Cast.npc(b, "dt2_barus", "Barus", "dt2_burning_man", 2, 2, Cast.TAN, 0x7A3A2A);
        Cast.npc(b, "dt2_assassin", "Assassin", "dt2_ghorrock_refugee_camp", 2, 2, Cast.PALE, 0x1A1A1A);
        Cast.npc(b, "dt2_ramarno", "Ramarno", "camdozaal", 3, 2, 0x9A8A70, 0x5A4A3A);
        Cast.npc(b, "dt2_prescott", "Prescott", "camdozaal", -3, -3, Cast.FAIR, 0x3A4A6A);
        Cast.npc(b, "dt2_ketla", "Ketla", "dt2_lassar_science", 3, 0, 0xB0A0C0, 0x3A2A5A);
        b.item("dt2_strange_potion", "Strange potion", "potion", 0x5A8A3A, "Kasonde's potion. It should keep the strangling at bay.");
        b.item("dt2_korbal_herb", "Korbal herb", "herb", 0x4A7A2A, "A korbal herb from the Stranglewood.");
        b.item("dt2_argian_berries", "Argian berries", "seed", 0x8A2A4A, "Argian berries from the Stranglewood's south-west beach.");
        b.item("dt2_very_long_rope", "Very long rope", "rope", 0xA08A5A, "A very long rope. Long enough for a very deep hole.");

        // Quest versions (osrs-dps-calc "Quest" rows: level, hitpoints, levels, defence bonuses, melee max hit).
        b.boss(BossDef.of("dt2_quest_vardorvis", "Vardorvis").level(572).stats(500, 260, 270, 215, 180, 1).speed(5) // str/def approx (they scale with his hp)
                .defenceBonus(150, 25, 50, 560, 560)
                .attack(AttackDef.of("axe_swing", AttackType.SLASH, 37, 4, 6))
                .attack(AttackDef.of("swinging_axes", AttackType.SLASH, 43, 16, 2).special(Special.SKY_STRIKE, 0) // approx
                        .telegraph(3, "Axes swing out from the walls!").particle("minecraft:sweep_attack"))
                .drops(Drop.always("bones")).model("vardorvis").size(0.9f, 2.4f, 1.6f).moveSpeed(0.28)
                .at("dt2_vardorvis_square").arena(12, "crypt"));
        b.boss(BossDef.of("dt2_quest_leviathan", "The Leviathan").level(593).stats(720, 250, 270, 200, 130, 130).speed(4)
                .defenceBonus(200, 150, 180, 220, 50)
                .attack(AttackDef.of("shadow_strike", AttackType.CRUSH, 34, 16, 1).particle("minecraft:squid_ink"))
                .attack(AttackDef.of("boulder", AttackType.RANGED, 16, 16, 1).particle("minecraft:crit")) // wiki quest version: ranged orb 16
                .attack(AttackDef.of("abyssal_orb", AttackType.MAGIC, 22, 16, 1).particle("minecraft:witch")) // magic orb 22
                .drops(Drop.always("bones")).model("the_leviathan").size(2.2f, 2.2f, 2.0f).moveSpeed(0.0)
                .at("dt2_perseriya_ship").arena(14, "water"));
        b.boss(BossDef.of("dt2_quest_duke_sucellus", "Duke Sucellus").level(538).stats(330, 250, 280, 215, 230, 1).speed(6)
                .defenceBonus(210, 30, 160, 400, 300)
                .attack(AttackDef.of("fist_slam", AttackType.CRUSH, 36, 5, 3))
                .attack(AttackDef.of("magic_blast", AttackType.MAGIC, 28, 16, 4).particle("minecraft:witch")) // wiki quest version: 28
                .drops(Drop.always("bones")).model("duke_sucellus").size(1.8f, 2.4f, 2.0f).moveSpeed(0.0)
                .at("dt2_ghorrock_asylum").arena(13, "ice"));
        b.boss(BossDef.of("dt2_quest_whisperer", "The Whisperer").level(587).stats(660, 240, 250, 200, 130, 130).speed(5)
                .defenceBonus(150, 250, 180, -10, 250)
                .attack(AttackDef.of("soul_bolt", AttackType.MAGIC, 26, 16, 3).particle("minecraft:soul")) // wiki quest version: magic 26
                .attack(AttackDef.of("tentacle_lash", AttackType.RANGED, 28, 16, 3).particle("minecraft:squid_ink")) // ranged 28
                .phase(50, "The Whisperer drags you into the Shadow Realm!", 1.0, false)
                .drops(Drop.always("bones")).model("the_whisperer").flying().size(1.8f, 2.6f, 2.0f).moveSpeed(0.18)
                .at("dt2_sunken_cathedral").arena(13, "void"));
        // Strangled (level 138) defending Kasonde's hideout, and Kasonde's betrayal (level 231).
        b.boss(BossDef.of("dt2_strangled", "Strangled").level(138).stats(40, 110, 50, 30, 50, 1).speed(4)
                .defenceBonus(30, 20, 30, 10, 0)
                .attack(AttackDef.of("strangle", AttackType.STAB, 3, 2.5, 6).special(Special.FREEZE, 3)) // typeless roots in OSRS
                .drops(Drop.always("bones")).skin(0x4A5A3A, 0x3A4A2A, 0x2A3A1A, 0x6A8A4A).size(0.7f, 1.9f, 1.0f)
                .at("dt2_kasonde_hideout").arena(8, "jungle"));
        b.boss(BossDef.of("dt2_kasonde", "Kasonde").level(231).stats(240, 190, 190, 90, 90, 190).speed(3)
                .defenceBonus(30, 30, 30, 250, 50)
                .attack(AttackDef.of("crossbow", AttackType.RANGED, 20, 10, 5))
                .attack(AttackDef.of("poison_vial", AttackType.RANGED, 15, 10, 2).special(Special.VENOM, 6).particle("minecraft:item_slime")) // approx
                .attack(AttackDef.of("explosive_vial", AttackType.RANGED, 15, 10, 1).special(Special.HAZARD, 4)
                        .telegraph(2, "Kasonde hurls an explosive vial!").particle("minecraft:flame")) // approx
                .drops(Drop.always("bones")).skin(Cast.BROWN, 0x5A6A3A, 0x3A4A2A, 0x2A2A2A).size(0.6f, 1.9f, 1.0f)
                .at("dt2_stranglewood_temple").arena(9, "jungle"));
        // The Desert Treasure: the Mysterious Figure (271) and Sliske's four wights.
        b.boss(BossDef.of("dt2_mysterious_figure", "Mysterious Figure").level(271).stats(450, 120, 120, 120, 120, 120).speed(4)
                .defenceBonus(20, 40, 60, 30, 20)
                .attack(AttackDef.of("shadow_strike", AttackType.SLASH, 18, 2.5, 4))
                .attack(AttackDef.of("shadow_bolt", AttackType.MAGIC, 18, 12, 3).particle("minecraft:squid_ink"))
                .attack(AttackDef.of("shadow_orbs", AttackType.MAGIC, 18, 12, 1).special(Special.FREEZE, 3)
                        .telegraph(2, "The figure freezes you and launches dark orbs - move!").particle("minecraft:portal")) // approx
                .drops(Drop.always("bones")).skin(0x2A2A2A, 0x1A1A2A, 0x101018, 0x6A2A8A).size(0.6f, 1.9f, 1.0f)
                .at("quest_the_vault").arena(10, "void"));
        b.boss(BossDef.of("dt2_forsaken_assassin", "The Forsaken Assassin").level(252).stats(200, 160, 160, 140, 140, 160).speed(4)
                .defenceBonus(60, 30, 80, 200, 50)
                .attack(AttackDef.of("blade", AttackType.SLASH, 20, 2.5, 4))
                .attack(AttackDef.of("knives", AttackType.RANGED, 20, 10, 3))
                .attack(AttackDef.of("poison_bottle", AttackType.RANGED, 12, 10, 1).special(Special.POISON, 6).particle("minecraft:item_slime")) // approx
                .drops(Drop.always("bones")).skin(Cast.PALE, 0x1A1A1A, 0x101010, 0x3A2A4A).size(0.6f, 1.9f, 1.0f)
                .at("quest_the_vault").arena(10, "void"));
        b.boss(BossDef.of("dt2_ketla_the_unworthy", "Ketla the Unworthy").level(236).stats(200, 1, 1, 130, 130, 160).speed(3)
                .defenceBonus(60, 30, 80, 200, 70)
                .attack(AttackDef.of("shadow_arrow", AttackType.RANGED, 18, 10, 5))
                .attack(AttackDef.of("shadow_clones", AttackType.RANGED, 18, 10, 1).special(Special.SUMMON, 3)
                        .telegraph(2, "Ketla splits into shadow clones!")) // approx
                .minion("minecraft:vex", 3)
                .drops(Drop.always("bones")).skin(0xB0A0C0, 0x3A2A5A, 0x2A1A3A, 0x6A4A8A).size(0.6f, 1.9f, 1.0f)
                .at("quest_the_vault").arena(10, "void"));
        b.boss(BossDef.of("dt2_kasonde_the_craven", "Kasonde the Craven").level(221).stats(200, 200, 200, 120, 120, 200).speed(3)
                .defenceBonus(30, 30, 30, 250, 50)
                .attack(AttackDef.of("crossbow", AttackType.RANGED, 22, 10, 4))
                .attack(AttackDef.of("slam", AttackType.CRUSH, 22, 2.5, 3))
                .attack(AttackDef.of("corruption_bottle", AttackType.RANGED, 15, 10, 1).special(Special.HAZARD, 4)
                        .telegraph(2, "Kasonde hurls a bottle of corruption!")) // approx
                .drops(Drop.always("bones")).skin(Cast.BROWN, 0x3A3A2A, 0x2A2A1A, 0x6A2A2A).size(0.6f, 1.9f, 1.0f)
                .at("quest_the_vault").arena(10, "void"));
        b.boss(BossDef.of("dt2_persten_the_deceitful", "Persten the Deceitful").level(264).stats(200, 1, 1, 130, 250, 1).speed(4)
                .defenceBonus(70, 70, 70, 200, 20)
                .attack(AttackDef.of("shadow_blast", AttackType.MAGIC, 32, 12, 5).particle("minecraft:witch"))
                .attack(AttackDef.of("lightning", AttackType.MAGIC, 25, 12, 1).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "Lightning crackles around Persten - find the open tiles!").particle("minecraft:electric_spark")) // approx
                .attack(AttackDef.of("leech_portal", AttackType.MAGIC, 10, 12, 1).special(Special.SUMMON, 3)
                        .telegraph(2, "Persten opens a portal - leeches pour out!")) // approx
                .minion("minecraft:silverfish", 3)
                .drops(Drop.always("bones")).skin(Cast.PALE, 0x2A2A6A, 0x1A1A4A, 0xE0E0E0).size(0.6f, 1.9f, 1.0f)
                .at("quest_the_vault").arena(10, "void"));
    }

    /**
     * The Curse of Arrav, after the wiki quick guide and quest-helper's steps (TheCurseOfArrav and its solvers): the Uzer Mastaba
     * (two mastaba keys from the looters' skeletons, the odd markings and the two levers, the golem guard, the coloured floor
     * tiles ({@link #COA_TILES}), the oil-filled canopic jar, the murals, dwellberries and a ring of life), the climb to
     * Zemouregal's fort on Trollweiss Mountain (the Trollheim cave and crevasse, the rubble mined through, Arrav, the tapestry's
     * base plans and key), and the heist on Zemouregal's base east of the Varrock sawmill (the kitchen, the electrified sewers in
     * insulated boots, the decoder strips and code key for the metal doors ({@link #COA_METAL_DOOR}), the pipe crossed with a
     * mith grapple) ending in the fight with Arrav at his heart's pedestal. OSRS fights no Zemouregal here: he only appears in
     * the closing scene.
     */
    private static void curseOfArrav(QuestBook b) {
        // Elias at Zemouregal's base (quest-helper 3341, 3516: east of the Varrock sawmill, north of the Digsite)
        b.npcAt("elias_white", "Elias White", 3341, 3516, 0xE0B89A, 0x6A6A8A, 0x4A4A5B, 0x2A2A2B);
        b.npcAt("elias_white_uzer", "Elias White", 3505, 3037, 0xE0B89C, 0x6A6A8C, 0x4A4A5D, 0x2A2A2D); // south of the ruins of Uzer
        // Arrav in Zemouregal's fort at the top of the Trollweiss cave's stairs (quest-helper 2856, 3871)
        b.npcAt("arrav_trollweiss", "Arrav", 2856, 3871, 0x8AA07B, 0xA08A3A, 0x6A5A2A, 0x5A6A4A);
        // the Uzer Mastaba's entrance on the hill south-west of Elias (3486, 3023); its rooms are an instance in OSRS, so the
        // tomb's steps happen at the entrance
        b.spot("coa_mastaba", "Uzer Mastaba", 3486, 3023);
        b.spot("coa_trollheim_cave", "Cave beside Trollheim", 2821, 3744);
        b.spot("coa_trollweiss_cave", "Trollweiss cave", 2809, 3861);
        b.spot("coa_fort_tapestry", "Zemouregal's fort, tapestry room", 2861, 3865);
        // Zemouregal's base (an instance below its entrance at 3343, 3515)
        b.spot("coa_zemouregals_base", "Zemouregal's base", 3343, 3515);
        b.item("mastaba_key", "Mastaba key", "key", 0xB09060, "A key from a tomb looter's remains.");
        b.item("canopic_jar_oil", "Canopic jar (oil)", "pot_item", 0xC0A060, "An ancient jar filled with oil.");
        b.item("canopic_jar_oil_berries", "Canopic jar (oil and berries)", "pot_item", 0x6A5AA0, "An ancient jar of oil and dwellberries.");
        b.item("canopic_jar_full", "Canopic jar (full)", "pot_item", 0xE0E0F0, "An ancient jar, ready to hold a heart.");
        b.item("base_plans", "Base plans", "note", 0xE0D0A0, "Plans of Zemouregal's base, in some sort of code.");
        b.item("base_key", "Base key", "key", 0x5A5A6A, "The key to Zemouregal's base.");
        b.item("decoder_strips", "Decoder strips", "note", 0xD0C0A0, "Four strips of metal, each with a single window and a number 1 to 4.");
        b.item("code_key", "Code key", "key", 0x8A8A9A, "A key engraved with four letters.");
        b.item("arravs_heart", "Arrav's heart", "orb", 0x8A1A1A, "The heart of Arrav, sealed in a canopic jar.");
        // DPS calculator (Golem guard): level 141, 150 HP, crush up to 18 every 4 ticks, weak to crush; when the screen shakes it
        // stands still and pounds the ground through prayer (wiki: up to 28 three times; one step away avoids it)
        b.boss(BossDef.of("coa_golem_guard", "Golem guard").level(141).stats(150, 170, 170, 100, 170, 1).speed(4)
                .defenceBonus(180, 200, 20, 40, 0)
                .attack(AttackDef.of("slam", AttackType.CRUSH, 18, 2.5, 5))
                // approx: one pound of up to 28 (OSRS: three in succession), a 3-tick shake to step away in
                .attack(AttackDef.of("ground_pound", AttackType.CRUSH, 28, 1.6, 1).special(Special.PRAYER_PIERCING, 0)
                        .telegraph(3, "The ground shakes! The golem guard plants its feet: step away from it!"))
                .drops(Drop.always("bones")).skin(0xB0A080, 0x8A7A5A, 0x6A5A3A, 0xC0B080).size(1.2f, 2.8f, 1.2f));
        // DPS calculator (Arrav, The Curse of Arrav): level 339, 600 HP, crush up to 30 every 5 ticks; every third attack he
        // swings his axe ahead of him for up to 26 (wiki), dodged by stepping beside or behind him (QuestBossScripts.EveryNth)
        // while armoured zombies (10 HP, up to 10) leave venom pools where they go (4 a tick)
        b.boss(BossDef.of("arrav", "Arrav").level(339).stats(600, 220, 220, 150, 150, 1).speed(5)
                .defenceBonus(80, 80, 40, 160, 80)
                .attack(AttackDef.of("axe", AttackType.CRUSH, 30, 2.5, 6))
                .attack(AttackDef.of("raise_zombies", AttackType.MAGIC, 0, 10, 1).special(Special.SUMMON, 2)
                        .telegraph(2, "Armoured zombies claw their way up around Arrav!"))
                // weight 0: only the script's every-third swing
                .attack(AttackDef.of("axe_swing", AttackType.SLASH, 26, 6, 0).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "Arrav raises his axe: get beside or behind him!"))
                .minion("coa_armoured_zombie", 2)
                .model("arrav").drops(Drop.always("bones")).skin(0x8AA07B, 0xA08A3A, 0x6A5A2A, 0x5A6A4A).size(1.0f, 2.6f, 1.1f));
        b.monster(MonsterDef.of("coa_armoured_zombie", "Armoured zombie").level(70).stats(10, 106, 94, 10, 1, 1).speed(4)
                .defenceBonus(5, 5, 5, -50, -100)
                // approx: OSRS leaves venom wherever they walk; here where their claws land
                .attack(AttackDef.of("claw", AttackType.CRUSH, 10, 2.5, 1).special(Special.HAZARD, 4))
                .skin(0x6A7A5A, 0x5A5A60, 0x3A3A40, 0x4A5A3A).size(0.7f, 2.0f, 1.0f));
        b.quest("the_curse_of_arrav", "The Curse of Arrav", Difficulty.MASTER, 2)
                .reqQuest("defender_of_varrock", "troll_romance")
                .req(Skill.MINING, 64).req(Skill.RANGED, 62).req(Skill.THIEVING, 62).req(Skill.AGILITY, 61).req(Skill.STRENGTH, 58).req(Skill.SLAYER, 37)
                .xp(Skill.MINING, 40000).xp(Skill.THIEVING, 40000).xp(Skill.AGILITY, 40000) // wiki: fixed quest XP
                .unlock("Access to Zemouregal's Fort")
                .stage("I can start this quest by speaking to Elias White at the Uzer Oasis. (Requires Defender of Varrock, Troll Romance, 64 Mining, 62 Ranged, 62 Thieving, 61 Agility, 58 Strength, 37 Slayer.)")
                .talk("elias_white_uzer").say("Arrav, the hero of Avarrocka, is still Zemouregal's undead slave. His heart is the key to him.",
                        "To carry a heart you'll need a proper canopic jar. The Mastaba on the hill to the south-west was built for such rites.")
                // ---- stage 1: the Uzer Mastaba ----
                .stage("I should search the Uzer Mastaba south-west of Elias for a canopic jar, and prepare it to hold Arrav's heart. "
                        + "I'll need three dwellberries and a ring of life.")
                .reach("coa_mastaba", 6).stay().unlessFlag("coa_key1").flag("coa_key1").give("mastaba_key")
                .say("* In the cave south of the entrance, the southernmost skeleton of a tomb looter still holds a mastaba key.")
                .reach("coa_mastaba", 6).stay().needFlag("coa_key1").unlessFlag("coa_key2").flag("coa_key2").give("mastaba_key")
                .say("* North-west of the stone boat in the central room, another skeleton: a second mastaba key.")
                .reach("coa_mastaba", 6).stay().needFlag("coa_key2").unlessFlag("coa_lever_south").reqSkill(Skill.THIEVING, 62)
                .take("mastaba_key").flag("coa_lever_south")
                .say("* In the far south-east you search the odd markings on the wall and disarm the spear traps.",
                        "* The key turns in the south lever's lock and you pull it. Something grinds in the walls.")
                .missing("* Odd markings run along the wall by a set of spear traps. Disarming them takes 62 Thieving, and the lever is locked.")
                .reach("coa_mastaba", 6).stay().needFlag("coa_lever_south").unlessFlag("coa_lever_north").reqSkill(Skill.THIEVING, 62)
                .take("mastaba_key").flag("coa_lever_north")
                .say("* East of the imposing doors in the north-east you disarm the traps the same way and pull the north lever.",
                        "* The imposing doors to the west unlock.")
                // raises the golem again whenever the player comes back without having beaten it (a lost fight's boss vanishes)
                .reach("coa_mastaba", 6).stay().needFlag("coa_lever_north").unlessFlag("coa_golem").spawn("coa_golem_guard")
                .say("* You open the imposing doors. A golem guard lumbers to life!")
                .kill("coa_golem_guard").stay().needFlag("coa_lever_north").unlessFlag("coa_golem").flag("coa_golem")
                .say("* The golem guard crumbles. Stairs lead down from its chamber.")
                .puzzle("coa_mastaba", COA_TILES).stay().needFlag("coa_golem").unlessFlag("coa_tiles").flag("coa_tiles")
                .say("* You reach the far side and pull the lever there: the coloured tiles go dark.")
                .reach("coa_mastaba", 6).stay().needFlag("coa_tiles").unlessFlag("coa_jar").flag("coa_jar").give("canopic_jar_oil")
                .say("* On the shelves in the north of the burial chamber stands an oil-filled canopic jar.")
                .reach("coa_mastaba", 6).stay().needFlag("coa_jar").unlessFlag("coa_mural").flag("coa_mural")
                .say("* In the room with the statue of Tumeken the southern mural shows the rite: dwellberries in the oil to bind the "
                        + "heart, then a ring of life to keep it beating.")
                .reach("coa_mastaba", 6).stay().needFlag("coa_mural").unless("canopic_jar_oil_berries", "canopic_jar_full")
                .take("canopic_jar_oil", "dwellberries:3").give("canopic_jar_oil_berries")
                .say("* You drop three dwellberries into the oil.")
                .missing("* The mural's rite wants three dwellberries in the jar first.")
                .reach("coa_mastaba", 6).stay().needFlag("coa_mural").unless("canopic_jar_full").take("canopic_jar_oil_berries", "ring_of_life")
                .give("canopic_jar_full")
                .say("* You lower the ring of life into the jar. The oil shimmers.")
                .missing("* Next the rite wants a ring of life in the jar.")
                .talk("elias_white_uzer").need("canopic_jar_full")
                .say("> I have a canopic jar, prepared as the murals showed.", "Then we only need the heart. Arrav himself may know where "
                        + "Zemouregal keeps it: he's held in Zemouregal's fort, at the top of Trollweiss Mountain.")
                .missing("?!coa_jar|The Mastaba is just south-west of here, on top of the hill. Bring dwellberries and a ring of life.",
                        "?coa_jar|Follow the murals: the jar must be prepared before it can hold a heart.")
                // ---- stage 2: Zemouregal's fort on Trollweiss Mountain ----
                .stage("I should find Arrav in Zemouregal's fort on Trollweiss Mountain: through the cave beside Trollheim, out at the "
                        + "crevasse and into the Trollweiss cave. I'll need a pickaxe.")
                .reach("coa_trollheim_cave", 6).stay().unlessFlag("coa_crevasse").flag("coa_crevasse")
                .say("* You follow the tunnel north past the ice trolls and climb out of a crevasse onto the summit of Trollweiss Mountain.")
                // approx: the rubble's rule (each pile mined once from each side, so some must be walked round) is folded into the
                // four piles' mining
                .reach("coa_trollweiss_cave", 8).stay().needFlag("coa_crevasse").unlessFlag("coa_rubble1").needTool("pickaxe")
                .reqSkill(Skill.MINING, 64).flag("coa_rubble1")
                .say("* Rubble chokes the cave. You mine the first pile from the south, then from the east, and squeeze past.")
                .missing("* Rubble blocks the cave: you need a pickaxe and 64 Mining.")
                .reach("coa_trollweiss_cave", 8).stay().needFlag("coa_rubble1").unlessFlag("coa_rubble2").needTool("pickaxe")
                .reqSkill(Skill.MINING, 64).flag("coa_rubble2")
                .say("* The next chamber's rubble is three piles deep: you work round them from the west, then the north.")
                .reach("coa_trollweiss_cave", 8).stay().needFlag("coa_rubble2").unlessFlag("coa_rubble3").needTool("pickaxe")
                .reqSkill(Skill.MINING, 64).flag("coa_rubble3")
                .say("* Further in you clear a fall of rock from both sides of a narrow bend.")
                .reach("coa_trollweiss_cave", 8).stay().needFlag("coa_rubble3").unlessFlag("coa_rubble4").needTool("pickaxe")
                .reqSkill(Skill.MINING, 64).flag("coa_rubble4")
                .say("* The last rubble gives way. Stairs climb from a room with a red tiled floor.")
                .talk("arrav_trollweiss").stay().needFlag("coa_rubble4").unlessFlag("coa_arrav").flag("coa_arrav")
                .say("> Arrav! I've come to free you.", "Zemouregal keeps my heart in his base below Varrock. While he holds it, I am his.",
                        "There's a tapestry in the next room: he hides more than dust behind it.")
                .missing("* The way up into the fort is still blocked by rubble.")
                .reach("coa_fort_tapestry", 4).stay().needFlag("coa_arrav").unless("base_plans").give("base_plans", "base_key")
                .say("* Behind the tapestry in the south of the room: plans of Zemouregal's base, written in a code, and its key.")
                .talk("elias_white_uzer").need("base_plans", "base_key")
                .say("> Arrav says Zemouregal keeps his heart in his base. I found its plans and key.", "These plans are in a simple "
                        + "cipher... There: the base is north of the Digsite, east of the Varrock sawmill. Meet me at its entrance.",
                        "You'll want a crossbow and a mith grapple, insulated boots for his sewers, and the jar.")
                .missing("?coa_arrav|Arrav spoke of a tapestry? Bring me whatever you find.")
                // ---- stage 3: the heist ----
                .stage("I should meet Elias at Zemouregal's base east of the Varrock sawmill and take Arrav's heart. I'll need the "
                        + "base key, the canopic jar, insulated boots, a crossbow and a mith grapple.")
                .talk("elias_white").stay().unlessFlag("coa_base_elias").need("base_key", "canopic_jar_full").flag("coa_base_elias")
                .say("I'll keep watch out here. The plans say the heart is at the very back. Go carefully.")
                .missing("Have you got the base key and the jar? We won't get a second try.")
                .reach("coa_zemouregals_base", 8).stay().needFlag("coa_base_elias").unlessFlag("coa_kitchen").need("base_key")
                .flag("coa_kitchen")
                .say("* The base key opens the gates. Past the zombies in the halls you reach the kitchen.")
                .reach("coa_zemouregals_base", 8).stay().needFlag("coa_kitchen").unlessFlag("coa_sewers").need("insulated_boots")
                .flag("coa_sewers")
                .say("* In your insulated boots you crawl through the pipe in the kitchen's south wall and follow the electrified sewers to "
                        + "the far end.")
                .missing("* The sewer floor crackles with electricity. Insulated boots would keep you safe.")
                .reach("coa_zemouregals_base", 8).stay().needFlag("coa_sewers").unless("decoder_strips").give("decoder_strips")
                .say("* On the western table of the southern room: four decoder strips.")
                .reach("coa_zemouregals_base", 8).stay().needFlag("coa_sewers").unless("code_key").give("code_key")
                .say("* In the storage room's chest: a code key engraved with four letters.")
                .puzzle("coa_zemouregals_base", COA_METAL_DOOR).stay().needFlag("coa_sewers").need("decoder_strips", "code_key")
                .unlessFlag("coa_doors").flag("coa_doors")
                .say("* The fourth number clicks in. The metal doors slide apart.")
                // approx: OSRS needs a crossbow and a mith grapple equipped (with the grapple's Ranged, Agility and Strength); here
                // the grapple carried and the levels
                .reach("coa_zemouregals_base", 8).stay().needFlag("coa_doors").unlessFlag("coa_pipe").need("mith_grapple")
                .reqSkill(Skill.RANGED, 62).reqSkill(Skill.AGILITY, 61).reqSkill(Skill.STRENGTH, 58).flag("coa_pipe")
                .say("* You fire your mith grapple at the metal bars over the fire traps and swing across the pipe.")
                .missing("* Fire traps guard the floor beyond the doors. A mith grapple could cross on the bars above them.")
                // raises Arrav again whenever the player comes back to the pedestal after a lost fight
                .reach("coa_zemouregals_base", 8).stay().needFlag("coa_pipe").need("canopic_jar_full").spawn("arrav")
                .say("* You reach for Arrav's heart on its pedestal. Arrav steps from the shadows, Zemouregal's will in his eyes!")
                .kill("arrav").needFlag("coa_pipe").take("canopic_jar_full").give("arravs_heart")
                .say("* Arrav falls to his knees. You seal his beating heart in the canopic jar, and Zemouregal's hold on him breaks.",
                        "* Zemouregal himself appears, cursing, and vanishes into the shadows.")
                .missing("* Without the canopic jar there is nothing to hold Arrav's heart.")
                // ---- stage 4 ----
                .stage("I should tell Elias White that Arrav is free.")
                .talk("elias_white").take("arravs_heart").say("Arrav is free. After all these centuries... Varrock owes you more than it knows.");
    }

    /** Coloured floor tiles in the Mastaba: 0 blue, 1 green, 2 yellow, 3 red (wiki: blue goes with green, yellow with red). */
    private static final String[] COA_TILE_COLOURS = {"blue", "green", "yellow", "red"};

    /**
     * The Mastaba's coloured floor (wiki quick guide; quest-helper TilePuzzleSolver): crossing west, every tile stepped on must be
     * of the starting tile's pair, blue and green or yellow and red; a tile of the other pair strikes the player and the crossing
     * starts again. The 12 by 12 floor is three rows of six here, one column a step, onto the same row or the one beside it.
     * // approx: the floor's layouts (OSRS: one fixed floor)
     */
    static final QuestPuzzle COA_TILES = coaTiles();

    private static QuestPuzzle coaTiles() {
        // [row][column], column 0 the east column beside the player
        int[][][] floors = {
                {{0, 2, 1, 3, 2, 0}, {3, 1, 2, 0, 1, 3}, {2, 3, 0, 2, 3, 1}},
                {{2, 0, 0, 3, 1, 2}, {1, 3, 2, 1, 0, 3}, {3, 1, 2, 0, 2, 0}},
                {{3, 1, 2, 2, 0, 1}, {0, 2, 1, 0, 3, 2}, {1, 0, 3, 1, 2, 0}},
                {{1, 3, 0, 2, 2, 3}, {2, 0, 3, 1, 0, 1}, {0, 2, 1, 3, 1, 2}}};
        String[] rows = {"North", "Middle", "South"};
        QuestPuzzle.Builder b = QuestPuzzle.builder("coa_tiles").room("floor");
        for (int[][] floor : floors) {
            List<String> prompt = new java.util.ArrayList<>();
            prompt.add("* Coloured tiles cover the floor west to a lever, three rows wide. From the east (beside you) to the west:");
            for (int r = 0; r < 3; r++) {
                StringBuilder line = new StringBuilder("* " + rows[r] + " row: ");
                for (int c = 0; c < 6; c++) {
                    line.append(c == 0 ? "" : ", ").append(COA_TILE_COLOURS[floor[r][c]]);
                }
                prompt.add(line.toString());
            }
            prompt.add("* A mural by the stairs pairs blue with green and yellow with red. Each step goes one column west, into the "
                    + "same row or the one beside it.");
            b.variant(null).prompt(prompt.toArray(String[]::new))
                    .options("Step west into the north row", "Step west into the middle row", "Step west into the south row");
            List<int[]> paths = coaTilePaths(floor);
            if (paths.isEmpty()) {
                throw new IllegalStateException("coa_tiles: a floor nobody can cross");
            }
            paths.forEach(b::solution);
        }
        return b.failLines("* The tile flares under your feet and throws you back to the eastern edge!").build();
    }

    /** Every crossing of a floor: a row for each column, east to west, each beside the last, all of the first tile's pair. */
    static List<int[]> coaTilePaths(int[][] floor) {
        List<int[]> out = new java.util.ArrayList<>();
        for (int start = 0; start < 3; start++) {
            coaTileWalk(floor, new int[]{start, 0, 0, 0, 0, 0}, 1, floor[start][0] / 2, out);
        }
        return out;
    }

    private static void coaTileWalk(int[][] floor, int[] path, int col, int pair, List<int[]> out) {
        if (floor[path[col - 1]][col - 1] / 2 != pair) {
            return;
        }
        if (col == 6) {
            out.add(path.clone());
            return;
        }
        for (int r = Math.max(0, path[col - 1] - 1); r <= Math.min(2, path[col - 1] + 1); r++) {
            path[col] = r;
            coaTileWalk(floor, path, col + 1, pair, out);
        }
    }

    /**
     * The metal doors' grid of numbers, rows A to I (quest-helper MetalDoorSolver: the numbers never change), and the column (1-9)
     * each decoder strip's window shows: strip 1 the 6th, strip 2 the 4th, strip 3 the 8th, strip 4 the 2nd.
     */
    static final String[] COA_DOOR_GRID = {"3 7 2 9 1 6 5 4 3", "6 5 4 3 2 1 9 0 4", "7 2 1 8 7 6 4 3 2", "9 0 7 6 5 4 3 7 1",
            "9 4 1 3 3 6 2 4 8", "6 2 3 2 4 1 6 9 7", "0 2 1 3 7 2 5 6 3", "9 4 6 3 8 8 0 1 9", "4 9 2 3 1 0 4 9 2"};
    static final int[] COA_STRIP_COLUMN = {6, 4, 8, 2};

    /** The metal doors' four numbers for a code key's four letters: letter n read through strip n's window. */
    static int[] coaDoorPassword(String code) {
        int[] out = new int[4];
        for (int i = 0; i < 4; i++) {
            String[] row = COA_DOOR_GRID[code.charAt(i) - 'A'].split(" ");
            out[i] = Integer.parseInt(row[COA_STRIP_COLUMN[i] - 1]);
        }
        return out;
    }

    /**
     * The metal doors (wiki quick guide; quest-helper MetalDoorSolver): the code key's four letters (random in OSRS, a code per
     * player here) are set on the four decoder strips in order, and each strip's window shows a number of the door's grid; the
     * four numbers open the doors.
     */
    static final QuestPuzzle COA_METAL_DOOR = coaMetalDoor();

    private static QuestPuzzle coaMetalDoor() {
        String[] codes = {"IFCB", "ADGH", "HEBC", "CIDF", "GBEA", "FHAI", "DCIG", "EGFB"};
        QuestPuzzle.Builder b = QuestPuzzle.builder("coa_metal_door").room("doors");
        for (String code : codes) {
            List<String> prompt = new java.util.ArrayList<>();
            prompt.add("* Your code key reads " + code + ". The metal doors' panel is a grid of numbers in rows A to I:");
            for (int r = 0; r < 9; r += 3) {
                prompt.add("* " + (char) ('A' + r) + ": " + COA_DOOR_GRID[r] + "   " + (char) ('A' + r + 1) + ": " + COA_DOOR_GRID[r + 1]
                        + "   " + (char) ('A' + r + 2) + ": " + COA_DOOR_GRID[r + 2]);
            }
            prompt.add("* Each decoder strip has one window: strip 1's shows the 6th number of a row, strip 2's the 4th, strip 3's "
                    + "the 8th and strip 4's the 2nd. Set strip 1 to the code's first letter, strip 2 to its second, and so on, and "
                    + "enter the four numbers in order.");
            b.variant(null).prompt(prompt.toArray(String[]::new)).options("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
                    .solution(coaDoorPassword(code));
        }
        return b.failLines("* The panel buzzes and the numbers reset. The doors stay shut.").build();
    }

    private static void myArmsBigAdventure(QuestBook b) {
        Cast.npc(b, "my_arm", "My Arm", "quest_troll_stronghold_roof", 0, 2, Cast.TROLL, 0x5A4A3A);
        Cast.npc(b, "murcaily", "Murcaily", "tai_bwo_wannai", 3, -3, Cast.BROWN, 0x3A6A2A);
        b.item("goutweed_tuber", "Goutweed tuber", "seed", 0x8A9A4A, "A tuber of the herb goutweed.");
        b.item("mab_goutweedy_lump", "Goutweedy lump", "pot", 0x6A7A3A, "A bucket of the trolls' goutweed stew, gone lumpy.");
        b.boss(BossDef.of("quest_giant_roc", "Giant Roc").level(172).stats(250, 120, 120, 120, 1, 120).speed(5)
                .attack(AttackDef.of("peck", AttackType.STAB, 20, 3, 5))
                .attack(AttackDef.of("boulder_drop", AttackType.RANGED, 18, 10, 3).special(Special.SKY_STRIKE, 0).telegraph(2, "The Giant Roc drops a boulder!"))
                .drops(Drop.always("big_bones")).skin(0x8A6A3A, 0x6A4A2A, 0x5A3A1A, 0xE0C040).size(2.0f, 2.4f, 1.3f).flying()
                .at("quest_troll_stronghold_roof").arena(10, "stone"));
        b.quest("my_arms_big_adventure", "My Arm's Big Adventure", Difficulty.EXPERIENCED, 1)
                .xp(Skill.HERBLORE, 10000).xp(Skill.FARMING, 5000) // OSRS (current quest XP rewards)
                .reqQuest("eadgars_ruse", "the_feud", "jungle_potion").req(Skill.WOODCUTTING, 10).req(Skill.FARMING, 29)
                .unlock("My Arm's herb patch on the Troll Stronghold roof")
                .stage("I can start this quest by speaking to Burntmeat in the Troll Stronghold kitchen. (Requires Eadgar's Ruse, The Feud, Jungle Potion, 10 Woodcutting, 29 Farming.)")
                .talk("burntmeat").say("My Arm want to grow things on roof! Silly troll. You go help him.")
                .stage("I should talk to My Arm on the Troll Stronghold roof.")
                // OSRS: fetch a goutweedy lump from the cooking pot on Death Plateau (past the thrower trolls) with a bucket.
                .reach("death_plateau", 8).stay().take("minecraft:bucket").unless("mab_goutweedy_lump").unlessFlag("mab_lump")
                .give("mab_goutweedy_lump").say("* You scoop the trolls' stew out of the cooking pot on Death Plateau: a goutweedy lump.")
                .missing("* A cooking pot bubbles in the middle of the plateau. You'd need an empty bucket.")
                .talk("my_arm").take("mab_goutweedy_lump").flag("mab_lump")
                .say("Goutweedy lump! My Arm try grow it... *it rots.* Need proper plant, goutweed. Humans in jungle have it. And lots of compost.")
                .missing("My Arm want goutweed! Burntmeat stew pot on Death Plateau got some. Bring in bucket.")
                // OSRS: the soil patch takes 3 buckets of ugthanki dung and 7 supercompost (the dung isn't in the mod: approx).
                .stage("I should fetch a goutweed tuber from Murcaily in Tai Bwo Wannai's hardwood grove, and bring 7 supercompost.")
                .talk("murcaily").stay().unless("goutweed_tuber").give("goutweed_tuber").say("Goutweed for a troll? Ha! Take it, and stay away from my teak.")
                .talk("my_arm").take("goutweed_tuber", "supercompost:7").say("*You and My Arm dig in the supercompost and plant the goutweed in the rooftop patch.* It growing! ...What that shadow?")
                .missing("My Arm need goutweed tuber from jungle man, and 7 supercompost.")
                .stage("A Giant Roc swoops down to protect its nest near the patch! I must defeat it.")
                .kill("quest_giant_roc").flag("mab_roc").say("* The Giant Roc flees, shedding feathers. My Arm's goutweed survives.")
                .stage("I should tell My Arm the patch is safe.")
                .talk("my_arm").needFlag("mab_roc").say("My Arm farmer now! You can use patch too. Thank you, human friend.");
    }

    /**
     * Making Friends with My Arm (wiki): after Snowflake leaves, Mother is talked round by picking every argument: goutweed is
     * delicious, Stronghold trolls are tough, she should respect her daughter, and you love her daughter.
     */
    static final QuestPuzzle MFA_MOTHER = QuestPuzzle.builder("mfa_mother").shuffle().room("mother").variant(null)
            .prompt("* Mother glowers at My Arm. Every good argument might win her round; a bad one will end the talk.")
            .options("Tell him goutweed is delicious", "Tell him how tough Stronghold trolls are", "Tell him to show respect to his daughter",
                    "Tell him you love his daughter", "Insult his cooking", "Challenge him to a fight now")
            .anyOrder(0, 1, 2, 3)
            .failLines("@Mother: Bah! Get out of my sight. *You'll have to start the talk again.*").build();

    private static void makingFriendsWithMyArm(QuestBook b) {
        Cast.npc(b, "snowflake", "Snowflake", "quest_weiss", 0, 3, Cast.TROLL, 0xE0E8F0);
        Cast.npc(b, "quest_odd_mushroom", "Odd Mushroom", "troll_stronghold", -4, -1, 0xC0A0C0, 0x8A4A8A);
        b.item("bucket_of_salt", "Bucket of salt", "pot", 0xF0F0F0, "Salt from the Weiss salt mine.");
        b.item("old_mans_coffin", "Old man's coffin", "log", 0x6A3A1A, "A coffin with a living Wise Old Man inside. Shh.");
        b.item("goat_dung", "Goat dung", "pot", 0x6A5A3A, "A bucket of goat dung. Snowflake wants it for the fire.");
        b.item("weiss_fire_notes", "Weiss fire notes", "note", 0xE0D0B0, "Snowflake's notes on keeping the Weiss fire pit burning.");
        // DPS calculator: ranged and crush, magic defence +200; Mother's attacks only land once the Fire of Domination is doused
        // (QuestBossScripts.MotherOfWeiss).
        b.boss(BossDef.of("dont_know_what", "Don't Know What").level(163).stats(220, 120, 90, 160, 60, 115).speed(3)
                .defenceBonus(0, 0, 0, 200, 0)
                .attack(AttackDef.of("rock_throw", AttackType.RANGED, 10, 8, 6))
                .attack(AttackDef.of("crush", AttackType.CRUSH, 10, 2.5, 4))
                .drops(Drop.always("big_bones")).skin(0x7A7A68, 0x4A4A3A, 0x3A3A2A, 0xC0C8D0).size(1.2f, 2.8f, 1.2f));
        b.boss(BossDef.of("quest_mother", "Mother").level(198).stats(235, 150, 150, 170, 60, 105).speed(4)
                .defenceBonus(0, 0, 0, 200, 0)
                .attack(AttackDef.of("crush", AttackType.CRUSH, 16, 3, 6))
                .attack(AttackDef.of("rock_throw", AttackType.RANGED, 16, 9, 4))
                .drops(Drop.always("big_bones")).skin(0x8A8A78, 0x5A5A4A, 0x4A4A3A, 0xE0E8F0).size(1.4f, 3.0f, 1.3f)
                .at("quest_weiss").arena(12, "ice"));
        b.quest("making_friends_with_my_arm", "Making Friends with My Arm", Difficulty.MASTER, 2)
                .xp(Skill.MINING, 50000).xp(Skill.AGILITY, 50000).xp(Skill.FIREMAKING, 40000).xp(Skill.CONSTRUCTION, 10000) // OSRS (current quest XP rewards)
                .reqQuest("my_arms_big_adventure", "swan_song", "cold_war", "ROMEO_AND_JULIET")
                .req(Skill.FIREMAKING, 66).req(Skill.MINING, 72).req(Skill.CONSTRUCTION, 35).req(Skill.AGILITY, 68)
                .unlock("Access to Weiss, its salt mine and the Weiss fire pit teleport")
                .stage("I can start this quest by speaking to My Arm on the Troll Stronghold roof. (Requires My Arm's Big Adventure, Swan Song, Cold War, Romeo & Juliet, 66 Firemaking, 72 Mining, 35 Construction, 68 Agility.)")
                .talk("my_arm").say("My Arm's old home is Weiss, in the north. My Arm want to go back and see Mother... but trolls there not friendly.")
                .stage("I should ask the Odd Mushroom in the Troll Stronghold how to reach Weiss.")
                .talk("quest_odd_mushroom").say("Weiss lies past the ice. You'll need a boat... and a lot of nerve. *The mushroom winks. Mushrooms shouldn't wink.*",
                        "Larry, east of Rellekka, has a boat that might carry a troll.")
                // Wiki: the wreck's rope, the rockslides and the ledge, Boulder at the fence, sneaking past the city's trolls and
                // crossing the cave by their thrown stones, mining the exit; Mother is talked round, then Snowflake takes over.
                .stage("I should sail to Weiss and speak to Snowflake.")
                .reach("quest_weiss", 12).stay().unlessFlag("mfa_climbed").need("rope").needTool("pickaxe").reqSkill(Skill.AGILITY, 68)
                .flag("mfa_climbed")
                .say("* Larry's boat leaves you and My Arm by a wreck. You climb the rockslides, rope across to the ledge and pass the fallen tree.",
                        "@Boulder: Wolfbone sent My Arm? Hmph. Come back when Mother will see you.")
                .reach("quest_weiss", 12).stay().needFlag("mfa_climbed").unlessFlag("mfa_inside").needTool("pickaxe").reqSkill(Skill.MINING, 72)
                .flag("mfa_inside")
                .say("* You sneak through the broken fence past the angry trolls, swim the cave over the stepping stones the trolls throw, "
                        + "and mine your way out by Mother's hall.")
                .puzzle("quest_weiss", MFA_MOTHER).stay().needFlag("mfa_inside").unlessFlag("mfa_mother").flag("mfa_mother")
                .say("@Mother: ...Fine. The weakling can stay. For now.")
                .talk("snowflake").needFlag("mfa_mother").travel("quest_weiss")
                .say("You come with My Arm? Mother is angry. Mother always angry. Mother only respect fighting... or the wise.",
                        "Bring a wise man to Weiss and maybe Mother listen. Or maybe he eat him.")
                .missing("Who you? Get past the trolls and see Mother first.")
                // Wiki: the Wise Old Man plays dead in a mahogany coffin, a cadava potion making it convincing.
                .stage("I should get a wise man to Weiss: the Wise Old Man in Draynor Village might play dead in a coffin for Mother.")
                .talk("quest_wise_old_man").stay().unlessFlag("mfa_wom").flag("mfa_wom")
                .say("Pretend I'm dead? Well, I do owe you for the fishing colony. Build me a coffin and fetch something to make me look the part.")
                .reach("DRAYNOR_VILLAGE", 12).stay().needFlag("mfa_wom").unlessFlag("mfa_coffin").take("mahogany_plank:5", "bolt_of_cloth")
                .need("hammer").reqSkill(Skill.CONSTRUCTION, 35).flag("mfa_coffin")
                .say("* You build a mahogany coffin beside the Wise Old Man's house and line it with cloth.")
                .missing("* A coffin takes 5 mahogany planks, a bolt of cloth and a hammer.")
                .talk("quest_wise_old_man").stay().needFlag("mfa_coffin").unless("old_mans_coffin").unlessFlag("mfa_fight").take("cadava_berries")
                .give("old_mans_coffin")
                .say("* The Apothecary's potion of cadava berries makes him deathly pale. He climbs into the coffin.", "Carry me carefully, now.")
                .missing("I need to look dead, not just sleepy. Cadava berries from the Apothecary should do it.")
                .talk("snowflake").need("old_mans_coffin").say("Wise man... dead? Mother will want to see. *She takes you to Mother's hall.*",
                        "You can't teleport out of there, and if you fall, maybe you lose stuff.")
                .missing("Bring the wise man.")
                // Wiki: Don't Know What first, then Mother; the Fire of Domination makes him untouchable until a bucket of water from
                // the large barrel puts it out.
                .stage("Mother, the troll chief of Weiss, attacks! I must defeat him.")
                .reach("quest_weiss", 12).stay().unlessFlag("mfa_dkw").spawn("dont_know_what")
                .kill("dont_know_what").stay().unlessFlag("mfa_dkw").flag("mfa_dkw").spawn("quest_mother")
                .say("* Don't Know What falls. Mother rises from his throne, wreathed in the Fire of Domination!")
                .reach("quest_weiss", 12).stay().needFlag("mfa_dkw").unlessFlag("mfa_fire_out").take("minecraft:bucket").flag("mfa_fire_out")
                .say("* You fill a bucket at the large barrel of water and throw it on the Fire of Domination. Mother's flames go out!")
                .missing("* The Fire of Domination protects Mother. A bucket of water from the large barrel might put it out.")
                .reach("quest_weiss", 12).stay().needFlag("mfa_dkw").spawn("quest_mother")
                .kill("quest_mother").needFlag("mfa_dkw").say("* Mother is defeated. The trolls of Weiss look to My Arm.")
                // Wiki: Snowflake becomes the new leader; goat dung keeps the fire pit burning, and her fire notes teach the Weiss fire.
                .stage("I should return to My Arm.")
                .talk("snowflake").stay().unless("goat_dung").unlessFlag("mfa_dung").take("minecraft:bucket").give("goat_dung")
                .say("New leader need warm fire. Goat dung burn best! *You scoop up goat poo in the north-east building.*")
                .talk("snowflake").stay().unlessFlag("mfa_dung").take("goat_dung").give("weiss_fire_notes").flag("mfa_dung")
                .say("Good dung! Read these notes: they tell how to keep the Weiss fire burning.")
                .talk("my_arm").needFlag("mfa_dung").say("Snowflake is chief of Weiss now, and My Arm her friend! You always welcome. Use fire pit to come back any time.")
                .missing("Help Snowflake with the fire first!")
                .talk("my_arm").need("bucket_of_salt").take("bucket_of_salt") // saves from before: the salt and fire pit route
                .say("My Arm is chief of Weiss now! You always welcome. Use fire pit to come back any time.");
    }
}
