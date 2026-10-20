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

    // ---- Kraken ----
    /**
     * The Kraken's and its tentacles' attacks are typeless (OSRS Wiki: "Protect from Magic does not provide any degree of damage
     * reduction"): the Kraken's water blast up to 28, each Enormous Tentacle's splash up to 2.
     */
    public static final AttackDef KRAKEN_BLAST = AttackDef.of("water_blast", AttackType.MAGIC, 28, 16, 1)
            .special(Special.PRAYER_PIERCING, 0).particle("minecraft:splash");
    /** Ranged hits on the Kraken deal a seventh of their damage (at least 1 when they hit); melee can't reach it at all. */
    public static final int KRAKEN_RANGED_DIVISOR = 7;

    /** Damage a rolled ranged hit deals to the Kraken (Mod Ash: "divided by 7, with a floor of 1"; 0 stays 0). */
    public static int krakenRangedDamage(int rolled) {
        return rolled <= 0 ? 0 : Math.max(1, rolled / KRAKEN_RANGED_DIVISOR);
    }

    // ---- Thermonuclear smoke devil ----
    /** Typeless "magical ranged" attack (protection prayers do nothing), up to 8 every 2 ticks, from eight tiles. */
    public static final AttackDef THERMY_BLAST = AttackDef.of("smoke_blast", AttackType.RANGED, 8, 8, 1)
            .special(Special.PRAYER_PIERCING, 0).particle("minecraft:large_smoke");

    // ---- Cerberus ----
    public static final AttackDef CERB_MELEE = AttackDef.of("bite", AttackType.STAB, 23, 3.5, 4);
    /** The triple attack's bite is "executed regardless of distance" (OSRS Wiki). */
    public static final AttackDef CERB_TRIPLE_MELEE = AttackDef.of("bite", AttackType.STAB, 23, 64, 1);
    public static final AttackDef CERB_RANGED = AttackDef.of("fireball", AttackType.RANGED, 23, 14, 3).particle("minecraft:flame");
    public static final AttackDef CERB_MAGIC = AttackDef.of("soul_blast", AttackType.MAGIC, 23, 14, 3)
            .particle("minecraft:soul_fire_flame");
    /** "Grrrrrrrrrrrrrr!" - lava pools erupt under and around you. */
    public static final AttackDef CERB_LAVA = AttackDef.of("lava_pool", AttackType.MAGIC, 15, 14, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(4, "Cerberus: Grrrrrrrrrrrrrr!").particle("minecraft:lava");
    /** Summoned souls: 30 unprotected (22 with an elysian spirit shield), else a 30 prayer drain (15 with a spectral). */
    public static final int CERB_SOUL_HIT = 30;
    public static final int CERB_SOUL_HIT_ELYSIAN = 22;
    public static final int CERB_SOUL_DRAIN = 30;
    public static final int CERB_SOUL_DRAIN_SPECTRAL = 15;
    /** Souls once she is under 400 hitpoints (2/3), lava pools under 200. */
    public static final int CERB_SOULS_BELOW = 400;
    public static final int CERB_LAVA_BELOW = 200;
    /** A lava pool deals 10 to whoever stands on it as it forms, then 15 every two ticks; it lasts a few seconds (approx). */
    public static final int CERB_LAVA_LANDING = 10;
    public static final int CERB_LAVA_TICK = 15;
    public static final int CERB_LAVA_TICKS = 10; // approx: "after a few seconds, the lava pools will dissipate"

    /** What Cerberus does with one attack of her pattern. */
    public enum CerberusMove { TRIPLE, SOULS, LAVA, AUTO }

    /**
     * Cerberus' attack pattern (OSRS Wiki "Cerberus/Strategies"): the triple attack is her first attack and every tenth after
     * it (1, 11, 21...), the summoned souls every seventh attack once she is under 400 hitpoints, the lava pools every fifth
     * once under 200; overlaps go triple, then souls, then lava, and a special due has a 10% chance of being an ordinary
     * attack instead ({@code skipSpecial}).
     *
     * @param attack the attack's number in her pattern, from 1
     */
    public static CerberusMove cerberusMove(int attack, int hitpoints, boolean skipSpecial) {
        if (attack % 10 == 1) {
            return CerberusMove.TRIPLE;
        }
        if (!skipSpecial && hitpoints < CERB_SOULS_BELOW && attack % 7 == 0) {
            return CerberusMove.SOULS;
        }
        if (!skipSpecial && hitpoints < CERB_LAVA_BELOW && attack % 5 == 0) {
            return CerberusMove.LAVA;
        }
        return CerberusMove.AUTO;
    }

    // ---- Alchemical Hydra ----
    /** Hydra max hits by phase (OSRS Wiki): serpentine 17 (two hits), electric and flame 35, extinguished 55. */
    public static final int HYDRA_MAX_SERPENTINE = 17;
    public static final int HYDRA_MAX_ELECTRIC_FLAME = 35;
    public static final int HYDRA_MAX_ENRAGED = 55;
    public static final AttackDef HYDRA_RANGED = AttackDef.of("ranged", AttackType.RANGED, 28, 14, 1).particle("minecraft:crit");
    public static final AttackDef HYDRA_MAGIC = AttackDef.of("magic", AttackType.MAGIC, 28, 14, 1).particle("minecraft:witch");
    /** Poison blobs: a 3x3 splatter up to 7 as they land, pools of up to 12 a tick, poison 6 (the targeted blob) or 4. */
    public static final AttackDef HYDRA_POISON = AttackDef.of("poison_pool", AttackType.RANGED, 12, 14, 1)
            .special(Special.HAZARD, 4).particle("minecraft:sneeze");
    /** Lightning currents from the four corners converging on the player: up to 22 and a bind. */
    public static final AttackDef HYDRA_LIGHTNING = AttackDef.of("lightning", AttackType.MAGIC, 22, 14, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(3, "The Alchemical Hydra calls down lightning!")
            .particle("minecraft:electric_spark");
    /** Fire walls and the tracking flame: up to 25, then a burn of 5 for 5 hits. */
    public static final AttackDef HYDRA_FIRE = AttackDef.of("fire_wall", AttackType.MAGIC, 25, 14, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(3, "The Alchemical Hydra breathes a wall of fire!").particle("minecraft:flame");
    public static final int HYDRA_SPLATTER_MAX = 7;
    public static final int HYDRA_POOL_MAX = 12;
    /** Empowered by a wrong vent (OSRS Wiki infobox): 26x2 in the serpentine and electric phases, 52 in the flame phase. */
    public static final int HYDRA_EMPOWERED_SERPENTINE = 26;
    public static final int HYDRA_EMPOWERED_ELECTRIC = 26;
    public static final int HYDRA_EMPOWERED_FLAME = 52;
    /** "At the start of each phase, the Hydra will perform three auto-attacks before performing a special attack", then 9. */
    public static final int HYDRA_FIRST_SPECIAL = 3;
    public static final int HYDRA_SPECIAL_EVERY = 9;
    /** For the first three phases its style changes after every three basic attacks. */
    public static final int HYDRA_STYLE_EVERY = 3;
    /** The Hydra doesn't attack for about seven ticks after launching its tracking flame. */
    public static final int HYDRA_FIRE_PAUSE = 7;

    /** Whether the Hydra uses its phase special now, after {@code autos} auto attacks since the phase began or its last special. */
    public static boolean hydraSpecialDue(int autos, int specialsThisPhase) {
        return autos >= (specialsThisPhase == 0 ? HYDRA_FIRST_SPECIAL : HYDRA_SPECIAL_EVERY);
    }

    /** The Hydra's max hit (per hit) in a phase, empowered by a wrong vent or not. */
    public static int hydraMaxHit(HydraPhase phase, boolean empowered) {
        return switch (phase) {
            case GREEN -> empowered ? HYDRA_EMPOWERED_SERPENTINE : HYDRA_MAX_SERPENTINE;
            case BLUE -> empowered ? HYDRA_EMPOWERED_ELECTRIC : HYDRA_MAX_ELECTRIC_FLAME;
            case RED -> empowered ? HYDRA_EMPOWERED_FLAME : HYDRA_MAX_ELECTRIC_FLAME;
            case ENRAGED -> HYDRA_MAX_ENRAGED;
        };
    }

    /** Whether its basic attacks hit twice: always in the serpentine phase, and in the electric phase once empowered. */
    public static boolean hydraHitsTwice(HydraPhase phase, boolean empowered) {
        return phase == HydraPhase.GREEN || phase == HydraPhase.BLUE && empowered;
    }

    /**
     * Colour phases: model/boss id, carapace colour, vent colour that must be used, special attack and
     * head count. As in OSRS the right vent is the colour that beats the carapace: green is lured to the
     * red vent, blue to the green vent and red to the blue vent.
     */
    public enum HydraPhase {
        GREEN("alchemical_hydra", "green", "red", HYDRA_POISON, 4, 75),
        BLUE("alchemical_hydra_blue", "blue", "green", HYDRA_LIGHTNING, 3, 50),
        RED("alchemical_hydra_red", "red", "blue", HYDRA_FIRE, 2, 25),
        ENRAGED("alchemical_hydra_enraged", "grey", null, HYDRA_POISON, 1, 0);

        public final String bossId;
        /** Colour of the Hydra's carapace in this phase. */
        public final String colour;
        /** Vent colour that removes this phase's 75% damage reduction (null: no shield). */
        public final String vent;
        public final AttackDef special;
        public final int heads;
        /** Hitpoint percentage at which the next phase begins. */
        public final int endsAtPercent;

        HydraPhase(String bossId, String colour, String vent, AttackDef special, int heads, int endsAtPercent) {
            this.bossId = bossId;
            this.colour = colour;
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
    /**
     * Grotesque Guardians (OSRS Wiki "Grotesque Guardians/Strategies"): Dusk claws for up to 15 in the first three phases
     * and 26 in the fourth, where he also fires two orbs of up to 15 (ranged) and tramples anyone under him for up to 40.
     * Dawn throws two rocks of up to 9 (ranged), kicks for up to 15 when next to the player and launches a stone ball
     * (3x3, up to 15 and a 10-tick stun).
     */
    public static final AttackDef DUSK_MELEE = AttackDef.of("claw", AttackType.SLASH, 26, 3.5, 1);
    public static final int DUSK_MELEE_EARLY = 15;
    public static final AttackDef DUSK_RANGED = AttackDef.of("orbs", AttackType.RANGED, 15, 14, 1).particle("minecraft:soul_fire_flame");
    public static final int DUSK_TRAMPLE_MAX = 40;
    public static final AttackDef DAWN_RANGED = AttackDef.of("rock", AttackType.RANGED, 9, 14, 3).particle("minecraft:crit");
    public static final AttackDef DAWN_KICK = AttackDef.of("kick", AttackType.CRUSH, 15, 2.5, 1);
    public static final AttackDef DAWN_STONE_BALL = AttackDef.of("stone_ball", AttackType.CRUSH, 15, 14, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(3, "Dawn launches a ball of stone!").particle("minecraft:ash");
    public static final int DAWN_STONE_STUN_TICKS = 10;
    public static final AttackDef GUARDIAN_LIGHTNING = AttackDef.of("lightning", AttackType.MAGIC, 15, 14, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(3, "Lightning crackles across the rooftop!").particle("minecraft:electric_spark");
    public static final AttackDef GUARDIAN_ROCKFALL = AttackDef.of("falling_rocks", AttackType.CRUSH, 22, 14, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(3, "Rocks fall from above!").particle("minecraft:ash");

    // ---- Abyssal Sire ----
    /**
     * Phase 2 melee (OSRS Wiki "Abyssal Sire/Strategies"): an arm swipe or a tendril flick (approx 32: the wiki gives no
     * figure), or the double tendril flick for up to 66. Protect from Melee only cuts them to 6, 6 and 26, so the attacks
     * pierce prayer and the script scales them ({@link #sireThroughPrayer}).
     */
    public static final AttackDef SIRE_TENDRIL = AttackDef.of("tendril_swipe", AttackType.SLASH, 32, 4, 4)
            .special(Special.PRAYER_PIERCING, 0);
    public static final AttackDef SIRE_SLAM = AttackDef.of("tentacle_slam", AttackType.CRUSH, 66, 4, 2)
            .special(Special.PRAYER_PIERCING, 0);
    public static final int SIRE_TENDRIL_THROUGH_PRAYER = 6;
    public static final int SIRE_SLAM_THROUGH_PRAYER = 26;
    /** Miasma: no hit of its own, a pool dealing 10-30 a tick on its centre tile (OSRS Wiki; approx: the 2-8 edge isn't modelled). */
    public static final AttackDef SIRE_MIASMA = AttackDef.of("miasma", AttackType.MAGIC, 0, 12, 2).special(Special.HAZARD, 30)
            .particle("minecraft:squid_ink");
    /** Under 140 hitpoints it teleports the player next to itself and explodes two ticks later for up to 96 (run 2+ tiles away). */
    public static final AttackDef SIRE_EXPLOSION = AttackDef.of("explosion", AttackType.MAGIC, 96, 6, 1).special(Special.AOE, 4)
            .telegraph(2, "The Abyssal Sire swells up... RUN!").particle("minecraft:explosion");
    /** Attacked from more than two tiles away in phase 2: it pulls the player in and discharges its portal, up to 60. */
    public static final AttackDef SIRE_PORTAL_BLAST = AttackDef.of("explosion", AttackType.MAGIC, 60, 6, 1).special(Special.AOE, 3)
            .telegraph(2, "Energy gathers in the portal in the Abyssal Sire's stomach... get away!").particle("minecraft:portal");
    /** Phase 3 starts under 210 hitpoints, the explosion under 140 (OSRS Wiki "Abyssal Sire"). */
    public static final int SIRE_PHASE3_BELOW = 210;
    public static final int SIRE_EXPLODE_BELOW = 140;
    /** A hit that would kill it before phase 3 leaves it at 20% for the next phase instead. */
    public static final double SIRE_PHASE3_FLOOR = 0.20;
    /** A spawn left alive 15 seconds matures into a scion. */
    public static final int SIRE_SPAWN_MATURES = 25;
    /** After the explosion it keeps spawning until 15 minions are alive, and uses its miasma at least three more times. */
    public static final int SIRE_MAX_MINIONS = 15;
    public static final int SIRE_MIASMAS_AFTER_EXPLOSION = 3;
    /** Each respiratory system killed gives 50 Slayer experience (they don't count as task kills). */
    public static final int SIRE_SYSTEM_SLAYER_XP = 50;

    /** The Sire's phase 2 melee hit through Protect from Melee: 6 for the swipe and flick, 26 for the double flick (scaled). */
    public static int sireThroughPrayer(AttackDef attack, int rolled) {
        int cap = attack == SIRE_SLAM ? SIRE_SLAM_THROUGH_PRAYER : SIRE_TENDRIL_THROUGH_PRAYER;
        return (int) Math.round(rolled * (double) cap / attack.maxHit());
    }

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
                .attack(KRAKEN_BLAST)
                .drops(Drop.of("kraken_tentacle", 1, 1, 400), Drop.of("trident_of_the_seas", 1, 1, 512), Drop.of("jar_of_dirt", 1, 1, 1000),
                        Drop.of("raw_shark", 10, 20, 8), Drop.of("runite_bar", 2, 2, 16), Drop.of("water_rune", 400, 400, 8),
                        Drop.of("coins", 10000, 30000, 4), Drop.of("clue_scroll_elite", 1, 1, 500))
                .drops(Drop.of("mystic_water_staff", 1, 1, 43), Drop.of("mystic_robe_top", 1, 1, 128),
                        Drop.of("mystic_robe_bottom", 1, 1, 128))
                .slayer(87, "cave_kraken").pet("pet_kraken").at("kraken_cove").arena(13, "water")
                .size(1.8f, 1.6f, 2.0f).moveSpeed(0.0);
    }

    static BossDef thermy() {
        return BossDef.of("thermonuclear_smoke_devil", "Thermonuclear smoke devil").level(301).stats(240, 1, 1, 600, 1, 280).speed(4)
                .defenceBonus(0, 0, 0, 600, 800)
                .attack(THERMY_BLAST)
                .drops(Drop.of("occult_necklace", 1, 1, 350), Drop.of("smoke_battlestaff", 1, 1, 512),
                        Drop.of("dragon_chainbody", 1, 1, 2000), Drop.of("jar_of_smoke", 1, 1, 2000), Drop.of("rune_platelegs", 1, 1, 12),
                        Drop.of("air_rune", 300, 300, 8), Drop.of("soul_rune", 50, 50, 10), Drop.of("runite_bar", 2, 2, 16),
                        Drop.of("coins", 10000, 20000, 4), Drop.of("clue_scroll_elite", 1, 1, 500))
                .drops(Drop.of("mystic_air_staff", 1, 1, 36), Drop.of("mystic_fire_staff", 1, 1, 36))
                .slayer(93, "smoke_devils").pet("pet_smoke_devil").at("smoke_devil_dungeon").arena(11, "void")
                .flying().size(1.0f, 1.8f, 1.8f).moveSpeed(0.22);
    }

    static BossDef cerberus() {
        return BossDef.of("cerberus", "Cerberus").level(318).stats(600, 220, 220, 100, 220, 220).speed(4)
                .attackBonus(50, 50, 50)
                .defenceBonus(50, 100, 25, 100, 100)
                .attack(CERB_MELEE).attack(CERB_RANGED).attack(CERB_MAGIC).attack(CERB_LAVA)
                .drops(Drop.always("infernal_ashes"), Drop.of("primordial_crystal", 1, 1, 520), Drop.of("pegasian_crystal", 1, 1, 520),
                        Drop.of("eternal_crystal", 1, 1, 520), Drop.of("smouldering_stone", 1, 1, 520), Drop.of("jar_of_souls", 1, 1, 2000),
                        Drop.of("rune_platebody", 1, 1, 12), Drop.of("rune_2h_sword", 1, 1, 12), Drop.of("runite_ore", 5, 5, 12),
                        Drop.of("death_rune", 100, 100, 12), Drop.of("blood_rune", 60, 60, 12), Drop.of("super_restore", 2, 2, 12),
                        Drop.of("coal", 120, 120, 16), Drop.of("coins", 10000, 20000, 6), Drop.of("clue_scroll_elite", 1, 1, 100))
                .slayer(91, "hellhounds").pet("pet_hellpuppy").at("cerberus_lair").arena(13, "lava")
                .size(1.6f, 1.6f, 2.0f).moveSpeed(0.24);
    }

    static BossDef abyssalSire() {
        return BossDef.of("abyssal_sire", "Abyssal Sire").level(350).stats(400, 180, 136, 250, 200, 1).speed(4)
                .defenceBonus(40, 60, 50, 20, 60)
                .attack(SIRE_TENDRIL).attack(SIRE_SLAM).attack(SIRE_MIASMA)
                .drops(Drop.always("abyssal_ashes"), Drop.of("unsired", 1, 1, 100), Drop.of("blood_rune", 75, 125, 8), Drop.of("soul_rune", 100, 100, 10),
                        Drop.of("runite_bar", 3, 3, 12), Drop.of("magic_logs", 50, 50, 12), Drop.of("super_restore", 2, 2, 12),
                        Drop.of("rune_platebody", 1, 1, 16), Drop.of("coins", 20000, 50000, 6), Drop.of("clue_scroll_elite", 1, 1, 100)) // approx
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
                            Drop.of("magic_logs", 40, 40, 16), Drop.of("coins", 20000, 40000, 6), Drop.of("clue_scroll_elite", 1, 1, 256))
                    .pet("pet_ikkle_hydra").at("karuulm_slayer_dungeon").arena(14, "lava");
        } else {
            d.raidOnly();
        }
        return d;
    }

    static BossDef dusk() {
        return BossDef.of("dusk", "Dusk").level(328).stats(450, 160, 160, 160, 1, 1).speed(6)
                .defenceBonus(90, 110, 70, 400, 400).immune(AttackType.MAGIC, AttackType.RANGED)
                .attack(DUSK_MELEE).attack(DUSK_RANGED)
                .drops(Drop.of("granite_dust", 50, 100, 1), Drop.of("granite_maul", 1, 1, 125), Drop.of("granite_ring", 1, 1, 250),
                        Drop.of("granite_gloves", 1, 1, 250), Drop.of("granite_hammer", 1, 1, 375),
                        Drop.of("black_tourmaline_core", 1, 1, 500), Drop.of("jar_of_stone", 1, 1, 5000),
                        Drop.of("rune_platebody", 1, 1, 16), Drop.of("adamantite_bar", 10, 10, 12), Drop.of("runite_ore", 3, 3, 16),
                        Drop.of("death_rune", 100, 100, 12), Drop.of("coins", 10000, 30000, 6), Drop.of("clue_scroll_elite", 1, 1, 230)) // approx
                .slayer(75, "gargoyles").pet("pet_noon").at("slayer_tower_roof").arena(13, "stone")
                .size(1.0f, 2.2f, 1.7f).moveSpeed(0.22);
    }

    static BossDef dawn() {
        return BossDef.of("dawn", "Dawn").level(328).stats(450, 140, 140, 140, 140, 140).speed(6)
                .defenceBonus(400, 400, 400, 80, 80).immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH)
                .attack(DAWN_RANGED).attack(DAWN_KICK).attack(DAWN_STONE_BALL)
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
                        Drop.of("clue_scroll_elite", 1, 1, 250)) // approx
                .slayer(92, "araxytes").pet("pet_nid").at("araxyte_cave").arena(14, "crypt")
                .size(2.0f, 1.4f, 2.2f).moveSpeed(0.22);
    }
}
