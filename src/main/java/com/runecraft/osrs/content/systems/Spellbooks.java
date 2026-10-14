package com.runecraft.osrs.content.systems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Ancient, Lunar and Arceuus spellbooks plus standard-spellbook spells the core book doesn't have
 * (enchanting, orbs, god spells, bind/snare/entangle, curses...). Levels, XP, runes and max hits are from
 * the OSRS wiki.
 */
public final class Spellbooks {
    public enum Book {
        STANDARD("Standard", null), ANCIENT("Ancient Magicks", "desert_treasure_i"), LUNAR("Lunar", "lunar_diplomacy"),
        ARCEUUS("Arceuus", null);

        public final String displayName;
        /** Quest required to use the book (looked up at runtime; missing quests only warn). */
        public final String quest;

        Book(String displayName, String quest) {
            this.displayName = displayName;
            this.quest = quest;
        }
    }

    public enum Kind { COMBAT, TELEPORT, UTILITY }

    public enum Effect { NONE, POISON, DRAIN_ATTACK, HEAL, FREEZE, DRAIN_DEFENCE, DRAIN_PRAYER, DRAIN_MAGIC, UNDEAD, DEMON, BIND,
        DRAIN_STRENGTH, TELEBLOCK }

    /**
     * One spell. {@code area} spells (bursts and barrages) hit up to 9 enemies around the target.
     * {@code item} is a weapon that must be wielded (Iban's staff, god staves, slayer's staff).
     */
    public record Spell(String id, String name, Book book, int level, double xp, Map<String, Integer> runes, Kind kind, int maxHit,
                        Effect effect, int param, boolean area, Teleports.Destination destination, String item, String description) {
        public boolean isCombat() {
            return kind == Kind.COMBAT;
        }
    }

    private static final Map<String, Spell> SPELLS = new LinkedHashMap<>();

