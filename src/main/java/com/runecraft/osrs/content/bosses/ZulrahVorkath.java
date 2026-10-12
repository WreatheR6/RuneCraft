package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;

/**
 * Zulrah (three forms sharing one hitpoint pool, following one of four fixed rotations) and Vorkath
 * (acid and frozen-spawn specials every seventh attack, alternating).
 */
public final class ZulrahVorkath {
    private ZulrahVorkath() {
    }

    // ================= Zulrah =================
    public enum Form {
        SERPENTINE("zulrah"), MAGMA("zulrah_magma"), TANZANITE("zulrah_tanzanite"),
        /** Tanzanite form alternating ranged/magic quickly ("Jad phase"), starting with ranged. */
        JAD_RANGED("zulrah_tanzanite"),
        /** Jad phase starting with magic. */
        JAD_MAGIC("zulrah_tanzanite");

        public final String bossId;

        Form(String bossId) {
            this.bossId = bossId;
        }
    }

    /** Where Zulrah surfaces, as block offsets from the arena centre (north is -Z). */
    public enum Spot {
        CENTRE(0, -6), WEST(-8, 0), EAST(8, 0), SOUTH(0, 6);

        public final int dx;
        public final int dz;

        Spot(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }
    }

    /** One surfacing: form, spot, number of attacks before diving, and whether it spits clouds / snakelings. */
    public record Step(Form form, Spot spot, int attacks, boolean clouds, boolean snakelings) {
    }

    private static Step s(Form f, Spot sp, int attacks, boolean clouds, boolean snakes) {
        return new Step(f, sp, attacks, clouds, snakes);
    }

    /**
     * The four rotations (Magma A, Magma B, Serpentine, Tanzanite). Every rotation opens with the
     * serpentine form in the centre spitting clouds; after the last step it loops back to the start.
     * Attack counts are approximations of the wiki's tick timings.
     */
    public static final List<List<Step>> ROTATIONS = List.of(
            List.of(s(Form.SERPENTINE, Spot.CENTRE, 4, true, true), s(Form.MAGMA, Spot.CENTRE, 2, false, false),
                    s(Form.TANZANITE, Spot.CENTRE, 4, false, true), s(Form.SERPENTINE, Spot.SOUTH, 5, false, false),
                    s(Form.MAGMA, Spot.CENTRE, 2, false, false), s(Form.TANZANITE, Spot.WEST, 5, false, true),
                    s(Form.SERPENTINE, Spot.SOUTH, 4, true, true), s(Form.TANZANITE, Spot.SOUTH, 5, false, false),
                    s(Form.JAD_RANGED, Spot.WEST, 10, false, false), s(Form.MAGMA, Spot.CENTRE, 2, false, false)),
            List.of(s(Form.SERPENTINE, Spot.CENTRE, 4, true, true), s(Form.MAGMA, Spot.CENTRE, 2, false, false),
                    s(Form.TANZANITE, Spot.CENTRE, 4, false, true), s(Form.SERPENTINE, Spot.WEST, 5, false, false),
                    s(Form.TANZANITE, Spot.SOUTH, 5, false, true), s(Form.MAGMA, Spot.CENTRE, 2, false, false),
                    s(Form.SERPENTINE, Spot.EAST, 4, true, true), s(Form.TANZANITE, Spot.SOUTH, 5, false, false),
                    s(Form.JAD_RANGED, Spot.WEST, 10, false, false), s(Form.MAGMA, Spot.CENTRE, 2, false, false)),
            List.of(s(Form.SERPENTINE, Spot.CENTRE, 4, true, true), s(Form.SERPENTINE, Spot.EAST, 5, false, false),
                    s(Form.MAGMA, Spot.CENTRE, 2, false, false), s(Form.TANZANITE, Spot.WEST, 5, false, true),
                    s(Form.SERPENTINE, Spot.SOUTH, 4, true, true), s(Form.TANZANITE, Spot.EAST, 5, false, false),
                    s(Form.SERPENTINE, Spot.CENTRE, 4, true, false), s(Form.SERPENTINE, Spot.WEST, 5, false, true),
                    s(Form.JAD_MAGIC, Spot.CENTRE, 10, false, false), s(Form.MAGMA, Spot.CENTRE, 2, false, false)),
            List.of(s(Form.SERPENTINE, Spot.CENTRE, 4, true, true), s(Form.TANZANITE, Spot.EAST, 5, false, false),
                    s(Form.SERPENTINE, Spot.SOUTH, 5, false, true), s(Form.TANZANITE, Spot.WEST, 5, false, false),
                    s(Form.MAGMA, Spot.CENTRE, 2, false, false), s(Form.SERPENTINE, Spot.EAST, 4, true, true),
                    s(Form.TANZANITE, Spot.SOUTH, 5, false, false), s(Form.TANZANITE, Spot.WEST, 4, false, true),
                    s(Form.JAD_MAGIC, Spot.CENTRE, 10, false, false), s(Form.MAGMA, Spot.CENTRE, 2, false, false)));

