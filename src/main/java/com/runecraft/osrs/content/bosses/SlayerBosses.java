package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;

/**
 * Slayer bosses: Kraken, Thermonuclear smoke devil, Cerberus, Abyssal Sire, Alchemical Hydra,
 * Grotesque Guardians and Araxxor. Each requires its real Slayer level.
 */
public final class SlayerBosses {
    private SlayerBosses() {
    }

    // ---- Cerberus ----
    public static final AttackDef CERB_MELEE = AttackDef.of("bite", AttackType.CRUSH, 23, 3.5, 4);
    public static final AttackDef CERB_RANGED = AttackDef.of("fireball", AttackType.RANGED, 23, 14, 3).particle("minecraft:flame");
    public static final AttackDef CERB_MAGIC = AttackDef.of("soul_blast", AttackType.MAGIC, 23, 14, 3)
            .particle("minecraft:soul_fire_flame");
    /** "Grrrrrrrrrrrrrr!" - lava pools erupt under and around you. */
    public static final AttackDef CERB_LAVA = AttackDef.of("lava_pool", AttackType.MAGIC, 15, 14, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(4, "Cerberus: Grrrrrrrrrrrrrr!").particle("minecraft:lava");

    // ---- Alchemical Hydra ----
    public static final AttackDef HYDRA_RANGED = AttackDef.of("ranged", AttackType.RANGED, 28, 14, 1).particle("minecraft:crit");
    public static final AttackDef HYDRA_MAGIC = AttackDef.of("magic", AttackType.MAGIC, 28, 14, 1).particle("minecraft:witch");
    public static final AttackDef HYDRA_POISON = AttackDef.of("poison_pool", AttackType.RANGED, 12, 14, 1)
            .special(Special.HAZARD, 4).particle("minecraft:sneeze");
    public static final AttackDef HYDRA_LIGHTNING = AttackDef.of("lightning", AttackType.MAGIC, 20, 14, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(3, "The Alchemical Hydra calls down lightning!")
            .particle("minecraft:electric_spark");
    public static final AttackDef HYDRA_FIRE = AttackDef.of("fire_wall", AttackType.MAGIC, 20, 14, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(3, "The Alchemical Hydra breathes a wall of fire!").particle("minecraft:flame");

    /** Colour phases: model/boss id, vent colour that must be used, special attack and head count. */
    public enum HydraPhase {
        GREEN("alchemical_hydra", "green", HYDRA_POISON, 4, 75),
        BLUE("alchemical_hydra_blue", "blue", HYDRA_LIGHTNING, 3, 50),
        RED("alchemical_hydra_red", "red", HYDRA_FIRE, 2, 25),
        ENRAGED("alchemical_hydra_enraged", null, HYDRA_POISON, 1, 0);

        public final String bossId;
        /** Vent colour that removes this phase's 75% damage reduction (null: no shield). */
        public final String vent;
        public final AttackDef special;
        public final int heads;
        /** Hitpoint percentage at which the next phase begins. */
        public final int endsAtPercent;

        HydraPhase(String bossId, String vent, AttackDef special, int heads, int endsAtPercent) {
            this.bossId = bossId;
            this.vent = vent;
            this.special = special;
            this.heads = heads;
            this.endsAtPercent = endsAtPercent;
        }

        public HydraPhase next() {
            return this == ENRAGED ? ENRAGED : values()[ordinal() + 1];
        }

        public static HydraPhase of(String bossId) {
            for (HydraPhase p : values()) {
                if (p.bossId.equals(bossId)) {
                    return p;
                }
            }
            return GREEN;
        }
    }

    /** Damage multiplier while the Hydra's chemical shield is intact. */
    public static final double HYDRA_SHIELD_MULTIPLIER = 0.25;

    // ---- Grotesque Guardians ----
    public static final AttackDef DUSK_MELEE = AttackDef.of("claw", AttackType.SLASH, 28, 3.5, 1);
    public static final AttackDef DUSK_EXPLOSION = AttackDef.of("explosion", AttackType.CRUSH, 50, 3, 1)
            .special(Special.AOE, 3).telegraph(5, "Dusk gathers a great power... get away from him!").particle("minecraft:explosion");
    public static final AttackDef DAWN_RANGED = AttackDef.of("rock", AttackType.RANGED, 28, 14, 1).particle("minecraft:crit");
    public static final AttackDef DAWN_SPHERES = AttackDef.of("energy_sphere", AttackType.MAGIC, 25, 14, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(5, "Dawn fires energy spheres - stand on them to absorb them!")
            .particle("minecraft:end_rod");
    public static final AttackDef GUARDIAN_LIGHTNING = AttackDef.of("lightning", AttackType.MAGIC, 15, 14, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(3, "Lightning crackles across the rooftop!").particle("minecraft:electric_spark");
    public static final AttackDef GUARDIAN_ROCKFALL = AttackDef.of("falling_rocks", AttackType.CRUSH, 22, 14, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(3, "Rocks fall from above!").particle("minecraft:ash");

    // ---- Abyssal Sire ----
    public static final AttackDef SIRE_TENDRIL = AttackDef.of("tendril_swipe", AttackType.SLASH, 32, 4, 4);
    public static final AttackDef SIRE_SLAM = AttackDef.of("tentacle_slam", AttackType.CRUSH, 40, 4, 2).special(Special.FREEZE, 3);
    public static final AttackDef SIRE_MIASMA = AttackDef.of("miasma", AttackType.MAGIC, 15, 12, 2).special(Special.HAZARD, 5)
            .particle("minecraft:squid_ink");
    public static final AttackDef SIRE_EXPLOSION = AttackDef.of("explosion", AttackType.MAGIC, 72, 6, 1).special(Special.AOE, 6)
            .telegraph(8, "The Abyssal Sire begins to swell... RUN!").particle("minecraft:explosion");

    // ---- Araxxor ----
    public static final AttackDef ARAXXOR_MELEE = AttackDef.of("fangs", AttackType.STAB, 38, 4, 5);
    public static final AttackDef ARAXXOR_RANGED = AttackDef.of("acid_spit", AttackType.RANGED, 30, 14, 3).special(Special.VENOM, 0)
            .particle("minecraft:sneeze");
    public static final AttackDef ARAXXOR_MAGIC = AttackDef.of("web_magic", AttackType.MAGIC, 30, 14, 2).particle("minecraft:witch");
    public static final AttackDef ARAXXOR_ACID = AttackDef.of("acid_ball", AttackType.RANGED, 20, 14, 1).special(Special.HAZARD, 6)
            .telegraph(3, "Araxxor sprays acid across the cave!").particle("minecraft:sneeze");
    public static final AttackDef ARAXXOR_CLEAVE = AttackDef.of("cleave", AttackType.SLASH, 60, 5, 1).special(Special.AOE, 5)
            .telegraph(4, "Araxxor rears up for a cleave... get back!").particle("minecraft:sweep_attack");

    public static List<BossDef> all() {
        return List.of(kraken(), thermy(), cerberus(), abyssalSire(), hydra(HydraPhase.GREEN), hydra(HydraPhase.BLUE),
                hydra(HydraPhase.RED), hydra(HydraPhase.ENRAGED), dusk(), dawn(), araxxor());
    }

    static BossDef kraken() {
        return BossDef.of("kraken", "Kraken").level(291).stats(255, 1, 1, 150, 120, 1).speed(4)
                .defenceBonus(0, 0, 0, 130, 300).immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH)
                .attack(AttackDef.of("water_blast", AttackType.MAGIC, 28, 16, 1).particle("minecraft:splash"))
                .drops(Drop.of("kraken_tentacle", 1, 1, 400), Drop.of("trident_of_the_seas", 1, 1, 512), Drop.of("jar_of_dirt", 1, 1, 1000),
                        Drop.of("raw_shark", 10, 20, 8), Drop.of("runite_bar", 2, 2, 16), Drop.of("water_rune", 400, 400, 8),
                        Drop.of("coins", 10000, 30000, 4), Drop.of("clue_scroll_elite", 1, 1, 23))
                .slayer(87, "kraken").pet("pet_kraken").at("kraken_cove").arena(13, "water")
                .size(1.8f, 1.6f, 2.0f).moveSpeed(0.0);
    }

    static BossDef thermy() {
        return BossDef.of("thermonuclear_smoke_devil", "Thermonuclear smoke devil").level(301).stats(240, 1, 1, 600, 1, 280).speed(4)
                .defenceBonus(0, 0, 0, 600, 800)
                .attack(AttackDef.of("smoke_blast", AttackType.RANGED, 8, 12, 1).particle("minecraft:large_smoke"))
                .drops(Drop.of("occult_necklace", 1, 1, 350), Drop.of("smoke_battlestaff", 1, 1, 512),
                        Drop.of("dragon_chainbody", 1, 1, 2000), Drop.of("jar_of_smoke", 1, 1, 2000), Drop.of("rune_platelegs", 1, 1, 12),
                        Drop.of("air_rune", 300, 300, 8), Drop.of("soul_rune", 50, 50, 10), Drop.of("runite_bar", 2, 2, 16),
                        Drop.of("coins", 10000, 20000, 4), Drop.of("clue_scroll_elite", 1, 1, 23))
                .slayer(93, "smoke_devils").pet("pet_smoke_devil").at("smoke_devil_dungeon").arena(11, "void")
                .flying().size(1.0f, 1.8f, 1.8f).moveSpeed(0.22);
    }

    static BossDef cerberus() {
        return BossDef.of("cerberus", "Cerberus").level(318).stats(600, 220, 220, 100, 220, 220).speed(4)
                .defenceBonus(50, 100, 25, 100, 100)
                .attack(CERB_MELEE).attack(CERB_RANGED).attack(CERB_MAGIC).attack(CERB_LAVA)
                .drops(Drop.of("primordial_crystal", 1, 1, 520), Drop.of("pegasian_crystal", 1, 1, 520),
                        Drop.of("eternal_crystal", 1, 1, 520), Drop.of("smouldering_stone", 1, 1, 520), Drop.of("jar_of_souls", 1, 1, 2000),
                        Drop.of("rune_platebody", 1, 1, 12), Drop.of("rune_2h_sword", 1, 1, 12), Drop.of("runite_ore", 5, 5, 12),
                        Drop.of("death_rune", 100, 100, 12), Drop.of("blood_rune", 60, 60, 12), Drop.of("super_restore", 2, 2, 12),
                        Drop.of("coal", 120, 120, 16), Drop.of("coins", 10000, 20000, 6), Drop.of("clue_scroll_elite", 1, 1, 23))
                .slayer(91, "hellhounds").pet("pet_hellpuppy").at("cerberus_lair").arena(13, "lava")
                .size(1.6f, 1.6f, 2.0f).moveSpeed(0.24);
    }

    static BossDef abyssalSire() {
        return BossDef.of("abyssal_sire", "Abyssal Sire").level(350).stats(400, 180, 136, 250, 200, 1).speed(4)
                .defenceBonus(40, 60, 50, 20, 60)
                .attack(SIRE_TENDRIL).attack(SIRE_SLAM).attack(SIRE_MIASMA)
                .drops(Drop.of("unsired", 1, 1, 100), Drop.of("blood_rune", 75, 125, 8), Drop.of("soul_rune", 100, 100, 10),
                        Drop.of("runite_bar", 3, 3, 12), Drop.of("magic_logs", 50, 50, 12), Drop.of("super_restore", 2, 2, 12),
                        Drop.of("rune_platebody", 1, 1, 16), Drop.of("coins", 20000, 50000, 6), Drop.of("clue_scroll_elite", 1, 1, 23))
                .slayer(85, "abyssal_demons").pet("pet_abyssal_orphan").at("abyssal_nexus").arena(13, "void")
                .size(2.0f, 2.6f, 2.0f).moveSpeed(0.12);
    }

    static BossDef hydra(HydraPhase phase) {
        BossDef d = BossDef.of(phase.bossId, "Alchemical Hydra").level(426).stats(1100, 100, 150, 100, 260, 260)
                .speed(phase == HydraPhase.ENRAGED ? 4 : 5)
                .defenceBonus(75, 75, 75, 150, 45).attack(HYDRA_RANGED).attack(HYDRA_MAGIC).attack(phase.special)
                .slayer(95, "hydras").size(1.8f, 1.8f, 2.2f).moveSpeed(0.2);
        if (phase == HydraPhase.GREEN) {
            d.drops(Drop.of("hydra_bones", 1, 1, 1), Drop.of("hydras_eye", 1, 1, 181), Drop.of("hydras_fang", 1, 1, 181),
                            Drop.of("hydras_heart", 1, 1, 181), Drop.of("hydra_tail", 1, 1, 513), Drop.of("hydra_leather", 1, 1, 514),
                            Drop.of("hydra_claw", 1, 1, 1001), Drop.of("alchemical_hydra_heads", 1, 1, 256),
                            Drop.of("jar_of_chemicals", 1, 1, 2000), Drop.of("dragon_bones", 5, 10, 8), Drop.of("rune_platelegs", 1, 1, 16),
                            Drop.of("dragon_longsword", 1, 1, 20), Drop.of("runite_bar", 3, 3, 16), Drop.of("blood_rune", 100, 100, 12),
                            Drop.of("magic_logs", 40, 40, 16), Drop.of("coins", 20000, 40000, 6), Drop.of("clue_scroll_elite", 1, 1, 23))
                    .pet("pet_ikkle_hydra").at("karuulm_slayer_dungeon").arena(14, "lava");
        } else {
            d.raidOnly();
        }
        return d;
    }

    static BossDef dusk() {
        return BossDef.of("dusk", "Dusk").level(328).stats(450, 160, 160, 160, 1, 1).speed(6)
                .defenceBonus(90, 110, 70, 400, 400).immune(AttackType.MAGIC, AttackType.RANGED)
                .attack(DUSK_MELEE).attack(GUARDIAN_ROCKFALL)
                .drops(Drop.of("granite_dust", 50, 100, 1), Drop.of("granite_maul", 1, 1, 250), Drop.of("granite_ring", 1, 1, 250),
                        Drop.of("granite_gloves", 1, 1, 250), Drop.of("granite_hammer", 1, 1, 750),
                        Drop.of("black_tourmaline_core", 1, 1, 1000), Drop.of("jar_of_stone", 1, 1, 5000),
                        Drop.of("rune_platebody", 1, 1, 16), Drop.of("adamantite_bar", 10, 10, 12), Drop.of("runite_ore", 3, 3, 16),
                        Drop.of("death_rune", 100, 100, 12), Drop.of("coins", 10000, 30000, 6), Drop.of("clue_scroll_elite", 1, 1, 23))
                .slayer(75, "gargoyles").pet("pet_noon").at("slayer_tower_roof").arena(13, "stone")
                .size(1.0f, 2.2f, 1.7f).moveSpeed(0.22);
    }

    static BossDef dawn() {
        return BossDef.of("dawn", "Dawn").level(328).stats(450, 140, 140, 140, 140, 140).speed(6)
                .defenceBonus(400, 400, 400, 80, 80).immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH)
                .attack(DAWN_RANGED).attack(DAWN_SPHERES)
                .slayer(75, "gargoyles").raidOnly().flying().size(1.0f, 2.2f, 1.6f).moveSpeed(0.24);
    }

    static BossDef araxxor() {
        return BossDef.of("araxxor", "Araxxor").level(731).stats(1020, 470, 350, 220, 220, 220).speed(4)
                .defenceBonus(100, 90, 120, 200, 280)
                .attack(ARAXXOR_MELEE).attack(ARAXXOR_RANGED).attack(ARAXXOR_MAGIC).attack(ARAXXOR_ACID)
                .phase(25, "Araxxor becomes enraged!", 1.25, false, ARAXXOR_MELEE, ARAXXOR_CLEAVE, ARAXXOR_ACID)
                .drops(Drop.of("noxious_point", 1, 1, 600), Drop.of("noxious_blade", 1, 1, 600), Drop.of("noxious_pommel", 1, 1, 600),
                        Drop.of("araxyte_fang", 1, 1, 1500), Drop.of("coagulated_venom", 1, 1, 400), Drop.of("araxyte_head", 1, 1, 2000),
                        Drop.of("echo_crystal", 1, 1, 750), Drop.of("jar_of_venom", 1, 1, 1500),
                        Drop.of("rune_platebody", 1, 1, 12), Drop.of("runite_bar", 4, 4, 12), Drop.of("death_rune", 200, 200, 10),
                        Drop.of("blood_rune", 150, 150, 10), Drop.of("super_restore", 3, 3, 10), Drop.of("coins", 30000, 60000, 6),
                        Drop.of("clue_scroll_elite", 1, 1, 23))
                .slayer(92, "araxytes").pet("pet_nid").at("araxyte_cave").arena(14, "crypt")
                .size(2.0f, 1.4f, 2.2f).moveSpeed(0.22);
    }
}
