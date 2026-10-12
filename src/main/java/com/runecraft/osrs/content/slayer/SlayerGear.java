package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.Skill;

import java.util.List;
import java.util.Map;

/**
 * Slayer mechanics as data: protective equipment, finishing items and special-weapon rules,
 * keyed by task category so superiors and other modules' creatures of that category share them.
 */
public final class SlayerGear {
    private SlayerGear() {
    }

    /** Where a protective item must be. */
    public enum Where { HEAD, SHIELD, FEET, INVENTORY }

    /**
     * Protection against a creature's special attack. Without any of {@code items} in {@code where}
     * the creature's attacks also deal up to {@code damage} extra and drain {@code drain} levels of
     * each listed skill.
     */
    public record Protection(String category, List<String> items, Where where, int damage, int drain, List<Skill> drains,
                             String message) {
    }

    /**
     * A creature that can't die until finished with an item at {@code threshold} Hitpoints or fewer.
     *
     * @param consumed whether the item is used up (bag of salt, ice cooler)
     * @param unlock   reward unlock that finishes it automatically
     */
    public record Finisher(String category, String item, int threshold, boolean consumed, String unlock, String verb) {
    }

    /** Slayer helmets count as every head-slot protective item. */
    public static final List<String> SLAYER_HELMETS = List.of("slayer_helmet", "slayer_helmet_i");
    /** Black masks and slayer helmets: 7/6 melee accuracy and damage against the current task. */
    public static final List<String> MELEE_BOOST_HEADGEAR = List.of("black_mask", "black_mask_i", "slayer_helmet", "slayer_helmet_i");
    /** Imbued variants also give 15% ranged and magic accuracy and damage on task. */
    public static final List<String> IMBUED_HEADGEAR = List.of("black_mask_i", "slayer_helmet_i");

    private static final List<Skill> COMBAT = List.of(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC);

    public static final List<Protection> PROTECTION = List.of(
            new Protection("banshees", List.of("earmuffs"), Where.HEAD, 8, 2, List.of(Skill.ATTACK, Skill.STRENGTH,
                    Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.AGILITY), "The banshee's piercing scream rings in your ears!"),
            new Protection("aberrant_spectres", List.of("nose_peg"), Where.HEAD, 14, 3, List.of(Skill.ATTACK, Skill.STRENGTH,
                    Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.PRAYER, Skill.AGILITY), "The aberrant spectre's stench overwhelms you!"),
            new Protection("dust_devils", List.of("facemask"), Where.HEAD, 16, 2, COMBAT, "The devil's dust blinds you and you choke!"),
            new Protection("smoke_devils", List.of("facemask"), Where.HEAD, 18, 0, List.of(), "The smoke burns your lungs!"),
            new Protection("wall_beasts", List.of("spiny_helmet"), Where.HEAD, 18, 0, List.of(),
                    "A giant hand grabs you from the wall and slams you about!"),
            new Protection("cockatrice", List.of("mirror_shield", "vs_shield"), Where.SHIELD, 11, 3, List.of(Skill.ATTACK,
                    Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.AGILITY), "The cockatrice's gaze petrifies you!"),
            new Protection("basilisks", List.of("mirror_shield", "vs_shield"), Where.SHIELD, 12, 3, List.of(Skill.ATTACK,
                    Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.AGILITY), "The basilisk's gaze turns your blood cold!"),
            new Protection("killerwatts", List.of("insulated_boots"), Where.FEET, 15, 0, List.of(),
                    "You're shocked by a jolt of electricity through the floor!"),
            new Protection("cave_horrors", List.of("witchwood_icon"), Where.INVENTORY, 10, 0, List.of(),
                    "The cave horror's terrible howl shakes you to the core!"),
            new Protection("fever_spiders", List.of("slayer_gloves"), Where.INVENTORY, 6, 2, List.of(Skill.ATTACK,
                    Skill.STRENGTH, Skill.DEFENCE), "The fever spider's bite gives you a terrible fever!"),
            new Protection("drakes", List.of("boots_of_stone", "boots_of_brimstone"), Where.FEET, 4, 0, List.of(),
                    "The volcanic floor of the dungeon burns your feet!"),
            new Protection("wyrms", List.of("boots_of_stone", "boots_of_brimstone"), Where.FEET, 4, 0, List.of(),
                    "The volcanic floor of the dungeon burns your feet!"),
            new Protection("hydras", List.of("boots_of_stone", "boots_of_brimstone"), Where.FEET, 4, 0, List.of(),
                    "The volcanic floor of the dungeon burns your feet!"));

    /**
     * Creatures whose gaze is stronger than their task category's (basilisk knights and their superior count as
     * basilisks since the Basilocked rework but hit harder), keyed by creature id.
     */
    public static final Map<String, Protection> CREATURE_PROTECTION = Map.of(
            "basilisk_knight", new Protection("basilisks", List.of("mirror_shield", "vs_shield"), Where.SHIELD, 20, 4, COMBAT,
                    "The basilisk knight's gaze petrifies you!"),
            "basilisk_sentinel", new Protection("basilisks", List.of("mirror_shield", "vs_shield"), Where.SHIELD, 20, 4, COMBAT,
                    "The basilisk sentinel's gaze petrifies you!"));

    public static final List<Finisher> FINISHERS = List.of(
            new Finisher("gargoyles", "rock_hammer", 9, false, "gargoyle_smasher", "You smash the gargoyle with the rock hammer."),
            new Finisher("rockslugs", "bag_of_salt", 4, true, "slug_salter", "The rockslug shrivels and dies."),
            new Finisher("lizards", "ice_cooler", 4, true, "reptile_freezer", "The lizard freezes and shatters."),
            new Finisher("mutated_zygomites", "fungicide_spray", 7, false, "shroom_sprayer",
                    "The zygomite seems to wither and dies."));

    /** Categories only hurt by leaf-bladed weapons, broad ammunition or Magic Dart (slayer staff). */
    public static final List<String> LEAF_BLADED_CATEGORIES = List.of("turoth", "kurask");
    public static final List<String> LEAF_BLADED_WEAPONS = List.of("leaf_bladed_spear", "leaf_bladed_sword",
            "leaf_bladed_battleaxe");
    public static final List<String> BROAD_AMMO = List.of("broad_arrows");
    public static final List<String> MAGIC_DART_STAVES = List.of("slayer_staff", "staff_of_the_dead");

    /** Harpie bug swarms can only be damaged while carrying a lit bug lantern. */
    public static final Map<String, String> LIGHT_SOURCE = Map.of("harpie_bug_swarms", "bug_lantern");

    public static Protection protection(String category) {
        for (Protection p : PROTECTION) {
            if (p.category().equals(category)) {
                return p;
            }
        }
        return null;
    }

    /** The protection a creature needs: its own override, else its task category's. */
    public static Protection protection(String creatureId, String category) {
        Protection own = creatureId == null ? null : CREATURE_PROTECTION.get(creatureId);
        return own != null ? own : protection(category);
    }

    public static Finisher finisher(String category) {
        for (Finisher f : FINISHERS) {
            if (f.category().equals(category)) {
                return f;
            }
        }
        return null;
    }
}