    public static final AttackDef ZULRAH_RANGED = AttackDef.of("venom_spit", AttackType.RANGED, 41, 30, 1)
            .special(Special.VENOM, 0).particle("minecraft:sneeze");
    public static final AttackDef ZULRAH_MAGIC = AttackDef.of("venom_bolt", AttackType.MAGIC, 41, 30, 1)
            .special(Special.VENOM, 0).particle("minecraft:witch");
    /** Magma form stares at your tile, then slams its head down: step off the tile or be hit and stunned. */
    public static final AttackDef ZULRAH_TAIL_SLAM = AttackDef.of("tail_slam", AttackType.CRUSH, 41, 30, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(4, "Zulrah stares at your position...").particle("minecraft:lava");
    /** Toxic fumes: the script places venom clouds where this lands. */
    public static final AttackDef ZULRAH_CLOUD = AttackDef.of("toxic_cloud", AttackType.RANGED, 4, 30, 1)
            .special(Special.HAZARD, 4).particle("minecraft:sneeze");

    public static List<BossDef> all() {
        return List.of(zulrah(), zulrahMagma(), zulrahTanzanite(), vorkath());
    }

    private static BossDef zulrahBase(String id) {
        return BossDef.of(id, "Zulrah").level(725).stats(500, 300, 300, 300, 300, 300).speed(4)
                .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH).poisonous()
                .size(1.4f, 3.4f, 1.6f).moveSpeed(0.0);
    }

    static BossDef zulrah() {
        return zulrahBase("zulrah").defenceBonus(0, 0, 0, -45, 50)
                .attack(ZULRAH_RANGED).attack(ZULRAH_CLOUD)
                .drops(Drop.of("zulrahs_scales", 100, 299, 1), Drop.of("zulrahs_scales", 500, 500, 2),
                        Drop.of("tanzanite_fang", 1, 1, 512), Drop.of("magic_fang", 1, 1, 512), Drop.of("serpentine_visage", 1, 1, 512),
                        Drop.of("uncut_onyx", 1, 1, 512), Drop.of("tanzanite_mutagen", 1, 1, 6553), Drop.of("magma_mutagen", 1, 1, 6553),
                        Drop.of("jar_of_swamp", 1, 1, 3000), Drop.of("magic_logs", 100, 100, 12), Drop.of("runite_ore", 2, 2, 12),
                        Drop.of("snapdragon_seed", 2, 2, 12), Drop.of("dragon_bones", 30, 30, 12), Drop.of("death_rune", 300, 300, 12),
                        Drop.of("law_rune", 200, 200, 12), Drop.of("coal", 200, 200, 12), Drop.of("raw_shark", 100, 100, 16),
                        Drop.of("antifire_potion", 5, 5, 16), Drop.of("clue_scroll_elite", 1, 1, 75))
                .pet("pet_snakeling").at("zul_andra").arena(14, "water");
    }

    static BossDef zulrahMagma() {
        return zulrahBase("zulrah_magma").defenceBonus(0, 0, 0, 0, 300).attack(ZULRAH_TAIL_SLAM).raidOnly();
    }

