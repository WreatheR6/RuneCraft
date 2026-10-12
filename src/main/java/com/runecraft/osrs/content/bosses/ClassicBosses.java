package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;

/**
 * Classic bosses: Giant Mole, Kalphite Queen, Dagannoth Kings, Sarachnis, Corporeal Beast,
 * Skotizo, Hespori, Scurrius and the Mimic. Stats, max hits and drop rates follow the OSRS wiki;
 * commodity loot lines are kept below 1/24 so only real uniques fill the collection log.
 */
public final class ClassicBosses {
    private ClassicBosses() {
    }

    // ---- Kalphite Queen ----
    public static final AttackDef KQ_MELEE = AttackDef.of("mandibles", AttackType.STAB, 31, 3.5, 6);
    public static final AttackDef KQ_SPINES = AttackDef.of("spines", AttackType.RANGED, 31, 12, 4).particle("minecraft:crit");
    public static final AttackDef KQ_MAGIC = AttackDef.of("lightning", AttackType.MAGIC, 31, 12, 4)
            .particle("minecraft:electric_spark");
    /** Airborne form: the magic bolt chains to every player nearby. */
    public static final AttackDef KQ_CHAIN_MAGIC = AttackDef.of("chain_lightning", AttackType.MAGIC, 31, 12, 3)
            .special(Special.AOE, 8).particle("minecraft:electric_spark").message("The Kalphite Queen's lightning arcs!");

    // ---- Dagannoth Kings ----
    public static final AttackDef REX_MELEE = AttackDef.of("bite", AttackType.SLASH, 26, 3.5, 1);
    public static final AttackDef PRIME_MAGIC = AttackDef.of("water_blast", AttackType.MAGIC, 50, 12, 1).particle("minecraft:splash");
    public static final AttackDef SUPREME_RANGED = AttackDef.of("spines", AttackType.RANGED, 30, 12, 1).particle("minecraft:crit");

    // ---- Corporeal Beast ----
    public static final AttackDef CORP_STOMP = AttackDef.of("stomp", AttackType.CRUSH, 51, 2.0, 1).special(Special.AOE, 2)
            .message("The Corporeal Beast stomps the ground!");
    public static final AttackDef CORP_MELEE = AttackDef.of("crush", AttackType.CRUSH, 33, 3.5, 4); // wiki: 33 (51 before its rework; stomp keeps 51)
    public static final AttackDef CORP_ORB = AttackDef.of("spectral_orb", AttackType.MAGIC, 65, 14, 4) // wiki: regular magic attack, up to 65
            .particle("minecraft:soul_fire_flame");
    public static final AttackDef CORP_DRAIN = AttackDef.of("draining_orb", AttackType.MAGIC, 55, 14, 3) // wiki: up to 55, drains Magic or Prayer
            .special(Special.DRAIN_PRAYER, 12).particle("minecraft:soul");
    public static final AttackDef CORP_SPLASH = AttackDef.of("splash_orb", AttackType.MAGIC, 42, 14, 3)
            .special(Special.SKY_STRIKE, 0).telegraph(3, "The Corporeal Beast hurls an orb that splits apart!")
            .particle("minecraft:soul_fire_flame");

    public static List<BossDef> all() {
        return List.of(giantMole(), kalphiteQueen(), kalphiteQueenAirborne(), dagannothRex(), dagannothPrime(), dagannothSupreme(),
                sarachnis(), corporealBeast(), skotizo(), hespori(), scurrius(), mimic());
    }

    static BossDef giantMole() {
        return BossDef.of("giant_mole", "Giant Mole").level(230).stats(200, 200, 200, 200, 200, 200).speed(4)
                .defenceBonus(60, 80, 100, 80, 60)
                .attack(AttackDef.of("claw", AttackType.CRUSH, 21, 3.5, 1))
                .phase(50, "The Giant Mole starts digging frantically...", 1.0, false)
                .drops(Drop.always("big_bones"), Drop.always("mole_claw"), Drop.of("mole_skin", 1, 3, 1),
                        Drop.of("adamant_longsword", 1, 1, 16), Drop.of("mithril_bar", 1, 1, 16), Drop.of("death_rune", 7, 7, 16),
                        Drop.of("iron_ore", 10, 20, 16), Drop.of("shark", 4, 4, 20), Drop.of("coins", 1500, 3000, 4),
                        Drop.herbs(16), Drop.of("clue_scroll_elite", 1, 1, 500))
                .pet("pet_baby_mole").at("falador_mole_hills").arena(12, "jungle")
                .size(1.2f, 1.0f, 2.2f).moveSpeed(0.22);
    }

