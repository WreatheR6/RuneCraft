package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;

/**
 * Bosses fought in special encounters: Crystalline/Corrupted Hunllef (The Gauntlet), Zalcano,
 * The Nightmare / Phosani's Nightmare, Phantom Muspah and Sol Heredit (Fortis Colosseum).
 */
public final class MinigameBosses {
    private MinigameBosses() {
    }

    // ---- Hunllef ----
    /** Hunllef switches between ranged and magic every {@value} attacks. */
    public static final int HUNLLEF_ATTACKS_PER_STYLE = 4;
    /** Hunllef switches its protection prayer after taking this many hits from the protected style. */
    public static final int HUNLLEF_HITS_PER_PRAYER = 6;
    public static final AttackDef HUNLLEF_RANGED = AttackDef.of("crystal_spikes", AttackType.RANGED, 16, 16, 1)
            .particle("minecraft:end_rod");
    public static final AttackDef HUNLLEF_MAGIC = AttackDef.of("crystal_orb", AttackType.MAGIC, 16, 16, 1)
            .particle("minecraft:glow");
    /** Its magic attack sometimes switches off the target's prayers. */
    public static final AttackDef HUNLLEF_PRAYER_DISABLE = AttackDef.of("prayer_disable", AttackType.MAGIC, 16, 16, 1)
            .particle("minecraft:reverse_portal").message("The Hunllef's magic attack disrupts your prayers!");
    public static final AttackDef HUNLLEF_STOMP = AttackDef.of("stomp", AttackType.CRUSH, 68, 1.5, 1).special(Special.AOE, 1)
            .message("The Hunllef stomps on you!");
    public static final AttackDef CORRUPTED_RANGED = AttackDef.of("corrupted_spikes", AttackType.RANGED, 20, 16, 1)
            .particle("minecraft:crimson_spore");
    public static final AttackDef CORRUPTED_MAGIC = AttackDef.of("corrupted_orb", AttackType.MAGIC, 20, 16, 1)
            .particle("minecraft:crimson_spore");

    // ---- The Nightmare ----
    public static final AttackDef NIGHTMARE_MELEE = AttackDef.of("claw_swipe", AttackType.SLASH, 35, 4.5, 4);
    public static final AttackDef NIGHTMARE_MAGIC = AttackDef.of("dream_blast", AttackType.MAGIC, 35, 14, 3)
            .telegraph(2, "The Nightmare raises her hands...").particle("minecraft:witch");
    public static final AttackDef NIGHTMARE_RANGED = AttackDef.of("thrown_soul", AttackType.RANGED, 35, 14, 3)
            .telegraph(2, "The Nightmare winds up a throw...").particle("minecraft:soul");
    public static final AttackDef NIGHTMARE_CLAWS = AttackDef.of("grasping_claws", AttackType.MAGIC, 50, 14, 2)
            .special(Special.SKY_STRIKE, 0).telegraph(4, "Grasping claws reach up from the ground!").particle("minecraft:squid_ink");
    public static final AttackDef NIGHTMARE_SPORES = AttackDef.of("spores", AttackType.MAGIC, 0, 14, 1)
            .special(Special.FREEZE, 6).particle("minecraft:spore_blossom_air").message("You inhale the spores and feel drowsy...");
    public static final AttackDef NIGHTMARE_SURGE = AttackDef.of("surge", AttackType.MAGIC, 60, 14, 1).special(Special.BLINK, 0)
            .telegraph(4, "The Nightmare prepares to surge across the arena!").particle("minecraft:portal");
    public static final AttackDef NIGHTMARE_CURSE = AttackDef.of("curse", AttackType.MAGIC, 0, 14, 1)
            .message("The Nightmare curses you, shuffling your prayers!").particle("minecraft:enchant");