    private static Map<String, Integer> r(Object... pairs) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put(pairs[i] + "_rune", (Integer) pairs[i + 1]);
        }
        return m;
    }

    private static void combat(Book book, String id, String name, int level, double xp, int max, Effect e, int param, boolean area,
                               Map<String, Integer> runes, String desc) {
        SPELLS.put(id, new Spell(id, name, book, level, xp, runes, Kind.COMBAT, max, e, param, area, null, null, desc));
    }

    private static void staffSpell(String id, String name, int level, double xp, int max, Effect e, int param, String item,
                                   Map<String, Integer> runes, String desc) {
        SPELLS.put(id, new Spell(id, name, Book.STANDARD, level, xp, runes, Kind.COMBAT, max, e, param, false, null, item, desc));
    }

    private static void tele(Book book, String id, String name, int level, double xp, int x, int y, Map<String, Integer> runes) {
        Teleports.Destination d = Teleports.at(name.replace(" Teleport", "").replace("Tele Group ", ""), x, y);
        SPELLS.put(id, new Spell(id, name, book, level, xp, runes, Kind.TELEPORT, 0, Effect.NONE, 0, false, d, null,
                "Teleports you to " + d.name() + "."));
    }

    private static void util(Book book, String id, String name, int level, double xp, Map<String, Integer> runes, String desc) {
        SPELLS.put(id, new Spell(id, name, book, level, xp, runes, Kind.UTILITY, 0, Effect.NONE, 0, false, null, null, desc));
    }

    static {
        // ======================= Standard spellbook extras =======================
        Book S = Book.STANDARD;
        util(S, "enchant_crossbow_bolt", "Enchant Crossbow Bolt", 4, 9, r("cosmic", 1, "air", 2),
                "Enchants 10 gem-tipped crossbow bolts (opal at 4 Magic up to onyx at 87; runes and XP depend on the bolts).");
        util(S, "enchant_1", "Lvl-1 Enchant", 7, 17.5, r("cosmic", 1, "water", 1), "Enchants sapphire jewellery.");
        combat(S, "confuse", "Confuse", 3, 13, 0, Effect.DRAIN_ATTACK, 5, false, r("body", 1, "earth", 2, "water", 3), "Lowers your target's Attack by 5%.");
        util(S, "bones_to_bananas", "Bones to Bananas", 15, 25, r("nature", 1, "water", 2, "earth", 2), "Turns the bones in your inventory into bananas.");
        combat(S, "weaken", "Weaken", 11, 21, 0, Effect.DRAIN_STRENGTH, 5, false, r("body", 1, "earth", 2, "water", 3), "Lowers your target's Strength by 5%.");
        combat(S, "curse", "Curse", 19, 29, 0, Effect.DRAIN_DEFENCE, 5, false, r("body", 1, "earth", 3, "water", 2), "Lowers your target's Defence by 5%.");
        combat(S, "bind", "Bind", 20, 30, 0, Effect.BIND, 8, false, r("nature", 2, "earth", 3, "water", 3), "Holds your target in place for 8 ticks (4.8 seconds).");
        util(S, "enchant_2", "Lvl-2 Enchant", 27, 37, r("cosmic", 1, "air", 3), "Enchants emerald jewellery.");
        util(S, "telekinetic_grab", "Telekinetic Grab", 33, 43, r("law", 1, "air", 1), "Pulls an item on the ground to you.");
        combat(S, "crumble_undead", "Crumble Undead", 39, 24.5, 15, Effect.UNDEAD, 0, false, r("chaos", 1, "earth", 2, "air", 2), "Hits undead for up to 15 damage.");
        util(S, "enchant_3", "Lvl-3 Enchant", 49, 59, r("cosmic", 1, "fire", 5), "Enchants ruby jewellery.");
        staffSpell("iban_blast", "Iban Blast", 50, 30, 25, Effect.NONE, 0, "ibans_staff", r("death", 1, "fire", 5), "Requires Iban's staff. Max hit 25.");
        staffSpell("magic_dart", "Magic Dart", 50, 30, 19, Effect.NONE, 0, "slayer_staff", r("death", 1, "mind", 4), "Requires a slayer's staff. Max hit 10 + Magic/10.");
        combat(S, "snare", "Snare", 50, 60, 3, Effect.BIND, 16, false, r("nature", 3, "earth", 4, "water", 4),
                "Holds your target in place for 16 ticks (9.6 seconds). Max hit 3.");
        util(S, "charge_water_orb", "Charge Water Orb", 56, 66, r("cosmic", 3, "water", 30), "Charges an unpowered orb at a water obelisk.");
        util(S, "enchant_4", "Lvl-4 Enchant", 57, 67, r("cosmic", 1, "earth", 10), "Enchants diamond jewellery.");
        util(S, "bones_to_peaches", "Bones to Peaches", 60, 35.5, r("nature", 2, "water", 4, "earth", 4), "Turns the bones in your inventory into peaches.");
        staffSpell("saradomin_strike", "Saradomin Strike", 60, 35, 20, Effect.DRAIN_PRAYER, 1, "saradomin_staff", r("blood", 2, "fire", 2, "air", 4), "Requires the Saradomin staff.");
        staffSpell("claws_of_guthix", "Claws of Guthix", 60, 35, 20, Effect.DRAIN_DEFENCE, 5, "guthix_staff", r("blood", 2, "fire", 1, "air", 4), "Requires the Guthix staff.");
        staffSpell("flames_of_zamorak", "Flames of Zamorak", 60, 35, 20, Effect.DRAIN_MAGIC, 5, "zamorak_staff", r("blood", 2, "fire", 4, "air", 1), "Requires the Zamorak staff.");
        util(S, "charge_earth_orb", "Charge Earth Orb", 60, 70, r("cosmic", 3, "earth", 30), "Charges an unpowered orb at an earth obelisk.");
        util(S, "charge_fire_orb", "Charge Fire Orb", 63, 73, r("cosmic", 3, "fire", 30), "Charges an unpowered orb at a fire obelisk.");
        combat(S, "vulnerability", "Vulnerability", 66, 76, 0, Effect.DRAIN_DEFENCE, 10, false, r("soul", 1, "earth", 5, "water", 5), "Lowers your target's Defence by 10%.");
        util(S, "charge_air_orb", "Charge Air Orb", 66, 76, r("cosmic", 3, "air", 30), "Charges an unpowered orb at an air obelisk.");
        util(S, "enchant_5", "Lvl-5 Enchant", 68, 78, r("cosmic", 1, "earth", 15, "water", 15), "Enchants dragonstone jewellery.");
        combat(S, "enfeeble", "Enfeeble", 73, 83, 0, Effect.DRAIN_STRENGTH, 10, false, r("soul", 1, "earth", 8, "water", 8), "Lowers your target's Strength by 10%.");
        combat(S, "stun", "Stun", 80, 90, 0, Effect.DRAIN_ATTACK, 10, false, r("soul", 1, "earth", 12, "water", 12), "Lowers your target's Attack by 10%.");
        combat(S, "entangle", "Entangle", 79, 89, 5, Effect.BIND, 24, false, r("nature", 4, "earth", 5, "water", 5), "Holds your target in place for 24 ticks (14.4 seconds). Max hit 5.");
        combat(S, "tele_block", "Tele Block", 85, 80, 0, Effect.TELEBLOCK, 500, false, r("law", 1, "chaos", 1, "death", 1),
                "Stops another player teleporting for 5 minutes (2.5 with Protect from Magic).");
        util(S, "charge", "Charge", 80, 180, r("blood", 3, "fire", 3, "air", 3), "God spells hit up to 30 for 7 minutes.");
        util(S, "enchant_6", "Lvl-6 Enchant", 87, 97, r("cosmic", 1, "earth", 20, "fire", 20), "Enchants onyx jewellery.");
        util(S, "enchant_7", "Lvl-7 Enchant", 93, 110, r("cosmic", 1, "blood", 20, "soul", 20), "Enchants zenyte jewellery.");
        tele(S, "kourend_castle_teleport", "Kourend Castle Teleport", 69, 82, 1643, 3673, r("soul", 2, "law", 2, "water", 4, "fire", 5));
        tele(S, "ape_atoll_teleport", "Ape Atoll Teleport", 64, 74, 2769, 2785, r("law", 2, "fire", 2, "water", 2));
        // Needs Twilight's Promise; lands at the Sunrise Palace. Runes approx (the wiki's portal costs 200 law / 100 earth / 100 fire).
        tele(S, "civitas_illa_fortis_teleport", "Civitas illa Fortis Teleport", 54, 64, 1681, 3133, r("law", 2, "earth", 1, "fire", 1));
        util(S, "house_teleport", "Teleport to House", 40, 30, r("law", 1, "air", 1, "earth", 1), "Teleports you to your player-owned house.");

        // ======================= Ancient Magicks =======================
        Book A = Book.ANCIENT;
        SPELLS.put("edgeville_home_teleport", new Spell("edgeville_home_teleport", "Edgeville Home Teleport", A, 1, 0, Map.of(), Kind.TELEPORT, 0,
                Effect.NONE, 0, false, Teleports.EDGEVILLE_HOME, null, "Teleports you to Edgeville."));
        combat(A, "smoke_rush", "Smoke Rush", 50, 30, 13, Effect.POISON, 2, false, r("chaos", 2, "death", 2, "fire", 1, "air", 1), "May poison the target.");
        combat(A, "shadow_rush", "Shadow Rush", 52, 31, 14, Effect.DRAIN_ATTACK, 10, false, r("chaos", 2, "death", 2, "air", 1, "soul", 1), "Lowers the target's Attack by 10%.");
        tele(A, "paddewwa_teleport", "Paddewwa Teleport", 54, 64, 3098, 3480, r("law", 2, "air", 1, "fire", 1));
        combat(A, "blood_rush", "Blood Rush", 56, 33, 15, Effect.HEAL, 25, false, r("blood", 1, "death", 2, "chaos", 2), "Heals you for 25% of the damage dealt.");
        combat(A, "ice_rush", "Ice Rush", 58, 34, 16, Effect.FREEZE, 8, false, r("water", 2, "death", 2, "chaos", 2), "Freezes the target for 5 seconds.");
        tele(A, "senntisten_teleport", "Senntisten Teleport", 60, 70, 3322, 3336, r("law", 2, "soul", 1));
        combat(A, "smoke_burst", "Smoke Burst", 62, 36, 17, Effect.POISON, 2, true, r("chaos", 4, "death", 2, "fire", 2, "air", 2), "Area attack that may poison.");
        combat(A, "shadow_burst", "Shadow Burst", 64, 37, 18, Effect.DRAIN_ATTACK, 10, true, r("chaos", 4, "death", 2, "air", 1, "soul", 2), "Area attack lowering Attack by 10%.");
        tele(A, "kharyrll_teleport", "Kharyrll Teleport", 66, 76, 3492, 3471, r("law", 2, "blood", 1));
        combat(A, "blood_burst", "Blood Burst", 68, 39, 21, Effect.HEAL, 25, true, r("chaos", 4, "death", 2, "blood", 2), "Area attack healing 25% of damage.");
        combat(A, "ice_burst", "Ice Burst", 70, 40, 22, Effect.FREEZE, 16, true, r("water", 4, "chaos", 4, "death", 2), "Area attack freezing for 16 ticks (9.6 seconds).");
        tele(A, "lassar_teleport", "Lassar Teleport", 72, 82, 3006, 3471, r("law", 2, "water", 4));
        combat(A, "smoke_blitz", "Smoke Blitz", 74, 42, 23, Effect.POISON, 4, false, r("blood", 2, "death", 2, "fire", 2, "air", 2), "May poison the target.");
        combat(A, "shadow_blitz", "Shadow Blitz", 76, 43, 24, Effect.DRAIN_ATTACK, 15, false, r("blood", 2, "death", 2, "air", 2, "soul", 2), "Lowers Attack by 15%.");
        tele(A, "dareeyak_teleport", "Dareeyak Teleport", 78, 88, 2966, 3696, r("law", 2, "air", 2, "fire", 3));
        combat(A, "blood_blitz", "Blood Blitz", 80, 45, 25, Effect.HEAL, 25, false, r("blood", 4, "death", 2), "Heals you for 25% of the damage dealt.");
        combat(A, "ice_blitz", "Ice Blitz", 82, 46, 26, Effect.FREEZE, 24, false, r("blood", 2, "death", 2, "water", 3), "Freezes the target for 24 ticks (14.4 seconds).");
        tele(A, "carrallanger_teleport", "Carrallanger Teleport", 84, 94, 3157, 3666, r("law", 2, "soul", 2));
        combat(A, "smoke_barrage", "Smoke Barrage", 86, 48, 27, Effect.POISON, 4, true, r("blood", 2, "death", 4, "fire", 4, "air", 4), "Area attack that may poison.");
        combat(A, "shadow_barrage", "Shadow Barrage", 88, 48, 28, Effect.DRAIN_ATTACK, 15, true, r("blood", 2, "death", 4, "air", 4, "soul", 3), "Area attack lowering Attack by 15%.");
        tele(A, "annakarl_teleport", "Annakarl Teleport", 90, 100, 3288, 3886, r("law", 2, "blood", 2));
        combat(A, "blood_barrage", "Blood Barrage", 92, 51, 29, Effect.HEAL, 25, true, r("blood", 4, "death", 4, "soul", 1), "Area attack healing 25% of damage.");
        combat(A, "ice_barrage", "Ice Barrage", 94, 52, 30, Effect.FREEZE, 32, true, r("blood", 2, "death", 4, "water", 6), "Area attack freezing for 32 ticks (19.2 seconds).");
        tele(A, "ghorrock_teleport", "Ghorrock Teleport", 96, 106, 2977, 3873, r("law", 2, "water", 8));

        // ======================= Lunar =======================
        Book L = Book.LUNAR;
        SPELLS.put("lunar_home_teleport", new Spell("lunar_home_teleport", "Lunar Home Teleport", L, 1, 0, Map.of(), Kind.TELEPORT, 0,
                Effect.NONE, 0, false, Teleports.LUNAR_HOME, null, "Teleports you to Lunar Isle."));
        util(L, "bake_pie", "Bake Pie", 65, 60, r("astral", 1, "fire", 5, "water", 4), "Bakes the uncooked pies in your inventory.");
        util(L, "geomancy", "Geomancy", 65, 60, r("astral", 2, "earth", 8, "nature", 3), "Shows the state of the farming patches around you.");
        util(L, "cure_plant", "Cure Plant", 66, 60, r("astral", 1, "earth", 8), "Cures the diseased patch you are looking at.");
        util(L, "monster_examine", "Monster Examine", 66, 61, r("astral", 1, "cosmic", 1, "mind", 1), "Shows a monster's combat stats.");
        util(L, "cure_other", "Cure Other", 68, 65, r("astral", 1, "law", 1, "earth", 10), "Cures another player of poison.");
        util(L, "npc_contact", "NPC Contact", 67, 63, r("astral", 1, "cosmic", 1, "air", 2), "Speak to an NPC from afar.");
        util(L, "humidify", "Humidify", 68, 65, r("astral", 1, "water", 3, "fire", 1), "Fills your empty vials with water.");
        tele(L, "moonclan_teleport", "Moonclan Teleport", 69, 66, 2113, 3917, r("astral", 2, "law", 1, "earth", 2));
        util(L, "cure_me", "Cure Me", 71, 69, r("astral", 2, "cosmic", 2, "law", 1), "Cures poison.");
        tele(L, "tele_group_moonclan", "Tele Group Moonclan", 70, 67, 2113, 3917, r("astral", 2, "law", 1, "earth", 4));
        tele(L, "ourania_teleport", "Ourania Teleport", 71, 69, 2468, 3246, r("astral", 2, "law", 1, "earth", 6));
        util(L, "hunter_kit", "Hunter Kit", 71, 70, r("astral", 2, "earth", 2), "Creates a kit of hunter gear.");
        tele(L, "waterbirth_teleport", "Waterbirth Teleport", 72, 71, 2546, 3757, r("astral", 2, "law", 1, "water", 1));
        tele(L, "tele_group_waterbirth", "Tele Group Waterbirth", 73, 72, 2546, 3757, r("astral", 2, "law", 1, "water", 5));
        util(L, "cure_group", "Cure Group", 74, 74, r("astral", 2, "cosmic", 2, "law", 2), "Cures poison for you and nearby players.");
        util(L, "stat_spy", "Stat Spy", 75, 76, r("astral", 2, "cosmic", 2, "body", 5), "Shows another player's stats.");
        tele(L, "barbarian_teleport", "Barbarian Teleport", 75, 76, 2543, 3568, r("astral", 2, "law", 2, "fire", 3));
        tele(L, "tele_group_barbarian", "Tele Group Barbarian", 76, 77, 2543, 3568, r("astral", 2, "law", 2, "fire", 6));
        util(L, "superglass_make", "Superglass Make", 77, 78, r("astral", 2, "fire", 6, "air", 10), "Turns sand and seaweed into molten glass.");
        util(L, "tan_leather", "Tan Leather", 78, 81, r("astral", 2, "fire", 5, "nature", 1), "Tans hides in your inventory.");
        tele(L, "khazard_teleport", "Khazard Teleport", 78, 80, 2636, 3167, r("astral", 2, "law", 2, "water", 4));
        tele(L, "tele_group_khazard", "Tele Group Khazard", 79, 81, 2636, 3167, r("astral", 2, "law", 2, "water", 8));
        util(L, "dream", "Dream", 79, 82, r("astral", 2, "cosmic", 1, "body", 5), "Rest to regenerate Hitpoints twice as fast.");
        util(L, "string_jewellery", "String Jewellery", 80, 83, r("astral", 2, "earth", 10, "water", 5), "Strings the amulets in your inventory.");
        util(L, "stat_restore_pot_share", "Stat Restore Pot Share", 81, 84, r("astral", 2, "water", 10, "earth", 10),
                "Shares a dose of your restore potion with nearby players.");
        util(L, "magic_imbue", "Magic Imbue", 82, 86, r("astral", 2, "fire", 7, "water", 7), "Combination runes without talismans for 12 seconds.");
        util(L, "fertile_soil", "Fertile Soil", 83, 87, r("astral", 3, "nature", 2, "earth", 15), "Treats the patch you are looking at with supercompost.");
        util(L, "boost_potion_share", "Boost Potion Share", 84, 88, r("astral", 3, "water", 10, "earth", 12),
                "Shares a dose of your boosting potion with nearby players.");
        tele(L, "fishing_guild_teleport", "Fishing Guild Teleport", 85, 89, 2611, 3393, r("astral", 3, "law", 3, "water", 10));
        tele(L, "tele_group_fishing_guild", "Tele Group Fishing Guild", 86, 90, 2611, 3393, r("astral", 3, "law", 3, "water", 14));
        util(L, "plank_make", "Plank Make", 86, 90, r("astral", 2, "nature", 1, "earth", 15), "Turns logs into planks.");
        tele(L, "catherby_teleport", "Catherby Teleport", 87, 92, 2804, 3433, r("astral", 3, "law", 3, "water", 10));
        tele(L, "tele_group_catherby", "Tele Group Catherby", 88, 93, 2804, 3433, r("astral", 3, "law", 3, "water", 15));
        util(L, "recharge_dragonstone", "Recharge Dragonstone", 89, 97.5, r("astral", 1, "water", 4, "soul", 1),
                "Recharges the dragonstone jewellery you are wearing.");
        tele(L, "ice_plateau_teleport", "Ice Plateau Teleport", 89, 96, 2973, 3939, r("astral", 3, "law", 3, "water", 8));
        tele(L, "tele_group_ice_plateau", "Tele Group Ice Plateau", 90, 99, 2973, 3939, r("astral", 3, "law", 3, "water", 16));
        util(L, "energy_transfer", "Energy Transfer", 91, 100, r("astral", 3, "law", 2, "nature", 1), "Gives your special energy and run energy to another player.");
        util(L, "heal_other", "Heal Other", 92, 101, r("astral", 3, "law", 3, "blood", 1), "Transfers 75% of your hitpoints to another player.");
        util(L, "vengeance_other", "Vengeance Other", 93, 108, r("astral", 3, "death", 2, "earth", 10), "Casts Vengeance on another player.");
        util(L, "vengeance", "Vengeance", 94, 112, r("astral", 4, "death", 2, "earth", 10), "Reflects 75% of the next hit you take.");
        util(L, "heal_group", "Heal Group", 95, 124, r("astral", 4, "law", 6, "blood", 3), "Shares 75% of your hitpoints with nearby players.");
        util(L, "spellbook_swap", "Spellbook Swap", 96, 130, r("astral", 3, "cosmic", 2, "law", 1), "Use another spellbook for one cast.");

        // ======================= Arceuus =======================
        Book R = Book.ARCEUUS;
        SPELLS.put("arceuus_home_teleport", new Spell("arceuus_home_teleport", "Arceuus Home Teleport", R, 1, 0, Map.of(), Kind.TELEPORT, 0,
                Effect.NONE, 0, false, Teleports.ARCEUUS_HOME, null, "Teleports you to Arceuus."));
        tele(R, "arceuus_library_teleport", "Arceuus Library Teleport", 6, 10, 1632, 3838, r("law", 1, "earth", 2));
        util(R, "basic_reanimation", "Basic Reanimation", 16, 32, r("body", 4, "nature", 2), "Reanimates a basic ensouled head.");
        tele(R, "draynor_manor_teleport", "Draynor Manor Teleport", 17, 16, 3108, 3352, r("law", 1, "earth", 1, "water", 1));
        tele(R, "battlefront_teleport", "Battlefront Teleport", 23, 19, 1349, 3739, r("law", 1, "earth", 1, "fire", 1));
        tele(R, "mind_altar_teleport", "Mind Altar Teleport", 28, 22, 2979, 3509, r("law", 1, "mind", 1));
        tele(R, "respawn_teleport", "Respawn Teleport", 34, 27, 3222, 3218, r("law", 1, "soul", 1));
        combat(R, "ghostly_grasp", "Ghostly Grasp", 35, 22.5, 12, Effect.BIND, 4, false, r("chaos", 1, "air", 4), "Binds the target for 2 seconds.");
        util(R, "resurrect_lesser_ghost", "Resurrect Lesser Ghost", 38, 55, r("air", 10, "cosmic", 1, "mind", 5), "Summons a lesser ghostly thrall.");
        util(R, "resurrect_lesser_skeleton", "Resurrect Lesser Skeleton", 38, 55, r("air", 10, "cosmic", 1, "mind", 5), "Summons a lesser skeletal thrall.");
        util(R, "resurrect_lesser_zombie", "Resurrect Lesser Zombie", 38, 55, r("air", 10, "cosmic", 1, "mind", 5), "Summons a lesser zombified thrall.");
        tele(R, "salve_graveyard_teleport", "Salve Graveyard Teleport", 40, 30, 3432, 3460, r("law", 1, "soul", 2));
        util(R, "adept_reanimation", "Adept Reanimation", 41, 80, r("body", 4, "nature", 3, "soul", 1), "Reanimates an adept ensouled head.");
        combat(R, "inferior_demonbane", "Inferior Demonbane", 44, 27, 16, Effect.DEMON, 0, false, r("fire", 4, "chaos", 1), "Deals up to 16 damage to demons.");
        util(R, "shadow_veil", "Shadow Veil", 47, 58, r("earth", 5, "fire", 5, "cosmic", 5),
                "Failed pickpockets have a 15% chance to go unnoticed (no stun or damage).");
        tele(R, "fenkenstrains_castle_teleport", "Fenkenstrain's Castle Teleport", 48, 50, 3548, 3528, r("law", 1, "earth", 1, "soul", 1));
        util(R, "dark_lure", "Dark Lure", 50, 60, r("death", 1, "nature", 1), "Lures a monster to attack you.");
        combat(R, "skeletal_grasp", "Skeletal Grasp", 56, 33, 17, Effect.BIND, 8, false, r("death", 1, "earth", 8), "Binds the target for 5 seconds.");
        util(R, "resurrect_superior_ghost", "Resurrect Superior Ghost", 57, 70, r("earth", 10, "cosmic", 1, "death", 5), "Summons a superior ghostly thrall.");
        util(R, "resurrect_superior_skeleton", "Resurrect Superior Skeleton", 57, 70, r("earth", 10, "cosmic", 1, "death", 5), "Summons a superior skeletal thrall.");
        util(R, "resurrect_superior_zombie", "Resurrect Superior Zombie", 57, 70, r("earth", 10, "cosmic", 1, "death", 5), "Summons a superior zombified thrall.");
        util(R, "mark_of_darkness", "Mark of Darkness", 59, 70, r("cosmic", 1, "soul", 1), "Increases damage against demons by 25%.");
        tele(R, "west_ardougne_teleport", "West Ardougne Teleport", 61, 68, 2500, 3290, r("law", 2, "soul", 2));
        combat(R, "superior_demonbane", "Superior Demonbane", 62, 36, 23, Effect.DEMON, 0, false, r("fire", 5, "soul", 1), "Deals up to 23 damage to demons.");
        util(R, "lesser_corruption", "Lesser Corruption", 64, 75, r("death", 1, "soul", 2),
                "Your next hit on a player has a 50% chance to drain 1, 2 then 3 of their Prayer points.");
        tele(R, "harmony_island_teleport", "Harmony Island Teleport", 65, 74, 3797, 2866, r("law", 1, "nature", 1, "soul", 1));
        util(R, "vile_vigour", "Vile Vigour", 66, 76, r("air", 3, "soul", 1), "Restores run energy at the cost of prayer.");
        util(R, "degrime", "Degrime", 70, 83, r("earth", 4, "nature", 2), "Cleans all grimy herbs in your inventory.");
        tele(R, "cemetery_teleport", "Cemetery Teleport", 71, 82, 2978, 3763, r("blood", 1, "law", 1, "soul", 1));
        util(R, "expert_reanimation", "Expert Reanimation", 72, 138, r("blood", 1, "nature", 3, "soul", 2), "Reanimates an expert ensouled head.");
        util(R, "ward_of_arceuus", "Ward of Arceuus", 73, 83, r("cosmic", 1, "nature", 2, "soul", 4),
                "Binds on you last only 1 tick and you can't be corrupted.");
        util(R, "resurrect_greater_ghost", "Resurrect Greater Ghost", 76, 88, r("fire", 10, "cosmic", 1, "blood", 5), "Summons a greater ghostly thrall.");
        util(R, "resurrect_greater_skeleton", "Resurrect Greater Skeleton", 76, 88, r("fire", 10, "cosmic", 1, "blood", 5), "Summons a greater skeletal thrall.");
        util(R, "resurrect_greater_zombie", "Resurrect Greater Zombie", 76, 88, r("fire", 10, "cosmic", 1, "blood", 5), "Summons a greater zombified thrall.");
        util(R, "resurrect_crops", "Resurrect Crops", 78, 90, r("earth", 25, "blood", 8, "nature", 12, "soul", 8),
                "May bring a dead crop back to life (50% at 78 Magic, 75% at 99).");
        combat(R, "undead_grasp", "Undead Grasp", 79, 46.5, 24, Effect.BIND, 12, false, r("blood", 1, "fire", 4), "Binds the target for 7 seconds.");
        util(R, "death_charge", "Death Charge", 80, 90, r("blood", 1, "death", 1, "soul", 1), "Your next kill restores 15% special attack energy.");
        combat(R, "dark_demonbane", "Dark Demonbane", 82, 43.5, 30, Effect.DEMON, 0, false, r("fire", 6, "soul", 2), "Deals up to 30 damage to demons.");
        tele(R, "barrows_teleport", "Barrows Teleport", 83, 90, 3565, 3315, r("blood", 1, "law", 2, "soul", 2));
        util(R, "demonic_offering", "Demonic Offering", 84, 175, r("soul", 1, "wrath", 1), "Sacrifices up to 3 demonic ashes for 300% prayer XP.");
        util(R, "greater_corruption", "Greater Corruption", 85, 95, r("blood", 1, "soul", 3),
                "Your next hit on a player has a 50% chance to drain 2, 4 then 6 of their Prayer points.");
        tele(R, "ape_atoll_dungeon_teleport", "Ape Atoll Teleport", 90, 100, 2770, 2703, r("blood", 2, "law", 2, "soul", 2));
        util(R, "master_reanimation", "Master Reanimation", 90, 170, r("blood", 2, "nature", 4, "soul", 4), "Reanimates a master ensouled head.");
        util(R, "sinister_offering", "Sinister Offering", 92, 180, r("blood", 1, "wrath", 1), "Sacrifices up to 3 bones for 300% prayer XP.");
    }

    /** OSRS spells this mod can't support, with the reason (tracked by SpellbookAuditTest). */
    public static final Map<String, String> UNIMPLEMENTED = new LinkedHashMap<>();

    static {
        UNIMPLEMENTED.put("Teleport to Target", "needs Bounty Hunter targets, which the mod doesn't have");
    }

    private Spellbooks() {
    }

    /** True for Lunar "Tele Group" spells, which also take the players next to the caster along. */
    public static boolean isGroupTeleport(Spell s) {
        return s.id().startsWith("tele_group_");
    }

    /** Resurrect Crops success chance: 50% at 78 Magic rising linearly to 75% at 99 (boosts above 99 don't help). */
    public static double resurrectCropsChance(int magicLevel) {
        int lvl = Math.max(78, Math.min(99, magicLevel));
        return 0.50 + 0.25 * (lvl - 78) / 21.0;
    }

    /** Corruption: Prayer points drained by pulse 0, 1 and 2 (1/2/3, Greater Corruption 2/4/6). */
    public static int corruptionDrain(boolean greater, int pulse) {
        return (pulse + 1) * (greater ? 2 : 1);
    }

    /** Ticks between corruption pulses: 10 (three pulses over 18 s), 5 with Mark of Darkness. */
    public static int corruptionInterval(boolean markOfDarkness) {
        return markOfDarkness ? 5 : 10;
    }

    /** Vile Vigour: prayer points converted 1:1 into run energy percent (only what's missing, at most all prayer). */
    public static int vileVigourCost(int runEnergyPercent, int prayerPoints) {
        return Math.max(0, Math.min(100 - runEnergyPercent, prayerPoints));
    }

    /** Thrall max hit by tier (lesser 1, superior 2, greater 3); thralls always hit and attack every 4 ticks. */
    public static int thrallMaxHit(int tier) {
        return Math.max(0, Math.min(3, tier));
    }

    /** Shadow Veil: chance that a failed pickpocket goes unnoticed. */
    public static final double SHADOW_VEIL_CHANCE = 0.15;

    /** Potions Stat Restore Pot Share can split (restoring potions). */
    public static final java.util.Set<String> RESTORE_SHARE = java.util.Set.of("restore_potion", "super_restore", "prayer_potion",
            "energy_potion");
    /** Potions Boost Potion Share can split (stat-boosting potions). */
    public static final java.util.Set<String> BOOST_SHARE = java.util.Set.of("attack_potion", "strength_potion", "defence_potion",
            "super_attack", "super_strength", "super_defence", "ranging_potion", "magic_potion");

    /** Whether a potion share spell can split this potion. */
    public static boolean canShare(String spellId, String potionId) {
        return spellId.equals("stat_restore_pot_share") ? RESTORE_SHARE.contains(potionId)
                : spellId.equals("boost_potion_share") && BOOST_SHARE.contains(potionId);
    }

    /** Ticks a bind lasts on a target protected by Ward of Arceuus. */
    public static final int WARDED_BIND_TICKS = 1;

    public static Spell get(String id) {
        return SPELLS.get(id);
    }

    public static List<Spell> book(Book book) {
        List<Spell> out = new ArrayList<>();
        for (Spell s : SPELLS.values()) {
            if (s.book() == book) {
                out.add(s);
            }
        }
        out.sort((a, b) -> Integer.compare(a.level(), b.level()));
        return out;
    }

    public static List<Spell> all() {
        return Collections.unmodifiableList(new ArrayList<>(SPELLS.values()));
    }

    /** Magic dart max hit: 10 + floor(magic level / 10) (with a slayer's staff). */
    public static int magicDartMax(int magicLevel) {
        return 10 + magicLevel / 10;
    }

    /** Freeze duration in game ticks. */
    public static int freezeTicks(Spell spell) {
        return spell.effect() == Effect.FREEZE || spell.effect() == Effect.BIND ? spell.param() : 0;
    }

    /** Thrall tier from a resurrect spell id: 1 lesser, 2 superior, 3 greater (also its max hit). */
    public static int thrallTier(String id) {
        if (id.contains("greater")) {
            return 3;
        }
        if (id.contains("superior")) {
            return 2;
        }
        return id.contains("lesser") ? 1 : 0;
    }

    /** Ensouled heads: id, name, reanimation spell, prayer XP (wiki). */
    public record Head(String id, String name, String spell, double xp, String monsterKeyword, int dropRate) {
    }

    public static final List<Head> HEADS = List.of(
            new Head("ensouled_goblin_head", "Ensouled goblin head", "basic_reanimation", 130, "goblin", 35),
            new Head("ensouled_monkey_head", "Ensouled monkey head", "basic_reanimation", 182, "monkey", 35),
            new Head("ensouled_imp_head", "Ensouled imp head", "basic_reanimation", 286, "imp", 35),
            new Head("ensouled_minotaur_head", "Ensouled minotaur head", "basic_reanimation", 364, "minotaur", 35),
            new Head("ensouled_scorpion_head", "Ensouled scorpion head", "basic_reanimation", 454, "scorpion", 35),
            new Head("ensouled_bear_head", "Ensouled bear head", "basic_reanimation", 480, "bear", 35),
            new Head("ensouled_unicorn_head", "Ensouled unicorn head", "basic_reanimation", 494, "unicorn", 35),
            new Head("ensouled_dog_head", "Ensouled dog head", "adept_reanimation", 520, "dog", 35),
            new Head("ensouled_chaos_druid_head", "Ensouled chaos druid head", "adept_reanimation", 584, "chaos druid", 35),
            new Head("ensouled_giant_head", "Ensouled giant head", "adept_reanimation", 650, "giant", 35),
            new Head("ensouled_ogre_head", "Ensouled ogre head", "adept_reanimation", 716, "ogre", 35),
            new Head("ensouled_elf_head", "Ensouled elf head", "adept_reanimation", 754, "elf", 35),
            new Head("ensouled_troll_head", "Ensouled troll head", "adept_reanimation", 780, "troll", 35),
            new Head("ensouled_horror_head", "Ensouled horror head", "adept_reanimation", 832, "horror", 35),
            new Head("ensouled_kalphite_head", "Ensouled kalphite head", "expert_reanimation", 884, "kalphite", 35),
            new Head("ensouled_dagannoth_head", "Ensouled dagannoth head", "expert_reanimation", 936, "dagannoth", 35),
            new Head("ensouled_bloodveld_head", "Ensouled bloodveld head", "expert_reanimation", 1040, "bloodveld", 35),
            new Head("ensouled_tzhaar_head", "Ensouled tzhaar head", "expert_reanimation", 1104, "tz", 35),
            new Head("ensouled_demon_head", "Ensouled demon head", "expert_reanimation", 1170, "demon", 35),
            new Head("ensouled_hellhound_head", "Ensouled hellhound head", "expert_reanimation", 1200, "hellhound", 35),
            new Head("ensouled_aviansie_head", "Ensouled aviansie head", "master_reanimation", 1234, "aviansie", 35),
            new Head("ensouled_abyssal_head", "Ensouled abyssal head", "master_reanimation", 1300, "abyssal", 35),
            new Head("ensouled_dragon_head", "Ensouled dragon head", "master_reanimation", 1560, "dragon", 35));

    public static Head head(String itemId) {
        for (Head h : HEADS) {
            if (h.id().equals(itemId)) {
                return h;
            }
        }
        return null;
    }

    /** The ensouled head a monster (by name) can drop, or null. Longest keyword wins (e.g. "hellhound" over "dog"). */
    public static Head headFor(String monsterName) {
        String n = monsterName.toLowerCase(java.util.Locale.ROOT);
        Head best = null;
        for (Head h : HEADS) {
            if (n.contains(h.monsterKeyword()) && (best == null || h.monsterKeyword().length() > best.monsterKeyword().length())) {
                best = h;
            }
        }
        return best;
    }

    /** Teleport tablets: item id -> destination (standard tablets plus the house-tab style ones from clue rewards). */
    public record Tablet(String id, String name, Teleports.Destination destination, int value) {
    }

    public static final List<Tablet> TABLETS = List.of(
            new Tablet("varrock_teleport_tablet", "Varrock teleport", Teleports.at("Varrock", 3213, 3424), 600),
            new Tablet("lumbridge_teleport_tablet", "Lumbridge teleport", Teleports.at("Lumbridge", 3221, 3218), 600),
            new Tablet("falador_teleport_tablet", "Falador teleport", Teleports.at("Falador", 2964, 3378), 600),
            new Tablet("camelot_teleport_tablet", "Camelot teleport", Teleports.at("Camelot", 2757, 3479), 700),
            new Tablet("ardougne_teleport_tablet", "Ardougne teleport", Teleports.at("Ardougne", 2662, 3305), 700),
            new Tablet("watchtower_teleport_tablet", "Watchtower teleport", Teleports.at("Watchtower", 2546, 3112), 800),
            new Tablet("trollheim_teleport_tablet", "Trollheim teleport", Teleports.at("Trollheim", 2890, 3679), 800),
            new Tablet("kourend_castle_teleport_tablet", "Kourend castle teleport", Teleports.at("Kourend Castle", 1643, 3673), 800),
            new Tablet("paddewwa_teleport_tablet", "Paddewwa teleport", Teleports.at("Paddewwa", 3098, 3480), 900),
            new Tablet("senntisten_teleport_tablet", "Senntisten teleport", Teleports.at("Senntisten", 3320, 3337), 900),
            new Tablet("kharyrll_teleport_tablet", "Kharyrll teleport", Teleports.at("Kharyrll", 3494, 3473), 900),
            new Tablet("lassar_teleport_tablet", "Lassar teleport", Teleports.at("Lassar", 3004, 3470), 900),
            new Tablet("dareeyak_teleport_tablet", "Dareeyak teleport", Teleports.at("Dareeyak", 2968, 3696), 900),
            new Tablet("carrallanger_teleport_tablet", "Carrallanger teleport", Teleports.at("Carrallanger", 3158, 3666), 900),
            new Tablet("annakarl_teleport_tablet", "Annakarl teleport", Teleports.at("Annakarl", 3288, 3888), 900),
            new Tablet("ghorrock_teleport_tablet", "Ghorrock teleport", Teleports.at("Ghorrock", 2976, 3875), 900),
            new Tablet("moonclan_teleport_tablet", "Moonclan teleport", Teleports.at("Moonclan", 2113, 3915), 900),
            new Tablet("waterbirth_teleport_tablet", "Waterbirth teleport", Teleports.at("Waterbirth Island", 2546, 3755), 900),
            new Tablet("barbarian_teleport_tablet", "Barbarian teleport", Teleports.at("Barbarian Outpost", 2543, 3569), 900),
            new Tablet("khazard_teleport_tablet", "Khazard teleport", Teleports.at("Port Khazard", 2636, 3167), 900),
            new Tablet("fishing_guild_teleport_tablet", "Fishing guild teleport", Teleports.at("Fishing Guild", 2613, 3391), 900),
            new Tablet("catherby_teleport_tablet", "Catherby teleport", Teleports.at("Catherby", 2801, 3449), 900),
            new Tablet("ice_plateau_teleport_tablet", "Ice plateau teleport", Teleports.at("Ice Plateau", 2975, 3938), 900),
            new Tablet("arceuus_library_teleport_tablet", "Arceuus library teleport", Teleports.at("Arceuus Library", 1632, 3837), 700),
            new Tablet("draynor_manor_teleport_tablet", "Draynor manor teleport", Teleports.at("Draynor Manor", 3109, 3351), 700),
            new Tablet("salve_graveyard_teleport_tablet", "Salve graveyard teleport", Teleports.at("Salve Graveyard", 3433, 3461), 800),
            new Tablet("fenkenstrains_castle_teleport_tablet", "Fenkenstrain's castle teleport", Teleports.at("Fenkenstrain's Castle", 3548, 3529), 800),
            new Tablet("west_ardougne_teleport_tablet", "West Ardougne teleport", Teleports.at("West Ardougne", 2500, 3290), 800),
            new Tablet("harmony_island_teleport_tablet", "Harmony island teleport", Teleports.at("Harmony Island", 3797, 2866), 800),
            new Tablet("cemetery_teleport_tablet", "Cemetery teleport", Teleports.at("Forgotten Cemetery", 2981, 3764), 800),
            new Tablet("barrows_teleport_tablet", "Barrows teleport", Teleports.at("Barrows", 3564, 3315), 900),
            new Tablet("ape_atoll_teleport_tablet", "Ape atoll teleport", Teleports.at("Ape Atoll", 2770, 2703), 900),
            // House tablets redirected with a scroll of redirection (Nightmare Zone): outside each town's house portal.
            new Tablet("rimmington_teleport_tablet", "Rimmington teleport", Teleports.at("Rimmington", 2953, 3224), 1),
            new Tablet("taverley_teleport_tablet", "Taverley teleport", Teleports.at("Taverley", 2893, 3465), 1),
            new Tablet("pollnivneach_teleport_tablet", "Pollnivneach teleport", Teleports.at("Pollnivneach", 3340, 3004), 1),
            new Tablet("hosidius_teleport_tablet", "Hosidius teleport", Teleports.at("Hosidius", 1743, 3517), 1),
            new Tablet("rellekka_teleport_tablet", "Rellekka teleport", Teleports.at("Rellekka", 2670, 3631), 1),
            new Tablet("brimhaven_teleport_tablet", "Brimhaven teleport", Teleports.at("Brimhaven", 2757, 3179), 1),
            new Tablet("yanille_teleport_tablet", "Yanille teleport", Teleports.at("Yanille", 2544, 3096), 1),
            new Tablet("prifddinas_teleport_tablet", "Prifddinas teleport", Teleports.at("Prifddinas", 3239, 6077), 1),
            // Treasure trail teleport scrolls
            new Tablet("nardah_teleport", "Nardah teleport", Teleports.at("Nardah", 3419, 2917), 2500),
            new Tablet("digsite_teleport", "Digsite teleport", Teleports.at("Digsite", 3325, 3412), 2500),
            new Tablet("feldip_hills_teleport", "Feldip hills teleport", Teleports.at("Feldip Hills", 2541, 2925), 2500),
            new Tablet("lunar_isle_teleport", "Lunar isle teleport", Teleports.at("Lunar Isle", 2094, 3913), 2500),
            new Tablet("mortton_teleport", "Mort'ton teleport", Teleports.at("Mort'ton", 3488, 3288), 2500),
            new Tablet("pest_control_teleport", "Pest control teleport", Teleports.at("Void Knights' Outpost", 2658, 2659), 2500),
            new Tablet("piscatoris_teleport", "Piscatoris teleport", Teleports.at("Piscatoris", 2339, 3649), 2500),
            new Tablet("tai_bwo_wannai_teleport", "Tai bwo wannai teleport", Teleports.at("Tai Bwo Wannai", 2789, 3066), 2500),
            new Tablet("iorwerth_camp_teleport", "Iorwerth camp teleport", Teleports.at("Iorwerth Camp", 2194, 3258), 2500),
            new Tablet("mos_leharmless_teleport", "Mos le'harmless teleport", Teleports.at("Mos Le'Harmless", 3701, 2996), 2500),
            new Tablet("lumberyard_teleport", "Lumberyard teleport", Teleports.at("Lumber Yard", 3302, 3487), 2500),
            new Tablet("zul_andra_teleport", "Zul-andra teleport", Teleports.at("Zul-Andra", 2196, 3056), 2500),
            new Tablet("key_master_teleport", "Key master teleport", Teleports.at("Key Master", 2911, 3735), 2500),
            new Tablet("revenant_cave_teleport", "Revenant cave teleport", Teleports.at("Revenant Caves", 3128, 3832), 2500),
            new Tablet("watson_teleport", "Watson teleport", Teleports.at("Watson's house", 1645, 3580), 2500));

    public static Tablet tablet(String id) {
        for (Tablet t : TABLETS) {
            if (t.id().equals(id)) {
                return t;
            }
        }
        return null;
    }
}
