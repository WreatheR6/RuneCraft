package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.world.StructureDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Slayer mechanics as data: protective equipment, finishing items and special-weapon rules,
 * keyed by task category so superiors and other modules' creatures of that category share them.
 */
public final class SlayerGear {
    private SlayerGear() {
    }

    /** Where a protective item must be (NECK and HANDS are the systems module's extra worn slots). */
    public enum Where { HEAD, SHIELD, FEET, INVENTORY, NECK, HANDS }

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

    /** Slayer helmets (every recolour, plain and imbued) count as every head-slot protective item. */
    public static final List<String> SLAYER_HELMETS = join(SlayerRewards.slayerHelmets(false), SlayerRewards.slayerHelmets(true));
    /** Black masks and slayer helmets: 7/6 melee accuracy and damage against the current task. */
    public static final List<String> MELEE_BOOST_HEADGEAR = join(List.of("black_mask", "black_mask_i"), SLAYER_HELMETS);
    /** Imbued variants also give 15% ranged and magic accuracy and damage on task. */
    public static final List<String> IMBUED_HEADGEAR = join(List.of("black_mask_i"), SlayerRewards.slayerHelmets(true));

    private static List<String> join(List<String> a, List<String> b) {
        List<String> out = new ArrayList<>(a);
        out.addAll(b);
        return List.copyOf(out);
    }

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
            new Protection("cave_horrors", List.of("witchwood_icon"), Where.NECK, 10, 0, List.of(),
                    "The cave horror's terrible howl shakes you to the core!"),
            new Protection("fever_spiders", List.of("slayer_gloves"), Where.HANDS, 6, 2, List.of(Skill.ATTACK,
                    Skill.STRENGTH, Skill.DEFENCE), "The fever spider's bite gives you a terrible fever!"));

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

    /**
     * Other items that finish a creature like a {@link Finisher}'s own (OSRS Wiki "Rock thrownhammer": it finishes gargoyles
     * like the rock hammer, from a distance, and is used up).
     */
    public static final Map<String, String> FINISHER_ALTERNATES = Map.of("rock_hammer", "rock_thrownhammer");
    /** Alternates that are used up on use. */
    public static final List<String> CONSUMED_ALTERNATES = List.of("rock_thrownhammer");

    /** Categories only hurt by leaf-bladed weapons, broad ammunition or Magic Dart (slayer staff). */
    public static final List<String> LEAF_BLADED_CATEGORIES = List.of("turoth", "kurask");
    public static final List<String> LEAF_BLADED_WEAPONS = List.of("leaf_bladed_spear", "leaf_bladed_sword",
            "leaf_bladed_battleaxe");
    public static final List<String> BROAD_AMMO = List.of("broad_arrows", "broad_bolts", "amethyst_broad_bolts");
    public static final List<String> MAGIC_DART_STAVES = List.of("slayer_staff", "staff_of_the_dead");

    /**
     * Molanisks cling to the walls until a slayer bell (39 Slayer) is rung nearby (OSRS Wiki); until then they can't be
     * hurt. A ring lures those within {@link #BELL_RADIUS} blocks for {@link #BELL_LURE_TICKS} game ticks (approx).
     */
    public static final String BELL_CATEGORY = "molanisks";
    public static final String SLAYER_BELL = "slayer_bell";
    public static final int BELL_SLAYER_LEVEL = 39;
    public static final int BELL_RADIUS = 8;
    public static final int BELL_LURE_TICKS = 100;

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
        if (creatureId != null && EXEMPT.contains(creatureId)) {
            return null;
        }
        Protection own = creatureId == null ? null : CREATURE_PROTECTION.get(creatureId);
        return own != null ? own : protection(category);
    }

    /**
     * Creatures of a finisher or protection category that need neither: sulphur and grimy lizards die without an ice
     * cooler (Neypotzli's wyrmlings, like every wyrm, need no boots of their own: the Karuulm floor burns, see
     * {@link #hotFloorAt}).
     */
    public static final List<String> EXEMPT = List.of("sulphur_lizard", "grimy_lizard", "wyrmling");

    /**
     * The Karuulm Slayer Dungeon's volcanic floor (OSRS Wiki): beyond the entrance chamber it burns a player for 4 every
     * tick unless they wear boots of stone, boots of brimstone or granite boots, or have the elite Kourend &amp; Kebos
     * Diary's reward. It burns wherever the player stands in the dungeon, whatever they fight (wyrms, drakes, hydras,
     * sulphur lizards...).
     */
    public static final String HOT_FLOOR_DUNGEON = "dng_karuulm_slayer_dungeon";
    public static final int HOT_FLOOR_DAMAGE = 4;
    public static final List<String> HOT_FLOOR_BOOTS = List.of("boots_of_stone", "boots_of_brimstone", "granite_boots");
    /** The dungeon's entrance tile (its ladder lands in the entrance chamber, which is safe). */
    public static final int KARUULM_ENTRANCE_X = 1299;
    public static final int KARUULM_ENTRANCE_Y = 3806;
    public static final String HOT_FLOOR_MESSAGE = "The volcanic floor of the dungeon burns your feet!";

    /** True if a player standing at this block is on the Karuulm Slayer Dungeon's burning floor (beyond the entrance chamber). */
    public static boolean hotFloorAt(int x, int y, int z) {
        return Dungeons.inside(HOT_FLOOR_DUNGEON, x, y, z) && !Dungeons.karuulmEntranceChamber(x, z);
    }

    /** Burn damage per tick for a player on the hot floor wearing {@code feet} (0 when protected). */
    public static int hotFloorDamage(String feet, boolean eliteKourendDiary) {
        return eliteKourendDiary || HOT_FLOOR_BOOTS.contains(feet) ? 0 : HOT_FLOOR_DAMAGE;
    }

    /** The finishing item a creature needs: none for the {@link #EXEMPT} ones, else its task category's. */
    public static Finisher finisher(String creatureId, String category) {
        return creatureId != null && EXEMPT.contains(creatureId) ? null : finisher(category);
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
