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
    // The Nightmare script (bosses module) performs every attack itself (NightmareRules): her standard attacks after their
    // wind-up (2 ticks melee, 3 magic and ranged), through her prayer rules (20% through the right protection prayer, 120%
    // through a wrong one; Phosani's are blocked by the right one); her specials by phase (Phosani: by special set).
    public static final AttackDef NIGHTMARE_MELEE = AttackDef.of("claw_swipe", AttackType.SLASH, 50, 4.5, 4) // wiki: 50 (60 vs wrong prayer)
            .telegraph(NightmareRules.MELEE_WIND_UP, "The Nightmare charges her claws...");
    public static final AttackDef NIGHTMARE_MAGIC = AttackDef.of("dream_blast", AttackType.MAGIC, 33, 14, 3) // wiki: 33
            .telegraph(NightmareRules.RANGED_WIND_UP, "The Nightmare raises her hands...").particle("minecraft:witch");
    public static final AttackDef NIGHTMARE_RANGED = AttackDef.of("thrown_soul", AttackType.RANGED, 33, 14, 3) // wiki: 33
            .telegraph(NightmareRules.RANGED_WIND_UP, "The Nightmare winds up a throw...").particle("minecraft:soul");
    public static final AttackDef NIGHTMARE_CLAWS = AttackDef.of("grasping_claws", AttackType.MAGIC, NightmareRules.CLAWS_MAX, 64, 2)
            .special(Special.SKY_STRIKE, 0).telegraph(NightmareRules.CLAWS_DELAY, "Grasping claws reach up from the ground!")
            .particle("minecraft:squid_ink");
    public static final AttackDef NIGHTMARE_SPORES = AttackDef.of("spores", AttackType.MAGIC, 0, 64, 1)
            .special(Special.FREEZE, 6).particle("minecraft:spore_blossom_air").message("The Nightmare scatters spores around the arena!");
    public static final AttackDef NIGHTMARE_SURGE = AttackDef.of("surge", AttackType.MAGIC, NightmareRules.SURGE_MAX, 64, 1)
            .special(Special.BLINK, 0).telegraph(NightmareRules.SURGE_WIND_UP, "The Nightmare prepares to surge across the arena!")
            .particle("minecraft:portal");
    /** Her specials that hit nothing themselves (performed by the script; not in the BossDef's attack lists). */
    public static final AttackDef NIGHTMARE_CURSE = AttackDef.of("curse", AttackType.MAGIC, 0, 64, 1)
            .message("The Nightmare curses you, shuffling your prayers!").particle("minecraft:enchant");
    public static final AttackDef NIGHTMARE_HUSKS = AttackDef.of("husks", AttackType.MAGIC, 0, 64, 1).special(Special.SUMMON, 0)
            .message("The Nightmare summons husks!");
    public static final AttackDef NIGHTMARE_PARASITES = AttackDef.of("parasites", AttackType.MAGIC, 0, 64, 1).special(Special.SUMMON, 0)
            .message("The Nightmare spits out parasites!");
    public static final AttackDef NIGHTMARE_FLOWERS = AttackDef.of("corpse_flowers", AttackType.MAGIC, NightmareRules.FLOWER_MAX, 64, 1)
            .special(Special.HAZARD, 0).message("The Nightmare splits the arena with corpse flowers!");
    // Phosani's Nightmare: the right protection prayer blocks her standard attacks fully, but they hit far harder (wiki:
    // melee 73, magic and ranged 80, Grasping Claws 50-65, the surge ~85).
    public static final AttackDef PHOSANI_MELEE = AttackDef.of("claw_swipe", AttackType.SLASH, 73, 4.5, 4)
            .telegraph(NightmareRules.MELEE_WIND_UP, "Phosani's Nightmare charges her claws...");
    public static final AttackDef PHOSANI_MAGIC = AttackDef.of("dream_blast", AttackType.MAGIC, 80, 14, 3)
            .telegraph(NightmareRules.RANGED_WIND_UP, "Phosani's Nightmare raises her hands...").particle("minecraft:witch");
    public static final AttackDef PHOSANI_RANGED = AttackDef.of("thrown_soul", AttackType.RANGED, 80, 14, 3)
            .telegraph(NightmareRules.RANGED_WIND_UP, "Phosani's Nightmare winds up a throw...").particle("minecraft:soul");
    public static final AttackDef PHOSANI_CLAWS = AttackDef.of("grasping_claws", AttackType.MAGIC, NightmareRules.PHOSANI_CLAWS_MAX, 64, 2)
            .special(Special.SKY_STRIKE, 0).telegraph(NightmareRules.CLAWS_DELAY, "Grasping claws reach up around you!")
            .particle("minecraft:squid_ink");
    public static final AttackDef PHOSANI_SURGE = AttackDef.of("surge", AttackType.MAGIC, NightmareRules.PHOSANI_SURGE_MAX, 64, 1)
            .special(Special.BLINK, 0) // approx (~85)
            .telegraph(NightmareRules.SURGE_WIND_UP, "Phosani's Nightmare prepares to surge across the arena!").particle("minecraft:portal");

    // ---- Phantom Muspah ----
    // The Muspah script (bosses module) performs every attack itself (MuspahRules): forms swap by damage, its magic attack
    // corrupts, the shielded form and after hit harder (ranged 72, magic 79) at speed 5.
    public static final AttackDef MUSPAH_RANGED = AttackDef.of("ice_shard", AttackType.RANGED, MuspahRules.RANGED, 14, 5)
            .particle("minecraft:snowflake");
    /** Its charged magic attack: up to 72 (79 shielded and after) and corruption, 18 Prayer over 18 s (OSRS Wiki). */
    public static final AttackDef MUSPAH_SMITE = AttackDef.of("smite", AttackType.MAGIC, MuspahRules.MAGIC, 14, 1)
            .telegraph(MuspahRules.MAGIC_FLIGHT_TICKS, "The Phantom Muspah charges a purple orb!").particle("minecraft:soul_fire_flame");
    /** Melee form: up to 34, stab (OSRS Wiki). */
    public static final AttackDef MUSPAH_MELEE = AttackDef.of("stomp", AttackType.STAB, MuspahRules.MELEE, 4, 5);
    /** The spikes it raises: standing on one as it erupts, or walking onto one, hurts (approx 20) and heals it. */
    public static final AttackDef MUSPAH_SPIKES = AttackDef.of("spike", AttackType.CRUSH, BossMechanics.MUSPAH_SPIKE_MAX, 64, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(MuspahRules.SPIKE_ERUPT_TICKS, "Spikes begin to rise from the ground!")
            .particle("minecraft:snowflake");
    /** Lightning Clouds: it teleports around the arena (the screen dims) leaving clouds that drift across it, up to 20. */
    public static final AttackDef MUSPAH_CLOUDS = AttackDef.of("lightning_cloud", AttackType.MAGIC, MuspahRules.LIGHTNING_CLOUD, 64, 1)
            .special(Special.HAZARD, 0).message("The Phantom Muspah vanishes into the darkness!").particle("minecraft:electric_spark");
    /** Below 127 health: from the centre, a shockwave over the whole arena that only spikes stop, up to 80, and corruption. */
    public static final AttackDef MUSPAH_SHOCKWAVE = AttackDef.of("shockwave", AttackType.MAGIC, MuspahRules.SHOCKWAVE, 64, 1)
            .special(Special.AOE, 32).telegraph(MuspahRules.SHOCKWAVE_WIND_UP, "The Phantom Muspah gathers its power - hide behind a spike!")
            .particle("minecraft:sonic_boom");

    // ---- Sol Heredit ----
    // Sol's attacks are scripted (SolHereditScript, rules in minigames.SolHeredit); these carry the wiki max hits and the
    // attack names his Combat Achievements watch: 44 typeless AoE (spear, shield), 45 triple, 75 light beam, 44 grapple.
    public static final AttackDef SOL_SPEAR = AttackDef.of("spear", AttackType.STAB, 44, 5, 5);
    public static final AttackDef SOL_SHIELD = AttackDef.of("shield_bash", AttackType.CRUSH, 44, 4, 3);
    public static final AttackDef SOL_TRIPLE = AttackDef.of("triple_attack", AttackType.STAB, 45, 5, 2)
            .telegraph(3, "Sol Heredit grips his spear tightly...");
    public static final AttackDef SOL_LASERS = AttackDef.of("solar_flare", AttackType.MAGIC, 75, 16, 2).special(Special.SKY_STRIKE, 0)
            .telegraph(3, "Beams of sunlight sweep across the arena!").particle("minecraft:end_rod");
    public static final AttackDef SOL_GRAPPLE = AttackDef.of("grapple", AttackType.CRUSH, 44, 16, 1).special(Special.PULL, 0)
            .message("Sol Heredit grapples you!");

    public static List<BossDef> all() {
        return List.of(crystallineHunllef(), corruptedHunllef(), zalcano(), nightmare(), phosani(), muspah(), solHeredit());
    }

    static BossDef crystallineHunllef() {
        return BossDef.of("crystalline_hunllef", "Crystalline Hunllef").level(674).stats(600, 240, 240, 240, 240, 240).speed(5)
                .defenceBonus(20, 20, 20, 20, 20)
                .attack(HUNLLEF_RANGED).attack(HUNLLEF_MAGIC).attack(HUNLLEF_STOMP)
                // Wiki Reward Chest (The Gauntlet), regular: the 100% and tertiary rows (shards 5-9 since September 2024). The two
                // main-table rolls come from the minigames module (Gauntlet.REGULAR) on every Hunllef kill; the pet from Pets.
                .drops(Drop.of("crystal_shard", 5, 9, 1), Drop.of("clue_scroll_elite", 1, 1, 25), Drop.of("crystal_weapon_seed", 1, 1, 120),
                        Drop.of("crystal_armour_seed", 1, 1, 120), Drop.of("enhanced_crystal_weapon_seed", 1, 1, 2000))
                .pet("pet_youngllef").at("the_gauntlet").arena(12, "ice")
                .size(1.6f, 1.8f, 1.8f).moveSpeed(0.22);
    }

    static BossDef corruptedHunllef() {
        return BossDef.of("corrupted_hunllef", "Corrupted Hunllef").level(894).stats(1000, 240, 240, 240, 240, 240).speed(5)
                .defenceBonus(90, 90, 90, 90, 90)
                .attack(CORRUPTED_RANGED).attack(CORRUPTED_MAGIC).attack(HUNLLEF_STOMP)
                // Wiki Reward Chest (The Gauntlet), corrupted: 100% and tertiary rows (shards 7-12); three main-table rolls come from
                // the minigames module (Gauntlet.CORRUPTED).
                .drops(Drop.of("crystal_shard", 7, 12, 1), Drop.of("gauntlet_cape", 1, 1, 1), Drop.of("clue_scroll_elite", 1, 1, 20),
                        Drop.of("crystal_weapon_seed", 1, 1, 50), Drop.of("crystal_armour_seed", 1, 1, 50),
                        Drop.of("enhanced_crystal_weapon_seed", 1, 1, 400))
                .pet("pet_youngllef").at("the_gauntlet").arena(12, "lava")
                .size(1.6f, 1.8f, 1.8f).moveSpeed(0.22);
    }

    static BossDef zalcano() {
        // Zalcano (wiki): break her 300 stone armour with imbued tephra (mined, refined at the furnace, imbued at the altar),
        // then mine her with a pickaxe while she is down. Her attacks are typeless and the script (ClassicScripts.Zalcano)
        // performs them: falling boulders (infobox max hit 53), orange demonic symbols (up to 26), exploding glowing rocks,
        // golems that restore her armour, pebbles. Attack speed 5.
        return BossDef.of("zalcano", "Zalcano").level(1).stats(1000, 1, 1, 1, 1, 1).speed(5)
                .attack(AttackDef.of("rockfall", AttackType.CRUSH, 53, 16, 3).special(Special.SKY_STRIKE, 0)
                        .telegraph(4, "Rocks begin to fall from the ceiling!").particle("minecraft:ash"))
                .attack(AttackDef.of("red_symbol", AttackType.MAGIC, 26, 16, 2).special(Special.HAZARD, 6)
                        .particle("minecraft:flame").message("Zalcano's demonic energy scorches the ground!"))
                // No BossDef minion: the script spawns and tracks her golems itself (the driven idle attack would summon one).
                // Round 28: the OSRS Wiki contribution table (one roll out of 36, as 1/N lines). Quantities scale with
                // contribution in OSRS; RuneCraft rolls the wiki's whole range (approx). Tertiaries: crystal tool seed table
                // 1/200 (39/8000 for the seed), Zalcano shard 1/750 (top contributor), uncut onyx 1/8000; the pet (1/2250)
                // is rolled by Pets. Was: uncut onyx and an elite clue at 1/23, which Zalcano never drops.
                .drops(Drop.of("crystal_shard", 1, 3, 1), Drop.always("infernal_ashes"),
                        Drop.of("blood_rune", 64, 480, 36), Drop.of("cosmic_rune", 295, 926, 36), Drop.of("death_rune", 201, 832, 36),
                        Drop.of("law_rune", 166, 790, 36), Drop.of("soul_rune", 47, 392, 36), Drop.of("nature_rune", 1, 937, 36),
                        Drop.of("silver_ore", 86, 800, 12), Drop.of("coal", 169, 938, 36), Drop.of("mithril_ore", 56, 469, 18),
                        Drop.of("adamantite_ore", 51, 343, 18), Drop.of("runite_ore", 3, 31, 18), Drop.of("gold_ore", 86, 756, 12),
                        Drop.of("steel_bar", 16, 699, 12), Drop.of("mithril_bar", 53, 501, 12), Drop.of("adamantite_bar", 13, 111, 18),
                        Drop.of("runite_bar", 3, 27, 18), Drop.of("uncut_diamond", 2, 21, 18), Drop.of("uncut_dragonstone", 1, 12, 18),
                        Drop.of("onyx_bolt_tips", 1, 45, 18), Drop.of("pure_essence", 784, 5500, 36),
                        Drop.of("crystal_tool_seed", 1, 1, 205), Drop.of("zalcano_shard", 1, 1, 750), Drop.of("uncut_onyx", 1, 1, 8000))
                .pet("pet_smolcano").at("zalcano_chamber").arena(13, "lava")
                .size(1.4f, 2.6f, 1.8f).moveSpeed(0.0);
    }

    private static BossDef nightmareBase(String id, String name, int level, int hp, AttackDef melee, AttackDef magic, AttackDef ranged,
                                         AttackDef claws, AttackDef surge) {
        // No BossDef phases: the script runs her shield, totem and desperation phases itself (they don't follow her health).
        return BossDef.of(id, name).level(level).stats(hp, 150, 150, 150, 150, 150).speed(6)
                .defenceBonus(120, 180, 40, 600, 600) // wiki/DPS calc: stab 120, slash 180, crush 40, magic/ranged 600
                .attack(melee).attack(magic).attack(ranged).attack(claws).attack(surge).attack(NIGHTMARE_SPORES)
                .at("sisterhood_sanctuary").arena(14, "crypt").size(1.4f, 3.2f, 1.8f).moveSpeed(0.2);
    }

    static BossDef nightmare() {
        // Wiki drop table (BossDropsTable patches the main lines); the elite clue 1/190 is the MVP's rate (1/200 for the
        // others), as the jar of dreams' 1/1,900. Uniques are two tables of their own (the script's loot roll). Regular
        // bones go to every player eligible for a unique, big bones (the patch) to the MVP.
        return nightmareBase("the_nightmare", "The Nightmare", 814, 2400, NIGHTMARE_MELEE, NIGHTMARE_MAGIC, NIGHTMARE_RANGED,
                NIGHTMARE_CLAWS, NIGHTMARE_SURGE).attackBonus(140, 140, 140)
                .drops(Drop.always("bones"), Drop.of("nightmare_staff", 1, 1, 300), Drop.of("inquisitors_great_helm", 1, 1, 420),
                        Drop.of("inquisitors_hauberk", 1, 1, 420), Drop.of("inquisitors_plateskirt", 1, 1, 420),
                        Drop.of("inquisitors_mace", 1, 1, 750), Drop.of("eldritch_orb", 1, 1, 960), Drop.of("harmonised_orb", 1, 1, 960),
                        Drop.of("volatile_orb", 1, 1, 960), Drop.of("jar_of_dreams", 1, 1, NightmareRules.JAR_MVP),
                        Drop.of("runite_ore", 1, 16, 50), Drop.of("coins", 2291, 43958, 50),
                        Drop.of("clue_scroll_elite", 1, 1, NightmareRules.CLUE_MVP))
                .pet("pet_little_nightmare");
    }

    static BossDef phosani() {
        // Wiki/DPS calculator: level 1,024, 3,200 hitpoints. Uniques per kill: staff 69/35,000, mace 31/35,000, armour 1/700
        // each, orbs 1/1,600 each; parasitic egg 1/200, jar 1/4,000, clue 1/35 (Mod Arcane), Slepey tablet 1/25 (and on the
        // 25th kill). Commons follow the wiki's /100 table.
        return nightmareBase("phosanis_nightmare", "Phosani's Nightmare", 1024, 3200, PHOSANI_MELEE, PHOSANI_MAGIC, PHOSANI_RANGED,
                PHOSANI_CLAWS, PHOSANI_SURGE).attackBonus(220, 220, 220)
                .drops(Drop.always("big_bones"), Drop.of("nightmare_staff", 1, 1, 507), Drop.of("inquisitors_great_helm", 1, 1, 700),
                        Drop.of("inquisitors_hauberk", 1, 1, 700), Drop.of("inquisitors_plateskirt", 1, 1, 700),
                        Drop.of("inquisitors_mace", 1, 1, 1129), Drop.of("eldritch_orb", 1, 1, 1600), Drop.of("harmonised_orb", 1, 1, 1600),
                        Drop.of("volatile_orb", 1, 1, 1600), Drop.of("parasitic_egg", 1, 1, 200), Drop.of("jar_of_dreams", 1, 1, 4000),
                        Drop.of("slepey_tablet", 1, 1, NightmareRules.SLEPEY_TABLET_RATE),
                        Drop.of("runite_ore", 11, 26, 50), Drop.of("blood_rune", 343, 765, 25), Drop.of("soul_rune", 110, 228, 25),
                        Drop.of("death_rune", 165, 305, 25), Drop.of("magic_logs", 40, 95, 25), Drop.of("shark", 13, 26, 17),
                        Drop.of("coins", 41417, 74500, 50), Drop.of("clue_scroll_elite", 1, 1, 35))
                .pet("pet_little_nightmare");
    }

    static BossDef muspah() {
        // No BossDef phases: the script swaps its forms by damage, uses its specials at 637 and 425 and raises its shield
        // below 127 (MuspahRules). Its levels and defences are the DPS calculator's (MonsterStatsTable).
        return BossDef.of("phantom_muspah", "Phantom Muspah").level(741).stats(850, 280, 280, 200, 150, 280).speed(MuspahRules.RANGED_SPEED)
                .defenceBonus(185, 134, 120, 437, 56)
                .attack(MUSPAH_RANGED).attack(MUSPAH_SMITE).attack(MUSPAH_MELEE).attack(MUSPAH_CLOUDS).attack(MUSPAH_SHOCKWAVE)
                // Wiki: one roll on its unique table (essence, icon, shard; the frozen cache isn't in RuneCraft) beside two on its
                // secondary table (BossDropsTable; a Venator shard comes alone, the script's loot roll); tertiaries hard clue
                // 1/30, elite clue 1/45; Muphin 1/2,500 (Pets).
                .drops(Drop.of("ancient_essence", 540, 2060, 1), Drop.of("venator_shard", 1, 1, 100), Drop.of("ancient_icon", 1, 1, 50),
                        Drop.of("runite_ore", 5, 5, 10), Drop.of("death_rune", 200, 200, 10),
                        Drop.of("super_restore", 3, 3, 12), Drop.of("clue_scroll_hard", 1, 1, 30), Drop.of("clue_scroll_elite", 1, 1, 45))
                .pet("pet_muphin").at("ghorrock_dungeon").arena(14, "ice")
                .size(1.4f, 2.6f, 1.8f).moveSpeed(0.2);
    }

    static BossDef solHeredit() {
        return BossDef.of("sol_heredit", "Sol Heredit").level(1563).stats(1500, 350, 350, 300, 300, 300).speed(5)
                .defenceBonus(65, 65, 65, 65, 65)
                .attack(SOL_SPEAR).attack(SOL_SHIELD).attack(SOL_TRIPLE).attack(SOL_LASERS).attack(SOL_GRAPPLE)
                // Wiki phase transitions and lines (minigames.SolHeredit); his damage doesn't scale with the phase.
                .phase(90, com.runecraft.osrs.content.minigames.SolHeredit.PHASE_LINES.get(0), 1.0, false)
                .phase(75, com.runecraft.osrs.content.minigames.SolHeredit.PHASE_LINES.get(1), 1.0, false)
                .phase(50, com.runecraft.osrs.content.minigames.SolHeredit.PHASE_LINES.get(2), 1.0, false)
                .phase(25, com.runecraft.osrs.content.minigames.SolHeredit.PHASE_LINES.get(3), 1.0, false)
                .phase(10, com.runecraft.osrs.content.minigames.SolHeredit.PHASE_LINES.get(4), 1.0, false)
                .drops(Drop.of("sunfire_splinters", 200, 600, 1), Drop.of("dizanas_quiver", 1, 1, 1),
                        Drop.of("sunfire_fanatic_helm", 1, 1, 90), Drop.of("sunfire_fanatic_cuirass", 1, 1, 90),
                        Drop.of("sunfire_fanatic_chausses", 1, 1, 90), Drop.of("echo_crystal", 1, 1, 60),
                        Drop.of("tonalztics_of_ralos", 1, 1, 150), Drop.of("runite_bar", 5, 10, 4), Drop.of("coins", 50000, 150000, 2))
                // Fought only as the Colosseum's wave 12 (ColosseumSession): no summoning stone or surface arena.
                .pet("pet_smol_heredit").at("fortis_colosseum").arena(15, "sand").raidOnly()
                .size(0.9f, 2.4f, 1.6f).moveSpeed(0.26);
    }
}