    // ---- Phantom Muspah ----
    public static final AttackDef MUSPAH_RANGED = AttackDef.of("ice_shard", AttackType.RANGED, 33, 14, 5).particle("minecraft:snowflake");
    public static final AttackDef MUSPAH_SMITE = AttackDef.of("smite", AttackType.MAGIC, 28, 14, 2).special(Special.DRAIN_PRAYER, 20)
            .particle("minecraft:soul_fire_flame").message("The Phantom Muspah smites you, draining your prayer!");
    public static final AttackDef MUSPAH_MELEE = AttackDef.of("stomp", AttackType.CRUSH, 54, 4, 5);
    public static final AttackDef MUSPAH_SPIKES = AttackDef.of("spikes", AttackType.CRUSH, 30, 14, 2).special(Special.SKY_STRIKE, 0)
            .telegraph(3, "Icy spikes erupt from the ground!").particle("minecraft:snowflake");
    public static final AttackDef MUSPAH_DARKNESS = AttackDef.of("darkness", AttackType.MAGIC, 15, 14, 1).special(Special.AOE, 14)
            .telegraph(3, "Darkness engulfs the arena!").particle("minecraft:squid_ink");

    // ---- Sol Heredit ----
    public static final AttackDef SOL_SPEAR = AttackDef.of("spear", AttackType.STAB, 46, 5, 5);
    public static final AttackDef SOL_SHIELD = AttackDef.of("shield_bash", AttackType.CRUSH, 46, 4, 3).special(Special.KNOCKBACK, 4);
    public static final AttackDef SOL_TRIPLE = AttackDef.of("triple_attack", AttackType.STAB, 60, 5, 2)
            .telegraph(3, "Sol Heredit grips his spear tightly...");
    public static final AttackDef SOL_LASERS = AttackDef.of("solar_flare", AttackType.MAGIC, 40, 16, 2).special(Special.SKY_STRIKE, 0)
            .telegraph(3, "Beams of sunlight sweep across the arena!").particle("minecraft:end_rod");
    public static final AttackDef SOL_GRAPPLE = AttackDef.of("grapple", AttackType.CRUSH, 30, 16, 1).special(Special.PULL, 0)
            .message("Sol Heredit grapples you!");

    public static List<BossDef> all() {
        return List.of(crystallineHunllef(), corruptedHunllef(), zalcano(), nightmare(), phosani(), muspah(), solHeredit());
    }

    static BossDef crystallineHunllef() {
        return BossDef.of("crystalline_hunllef", "Crystalline Hunllef").level(674).stats(600, 240, 240, 240, 240, 240).speed(5)
                .defenceBonus(20, 20, 20, 20, 20)
                .attack(HUNLLEF_RANGED).attack(HUNLLEF_MAGIC).attack(HUNLLEF_STOMP)
                // Wiki: The Gauntlet's chest gives each crystal seed at 1/120 and the enhanced seed at 1/2,000.
                .drops(Drop.of("crystal_shard", 3, 7, 1), Drop.of("crystal_weapon_seed", 1, 1, 120), Drop.of("crystal_armour_seed", 1, 1, 120),
                        Drop.of("enhanced_crystal_weapon_seed", 1, 1, 2000), Drop.of("runite_bar", 1, 2, 8),
                        Drop.of("death_rune", 50, 100, 8), Drop.of("coins", 5000, 15000, 4), Drop.of("clue_scroll_elite", 1, 1, 23))
                .pet("pet_youngllef").at("the_gauntlet").arena(12, "ice")
                .size(1.6f, 1.8f, 1.8f).moveSpeed(0.22);
    }

    static BossDef corruptedHunllef() {
        return BossDef.of("corrupted_hunllef", "Corrupted Hunllef").level(894).stats(1000, 240, 240, 240, 240, 240).speed(5)
                .defenceBonus(90, 90, 90, 90, 90)
                .attack(CORRUPTED_RANGED).attack(CORRUPTED_MAGIC).attack(HUNLLEF_STOMP)
                .drops(Drop.of("crystal_shard", 5, 9, 1), Drop.of("gauntlet_cape", 1, 1, 1), Drop.of("crystal_weapon_seed", 1, 1, 50),
                        Drop.of("crystal_armour_seed", 1, 1, 50), Drop.of("enhanced_crystal_weapon_seed", 1, 1, 400),
                        Drop.of("runite_bar", 2, 4, 8), Drop.of("blood_rune", 100, 200, 8), Drop.of("coins", 10000, 30000, 4),
                        Drop.of("clue_scroll_elite", 1, 1, 20))
                .pet("pet_youngllef").at("the_gauntlet").arena(12, "lava")
                .size(1.6f, 1.8f, 1.8f).moveSpeed(0.22);
    }