    static BossDef kalphiteQueen() {
        return BossDef.of("kalphite_queen", "Kalphite Queen").level(333).stats(255, 300, 300, 300, 150, 1).speed(4)
                // Crawling form: weak to crush; its chitin halves ranged and magic damage (see script).
                .defenceBonus(50, 50, 10, 100, 100)
                .attack(KQ_MELEE).attack(KQ_SPINES).attack(KQ_MAGIC)
                .drops(Drop.always("bones"), Drop.of("kq_head", 1, 1, 128), Drop.of("dragon_chainbody", 1, 1, 128),
                        Drop.of("dragon_2h_sword", 1, 1, 256), Drop.of("dragon_pickaxe", 1, 1, 400), Drop.of("jar_of_sand", 1, 1, 2000),
                        Drop.of("rune_platebody", 1, 1, 16), Drop.of("rune_2h_sword", 1, 1, 16), Drop.of("death_rune", 150, 150, 16),
                        Drop.of("blood_rune", 100, 100, 16), Drop.of("magic_logs", 60, 60, 20), Drop.of("runite_bar", 3, 3, 20),
                        Drop.of("coins", 15000, 20000, 6), Drop.of("clue_scroll_elite", 1, 1, 100))
                .drops(Drop.of("lava_battlestaff", 1, 1, 64))
                .pet("pet_kalphite_princess").at("kalphite_lair").arena(13, "sand")
                .size(1.4f, 1.2f, 2.0f).moveSpeed(0.2);
    }

    static BossDef kalphiteQueenAirborne() {
        // Second form: spawned by the crawling form's script with full hitpoints; weak to stab, melee is halved.
        return BossDef.of("kalphite_queen_airborne", "Kalphite Queen").level(333).stats(255, 300, 300, 300, 150, 1).speed(4)
                .defenceBonus(10, 100, 100, 10, 10)
                .attack(KQ_MELEE).attack(KQ_SPINES).attack(KQ_CHAIN_MAGIC)
                .raidOnly().size(1.2f, 2.0f, 2.0f).moveSpeed(0.24);
    }

    static BossDef dagannothRex() {
        return dk("dagannoth_rex", "Dagannoth Rex", REX_MELEE, "berserker_ring", "warrior_ring", "pet_dagannoth_rex")
                .drops(Drop.of("rock_shell_plate", 1, 1, 128), Drop.of("rock_shell_legs", 1, 1, 128),
                        Drop.of("fremennik_blade", 1, 1, 64))
                .defenceBonus(255, 255, 255, 10, 255)
                .stats(255, 255, 255, 255, 1, 1).at("waterbirth_island");
    }

    static BossDef dagannothPrime() {
        return dk("dagannoth_prime", "Dagannoth Prime", PRIME_MAGIC, "seers_ring", "mud_battlestaff", "pet_dagannoth_prime")
                .drops(Drop.of("farseer_helm", 1, 1, 128), Drop.of("skeletal_top", 1, 1, 128),
                        Drop.of("skeletal_bottoms", 1, 1, 128), Drop.of("fremennik_blade", 1, 1, 128))
                .defenceBonus(255, 255, 255, 255, 10)
                .stats(255, 255, 255, 255, 255, 1).raidOnly();
    }

    static BossDef dagannothSupreme() {
        return dk("dagannoth_supreme", "Dagannoth Supreme", SUPREME_RANGED, "archers_ring", "seercull", "pet_dagannoth_supreme")
                .drops(Drop.of("archer_helm", 1, 1, 128), Drop.of("spined_body", 1, 1, 128),
                        Drop.of("spined_chaps", 1, 1, 128), Drop.of("fremennik_blade", 1, 1, 128),
                        Drop.of("rune_thrownaxe", 5, 10, 26))
                .defenceBonus(10, 10, 10, 255, 550)
                .stats(255, 255, 255, 128, 255, 255).raidOnly();
    }

    private static BossDef dk(String id, String name, AttackDef attack, String unique1, String unique2, String pet) {
        return BossDef.of(id, name).level(303).speed(4).attack(attack)
                .drops(Drop.always("dagannoth_bones"), Drop.of(unique1, 1, 1, 128), Drop.of(unique2, 1, 1, 128),
                        Drop.of("dragon_axe", 1, 1, 128), Drop.of("rune_axe", 1, 1, 16), Drop.of("adamant_platebody", 1, 1, 16),
                        Drop.of("steel_bar", 15, 30, 16), Drop.of("raw_shark", 5, 5, 16), Drop.of("coins", 1000, 10000, 8),
                        Drop.of("clue_scroll_elite", 1, 1, 750),
                        Drop.of("fremennik_shield", 1, 1, 128), Drop.of("fremennik_helm", 1, 1, 128))
                .pet(pet).arena(13, "water").size(1.0f, 2.2f, 1.6f).moveSpeed(0.24);
    }