    static BossDef zulrahTanzanite() {
        return zulrahBase("zulrah_tanzanite").defenceBonus(0, 0, 0, 300, 0).attack(ZULRAH_MAGIC).attack(ZULRAH_RANGED).raidOnly();
    }

    // ================= Vorkath =================
    public static final AttackDef VORKATH_CLAW = AttackDef.of("claw", AttackType.SLASH, 32, 3.5, 2);
    public static final AttackDef VORKATH_SPIKE = AttackDef.of("bone_spike", AttackType.RANGED, 32, 30, 3).particle("minecraft:crit");
    public static final AttackDef VORKATH_MAGIC = AttackDef.of("undead_magic", AttackType.MAGIC, 30, 30, 3)
            .particle("minecraft:soul_fire_flame");
    public static final AttackDef VORKATH_DRAGONFIRE = AttackDef.of("dragonfire", AttackType.MAGIC, 73, 30, 2)
            .special(Special.DRAGONFIRE, 0).particle("minecraft:flame");
    /** Pink dragonfire switches off the target's prayers (script). */
    public static final AttackDef VORKATH_PINK_FIRE = AttackDef.of("pink_dragonfire", AttackType.MAGIC, 73, 30, 1)
            .special(Special.DRAGONFIRE, 0).particle("minecraft:dragon_breath");
    public static final AttackDef VORKATH_VENOM_FIRE = AttackDef.of("venomous_dragonfire", AttackType.MAGIC, 8, 30, 1)
            .special(Special.VENOM, 0).particle("minecraft:sneeze");
    /** The dragonfire bomb arcs onto your tile: move two tiles or take up to 121. */
    public static final AttackDef VORKATH_BOMB = AttackDef.of("dragonfire_bomb", AttackType.MAGIC, 121, 30, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(4, "Vorkath launches a dragonfire bomb high into the air!")
            .particle("minecraft:flame");
    /** Acid phase rapid fire: 25 quick fireballs at your previous tiles. */
    public static final AttackDef VORKATH_RAPID_FIRE = AttackDef.of("rapid_fire", AttackType.MAGIC, 30, 30, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(2, null).particle("minecraft:small_flame");
    public static final AttackDef VORKATH_FREEZE = AttackDef.of("ice_dragonfire", AttackType.MAGIC, 0, 30, 1)
            .special(Special.FREEZE, 30).particle("minecraft:snowflake")
            .message("Vorkath's icy breath freezes you solid! Kill the zombified spawn!");

    static BossDef vorkath() {
        return BossDef.of("vorkath", "Vorkath").level(732).stats(750, 560, 308, 214, 150, 308).speed(5)
                .defenceBonus(26, 108, 108, 240, 26)
                .attack(VORKATH_CLAW).attack(VORKATH_SPIKE).attack(VORKATH_MAGIC).attack(VORKATH_DRAGONFIRE)
                .attack(VORKATH_PINK_FIRE).attack(VORKATH_VENOM_FIRE).attack(VORKATH_BOMB)
                .drops(Drop.of("superior_dragon_bones", 2, 2, 1), Drop.of("blue_dragonhide", 2, 2, 1),
                        Drop.of("vorkaths_head", 1, 1, 50), Drop.of("skeletal_visage", 1, 1, 5000), Drop.of("draconic_visage", 1, 1, 5000),
                        Drop.of("dragonbone_necklace", 1, 1, 1000), Drop.of("jar_of_decay", 1, 1, 3000),
                        Drop.of("dragon_bones", 10, 20, 8), Drop.of("rune_kiteshield", 1, 1, 12), Drop.of("dragon_platelegs", 1, 1, 20),
                        Drop.of("runite_ore", 4, 4, 12), Drop.of("magic_logs", 50, 50, 12), Drop.of("death_rune", 150, 150, 12),
                        Drop.of("wrath_rune", 60, 60, 12), Drop.of("coins", 20000, 80000, 6), Drop.of("dragonstone", 2, 2, 16),
                        Drop.of("clue_scroll_elite", 1, 1, 65))
                .pet("pet_vorki").at("ungael").arena(13, "ice")
                .size(1.8f, 2.0f, 2.2f).moveSpeed(0.0);
    }
}
