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
        /** The green form alternating ranged/magic every attack ("Jad" phase), starting with ranged (rotations 1 and 2). */
        JAD_RANGED("zulrah"),
        /** The green "Jad" phase starting with magic (rotations 3 and 4). */
        JAD_MAGIC("zulrah");

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

    /**
     * One surfacing (a wiki "phase"): form, spot and the actions it performs in order before diving, one per attack
     * cycle: {@code A} an attack, {@code C} a venom cloud barrage, {@code S} a snakeling orb.
     */
    public record Step(Form form, Spot spot, String actions) {
        public Step {
            if (!actions.matches("[ACS]+")) {
                throw new IllegalArgumentException("Bad Zulrah actions " + actions);
            }
        }

        private long count(char c) {
            return actions.chars().filter(x -> x == c).count();
        }

        public int attacks() {
            return (int) count('A');
        }

        public int cloudBarrages() {
            return (int) count('C');
        }

        public int snakelingOrbs() {
            return (int) count('S');
        }

        public boolean clouds() {
            return cloudBarrages() > 0;
        }

        public boolean snakelings() {
            return snakelingOrbs() > 0;
        }
    }

    private static Step s(Form f, Spot sp, String actions) {
        return new Step(f, sp, actions);
    }

    private static final String JAD10 = "AAAAAAAAAA";

    /**
     * The first phase of the fight: green, in the middle, filling the shrine with 4 venom cloud barrages without attacking.
     * Every rotation's first entry is this.
     */
    public static final Step OPENING = s(Form.SERPENTINE, Spot.CENTRE, "CCCC");
    /**
     * The first phase of every later rotation (the wiki's last phase of each rotation): green, in the middle, 5 ranged
     * attacks and then 4 venom cloud barrages.
     */
    public static final Step LOOP_START = s(Form.SERPENTINE, Spot.CENTRE, "AAAAACCCC");

    /**
     * The four rotations (OSRS Wiki, Zulrah/Strategies "Rotation overview": Crimson A, Crimson B, Serp, Tanz), phase by
     * phase. Phase 1 is {@link #OPENING} at the start of the fight and {@link #LOOP_START} when a new rotation begins
     * after a full one. Where the wiki says "an alternating series" of orbs and barrages, they alternate starting with
     * the first one named.
     */
    public static final List<List<Step>> ROTATIONS = List.of(
            List.of(OPENING, s(Form.MAGMA, Spot.CENTRE, "AA"), s(Form.TANZANITE, Spot.CENTRE, "AAAA"),
                    s(Form.SERPENTINE, Spot.SOUTH, "AAAAASSCCSS"), s(Form.MAGMA, Spot.CENTRE, "AA"),
                    s(Form.TANZANITE, Spot.WEST, "AAAAA"), s(Form.SERPENTINE, Spot.SOUTH, "CCCSSSS"),
                    s(Form.TANZANITE, Spot.SOUTH, "AAAAASCSCS"), s(Form.JAD_RANGED, Spot.WEST, JAD10 + "CCCC"),
                    s(Form.MAGMA, Spot.CENTRE, "AA")),
            List.of(OPENING, s(Form.MAGMA, Spot.CENTRE, "AA"), s(Form.TANZANITE, Spot.CENTRE, "AAAA"),
                    s(Form.SERPENTINE, Spot.WEST, "CCCSSSS"), s(Form.TANZANITE, Spot.SOUTH, "AAAAASSCCSS"),
                    s(Form.MAGMA, Spot.CENTRE, "AA"), s(Form.SERPENTINE, Spot.EAST, "AAAAA"),
                    s(Form.TANZANITE, Spot.SOUTH, "AAAAASCSCS"), s(Form.JAD_RANGED, Spot.WEST, JAD10 + "CCCC"),
                    s(Form.MAGMA, Spot.CENTRE, "AA")),
            List.of(OPENING, s(Form.SERPENTINE, Spot.EAST, "AAAAASSS"), s(Form.MAGMA, Spot.CENTRE, "CSCSCSAA"),
                    s(Form.TANZANITE, Spot.WEST, "AAAAA"), s(Form.SERPENTINE, Spot.SOUTH, "AAAAA"),
                    s(Form.TANZANITE, Spot.EAST, "AAAAA"), s(Form.SERPENTINE, Spot.CENTRE, "CCCSSS"),
                    s(Form.SERPENTINE, Spot.WEST, "AAAAA"), s(Form.TANZANITE, Spot.CENTRE, "AAAAACCSSS"),
                    s(Form.JAD_MAGIC, Spot.EAST, JAD10), s(Form.TANZANITE, Spot.CENTRE, "SSSS")),
            List.of(OPENING, s(Form.TANZANITE, Spot.EAST, "SSSSAAAAAA"), s(Form.SERPENTINE, Spot.SOUTH, "AAAACC"),
                    s(Form.TANZANITE, Spot.WEST, "SSSSAAAA"), s(Form.MAGMA, Spot.CENTRE, "AACC"),
                    s(Form.SERPENTINE, Spot.EAST, "AAAA"), s(Form.SERPENTINE, Spot.SOUTH, "SSSSSSCCC"),
                    s(Form.TANZANITE, Spot.WEST, "AAAAASSSS"), s(Form.SERPENTINE, Spot.CENTRE, "AAAA"),
                    s(Form.TANZANITE, Spot.CENTRE, "AAAACCC"), s(Form.JAD_MAGIC, Spot.EAST, "AAAAAAAA"),
                    s(Form.TANZANITE, Spot.CENTRE, "SSSS")));

    /** Damage to Zulrah is capped at 50; a bigger hit deals 45-50 instead (OSRS Wiki). */
    public static final int DAMAGE_CAP = 50;

    public static int capDamage(int damage, int roll0to5) {
        return damage > DAMAGE_CAP ? 45 + roll0to5 : damage;
    }

    /** Snakelings die off after 40 seconds (67 game ticks) and have 1 hitpoint. */
    public static final int SNAKELING_LIFE_TICKS = 67;

    public static final AttackDef ZULRAH_RANGED = AttackDef.of("venom_spit", AttackType.RANGED, 41, 30, 1)
            .special(Special.VENOM, 0).particle("minecraft:sneeze");
    public static final AttackDef ZULRAH_MAGIC = AttackDef.of("venom_bolt", AttackType.MAGIC, 41, 30, 1)
            .special(Special.VENOM, 0).particle("minecraft:witch");
    /** Magma form stares at your tile, then whips its tail down: step off the tile or take 20-30 and a stun (OSRS Wiki). */
    public static final AttackDef ZULRAH_TAIL_SLAM = AttackDef.of("tail_slam", AttackType.CRUSH, 30, 30, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(4, "Zulrah stares at your position...").particle("minecraft:lava");
    /** Toxic fumes: the script places venom clouds where this lands. */
    /** The tail slam's least damage when it lands. */
    public static final int ZULRAH_TAIL_SLAM_MIN = 20;
    public static final AttackDef ZULRAH_CLOUD = AttackDef.of("toxic_cloud", AttackType.RANGED, 4, 30, 1)
            .special(Special.HAZARD, 4).particle("minecraft:sneeze");

    public static List<BossDef> all() {
        return List.of(zulrah(), zulrahMagma(), zulrahTanzanite(), vorkath());
    }

    private static BossDef zulrahBase(String id) {
        return BossDef.of(id, "Zulrah").level(725).stats(500, 300, 300, 300, 300, 300).speed(4)
                .attackBonus(0, 50, 50)
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
    // Wiki: magic and ranged weighted 3 to 4 (Mod Ash); dragonfire up to 80 after the quest (72 in Dragon Slayer II).
    public static final AttackDef VORKATH_SPIKE = AttackDef.of("bone_spike", AttackType.RANGED, 32, 30, 4).particle("minecraft:crit");
    public static final AttackDef VORKATH_MAGIC = AttackDef.of("undead_magic", AttackType.MAGIC, 30, 30, 3)
            .particle("minecraft:soul_fire_flame");
    public static final AttackDef VORKATH_DRAGONFIRE = AttackDef.of("dragonfire", AttackType.MAGIC, 80, 30, 2)
            .special(Special.DRAGONFIRE, 0).particle("minecraft:flame");
    /** Pink dragonfire switches off the target's prayers (script). */
    public static final AttackDef VORKATH_PINK_FIRE = AttackDef.of("pink_dragonfire", AttackType.MAGIC, 80, 30, 1)
            .special(Special.DRAGONFIRE, 0).particle("minecraft:dragon_breath");
    public static final AttackDef VORKATH_VENOM_FIRE = AttackDef.of("venomous_dragonfire", AttackType.MAGIC, 8, 30, 1)
            .special(Special.VENOM, 0).particle("minecraft:sneeze");
    /** The dragonfire bomb arcs onto your tile: move two tiles or take up to 121. */
    public static final AttackDef VORKATH_BOMB = AttackDef.of("dragonfire_bomb", AttackType.MAGIC, 121, 30, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(4, "Vorkath launches a dragonfire bomb high into the air!")
            .particle("minecraft:flame");
    /** Acid phase rapid fire: a fireball every tick at the tile you stand on, 25-41 regardless of dragonfire protection. */
    public static final AttackDef VORKATH_RAPID_FIRE = AttackDef.of("rapid_fire", AttackType.MAGIC, 41, 30, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(2, null).particle("minecraft:small_flame");
    public static final AttackDef VORKATH_FREEZE = AttackDef.of("ice_dragonfire", AttackType.MAGIC, 0, 30, 1)
            .special(Special.FREEZE, 30).particle("minecraft:snowflake")
            .message("Vorkath's icy breath freezes you solid! Kill the zombified spawn!");

    /** Rapid fire's minimum damage (OSRS Wiki: 25-41). */
    public static final int RAPID_FIRE_MIN = 25;
    /** Fireballs in the acid phase (one per tick). */
    public static final int RAPID_FIRE_SHOTS = 25;
    /** An acid pool deals up to 10 and Vorkath leeches it as health. */
    public static final int ACID_MAX = 10;
    /** Vorkath takes half damage during the acid phase. */
    public static final double ACID_PHASE_DAMAGE = 0.5;
    /** The zombified spawn's explosion: up to 60, scaled by its remaining hitpoints (38 at full). */
    public static final int SPAWN_EXPLOSION_MAX = 60;

    public static int spawnExplosion(double spawnHp, double spawnMaxHp) {
        return (int) Math.floor(SPAWN_EXPLOSION_MAX * Math.max(0, Math.min(1, spawnHp / spawnMaxHp)));
    }

    /** Hits on Vorkath during one acid phase that complete "The Walk" (with no rapid fire or acid damage taken). */
    public static final int THE_WALK_HITS = 12;

    static BossDef vorkath() {
        return BossDef.of("vorkath", "Vorkath").level(732).stats(750, 560, 308, 214, 150, 308).speed(5)
                .attackBonus(16, 150, 78) // approx.
                .defenceBonus(26, 108, 108, 240, 26)
                .attack(VORKATH_CLAW).attack(VORKATH_SPIKE).attack(VORKATH_MAGIC).attack(VORKATH_DRAGONFIRE)
                .attack(VORKATH_PINK_FIRE).attack(VORKATH_VENOM_FIRE).attack(VORKATH_BOMB)
                .drops(Drop.of("superior_dragon_bones", 2, 2, 1), Drop.of("blue_dragonhide", 2, 2, 1),
                        Drop.of("vorkaths_head", 1, 1, 50), Drop.of("skeletal_visage", 1, 1, 5000), Drop.of("draconic_visage", 1, 1, 5000),
                        Drop.of("dragonbone_necklace", 1, 1, 1000), Drop.of("jar_of_decay", 1, 1, 3000),
                        Drop.of("dragon_bones", 10, 20, 8), Drop.of("rune_kiteshield", 1, 1, 12), Drop.of("dragon_platelegs", 1, 1, 20),
                        Drop.of("runite_ore", 4, 4, 12), Drop.of("magic_logs", 50, 50, 12), Drop.of("death_rune", 150, 150, 12),
                        Drop.of("wrath_rune", 60, 60, 12), Drop.of("coins", 20000, 80000, 6), Drop.of("dragonstone", 2, 2, 16),
                        Drop.of("dragon_bolts_unf", 50, 100, 19),
                        Drop.of("clue_scroll_elite", 1, 1, 65))
                .pet("pet_vorki").at("ungael").arena(13, "ice")
                .size(1.8f, 2.0f, 2.2f).moveSpeed(0.0);
    }
}