    static BossDef zalcano() {
        // Zalcano's rocky armour must be mined (attack her with a pickaxe); falling rocks and red symbols punish standing still.
        return BossDef.of("zalcano", "Zalcano").level(1).stats(1000, 1, 1, 1, 1, 1).speed(6)
                .attack(AttackDef.of("rockfall", AttackType.CRUSH, 25, 16, 3).special(Special.SKY_STRIKE, 0)
                        .telegraph(4, "Rocks begin to fall from the ceiling!").particle("minecraft:ash"))
                .attack(AttackDef.of("red_symbol", AttackType.MAGIC, 15, 16, 2).special(Special.HAZARD, 6)
                        .particle("minecraft:flame").message("Zalcano's demonic energy scorches the ground!"))
                .attack(AttackDef.of("golem", AttackType.MAGIC, 0, 16, 1).special(Special.SUMMON, 1)
                        .message("Zalcano animates a golem from the rubble!"))
                .minion("zalcano_golem", 1)
                .drops(Drop.of("crystal_shard", 10, 20, 1), Drop.of("crystal_tool_seed", 1, 1, 200), Drop.of("zalcano_shard", 1, 1, 1500),
                        Drop.of("runite_ore", 3, 8, 4), Drop.of("adamantite_ore", 10, 20, 4), Drop.of("coal", 30, 60, 3),
                        Drop.of("runite_bar", 2, 4, 6), Drop.of("uncut_onyx", 1, 1, 23), Drop.of("clue_scroll_elite", 1, 1, 23))
                .pet("pet_smolcano").at("zalcano_chamber").arena(13, "lava")
                .size(1.4f, 2.6f, 1.8f).moveSpeed(0.0);
    }

    private static BossDef nightmareBase(String id, String name, int level, int hp) {
        return BossDef.of(id, name).level(level).stats(hp, 150, 150, 150, 150, 150).speed(6)
                .defenceBonus(40, 120, 0, 600, 600)
                .attack(NIGHTMARE_MELEE).attack(NIGHTMARE_MAGIC).attack(NIGHTMARE_RANGED).attack(NIGHTMARE_CLAWS)
                .phase(66, "The Nightmare's shield reforms! Charge the totems!", 1.0, false,
                        NIGHTMARE_MELEE, NIGHTMARE_MAGIC, NIGHTMARE_RANGED, NIGHTMARE_CLAWS, NIGHTMARE_SPORES, NIGHTMARE_CURSE)
                .phase(33, "The Nightmare's shield reforms once more!", 1.1, false,
                        NIGHTMARE_MELEE, NIGHTMARE_MAGIC, NIGHTMARE_RANGED, NIGHTMARE_CLAWS, NIGHTMARE_SURGE, NIGHTMARE_SPORES)
                .at("sisterhood_sanctuary").arena(14, "crypt").size(1.4f, 3.2f, 1.8f).moveSpeed(0.2);
    }

    static BossDef nightmare() {
        return nightmareBase("the_nightmare", "The Nightmare", 814, 2400)
                .drops(Drop.of("nightmare_staff", 1, 1, 400), Drop.of("inquisitors_great_helm", 1, 1, 600),
                        Drop.of("inquisitors_hauberk", 1, 1, 600), Drop.of("inquisitors_plateskirt", 1, 1, 600),
                        Drop.of("inquisitors_mace", 1, 1, 1200), Drop.of("eldritch_orb", 1, 1, 1200), Drop.of("harmonised_orb", 1, 1, 1200),
                        Drop.of("volatile_orb", 1, 1, 1200), Drop.of("jar_of_dreams", 1, 1, 2000),
                        Drop.of("rune_platebody", 1, 1, 8), Drop.of("runite_ore", 10, 10, 8), Drop.of("soul_rune", 300, 300, 8),
                        Drop.of("coins", 30000, 80000, 4), Drop.of("clue_scroll_elite", 1, 1, 23))
                .pet("pet_little_nightmare");
    }

