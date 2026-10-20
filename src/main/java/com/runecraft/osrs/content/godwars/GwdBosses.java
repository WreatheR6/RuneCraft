package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.List;

/**
 * Kree'arra, Commander Zilyana and Nex (plus Nex's four generals). General Graardor and K'ril
 * Tsutsaroth are built-in bosses; this module only adds their models, kill count gate, hilts,
 * shards and pets (see the module hooks).
 *
 * <p>Attack lists are the source data; the module's boss scripts choose between them, add the
 * multi-target attacks and implement the special mechanics.
 */
public final class GwdBosses {
    private GwdBosses() {
    }

    // --- Kree'arra ---
    public static final AttackDef KREE_MELEE = AttackDef.of("talon swipe", AttackType.SLASH, 26, 3.5, 2);
    public static final AttackDef KREE_TORNADO = AttackDef.of("tornado", AttackType.RANGED, 69, 24, 6)
            .special(Special.KNOCKBACK, 3).particle("minecraft:cloud");
    public static final AttackDef KREE_WIND = AttackDef.of("wind blast", AttackType.MAGIC, 21, 24, 3).particle("minecraft:sweep_attack");

    // --- Commander Zilyana ---
    public static final AttackDef ZILY_MELEE = AttackDef.of("saradomin sword", AttackType.SLASH, 27, 3.0, 6);
    public static final AttackDef ZILY_MAGIC = AttackDef.of("saradomin strike", AttackType.MAGIC, 20, 24, 3)
            .particle("minecraft:end_rod").message("Commander Zilyana calls down the power of Saradomin!");

    // --- Nex ---
    public static final AttackDef NEX_MELEE = AttackDef.of("melee", AttackType.SLASH, 30, 3.0, 2);
    public static final AttackDef NEX_SMOKE = AttackDef.of("smoke bullet", AttackType.MAGIC, 33, 24, 6)
            .special(Special.POISON, 8).particle("minecraft:large_smoke");
    public static final AttackDef NEX_SHADOW = AttackDef.of("shadow shot", AttackType.RANGED, 30, 24, 6)
            .particle("minecraft:squid_ink");
    /** Heals Nex for 25% of the damage dealt (applied by the script, scaled to her 3400 hitpoints). */
    public static final AttackDef NEX_BLOOD = AttackDef.of("blood barrage", AttackType.MAGIC, 33, 24, 6)
            .particle("minecraft:damage_indicator");
    public static final AttackDef NEX_ICE = AttackDef.of("ice barrage", AttackType.MAGIC, 33, 24, 6)
            .special(Special.FREEZE, 8).particle("minecraft:snowflake");
    public static final AttackDef NEX_ZAROS = AttackDef.of("zarosian blast", AttackType.MAGIC, 33, 24, 6)
            .special(Special.DRAIN_PRAYER, 5).particle("minecraft:dragon_breath");

    /** Nex's generals in phase order (Smoke, Shadow, Blood, Ice). */
    public static final String[] NEX_MINIONS = {"fumus", "umbra", "cruor", "glacies"};

    /**
     * Extra drops for the built-in General Graardor and K'ril Tsutsaroth (rolled by the module hooks
     * on top of their built-in tables): hilt 1/508, each godsword shard 1/762.
     */
    public static List<Drop> builtinExtraDrops(String bossId) {
        String hilt = switch (bossId) {
            case "general_graardor" -> "bandos_hilt";
            case "kril_tsutsaroth" -> "zamorak_hilt";
            default -> null;
        };
        if (hilt == null) {
            return List.of();
        }
        return List.of(Drop.of(hilt, 1, 1, 508), Drop.of("godsword_shard_1", 1, 1, 762), Drop.of("godsword_shard_2", 1, 1, 762),
                Drop.of("godsword_shard_3", 1, 1, 762), Drop.of("clue_scroll_elite", 1, 1, 250));
    }

    /** Pets of the built-in generals (1/5000 in OSRS), or null. */
    public static String builtinPet(String bossId) {
        return switch (bossId) {
            case "general_graardor" -> "pet_general_graardor";
            case "kril_tsutsaroth" -> "pet_kril_tsutsaroth";
            default -> null;
        };
    }

    public static final int BUILTIN_PET_RATE = 5000;

    public static List<BossDef> all() {
        List<BossDef> list = new ArrayList<>();

        list.add(BossDef.of("kreearra", "Kree'arra").level(580).stats(255, 300, 200, 260, 200, 380).speed(3)
                .attackBonus(136, 0, 120)
                .defenceBonus(180, 180, 180, 200, 200)
                .attack(KREE_TORNADO).attack(KREE_WIND).attack(KREE_MELEE)
                .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH)
                .drops(Drop.always("big_bones"),
                        Drop.of("coins", 19362, 20073, 4), Drop.of("ranging_potion", 1, 1, 12), Drop.of("super_defence", 1, 1, 12),
                        Drop.of("black_dragonhide", 10, 15, 16), Drop.of("dwarf_weed_seed", 3, 3, 24), Drop.of("runite_ore", 3, 5, 32),
                        Drop.of("dragonstone_bolts_e", 5, 10, 16),
                        Drop.of("armadyl_helmet", 1, 1, 381), Drop.of("armadyl_chestplate", 1, 1, 381),
                        Drop.of("armadyl_chainskirt", 1, 1, 381), Drop.of("armadyl_hilt", 1, 1, 508),
                        Drop.of("godsword_shard_1", 1, 1, 762), Drop.of("godsword_shard_2", 1, 1, 762),
                        Drop.of("godsword_shard_3", 1, 1, 762), Drop.of("clue_scroll_elite", 1, 1, 250))
                .pet("pet_kreearra").minion("flight_kilisa", 1).flying()
                .arena(14, "stone").category("God Wars Dungeon")
                .size(1.4f, 2.4f, 1.8f));