    static BossDef sarachnis() {
        return BossDef.of("sarachnis", "Sarachnis").level(318).stats(400, 150, 150, 150, 150, 150).speed(4)
                .defenceBonus(150, 150, 40, 60, 150)
                .attack(AttackDef.of("bite", AttackType.STAB, 31, 3.5, 6).special(Special.POISON, 8))
                .attack(AttackDef.of("web_spit", AttackType.RANGED, 15, 12, 4).special(Special.FREEZE, 4)
                        .particle("minecraft:item_snowball").message("Sarachnis spits a sticky web!"))
                .phase(66, "Sarachnis calls upon her children!", 1.0, true)
                .phase(33, "Sarachnis calls upon her children once more!", 1.1, true)
                .minion("spawn_of_sarachnis", 2)
                .drops(Drop.always("big_bones"), Drop.of("sarachnis_cudgel", 1, 1, 384), Drop.of("jar_of_eyes", 1, 1, 2000),
                        Drop.of("giant_egg_sac_full", 1, 1, 20), Drop.of("red_spiders_eggs", 30, 60, 8),
                        Drop.of("snapdragon_seed", 1, 2, 16), Drop.of("ranarr_seed", 1, 2, 16), Drop.of("coal", 50, 50, 16),
                        Drop.of("adamantite_bar", 6, 8, 16), Drop.of("rune_full_helm", 1, 1, 16), Drop.of("coins", 5000, 15000, 6),
                        Drop.of("clue_scroll_elite", 1, 1, 60))
                .pet("pet_sraracha").at("forthos_dungeon").arena(12, "crypt")
                .size(1.4f, 1.1f, 1.9f).moveSpeed(0.24);
    }

    static BossDef corporealBeast() {
        return BossDef.of("corporeal_beast", "Corporeal Beast").level(785).stats(2000, 320, 320, 310, 350, 150).speed(4)
                .attackBonus(50, 0, 0)
                .defenceBonus(25, 200, 100, 150, 230)
                .attack(CORP_MELEE).attack(CORP_ORB).attack(CORP_DRAIN).attack(CORP_SPLASH)
                .phase(75, "The Corporeal Beast releases a dark energy core!", 1.0, false)
                .drops(Drop.of("spirit_shield", 1, 1, 64), Drop.of("holy_elixir", 1, 1, 171), Drop.of("spectral_sigil", 1, 1, 1365),
                        Drop.of("arcane_sigil", 1, 1, 1365), Drop.of("elysian_sigil", 1, 1, 4095), Drop.of("jar_of_spirits", 1, 1, 1000),
                        Drop.of("death_rune", 300, 300, 8), Drop.of("soul_rune", 250, 250, 8), Drop.of("runite_bar", 10, 10, 16),
                        Drop.of("magic_logs", 75, 75, 16), Drop.of("shark", 70, 70, 16), Drop.of("coins", 20000, 50000, 8),
                        Drop.of("uncut_onyx", 1, 1, 23), Drop.of("onyx_bolts_e", 175, 175, 26), Drop.of("clue_scroll_elite", 1, 1, 200))
                .drops(Drop.of("mystic_robe_top", 1, 1, 28), Drop.of("mystic_robe_bottom", 1, 1, 28),
                        Drop.of("mystic_air_staff", 1, 1, 43), Drop.of("mystic_water_staff", 1, 1, 43),
                        Drop.of("mystic_earth_staff", 1, 1, 43), Drop.of("mystic_fire_staff", 1, 1, 43))
                .pet("pet_dark_core").at("corporeal_beast_lair").arena(14, "void").wilderness()
                .size(1.6f, 2.2f, 2.0f).moveSpeed(0.2);
    }

    static BossDef skotizo() {
        return BossDef.of("skotizo", "Skotizo").level(321).stats(450, 240, 240, 200, 280, 1).speed(5)
                .defenceBonus(80, 80, 80, 160, 80)
                .attack(AttackDef.of("claws", AttackType.SLASH, 38, 3.5, 6))
                .attack(AttackDef.of("shadow_bolt", AttackType.MAGIC, 38, 12, 4).particle("minecraft:squid_ink"))
                .phase(50, "Skotizo summons the dead to its side!", 1.0, true)
                .minion("reanimated_demon_spawn", 3)
                .drops(Drop.always("infernal_ashes"), Drop.of("dark_claw", 1, 1, 25), Drop.of("jar_of_darkness", 1, 1, 200),
                        Drop.of("uncut_onyx", 1, 1, 1000), Drop.of("ancient_shard", 1, 3, 4), Drop.of("dark_totem_base", 1, 1, 8),
                        Drop.of("soul_rune", 100, 200, 8), Drop.of("rune_platebody", 1, 1, 12), Drop.of("coins", 20000, 30000, 4),
                        Drop.of("clue_scroll_elite", 1, 1, 5))
                .key("dark_totem").pet("pet_skotos").at("catacombs_of_kourend").arena(12, "crypt")
                .size(1.2f, 2.4f, 1.8f).moveSpeed(0.22);
    }