    static BossDef phosani() {
        return nightmareBase("phosanis_nightmare", "Phosani's Nightmare", 1000, 2000)
                .drops(Drop.of("nightmare_staff", 1, 1, 450), Drop.of("inquisitors_great_helm", 1, 1, 700),
                        Drop.of("inquisitors_hauberk", 1, 1, 700), Drop.of("inquisitors_plateskirt", 1, 1, 700),
                        Drop.of("inquisitors_mace", 1, 1, 1400), Drop.of("eldritch_orb", 1, 1, 1400), Drop.of("harmonised_orb", 1, 1, 1400),
                        Drop.of("volatile_orb", 1, 1, 1400), Drop.of("parasitic_egg", 1, 1, 200), Drop.of("jar_of_dreams", 1, 1, 4000),
                        Drop.of("runite_ore", 10, 10, 8), Drop.of("blood_rune", 300, 300, 8), Drop.of("coins", 30000, 80000, 4),
                        Drop.of("clue_scroll_elite", 1, 1, 23))
                .pet("pet_little_nightmare");
    }

    static BossDef muspah() {
        return BossDef.of("phantom_muspah", "Phantom Muspah").level(541).stats(850, 150, 150, 200, 200, 200).speed(5)
                .defenceBonus(60, 60, 60, 300, 100)
                .attack(MUSPAH_RANGED).attack(MUSPAH_SMITE)
                .phase(60, "The Phantom Muspah shatters its prison and charges in!", 1.0, false, MUSPAH_MELEE, MUSPAH_SPIKES)
                .phase(30, "The Phantom Muspah surrounds itself with a frozen shield!", 1.1, false,
                        MUSPAH_RANGED, MUSPAH_SMITE, MUSPAH_SPIKES, MUSPAH_DARKNESS)
                .drops(Drop.of("ancient_essence", 540, 2060, 1), Drop.of("venator_shard", 1, 1, 100), Drop.of("ancient_icon", 1, 1, 50),
                        Drop.of("runite_ore", 5, 5, 10), Drop.of("death_rune", 200, 200, 10), Drop.of("blood_rune", 100, 100, 10),
                        Drop.of("super_restore", 3, 3, 12), Drop.of("coins", 20000, 50000, 6), Drop.of("clue_scroll_elite", 1, 1, 23))
                .pet("pet_muphin").at("ghorrock_dungeon").arena(14, "ice")
                .size(1.4f, 2.6f, 1.8f).moveSpeed(0.2);
    }

    static BossDef solHeredit() {
        return BossDef.of("sol_heredit", "Sol Heredit").level(1563).stats(1500, 350, 350, 300, 300, 300).speed(5)
                .defenceBonus(65, 65, 65, 65, 65)
                .attack(SOL_SPEAR).attack(SOL_SHIELD).attack(SOL_TRIPLE).attack(SOL_LASERS).attack(SOL_GRAPPLE)
                .phase(50, "Sol Heredit: Impressive. Let's see how you handle this!", 1.15, false)
                .phase(25, "Sol Heredit: You fight well... but the sun never sets on the Colosseum!", 1.3, false)
                .drops(Drop.of("sunfire_splinters", 200, 600, 1), Drop.of("dizanas_quiver", 1, 1, 1),
                        Drop.of("sunfire_fanatic_helm", 1, 1, 90), Drop.of("sunfire_fanatic_cuirass", 1, 1, 90),
                        Drop.of("sunfire_fanatic_chausses", 1, 1, 90), Drop.of("echo_crystal", 1, 1, 60),
                        Drop.of("tonalztics_of_ralos", 1, 1, 150), Drop.of("runite_bar", 5, 10, 4), Drop.of("coins", 50000, 150000, 2))
                .pet("pet_smol_heredit").at("fortis_colosseum").arena(15, "sand")
                .size(0.9f, 2.4f, 1.6f).moveSpeed(0.26);
    }
}