        list.add(BossDef.of("commander_zilyana", "Commander Zilyana").level(596).stats(255, 280, 196, 300, 300, 250).speed(2)
                .attackBonus(195, 200, 0)
                .defenceBonus(100, 100, 100, 100, 100)
                .attack(ZILY_MELEE).attack(ZILY_MAGIC)
                .drops(Drop.always("bones"),
                        Drop.of("coins", 19362, 20073, 4), Drop.of("prayer_potion", 1, 2, 10), Drop.of("super_restore", 1, 2, 10),
                        Drop.of("magic_potion", 1, 1, 12), Drop.of("saradomin_brew", 1, 2, 12), Drop.of("ranarr_seed", 2, 2, 24),
                        Drop.of("diamond", 6, 6, 32),
                        Drop.of("saradomin_sword", 1, 1, 127), Drop.of("saradomins_light", 1, 1, 254),
                        Drop.of("armadyl_crossbow", 1, 1, 508), Drop.of("saradomin_hilt", 1, 1, 508),
                        Drop.of("godsword_shard_1", 1, 1, 762), Drop.of("godsword_shard_2", 1, 1, 762),
                        Drop.of("godsword_shard_3", 1, 1, 762), Drop.of("clue_scroll_elite", 1, 1, 250))
                .pet("pet_zilyana").minion("starlight", 1)
                .arena(12, "stone").category("God Wars Dungeon")
                .moveSpeed(0.32).size(0.7f, 2.0f, 1.5f));

        // Nex: 3400 hitpoints (the script scales damage because Minecraft caps max health at 1024).
        list.add(BossDef.of("nex", "Nex").level(1001).stats(3400, 315, 200, 260, 230, 350).speed(4)
                .attackBonus(200, 100, 150)
                .defenceBonus(40, 140, 60, 300, 190) // approx.
                .attack(NEX_SMOKE).attack(NEX_MELEE)
                .phase(80, "Fumus, don't fail me!", 1.0, false, NEX_SHADOW, NEX_MELEE)
                .phase(60, "Umbra, don't fail me!", 1.0, false, NEX_BLOOD, NEX_MELEE)
                .phase(40, "Cruor, don't fail me!", 1.0, false, NEX_ICE, NEX_MELEE)
                .phase(20, "Glacies, don't fail me!", 1.0, false, NEX_ZAROS, NEX_MELEE)
                .drops(Drop.of("nihil_shard", 85, 105, 2),
                        Drop.of("coins", 25000, 50000, 4), Drop.of("blood_rune", 300, 600, 10), Drop.of("death_rune", 300, 600, 10),
                        Drop.of("soul_rune", 100, 300, 12), Drop.of("runite_ore", 20, 30, 16), Drop.of("dragon_bones", 30, 30, 16),
                        Drop.of("saradomin_brew", 10, 10, 16), Drop.of("super_restore", 10, 10, 16), Drop.of("magic_logs", 150, 150, 20),
                        Drop.of("torva_full_helm", 1, 1, 258), Drop.of("torva_platebody", 1, 1, 258),
                        Drop.of("torva_platelegs", 1, 1, 258), Drop.of("nihil_horn", 1, 1, 258),
                        Drop.of("zaryte_vambraces", 1, 1, 172), Drop.of("ancient_hilt", 1, 1, 516),
                        Drop.of("clue_scroll_elite", 1, 1, 48))
                .pet("nexling").minion("blood_reaver", 2)
                .arena(16, "ice").category("God Wars Dungeon")
                .flying().size(0.9f, 2.2f, 1.7f));

        String[] names = {"Fumus", "Umbra", "Cruor", "Glacies"};
        AttackDef[] attacks = {
                AttackDef.of("smoke", AttackType.MAGIC, 18, 20, 10).special(Special.POISON, 6).particle("minecraft:large_smoke"),
                AttackDef.of("shadow", AttackType.RANGED, 18, 20, 10).particle("minecraft:squid_ink"),
                AttackDef.of("blood", AttackType.MAGIC, 18, 20, 10).special(Special.LIFESTEAL, 25).particle("minecraft:damage_indicator"),
                AttackDef.of("ice", AttackType.MAGIC, 18, 20, 10).special(Special.FREEZE, 4).particle("minecraft:snowflake")};
        for (int i = 0; i < 4; i++) {
            list.add(BossDef.of(NEX_MINIONS[i], names[i]).level(285).stats(200, 1, 1, 150, 200, 200).speed(5)
                    .defenceBonus(100, 100, 100, 100, 100)
                    .attack(attacks[i]) // OSRS: no bones; only the 1/200 ancient ceremonial robes (BossDropsTable)
                    .raidOnly().category("God Wars Dungeon").moveSpeed(0.0).flying().size(0.7f, 1.9f, 1.25f));
        }
        return list;
    }
}
