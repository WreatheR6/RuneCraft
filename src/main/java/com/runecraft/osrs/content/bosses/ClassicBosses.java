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
    // OSRS Wiki: every attack hits up to 31; her ranged and magic attacks have 100% accuracy (Mod Ash), the spines also hit
    // players near her target and the lightning bounces from player to player: area attacks (which always roll 0 to the max
    // hit; protection prayers still block them) over the players around her.
    public static final AttackDef KQ_MELEE = AttackDef.of("mandibles", AttackType.STAB, 31, 3.5, 6);
    public static final AttackDef KQ_SPINES = AttackDef.of("spines", AttackType.RANGED, 31, 12, 4).special(Special.AOE, 12)
            .particle("minecraft:crit");
    public static final AttackDef KQ_MAGIC = AttackDef.of("lightning", AttackType.MAGIC, 31, 12, 4).special(Special.AOE, 12)
            .particle("minecraft:electric_spark");
    /** Airborne form: the magic bolt chains to every player nearby. */
    public static final AttackDef KQ_CHAIN_MAGIC = AttackDef.of("chain_lightning", AttackType.MAGIC, 31, 12, 3)
            .special(Special.AOE, 12).particle("minecraft:electric_spark").message("The Kalphite Queen's lightning arcs!");
    /** Each of her spines drains a prayer point (OSRS Wiki). */
    public static final int KQ_SPINE_PRAYER_DRAIN = 1;
    /** Game ticks her first form takes to shed its exoskeleton before the airborne form emerges (OSRS Wiki: 20, 12 seconds). */
    public static final int KQ_TRANSFORM_TICKS = 20;

    // ---- Dagannoth Kings ----
    public static final AttackDef REX_MELEE = AttackDef.of("bite", AttackType.SLASH, 26, 3.5, 1);
    public static final AttackDef PRIME_MAGIC = AttackDef.of("water_blast", AttackType.MAGIC, 50, 12, 1).particle("minecraft:splash");
    public static final AttackDef SUPREME_RANGED = AttackDef.of("spines", AttackType.RANGED, 30, 12, 1).particle("minecraft:crit");

    // ---- Corporeal Beast ----
    // OSRS Wiki "Corporeal Beast/Strategies", Mechanics: in melee range it uses its melee attack 40% of the time and each of
    // its three magic attacks 20%; out of range each magic attack a third (weights 2:1:1:1, melee only in reach). Protect from
    // Magic blocks only a third of its magic damage, so the magic attacks pierce prayer and the script takes the third off.
    /** The stomp at anyone standing under it: checked every 7 ticks, always 30-51, through prayer (scripted timer). */
    public static final AttackDef CORP_STOMP = AttackDef.of("stomp", AttackType.CRUSH, 51, 2.0, 1).special(Special.AOE, 2)
            .message("The Corporeal Beast stomps the ground!");
    public static final AttackDef CORP_MELEE = AttackDef.of("crush", AttackType.CRUSH, 33, 3.5, 2); // wiki: 33 (51 before its rework; stomp keeps 51)
    public static final AttackDef CORP_ORB = AttackDef.of("spectral_orb", AttackType.MAGIC, 65, 14, 1) // wiki: regular magic attack, up to 65
            .special(Special.PRAYER_PIERCING, 0).particle("minecraft:soul_fire_flame");
    /** Up to 55; a hit drains Magic or Prayer by 1-2 and heals the Beast for half the damage (script). */
    public static final AttackDef CORP_DRAIN = AttackDef.of("draining_orb", AttackType.MAGIC, 55, 14, 1)
            .special(Special.PRAYER_PIERCING, 0).particle("minecraft:soul");
    /** Aimed at the player's tile, it splits into six smaller orbs over a 7x7 area (the script lands them). */
    public static final AttackDef CORP_SPLASH = AttackDef.of("splash_orb", AttackType.MAGIC, 40, 14, 1)
            .special(Special.PRAYER_PIERCING, 0).message("The Corporeal Beast hurls an orb that splits apart!")
            .particle("minecraft:soul_fire_flame");

    /** Stomp: 30-51 to everyone under the Beast, checked every 7 game ticks (OSRS Wiki). */
    public static final int CORP_STOMP_MIN = 30;
    public static final int CORP_STOMP_TICKS = 7;
    /** Splash orb: the main orb hits up to 40 on its tile, 30 next to it; each of the six splinters 30 / 20 (OSRS Wiki). */
    public static final int CORP_SPLASH_DIRECT = 40;
    public static final int CORP_SPLASH_ADJACENT = 30;
    public static final int CORP_SPLINTER_DIRECT = 30;
    public static final int CORP_SPLINTER_ADJACENT = 20;
    public static final int CORP_SPLINTERS = 6;
    /** The splinters land within 3 tiles of the targeted tile (a 7x7 area). */
    public static final int CORP_SPLINTER_RANGE_TILES = 3;
    /** Drained stats come back one level every 20 game ticks (OSRS Wiki). */
    public static final int CORP_STAT_REGEN_TICKS = 20;
    /** The dark energy core: 1/8 after a hit of 32+ or an attack below 1,000 hitpoints; it drains 5-13 every 2 ticks. */
    public static final int CORE_CHANCE = 8;
    public static final int CORE_BIG_HIT = 32;
    public static final int CORE_BELOW_HP = 1000;

    /**
     * Corpbane weapons (OSRS Wiki "Corpbane weapons"): the spears and halberds enabled against the Corporeal Beast, Osmumten's
     * fang, the thunder khopesh and King's barrage. Not hastas, the dragon hunter lance or the Moons' and Gauntlet's spears.
     */
    private static final java.util.Set<String> CORPBANE = java.util.Set.of(
            "bronze_spear", "iron_spear", "steel_spear", "black_spear", "mithril_spear", "adamant_spear", "rune_spear", "dragon_spear",
            "bone_spear", "gilded_spear", "leaf_bladed_spear", "zamorakian_spear", "sunspear",
            "bronze_halberd", "iron_halberd", "steel_halberd", "black_halberd", "white_halberd", "mithril_halberd", "adamant_halberd",
            "rune_halberd", "dragon_halberd", "crystal_halberd", "noxious_halberd",
            "osmumtens_fang", "thunder_khopesh", "kings_barrage");

    /** Whether a held weapon is a Corpbane weapon (poisoned variants and Guthan's warspear at any charge too). */
    public static boolean corpbane(String weaponId) {
        if (weaponId == null || weaponId.isEmpty()) {
            return false;
        }
        String id = weaponId.replaceAll("_p+$", "");
        return CORPBANE.contains(id) || id.startsWith("guthans_warspear");
    }

    /**
     * Damage the Corporeal Beast takes (OSRS Wiki): magic in full, a Corpbane weapon on the stab style in full, every other
     * melee and ranged hit halved (rounded down).
     */
    public static int corpDamage(int damage, AttackType type, String weaponId) {
        if (type == AttackType.MAGIC || type == AttackType.STAB && corpbane(weaponId)) {
            return damage;
        }
        return damage / 2;
    }

    /** Its magic damage under Protect from Magic: the prayer blocks a third (OSRS Wiki). */
    public static int corpMagicThroughPrayer(int damage) {
        return damage - damage / 3;
    }

    public static List<BossDef> all() {
        return List.of(giantMole(), kalphiteQueen(), kalphiteQueenAirborne(), dagannothRex(), dagannothPrime(), dagannothSupreme(),
                sarachnis(), corporealBeast(), skotizo(), hespori(), scurrius(), mimic());
    }

    /** Giant Mole (OSRS Wiki, Mod Ash): each attack has a 1/4 chance to make her burrow between 50% and 5% of her health. */
    public static final int MOLE_BURROW_CHANCE = 4;
    public static final int MOLE_BURROW_BELOW = 50;
    public static final int MOLE_BURROW_ABOVE = 5;

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
                // No surface arena: the summoning stone stands in the Kings' lair below the Waterbirth Island Dungeon (dungeons module).
                .stats(255, 255, 255, 255, 1, 1);
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
                .drops(Drop.always("dagannoth_bones"), Drop.always("dagannoth_hide"), Drop.of(unique1, 1, 1, 128), Drop.of(unique2, 1, 1, 128),
                        Drop.of("dragon_axe", 1, 1, 128), Drop.of("rune_axe", 1, 1, 16), Drop.of("adamant_platebody", 1, 1, 16),
                        Drop.of("steel_bar", 15, 30, 16), Drop.of("raw_shark", 5, 5, 16), Drop.of("coins", 1000, 10000, 8),
                        Drop.of("clue_scroll_elite", 1, 1, 750),
                        Drop.of("fremennik_shield", 1, 1, 128), Drop.of("fremennik_helm", 1, 1, 128))
                .pet(pet).arena(13, "water").size(1.0f, 2.2f, 1.6f).moveSpeed(0.24);
    }

    // ---- Sarachnis (OSRS Wiki "Sarachnis/Strategies", Mechanics) ----
    /** Her legs when the player is next to her (up to 31; a successful hit heals her 5). */
    public static final AttackDef SARACHNIS_MELEE = AttackDef.of("bite", AttackType.STAB, 31, 3.5, 1);
    /** Barbed projectiles when the player is out of reach (up to 31; a successful hit heals her 10). */
    public static final AttackDef SARACHNIS_RANGED = AttackDef.of("web_spit", AttackType.RANGED, 31, 12, 1)
            .particle("minecraft:crit");
    /** "Hsss!": every fourth standard attack binds the player for 6 ticks (no damage), then she moves away. */
    public static final AttackDef SARACHNIS_WEB = AttackDef.of("sticky_web", AttackType.RANGED, 0, 12, 1)
            .particle("minecraft:item_snowball");
    public static final int SARACHNIS_MELEE_HEAL = 5;
    public static final int SARACHNIS_RANGED_HEAL = 10;
    public static final int SARACHNIS_WEB_EVERY = 4;
    public static final int SARACHNIS_WEB_BIND_TICKS = 6;
    /** At 66% and 33% she summons a melee spawn (west) and a magic spawn (east) for each player. */
    public static final String SARACHNIS_SPAWN_MAGE = "spawn_of_sarachnis_mage";
    public static final String SARACHNIS_SPAWN_MELEE = "spawn_of_sarachnis";

    static BossDef sarachnis() {
        return BossDef.of("sarachnis", "Sarachnis").level(318).stats(400, 150, 150, 150, 150, 150).speed(4)
                .defenceBonus(150, 150, 40, 60, 150)
                .attack(SARACHNIS_MELEE).attack(SARACHNIS_RANGED)
                .phase(66, "Sarachnis calls upon her children!", 1.0, false)
                .phase(33, "Sarachnis calls upon her children once more!", 1.0, false)
                .minion(SARACHNIS_SPAWN_MELEE, 2)
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

    /**
     * Skotizo's ancient shards (OSRS Wiki, Mod Ash): a table of their own on every kill, 1 shard 75/100, 2 15/100, 3 5/100,
     * 4 4/100 and 5 1/100 (1.41 a kill). {@code roll100} is uniform in [0, 100).
     */
    public static int skotizoShards(int roll100) {
        int r = Math.floorMod(roll100, 100);
        return r < 75 ? 1 : r < 90 ? 2 : r < 95 ? 3 : r < 99 ? 4 : 5;
    }

    static BossDef skotizo() {
        // OSRS wiki: Str 250; 80 stab/slash/crush/magic and 130 ranged defence.
        return BossDef.of("skotizo", "Skotizo").level(321).stats(450, 240, 250, 200, 280, 1).speed(5)
                .defenceBonus(80, 80, 80, 80, 130)
                .attack(AttackDef.of("claws", AttackType.SLASH, 38, 3.5, 6))
                .attack(AttackDef.of("shadow_bolt", AttackType.MAGIC, 38, 12, 4).particle("minecraft:squid_ink"))
                // Gar mulno ful taglo! Three reanimated demon spawns once two thirds of his hitpoints are gone (OSRS wiki);
                // the script summons dark ankou and re-summons slain spawns.
                .phase(SkotizoRules.SPAWN_BELOW_PERCENT, "Gar mulno ful taglo!", 1.0, false)
                .minion("reanimated_demon_spawn", SkotizoRules.DEMON_SPAWNS)
                // OSRS Wiki: a hard clue scroll on every kill (his best-known drop) besides the 1/5 elite one. Ancient shards:
                // 1-5 on every kill, rolled by his script (skotizoShards); BossDropsTable puts the wiki's five shard lines here.
                .drops(Drop.always("infernal_ashes"), Drop.always("clue_scroll_hard"), Drop.of("dark_claw", 1, 1, 25),
                        Drop.of("jar_of_darkness", 1, 1, 200),
                        Drop.of("uncut_onyx", 1, 1, 1000), Drop.always("ancient_shard"), Drop.of("dark_totem_base", 1, 1, 8),
                        Drop.of("soul_rune", 100, 200, 8), Drop.of("rune_platebody", 1, 1, 12), Drop.of("coins", 20000, 30000, 4),
                        Drop.of("clue_scroll_elite", 1, 1, 5))
                .key("dark_totem").pet("pet_skotos").at("catacombs_of_kourend").arena(12, "crypt")
                .size(1.2f, 2.4f, 1.8f).moveSpeed(0.22);
    }

    /**
     * Hespori (OSRS Wiki): green spores that hit twice for up to 8 each (ranged), a green wave up to 14 (magic), and the
     * entangle special: rooted for 10 ticks, then 40+ unless the player struggles free (scripted). Its four flower buds
     * open at the start of the fight and at 66% and 33% health.
     */
    public static final AttackDef HESPORI_SPORES = AttackDef.of("spores", AttackType.RANGED, 8, 12, 1)
            .special(Special.PRAYER_PIERCING, 0).particle("minecraft:spore_blossom_air");
    public static final AttackDef HESPORI_WAVE = AttackDef.of("green_wave", AttackType.MAGIC, 14, 12, 1)
            .special(Special.PRAYER_PIERCING, 0).particle("minecraft:composter");
    /**
     * Protection prayers don't block Hespori's attacks, they cut them (OSRS Wiki "Hespori/Strategies"): each spore to a max of
     * 2 under Protect from Missiles, the wave to 3 under Protect from Magic. The script scales the rolled hit down.
     */
    public static final int HESPORI_PRAYED_SPORE_MAX = 2;
    public static final int HESPORI_PRAYED_WAVE_MAX = 3;

    /** A rolled Hespori hit ({@code max} its unprayed max hit) under the matching protection prayer. */
    public static int hesporiPrayed(int damage, AttackType type, int max) {
        int cap = type == AttackType.RANGED ? HESPORI_PRAYED_SPORE_MAX : HESPORI_PRAYED_WAVE_MAX;
        return max <= 0 ? 0 : damage * cap / max;
    }
    public static final AttackDef HESPORI_ENTANGLE = AttackDef.of("entangle", AttackType.MAGIC, 44, 12, 1)
            .message("Hespori entangles you in some vines!");

    static BossDef hespori() {
        return BossDef.of("hespori", "Hespori").level(284).stats(300, 1, 1, 100, 150, 1).speed(5)
                .defenceBonus(80, 20, 80, 60, 80)
                .attack(HESPORI_SPORES).attack(HESPORI_WAVE).attack(HESPORI_ENTANGLE)
                .phase(66, null, 1.0, false)
                .phase(33, null, 1.0, false)
                .drops(Drop.of("bottomless_compost_bucket", 1, 1, 35), Drop.of("attas_seed", 1, 1, 3), Drop.of("iasor_seed", 1, 1, 3),
                        Drop.of("kronos_seed", 1, 1, 3), Drop.seeds(1), Drop.herbs(2), Drop.of("snapdragon_seed", 1, 2, 12),
                        Drop.of("torstol_seed", 1, 1, 16))
                .at("farming_guild_hespori").arena(11, "jungle")
                .size(1.6f, 1.8f, 1.8f).moveSpeed(0.0);
    }

    // ---- Scurrius (OSRS Wiki "Scurrius/Strategies"): the private instance's solo version ----
    // OSRS Wiki solo max hits: 13 tail swipe (melee), 8 bolts of electricity (magic), 7 flying fur (ranged), falling bricks
    // 15-22 (group: 13/14/13/24). A summoned Scurrius is a private fight, so the solo stats (MonsterStatsTable) apply.
    public static final AttackDef SCURRIUS_TAIL = AttackDef.of("bite", AttackType.STAB, 13, 3.0, 1);
    public static final AttackDef SCURRIUS_FUR = AttackDef.of("flying_rats", AttackType.RANGED, 7, 10, 1).particle("minecraft:crit");
    public static final AttackDef SCURRIUS_BOLTS = AttackDef.of("rat_magic", AttackType.MAGIC, 8, 10, 1).particle("minecraft:witch");
    /** Falling bricks: one at the player's tile, others around the arena; 15-22 each (script strikes). */
    public static final AttackDef SCURRIUS_BRICKS = AttackDef.of("falling_ceiling", AttackType.CRUSH, 22, 10, 1)
            .special(Special.SKY_STRIKE, 0).telegraph(3, "Debris begins to fall from the ceiling!").particle("minecraft:ash");
    public static final int SCURRIUS_BRICKS_MIN = 15;
    /** At 80% he eats at a food pile: five bites of 5 or 10 hitpoints per player, only magic, ranged and bricks meanwhile. */
    public static final int SCURRIUS_EAT_BELOW = 80;
    public static final int SCURRIUS_BITES = 5;
    /**
     * Between 80% and 30% he keeps going back to a pile "occasionally" (OSRS Wiki): game ticks of melee after he finishes
     * eating before the next pile. // approx: the wiki gives no interval
     */
    public static final int SCURRIUS_EAT_AGAIN_TICKS = 25;
    /** At 30% he holds the centre of the arena, only magic and ranged, faster; healed above it he goes back to eat. */
    public static final int SCURRIUS_CENTRE_BELOW = 30;
    /** Six giant rats a wave, at most twelve waves. */
    public static final int SCURRIUS_RATS = 6;
    public static final int SCURRIUS_RAT_WAVES = 12;

    static BossDef scurrius() {
        return BossDef.of("scurrius", "Scurrius").level(200).stats(500, 300, 100, 60, 50, 50).speed(4)
                .defenceBonus(20, 20, 20, 20, 20)
                .attack(SCURRIUS_TAIL).attack(SCURRIUS_FUR).attack(SCURRIUS_BOLTS).attack(SCURRIUS_BRICKS)
                .minion("scurrius_giant_rat", SCURRIUS_RATS)
                .drops(Drop.always("big_bones"), Drop.of("scurrius_spine", 1, 1, 33), Drop.of("coins", 1000, 3000, 2),
                        Drop.of("iron_bar", 5, 10, 8), Drop.of("mithril_bar", 3, 6, 8), Drop.of("adamant_platebody", 1, 1, 16),
                        Drop.of("rune_full_helm", 1, 1, 20), Drop.of("law_rune", 20, 30, 8))
                .pet("pet_scurry").at("varrock_sewers").arena(11, "crypt")
                .size(1.0f, 1.0f, 2.2f).moveSpeed(0.25);
    }

    /**
     * The Mimic from a master casket, killed on the first attempt (OSRS Wiki "The Mimic", master drops): always a mahogany
     * plank; main table 221/1,140 each of 600 death runes, 500 blood runes, 25 noted grimy ranarr weed, 25 noted wine of zamorak
     * and 15 noted raw manta rays. The ring of 3rd age / 3rd age pre-roll is the systems module's (MimicRewards). Later
     * attempts (smaller quantities, 6/240) aren't modelled.
     */
    static List<Drop> mimicDrops() {
        List<Drop> d = new java.util.ArrayList<>(List.of(Drop.always("mahogany_plank")));
        d.add(Drop.of("death_rune", 600, 600, 5));
        d.add(Drop.of("blood_rune", 500, 500, 5));
        d.add(Drop.of("grimy_ranarr_weed", 25, 25, 5));
        d.add(Drop.of("wine_of_zamorak", 25, 25, 5));
        d.add(Drop.of("raw_manta_ray", 15, 15, 5));
        return d;
    }

    static BossDef mimic() {
        // The Mimic: elite (1/35) and master (1/15) caskets can be a Mimic, fought at the strange casket in Watson's house.
        // OSRS Wiki infobox: 230 HP, 185/120/120, magic 60, ranged 1; +135 melee / +180 magic accuracy; defence 160/165/150,
        // magic 30, ranged 145; attack speed 3 (was 220 HP, 160/160/150, 150 magic, speed 4, flat 100 defences).
        return BossDef.of("the_mimic", "The Mimic").level(186).stats(230, 185, 120, 120, 60, 1).speed(3)
                .attackBonus(135, 180, 0)
                .defenceBonus(160, 165, 150, 30, 145)
                // OSRS Wiki: melee up to 23 (crush); six candies (magic) of about 10 each that can be stepped away from; its
                // stomp hurts 1-6 a tick while the player stands under it (was 30/28/25).
                .attack(AttackDef.of("chomp", AttackType.CRUSH, 23, 3.0, 5))
                .attack(AttackDef.of("candy_barrage", AttackType.MAGIC, 10, 12, 3).special(Special.SKY_STRIKE, 0)
                        .telegraph(3, "The Mimic flings a barrage of candies!").particle("minecraft:totem_of_undying"))
                .attack(AttackDef.of("lid_slam", AttackType.CRUSH, 6, 3, 2).special(Special.AOE, 1))
                .drops(mimicDrops().toArray(new Drop[0]))
                .key("mimic").at("watsons_house").arena(10, "stone") // the mimic casket from an elite or master casket
                .size(1.0f, 1.0f, 1.8f).moveSpeed(0.2);
    }
}
