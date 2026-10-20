package com.runecraft.osrs.content;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;

import java.util.Map;

/**
 * Items referenced by more than one content module (defined once here so modules can reference
 * them freely in drop tables and recipes).
 */
public final class CommonItems {
    private CommonItems() {
    }

    /** Shades of Mort'ton keys, worst to best (minigames: pyre rewards and chests; dungeons: the catacomb doors). */
    public static final String[] SHADE_KEY_METALS = {"bronze", "steel", "black", "silver", "gold"};
    public static final String[] SHADE_KEY_COLOURS = {"red", "brown", "crimson", "black", "purple"};
    private static final int[] SHADE_KEY_RGB = {0x8A5A2A, 0x9A9AA0, 0x2A2A2A, 0xD0D0E0, 0xE0B030};

    /** A shade key id: the red key keeps the old id ({@code bronze_shade_key}), the others add their colour. */
    public static String shadeKey(String metal, String colour) {
        return metal + "_shade_key" + (colour.equals("red") ? "" : "_" + colour);
    }

    public static void add(ItemSink items) {
        // Aether runes (skilling: Cosmic Altar; minigames: Guardians of the Rift) and their catalyst (Doom of Mokhaiotl drop).
        items.add("aether_rune", "Aether rune", Kind.RUNE, "rune", 0xB070E0).value(300).examine("A combined cosmic and soul rune.");
        items.add("aether_catalyst", "Aether catalyst", Kind.SIMPLE, "orb", 0x9A60D0).value(100)
                .examine("Used with soul runes at the Cosmic Altar to make aether runes.");
        // Weiss salts (systems: Weiss salt mine and basalt teleports; skilling: the Sailing eternal brazier takes 250 of each).
        items.add("te_salt", "Te salt", Kind.SIMPLE, "ore", 0xE0E0D0).value(1).stack(100000).examine("A salt from Weiss's salt mine.");
        items.add("efh_salt", "Efh salt", Kind.SIMPLE, "ore", 0xB0D8F0).value(1).stack(100000).examine("A salt from Weiss's salt mine.");
        items.add("urt_salt", "Urt salt", Kind.SIMPLE, "ore", 0x9A6A4A).value(1).stack(100000).examine("A salt from Weiss's salt mine.");
        // Lit bug lantern (slayer: harpie bug swarms; skilling: lights lantern harpooning).
        items.add("bug_lantern", "Lit bug lantern", Kind.SIMPLE, "lantern_lit", 0xF0C040).value(130).stack(1)
                .examine("Harpie bug swarms are drawn to the light.");
        // Machete (npcs: Karamja shops; minigames: Tai Bwo Wannai Cleanup; skilling: hunter loot).
        items.add("machete", "Machete", Kind.TOOL, "scimitar", 0x9A9AA0).value(40).stack(1).examine("An effective tool for chopping tough jungle plants.");
        // Book of the dead (zeah: the Kourend graveyard teleports; quests: A Kingdom Divided's reward).
        items.add("book_of_the_dead", "Book of the dead", Kind.SIMPLE, "book", 0x3A1A4A).value(1).stack(1)
                .examine("A tome of Arceuus necromancy. Its pages allow teleports to the Kourend graveyards.");
        items.add("green_dragonhide", "Green dragonhide", Kind.SIMPLE, "hide", 0x3A8A3A).value(1800);
        items.add("blue_dragonhide", "Blue dragonhide", Kind.SIMPLE, "hide", 0x3A5AB0).value(2000);
        items.add("red_dragonhide", "Red dragonhide", Kind.SIMPLE, "hide", 0xB03A3A).value(2400);
        items.add("black_dragonhide", "Black dragonhide", Kind.SIMPLE, "hide", 0x2A2A2A).value(3200);
        items.add("anti_dragon_shield", "Anti-dragon shield", Kind.SHIELD, "kiteshield", 0x6A8AA0).value(20).stack(1).tier("steel")
                .equipment(new EquipmentDef(EquipmentDef.Slot.SHIELD, new EquipmentStats(0, 0, 0, -10, -5, 20, 24, 22, 10, 22, 0, 0, 0, 0),
                        Map.of(), 0, AttackType.CRUSH, EquipmentDef.WeaponClass.NONE))
                .examine("Protects you from dragonfire.");
        items.add("antifire_potion", "Antifire potion", Kind.POTION, "potion", 0x9A3A9A).value(180).stack(1);
        items.add("extended_antifire", "Extended antifire", Kind.POTION, "potion", 0xB04AB0).value(400).stack(1);
        items.add("abyssal_whip", "Abyssal whip", Kind.MELEE_WEAPON, "whip", 0x5A2A2A).value(120001).stack(1).tier("dragon")
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(0, 82, 0, 0, 0, 0, 0, 0, 0, 0, 82, 0, 0, 0),
                        Map.of(Skill.ATTACK, 70), 4, AttackType.SLASH, EquipmentDef.WeaponClass.MELEE));
        for (String tier : new String[]{"beginner", "easy", "medium", "hard", "elite", "master"}) {
            String name = Character.toUpperCase(tier.charAt(0)) + tier.substring(1);
            items.add("clue_scroll_" + tier, "Clue scroll (" + tier + ")", Kind.SIMPLE, "note", 0xE8D8A0).value(1).stack(1)
                    .examine("A " + name.toLowerCase() + " clue scroll.");
        }
        // Boss/slayer materials upgraded by other modules: Doom of Mokhaiotl and Maggot King drops (bosses) become
        // confliction gauntlets and the necklace of rupture (systems); the tormented synapse (slayer, Tormented Demons)
        // becomes the purging staff (skilling) and the scorching bow (slayer).
        items.add("mokhaiotl_cloth", "Mokhaiotl cloth", Kind.SIMPLE, "cloth", 0x6A3A8A).value(30_000_000).stack(1)
                .examine("Woven from the Doom's flesh. Upgrades a tormented bracelet into confliction gauntlets.");
        items.add("demon_tear", "Demon tear", Kind.SIMPLE, "gem", 0xC03050).value(1).stack(100000)
                .examine("Charges the eye of ayak.");
        items.add("elder_venator_fang", "Elder venator fang", Kind.SIMPLE, "fang", 0xE0D0B0).value(40_000_000).stack(1)
                .examine("Combine with a necklace of anguish to make a necklace of rupture (84 Crafting).");
        items.add("tormented_synapse", "Tormented synapse", Kind.SIMPLE, "orb", 0xE05020).value(800_000).stack(1)
                .examine("A twitching synapse from a tormented demon. Its fiery energy could empower a demonbane weapon.");
        // Caught from maniacal monkeys (skilling, Hunter) and dropped by demonic gorillas (slayer); strings a ballista.
        // Revenant unique (godwars module drops it; systems' Jewellery makes it wearable).
        items.add("amulet_of_avarice", "Amulet of avarice", Kind.SIMPLE, "amulet", 0x6AD0B0).value(60_000).stack(1)
                .examine("A hauntingly beautiful amulet. Revenants fear it; the Wilderness covets it.");
        items.add("monkey_tail", "Monkey tail", Kind.SIMPLE, "tail", 0x8A6A3A).value(1).examine("A whole maniacal monkey tail. Ballista string!");
        // Demonic gorilla uniques (slayer module drops them; ballistae are fletched from them, uncut zenyte crafted).
        items.add("zenyte_shard", "Zenyte shard", Kind.SIMPLE, "shard", 0xF0A020).value(75_000)
                .examine("A shard of unstable zenyte.");
        items.add("ballista_limbs", "Ballista limbs", Kind.SIMPLE, "limbs", 0x8A6A4A).value(50_000).examine("Sturdy struts.");
        items.add("ballista_spring", "Ballista spring", Kind.SIMPLE, "clockwork", 0x9A9AA0).value(50_000)
                .examine("A spring-loaded mechanism.");
        items.add("light_frame", "Light frame", Kind.SIMPLE, "stock", 0xA0784A).value(50_000).examine("A light wooden frame.");
        items.add("heavy_frame", "Heavy frame", Kind.SIMPLE, "stock", 0x6A4A2A).value(50_000).examine("A heavy wooden frame.");
        // Construction material also used to repair the Mort'ton temple (minigames); sacred oil, made at the temple's
        // fire altar and turned into pyre logs (minigames), is also a Shades of Mort'ton quest item (quests).
        items.add("limestone_brick", "Limestone brick", Kind.SIMPLE, "brick", 0xD8D0B0).value(21);
        items.add("sacred_oil", "Sacred oil", Kind.POTION, "potion", 0xE0E0A0).value(100).stack(1).notDrinkable() // OSRS: Empty/Drop only
                .examine("4 doses of sacred oil.");
        for (int m = 0; m < SHADE_KEY_METALS.length; m++) {
            String metal = SHADE_KEY_METALS[m];
            String cap = Character.toUpperCase(metal.charAt(0)) + metal.substring(1);
            for (String c : SHADE_KEY_COLOURS) {
                items.add(shadeKey(metal, c), cap + " key " + c, Kind.SIMPLE, "key", SHADE_KEY_RGB[m]).value(81).stack(1)
                        .examine("A " + metal + " key with a " + c + " painted eyelet (opens chests found in the Mort'ton catacombs).");
            }
        }
        // Herblore secondaries from other modules (skilling grinds and brews them): the nihil shard (godwars, Nex)
        // becomes nihil dust for ancient brews; the star flower and gorak claws (quests, Fairytale II) make magic essence.
        items.add("nihil_shard", "Nihil shard", Kind.SIMPLE, "shard", 0x7A3A9A).value(400).stack(64)
                .examine("A shard of a nihil.");
        items.add("star_flower", "Star flower", Kind.SIMPLE, "herb", 0xE0E080).value(0)
                .examine("A rare flower, ingredient of the Fairy Queen's cure.");
        items.add("gorak_claws", "Gorak claws", Kind.SIMPLE, "bones", 0x6A3A6A).value(0).examine("Claws from a gorak.");
        // Monster drops whose lines the wiki drop sweep used to skip (MonsterDropsTable): long and curved bones (1/400
        // and 1/5,012 from most big-bone monsters; Construction bones sold to Barlak - approx: not buryable here),
        // oyster pearls (Waterbirth and lighthouse dagannoth) and unicorn horns (implings, Varlamore; ground to dust).
        items.add("long_bone", "Long bone", Kind.SIMPLE, "bones", 0xE0D8C0).value(5).examine("A Construction bone.");
        items.add("curved_bone", "Curved bone", Kind.SIMPLE, "bones", 0xD8D0B0).value(7).examine("A curved Construction bone.");
        items.add("oyster_pearls", "Oyster pearls", Kind.SIMPLE, "bead", 0xF0F0E8).value(1400)
                .examine("I could work wonders with a chisel on these pearls.");
        items.add("unicorn_horn", "Unicorn horn", Kind.SIMPLE, "tail", 0xF0F0F0).value(20).examine("This horn has restorative properties.");
        // Mort Myre snail drops (npcs module): shells chiselled into snelms and snail corpses cooked (skilling).
        String[][] shells = {{"myre", "7A7A3A"}, {"ochre", "C0902A"}, {"red", "8A1A1A"}, {"blue", "3A4A8A"}};
        for (String[] sh : shells) {
            for (String form : new String[]{"round", "pointed"}) {
                items.add("blamish_" + sh[0] + "_shell_" + form, "Blamish " + sh[0] + " shell (" + form + ")", Kind.SIMPLE, "shell",
                        Integer.parseInt(sh[1], 16)).value(150).examine("A " + form + " snail shell. I could make a snelm from it.");
            }
        }
        items.add("blamish_bark_shell", "Blamish bark shell", Kind.SIMPLE, "shell", 0x5A4A3A).value(150)
                .examine("A snail shell. I could make a snelm from it.");
        // Karamja cooking ingredients: dropped by the slayer module's jungle spiders and oomlie birds, cooked by the skilling module.
        items.add("spider_carcass", "Spider carcass", Kind.SIMPLE, "meat_raw", 0x5A4A3A).value(15).weight(0.8)
                .examine("Its creeping days are over!");
        items.add("raw_oomlie", "Raw oomlie", Kind.SIMPLE, "meat_raw", 0xC07A6A).value(10).weight(0.225)
                .examine("Raw meat from the oomlie bird.");
        items.add("thin_snail", "Thin snail", Kind.SIMPLE, "meat_raw", 0x8A7A5A).value(5)
                .examine("The thin, slimy corpse of a deceased giant snail.");
        items.add("lean_snail", "Lean snail", Kind.SIMPLE, "meat_raw", 0x8A7A5A).value(8)
                .examine("The lean, slimy corpse of a deceased giant snail.");
        items.add("fat_snail", "Fat snail", Kind.SIMPLE, "meat_raw", 0x8A7A5A).value(10)
                .examine("The fat, slimy corpse of a deceased giant snail.");
        // Dwarf multicannon ammunition: smithed by the skilling module (ammo moulds), handed out by the Dwarf Cannon quest
        // (quests), sold in Giants' Foundry (minigames: double mould), dropped by monsters and fired by the multicannon
        // (systems); granite dust is a Grotesque Guardians drop (bosses) that coats cannonballs.
        items.add("cannonball", "Steel cannonball", Kind.SIMPLE, "cannonball", 0x5A5A60).value(5).examine("Ammo for the Dwarf Cannon.");
        items.add("ammo_mould", "Ammo mould", Kind.TOOL, "mould", 0x6A6A70).value(5).stack(1).examine("Used to make cannon ammunition.");
        items.add("double_ammo_mould", "Double ammo mould", Kind.SIMPLE, "mould", 0x8A8A8A).value(6).stack(1)
                .examine("Looks like two ammo moulds roughly put together with a metal strip.");
        items.add("granite_dust", "Granite dust", Kind.SIMPLE, "essence", 0x9A9A9A).value(1).stack(64000);
        dropItems(items);
        diaryItems(items);
        CommonEquipment.add(items);
    }

    /**
     * More monster drops the wiki drop sweep used to skip (OSRS Wiki values; MonsterDropsTable and the Dagannoth Kings
     * drop them): Fremennik armour materials, taken with dagannoth hide and coins to Rellekka's craftsmen (CraftingData,
     * at the tanner), edible seaweed and ugthanki kebabs, the stuffable Slayer heads, Xerician fabric (lizardmen) and
     * Yama's contracts (Catacombs black and greater demons; nothing to hand them to yet).
     */
    private static void dropItems(ItemSink items) {
        items.add("dagannoth_hide", "Dagannoth hide", Kind.SIMPLE, "hide", 0x5A6A5A).value(60).weight(3.175)
                .examine("A sturdy piece of dagannoth hide.");
        String[][] parts = {
                {"circular_hide", "Circular hide", "hide", "6A8A6A", "A toughened chunk of dagannoth hide."},
                {"flattened_hide", "Flattened hide", "hide", "6A8A6A", "A tattered chunk of dagannoth hide."},
                {"stretched_hide", "Stretched hide", "hide", "6A8A6A", "A weathered chunk of dagannoth hide."},
                {"rock_shell_chunk", "Rock-shell chunk", "shell", "8A7A5A", "A spherical chunk of rock-shell."},
                {"rock_shell_shard", "Rock-shell shard", "shell", "8A7A5A", "A curved piece of rock-shell."},
                {"rock_shell_splinter", "Rock-shell splinter", "shell", "8A7A5A", "A slim piece of rock-shell."},
                {"skull_piece", "Skull piece", "skull", "D8D0B8", "A fearsome looking skull."},
                {"ribcage_piece", "Ribcage piece", "bones", "D8D0B8", "A slightly damaged ribcage."},
                {"fibula_piece", "Fibula piece", "bones", "D8D0B8", "An interesting looking bone shard."}};
        for (String[] p : parts) {
            items.add(p[0], p[1], Kind.SIMPLE, p[2], Integer.parseInt(p[3], 16)).value(60).weight(3.175).examine(p[4]);
        }
        items.add("edible_seaweed", "Edible seaweed", Kind.FOOD, "secondary", 0x4A7A3A).value(2).heal(4).weight(0.25)
                .examine("Slightly damp seaweed.");
        items.add("ugthanki_kebab", "Ugthanki kebab", Kind.FOOD, "meat_cooked", 0xB07A4A).value(20).heal(19).weight(0.5)
                .examine("A fresh kebab made from ugthanki meat.");
        // Blighted supplies (Soul Wars shop and spoils of war, Emir's Arena, muddy chest, Wilderness course): usable only
        // in the Wilderness (enforced by the minigames module). The spell sacks stand in for runes there (not modelled).
        items.add("blighted_karambwan", "Blighted karambwan", Kind.FOOD, "fish_cooked", 0x7A5A8A).value(1).heal(18)
                .examine("Can only be eaten in the Wilderness.");
        items.add("blighted_anglerfish", "Blighted anglerfish", Kind.FOOD, "fish_cooked", 0x6A3A6A).value(1).heal(22) // approx: 22 at 99 HP
                .examine("Can only be eaten in the Wilderness.");
        items.add("blighted_manta_ray", "Blighted manta ray", Kind.FOOD, "fish_cooked", 0x3A3A6A).value(1).heal(22)
                .examine("Can only be eaten in the Wilderness.");
        items.add("blighted_super_restore", "Blighted super restore", Kind.POTION, "potion", 0xA03070).value(1).stack(1)
                .examine("4 doses of blighted super restore. Can only be drunk in the Wilderness.");
        for (String[] sack : new String[][]{{"vengeance", "Vengeance"}, {"ancient_ice", "Ancient ice"}, {"entangle", "Entangle"},
                {"teleport_spell", "Teleport spell"}}) {
            items.add("blighted_" + sack[0] + "_sack", "Blighted " + sack[1].toLowerCase() + " sack", Kind.SIMPLE, "pouch", 0x5A4A6A)
                    .value(1).stack(100000).examine("A sack of runes that only work in the Wilderness.");
        }
        for (String[] h : new String[][]{{"cockatrice", "Cockatrice", "2000", "8AA05A"}, {"basilisk", "Basilisk", "4000", "5A8A4A"},
                {"kurask", "Kurask", "6000", "6A7A4A"}}) {
            items.add(h[0] + "_head", h[1] + " head", Kind.SIMPLE, "skull", Integer.parseInt(h[3], 16)).value(Integer.parseInt(h[2]))
                    .stack(1).weight(10).examine("I should get it stuffed!");
        }
        // The crawling hand's stuffable hand (same id as the monster, like undead_chicken; the Slayer log page lists it).
        items.add("crawling_hand", "Crawling hand", Kind.SIMPLE, "skull", 0xB8A890).value(1000).stack(1).weight(10)
                .examine("I should get it stuffed!");
        items.add("xerician_fabric", "Xerician fabric", Kind.SIMPLE, "cloth", 0x4A6A4A).value(3).weight(0.06)
                .examine("An old scrap of cloth, containing remnants of Xeric's forgotten magicks.");
        // Round 27 drops (OSRS Wiki values/examines): remains and keys of the slayer monsters, venom sacs, Varlamore shards,
        // the looting bag (Wilderness drop, SystemsHooks), scaly blue dragonhide and crystal teleport seeds.
        items.add("vampyre_dust", "Vampyre dust", Kind.SIMPLE, "secondary", 0xB8B0A8).value(2).weight(0.1)
                .examine("That used to be a vampyre!");
        items.add("ogre_coffin_key", "Ogre coffin key", Kind.SIMPLE, "key", 0x8A7A5A).value(100).weight(0.01)
                .examine("A key to open ogre coffins.");
        items.add("araxyte_venom_sac", "Araxyte venom sac", Kind.SIMPLE, "secondary", 0x6AA03A).value(500).stack(1000).weight(0.1)
                .examine("A sac of araxyte venom.");
        items.add("blessed_bone_shards", "Blessed bone shards", Kind.SIMPLE, "bones", 0xE8E0C8).value(18).stack(100000)
                .examine("Blessed shards of bone, ready to be offered at the libation bowl.");
        items.add("looting_bag", "Looting bag", Kind.SIMPLE, "pouch", 0x7A5A3A).value(1).stack(1).weight(0.2)
                .examine("You can store stuff in the bag when you're in the Wilderness.");
        items.add("scaly_blue_dragonhide", "Scaly blue dragonhide", Kind.SIMPLE, "hide", 0x3A5AA0).value(100).weight(2)
                .examine("A scaled blue dragonhide.");
        items.add("crystal_teleport_seed", "Crystal teleport seed", Kind.SIMPLE, "seed", 0x9AE0E0).value(1).weight(0.01)
                .examine("A seed to be sung into a powerful teleport crystal.");
        // Round 29: Karil's crossbow ammunition from the Barrows chest, and the holy wrench (Rum Deal; prayer restores, imbues).
        items.add("bolt_rack", "Bolt rack", Kind.ARROW, "arrow", 0x6A6A70).value(50).rangedStrength(55).stack(100000).weight(0) // value approx
                .examine("Bolt racks for use with Karil's crossbow.");
        items.add("holy_wrench", "Holy wrench", Kind.SIMPLE, "hammer", 0xE0D070).value(5).stack(1).weight(0.4)
                .examine("Deep in Davey's bag of tricks, the holiest of wrenches.");
        teleportItems(items);
        // Ancient sceptre (systems: moved here in round 32; bosses: the four elemental sceptres are made by using a Desert
        // Treasure II quartz on it). Stats come from EquipmentBonuses.
        items.add("ancient_sceptre", "Ancient sceptre", Kind.STAFF, "staff_orb", 0x6A3A8A).value(3_000_000).stack(1).tier("rune")
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(0, 0, 0, 20, 0, 0, 0, 0, 20, 0, 0, 0, 5, 0),
                        Map.of(Skill.MAGIC, 70), 4, AttackType.MAGIC, EquipmentDef.WeaponClass.STAFF))
                .examine("Boosts the power of Ancient Magicks.");
        for (String c : new String[]{"bloodied_blows", "glyphic_attenuation", "divine_severance", "forfeit_breath", "sensory_clouding"}) {
            String words = c.replace('_', ' ');
            items.add("contract_of_" + c, "Contract of " + words, Kind.SIMPLE, "scroll", 0x8A2A2A).value(75000).weight(0.4)
                    .examine("A contract issued by Yama. Present it to him to discuss some new terms.");
        }
    }

    /**
     * Round 27 teleport items whose OSRS source lives in another module (round 28; the systems module's Jewellery still
     * gives them their teleports and worn stats). OSRS Wiki values. Chronicle and teleport cards: Diango's Toy Store (npcs);
     * mythical cape: Jack's Mythical Cape Store in the Myths' Guild (npcs); music cape: Olaf the Bard in Rellekka (npcs);
     * grand seed pod: a rare Gnome Restaurant tip (minigames); eternal teleport crystal: sung from an enhanced crystal
     * teleport seed and 100 crystal shards (bosses, Prifddinas guards drop the seed); tarnished locket: an intricate pouch
     * (minigames), shown to the Lumbridge Guide for the amulet of the eye (systems).
     */
    private static void teleportItems(ItemSink items) {
        items.add("chronicle", "Chronicle", Kind.SIMPLE, "book", 0x6A3A1A).value(200).stack(1).weight(0.5)
                .examine("A book that charges with teleport cards.");
        items.add("teleport_card", "Teleport card", Kind.SIMPLE, "note", 0xE0D0A0).value(100).stack(100000)
                .examine("Charges the Chronicle with one teleport.");
        items.add("mythical_cape", "Mythical cape", Kind.SIMPLE, "cape", 0x3A6A4A).value(10_000).stack(1).weight(0.4)
                .examine("A cape worn by those who have proven themselves in the Myths' Guild.");
        items.add("music_cape", "Music cape", Kind.SIMPLE, "cape", 0xC03030).value(99_000).stack(1).weight(0.4)
                .examine("The cape worn by those who have unlocked every song.");
        items.add("grand_seed_pod", "Grand seed pod", Kind.SIMPLE, "seed", 0x8AB040).value(250).stack(1).weight(0.01)
                .examine("Squash or launch it to reach the Grand Tree.");
        items.add("eternal_teleport_crystal", "Eternal teleport crystal", Kind.SIMPLE, "gem", 0xC8F4FF).value(500).stack(1)
                .examine("A crystal with unlimited teleports to Lletya and Prifddinas.");
        items.add("enhanced_crystal_teleport_seed", "Enhanced crystal teleport seed", Kind.SIMPLE, "seed", 0xB8F0FF).value(1).stack(1)
                .weight(0.01).examine("A seed to be sung into an eternal teleport crystal."); // value approx
        items.add("tarnished_locket", "Tarnished locket", Kind.SIMPLE, "amulet", 0x8A7A5A).value(1).stack(1)
                .examine("An old locket. The Lumbridge Guide might know something about it."); // value approx
    }

    /** Team capes 1-50 (pink 1-10, yellow 11-20, blue 21-30, green 31-40, red 41-50), sold by the Wilderness cape merchants. */
    public static final int[] TEAM_CAPE_RGB = {0xE070B0, 0xE0D040, 0x3A5AC0, 0x3A9A3A, 0xC03030};

    public static String teamCape(int n) {
        return "team_" + n + "_cape";
    }

    /**
     * Items behind diary tasks that several modules hand out (OSRS Wiki values): the rusty sword (H.A.M. members, Tindel
     * Marchant), the muddy key (chaos dwarves; the Lava Maze muddy chest), the crystal key (joined from the slayer module's
     * key halves; the Taverley crystal chest), the Stronghold of Security sceptre pieces, the white 2h sword (Sir Vyvin) and
     * the team capes (cape merchants).
     */
    private static void diaryItems(ItemSink items) {
        items.add("rusty_sword", "Rusty sword", Kind.SIMPLE, "longsword", 0x8A5A3A).value(1).stack(1).weight(2.267)
                .examine("A decent-enough weapon gone rusty.");
        items.add("muddy_key", "Muddy key", Kind.SIMPLE, "key", 0x7A5A3A).value(80).stack(1)
                .examine("It looks like the key to a chest (Used to open the muddy chest in the lava maze).");
        items.add("crystal_key", "Crystal key", Kind.SIMPLE, "key", 0xB0E0E8).value(150).stack(1)
                .examine("A mysterious key for a mysterious chest.");
        items.add("left_skull_half", "Left skull half", Kind.SIMPLE, "skull", 0xE0D8C0).value(1).stack(1)
                .examine("The left half of a strange skull.");
        items.add("right_skull_half", "Right skull half", Kind.SIMPLE, "skull", 0xE0D8C0).value(1).stack(1)
                .examine("The right half of a strange skull.");
        items.add("strange_skull", "Strange skull", Kind.SIMPLE, "skull", 0xE0D8C0).value(1).stack(1)
                .examine("A weird looking skull, it looks like there are holes for a staff.");
        items.add("top_of_sceptre", "Top of sceptre", Kind.SIMPLE, "staff", 0xA08A50).value(1).stack(1)
                .examine("The top half of a sceptre.");
        items.add("bottom_of_sceptre", "Bottom of sceptre", Kind.SIMPLE, "staff", 0xA08A50).value(1).stack(1)
                .examine("The bottom half of a sceptre.");
        items.add("runed_sceptre", "Runed sceptre", Kind.SIMPLE, "staff", 0xA08A50).value(1).stack(1)
                .examine("A sceptre covered in runes, it looks like a skull should fit on top.");
        items.add("skull_sceptre", "Skull sceptre", Kind.STAFF, "staff_orb", 0xE0D8C0).value(1).stack(1).weight(1.814)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(0, -1, 7, 10, 0, 2, 3, 1, 4, 0, 3, 0, 0, 0),
                        Map.of(), 5, AttackType.CRUSH, EquipmentDef.WeaponClass.STAFF))
                .examine("A fragile magical Sceptre.");
        items.add("white_2h_sword", "White 2h sword", Kind.MELEE_WEAPON, "2h_sword", 0xE8E8F0).value(3840).stack(1).tier("black")
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, new EquipmentStats(-4, 27, 21, -4, 0, 0, 0, 0, 0, -1, 26, 0, 0, 1),
                        Map.of(Skill.ATTACK, 10), 7, AttackType.SLASH, EquipmentDef.WeaponClass.MELEE))
                .examine("A two handed sword, favoured by the White Knights.");
        // Gold helmet (Between a Rock...; 3 gold bars at an anvil, 50 Smithing) and dwarven helmet (Grim Tales reward).
        items.add("gold_helmet", "Gold helmet", Kind.ARMOUR, "full_helm", 0xE0C040).value(1).stack(1).weight(31.751)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 0, 0, 0, 0, 10, 20, 0, 10, 0, 0, 0, 0), 30))
                .examine("A gold helmet. Needed to mine the Arzinian mine's gold.");
        items.add("dwarven_helmet", "Dwarven helmet", Kind.ARMOUR, "full_helm", 0x8A6A3A).value(5000).stack(1)
                .equipment(EquipmentDef.armour(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 6, -2, -2, 27, 28, 31, 5, 24, 0, 0, 0, 0), 50))
                .examine("A sturdy dwarven helmet.");
        for (int n = 1; n <= 50; n++) {
            items.add(teamCape(n), "Team-" + n + " cape", Kind.SIMPLE, "cape", TEAM_CAPE_RGB[(n - 1) / 10]).value(50).stack(1)
                    .equipment(new EquipmentDef(EquipmentDef.Slot.HEAD, new EquipmentStats(0, 0, 0, 0, 0, 0, 1, 1, 0, 2, 0, 0, 0, 0),
                            Map.of(), 0, AttackType.CRUSH, EquipmentDef.WeaponClass.NONE))
                    .examine("Ooohhh look at the pretty colours...");
        }
    }
}