    static BossDef hespori() {
        return BossDef.of("hespori", "Hespori").level(284).stats(300, 1, 1, 100, 150, 1).speed(5)
                .defenceBonus(80, 20, 80, 60, 80)
                .attack(AttackDef.of("thorn_spray", AttackType.MAGIC, 15, 12, 7).particle("minecraft:spore_blossom_air"))
                .attack(AttackDef.of("entangle", AttackType.MAGIC, 8, 12, 3).special(Special.FREEZE, 6)
                        .message("Hespori's vines entangle you!"))
                .drops(Drop.of("bottomless_compost_bucket", 1, 1, 35), Drop.of("attas_seed", 1, 1, 3), Drop.of("iasor_seed", 1, 1, 3),
                        Drop.of("kronos_seed", 1, 1, 3), Drop.seeds(1), Drop.herbs(2), Drop.of("snapdragon_seed", 1, 2, 12),
                        Drop.of("torstol_seed", 1, 1, 16))
                .at("farming_guild_hespori").arena(11, "jungle")
                .size(1.6f, 1.8f, 1.8f).moveSpeed(0.0);
    }

    static BossDef scurrius() {
        return BossDef.of("scurrius", "Scurrius").level(250).stats(500, 140, 120, 80, 120, 120).speed(4)
                .defenceBonus(20, 20, 20, 20, 20)
                .attack(AttackDef.of("bite", AttackType.STAB, 18, 3.0, 6))
                .attack(AttackDef.of("flying_rats", AttackType.RANGED, 12, 10, 2).particle("minecraft:crit"))
                .attack(AttackDef.of("rat_magic", AttackType.MAGIC, 12, 10, 2).particle("minecraft:witch"))
                .attack(AttackDef.of("falling_ceiling", AttackType.CRUSH, 20, 10, 2).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "Debris begins to fall from the ceiling!").particle("minecraft:ash"))
                .phase(66, "Scurrius calls for his minions!", 1.0, true)
                .phase(33, "Scurrius calls for his minions!", 1.1, true)
                .minion("scurrius_giant_rat", 3)
                .drops(Drop.always("big_bones"), Drop.of("scurrius_spine", 1, 1, 33), Drop.of("coins", 1000, 3000, 2),
                        Drop.of("iron_bar", 5, 10, 8), Drop.of("mithril_bar", 3, 6, 8), Drop.of("adamant_platebody", 1, 1, 16),
                        Drop.of("rune_full_helm", 1, 1, 20), Drop.of("law_rune", 20, 30, 8))
                .pet("pet_scurry").at("varrock_sewers").arena(11, "crypt")
                .size(1.0f, 1.0f, 2.2f).moveSpeed(0.25);
    }

    static BossDef mimic() {
        // The Mimic guards elite/master caskets: summon it with a master clue and loot extra clue rewards.
        return BossDef.of("the_mimic", "The Mimic").level(186).stats(220, 160, 160, 150, 1, 150).speed(4)
                .defenceBonus(100, 100, 100, 60, 100)
                .attack(AttackDef.of("chomp", AttackType.CRUSH, 30, 3.0, 5))
                .attack(AttackDef.of("candy_barrage", AttackType.RANGED, 28, 12, 3).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "The Mimic flings a barrage of candies!").particle("minecraft:totem_of_undying"))
                .attack(AttackDef.of("lid_slam", AttackType.CRUSH, 25, 3, 2).special(Special.AOE, 3))
                .drops(Drop.of("coins", 50000, 150000, 1), Drop.of("rune_platebody", 1, 1, 4), Drop.of("dragon_scimitar", 1, 1, 8),
                        Drop.of("dragon_longsword", 1, 1, 12), Drop.of("onyx", 1, 1, 20), Drop.of("clue_scroll_elite", 1, 1, 4))
                .key("clue_scroll_master").at("watsons_house").arena(10, "stone")
                .size(1.0f, 1.0f, 1.8f).moveSpeed(0.2);
    }
}
