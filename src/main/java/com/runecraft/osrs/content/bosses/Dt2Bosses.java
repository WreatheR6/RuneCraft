package com.runecraft.osrs.content.bosses;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Desert Treasure II bosses: Duke Sucellus, The Leviathan, The Whisperer and Vardorvis. Each drops
 * chromium ingots, its vestige (for the improved Fremennik rings), a soulreaper axe piece and virtus.
 */
public final class Dt2Bosses {
    private Dt2Bosses() {
    }

    // ---- Duke Sucellus ----
    public static final AttackDef DUKE_SLAM = AttackDef.of("fist_slam", AttackType.CRUSH, 46, 5, 3);
    public static final AttackDef DUKE_MAGIC = AttackDef.of("magic_blast", AttackType.MAGIC, 48, 16, 4).particle("minecraft:witch");
    /** The freezing gaze hits everyone the Duke can see; hide behind a pillar. */
    public static final AttackDef DUKE_GAZE = AttackDef.of("freezing_gaze", AttackType.MAGIC, 101, 16, 1)
            .special(Special.PRAYER_PIERCING, 0).telegraph(5, "Duke Sucellus' eyes begin to glow... break his line of sight!")
            .particle("minecraft:snowflake");
    public static final AttackDef DUKE_VENTS = AttackDef.of("gas_vent", AttackType.MAGIC, 12, 16, 2).special(Special.HAZARD, 5)
            .particle("minecraft:sneeze").message("Poisonous gas erupts from the vents!");

    // ---- The Leviathan ----
    public static final AttackDef LEVI_RANGED = AttackDef.of("boulder", AttackType.RANGED, 24, 16, 1).particle("minecraft:crit");
    public static final AttackDef LEVI_MAGIC = AttackDef.of("abyssal_orb", AttackType.MAGIC, 32, 16, 1).particle("minecraft:witch");
    public static final AttackDef LEVI_MELEE = AttackDef.of("shadow_strike", AttackType.CRUSH, 50, 16, 1).particle("minecraft:squid_ink");
    public static final AttackDef LEVI_DEBRIS = AttackDef.of("falling_debris", AttackType.CRUSH, 45, 16, 1).special(Special.SKY_STRIKE, 0)
            .telegraph(3, "Debris shakes loose from the cavern roof!").particle("minecraft:ash");
    public static final AttackDef LEVI_LIGHTNING = AttackDef.of("lightning", AttackType.MAGIC, 35, 16, 1).special(Special.SKY_STRIKE, 0)
            .telegraph(3, "The Leviathan's shell crackles with lightning!").particle("minecraft:electric_spark");

    // ---- The Whisperer ----
    public static final AttackDef WHISPER_MAGIC = AttackDef.of("soul_bolt", AttackType.MAGIC, 36, 16, 3).particle("minecraft:soul");
    public static final AttackDef WHISPER_RANGED = AttackDef.of("tentacle_lash", AttackType.RANGED, 40, 16, 3).particle("minecraft:squid_ink");
    public static final AttackDef WHISPER_SCREECH = AttackDef.of("screech", AttackType.MAGIC, 45, 16, 1).special(Special.AOE, 6)
            .telegraph(5, "The Whisperer lets out a piercing screech... get away!").particle("minecraft:sonic_boom");
    public static final AttackDef WHISPER_SIPHON = AttackDef.of("soul_siphon", AttackType.MAGIC, 50, 16, 1).special(Special.LIFESTEAL, 100)
            .particle("minecraft:sculk_soul").message("The Whisperer siphons your soul!");

    // ---- Vardorvis ----
    public static final AttackDef VARD_SLASH = AttackDef.of("axe_swing", AttackType.SLASH, 35, 4, 6);
    /** Swinging axes: up to 35 (OSRS Wiki; awakened 96), halved by Protect from Melee, and a bleed. */
    public static final AttackDef VARD_AXES = AttackDef.of("swinging_axes", AttackType.SLASH, 35, 16, 2).special(Special.SKY_STRIKE, 0)
            .telegraph(3, "Axes swing out from the walls!").particle("minecraft:sweep_attack");
    /**
     * Darting spikes: tendrils burst from the cracks he leaves for up to 25 (awakened 40), healing him for half. (The
     * head's gaze deals no damage: an unblocked gaze drains 10 Prayer and disables overhead prayers; it's scripted.)
     */
    public static final AttackDef VARD_SPIKES = AttackDef.of("darting_spikes", AttackType.SLASH, 25, 16, 1).special(Special.SKY_STRIKE, 0)
            .telegraph(3, "Vardorvis darts around you, leaving cracks in the ground!").particle("minecraft:damage_indicator");

    /**
     * The weaker versions fought during Desert Treasure II (quest module ids {@code dt2_quest_*}) reuse these bosses'
     * scripts with every max hit scaled by quest/post-quest melee max hit (osrs-dps-calc "Quest" vs "Post-quest"):
     * Duke 36/56, Leviathan 34/50, Whisperer 30/42, Vardorvis 37/43.
     */
    public static final double QUEST_DUKE_SCALE = 36.0 / 56;
    public static final double QUEST_LEVIATHAN_SCALE = 34.0 / 50;
    public static final double QUEST_WHISPERER_SCALE = 30.0 / 42;
    public static final double QUEST_VARDORVIS_SCALE = 37.0 / 43;

    public static final int TABLET_RATE = 25;
    public static final int QUARTZ_RATE = 200;
    /** Each boss's tablet (a ring of shadows teleport unlock) and quartz (for its elemental ancient sceptre). */
    public static final List<String> TABLETS = List.of("frozen_tablet", "scarred_tablet", "sirenic_tablet", "strangled_tablet");
    public static final List<String> QUARTZES = List.of("ice_quartz", "smoke_quartz", "shadow_quartz", "blood_quartz");

    /**
     * The Ancient Vault's chest (OSRS Wiki "Chest (Ancient Vault)"): using a quartz on it (consumed) gives one of five
     * rewards at 1/5 each, {item, quantity, noted (1) or not (0)}, by quartz (same order as {@link #QUARTZES}).
     */
    public static final List<List<Object[]>> VAULT_CHEST = List.of(
            List.of(new Object[]{"mist_rune", 500, 0}, new Object[]{"raw_sea_turtle", 100, 1}, new Object[]{"dragon_arrowtips", 100, 0},
                    new Object[]{"dragon_platelegs", 1, 0}, new Object[]{"runite_ore", 20, 1}),
            List.of(new Object[]{"smoke_rune", 500, 0}, new Object[]{"raw_manta_ray", 100, 1}, new Object[]{"dragon_bolts_unf", 100, 0},
                    new Object[]{"gold_ore", 400, 1}, new Object[]{"dragon_javelin_heads", 100, 0}),
            List.of(new Object[]{"raw_monkfish", 100, 1}, new Object[]{"soul_rune", 500, 0}, new Object[]{"dragon_plateskirt", 1, 0},
                    new Object[]{"steam_rune", 500, 0}, new Object[]{"runite_ore", 20, 1}),
            List.of(new Object[]{"dragon_platelegs", 1, 0}, new Object[]{"dragon_dart_tip", 100, 0}, new Object[]{"blood_rune", 500, 0},
                    new Object[]{"tuna_potato", 100, 1}, new Object[]{"runite_ore", 20, 1}));

    /** The supply table is rolled at 1/5 once the unique table, orb, tablet and quartz rolls have failed (OSRS Wiki). */
    public static final int SUPPLY_RATE = 5;

    /**
     * Supplies (OSRS Wiki drop tables, "These supplies are all dropped together"): {@link Dt2Loot} drops the whole set on
     * a successful supply roll; each line's rarity here is that roll's 1/5.
     */
    public static List<Drop> supplies(String bossId) {
        int r = SUPPLY_RATE;
        return switch (bossId) {
            case "duke_sucellus" -> List.of(Drop.of("pineapple_pizza", 3, 4, r), Drop.of(Drop.dosed("prayer_potion", 3), 1, 1, r),
                    Drop.of(Drop.dosed("super_combat_potion", 2), 1, 1, r));
            case "the_leviathan" -> List.of(Drop.of(Drop.dosed("prayer_potion", 3), 1, 1, r),
                    Drop.of(Drop.dosed("ranging_potion", 2), 1, 1, r), Drop.of("sea_turtle", 3, 4, r));
            case "the_whisperer" -> List.of(Drop.of(Drop.dosed("ancient_brew", 2), 1, 1, r),
                    Drop.of(Drop.dosed("prayer_potion", 3), 1, 1, r), Drop.of("manta_ray", 3, 4, r));
            case "vardorvis" -> List.of(Drop.of("tuna_potato", 3, 4, r), Drop.of(Drop.dosed("prayer_potion", 3), 1, 1, r),
                    Drop.of(Drop.dosed("super_combat_potion", 2), 1, 1, r));
            default -> throw new IllegalArgumentException(bossId);
        };
    }

    /**
     * A perfect kill (no avoidable damage) gives 50% more loot from the standard drop table (OSRS Wiki). The wiki lists
     * those quantities as "normal,perfect" (e.g. Vardorvis' soul runes "400,600"): the tables drop the normal amount and
     * this map holds the perfect one, boss -> item -> {normal, perfect}.
     */
    private static final Map<String, Map<String, int[]>> PERFECT = new HashMap<>();

    /**
     * What counts as avoidable damage for a perfect kill (OSRS Wiki, each boss's Drops/Mechanics list), in the Combat
     * Achievements rule language: the Duke's slam and magic (even through prayer), gaze, vents and icefall; the
     * Leviathan's volleys, debris and lightning; the Whisperer's ranged/magic, screech, siphon and tentacles; Vardorvis'
     * unprotected melee and all of his specials. Unnamed scripted damage ("mechanic") and hazards always count.
     */
    public static final Map<String, String> PERFECT_RULE = Map.of(
            "duke_sucellus", "nodmg=mechanic,hazard,gas_vent,freezing_gaze,fist_slam,magic_blast,offprayer",
            "the_leviathan", "nodmg=mechanic,hazard,falling_debris,lightning,offprayer",
            "the_whisperer", "nodmg=mechanic,hazard,soul_bolt,tentacle_lash,screech,soul_siphon,offprayer",
            "vardorvis", "nodmg=mechanic,hazard,swinging_axe,darting_spikes,strangle,offprayer");

    static {
        // duke_sucellus
        perfect("duke_sucellus", "bronze_chainbody", 11, 17);
        perfect("duke_sucellus", "mithril_chainbody", 5, 7);
        perfect("duke_sucellus", "adamant_chainbody", 6, 10);
        perfect("duke_sucellus", "dragon_platelegs", 5, 7);
        perfect("duke_sucellus", "pure_essence", 200, 300);
        perfect("duke_sucellus", "iron_ore", 63, 95);
        perfect("duke_sucellus", "coal", 216, 325);
        perfect("duke_sucellus", "mithril_ore", 33, 50);
        perfect("duke_sucellus", "adamantite_ore", 75, 112);
        perfect("duke_sucellus", "runite_ore", 30, 45);
        perfect("duke_sucellus", "sapphire", 28, 42);
        perfect("duke_sucellus", "emerald", 28, 42);
        perfect("duke_sucellus", "ruby", 28, 42);
        perfect("duke_sucellus", "uncut_ruby", 41, 62);
        perfect("duke_sucellus", "uncut_diamond", 41, 62);
        perfect("duke_sucellus", "bronze_bar", 63, 95);
        perfect("duke_sucellus", "dragon_arrowtips", 166, 250);
        perfect("duke_sucellus", "rune_javelin_heads", 60, 90);
        perfect("duke_sucellus", "dragon_javelin_heads", 60, 90);
        perfect("duke_sucellus", "raw_sea_turtle", 200, 300);
        perfect("duke_sucellus", "air_rune", 200, 300);
        perfect("duke_sucellus", "mist_rune", 100, 150);
        perfect("duke_sucellus", "chaos_rune", 100, 150);
        perfect("duke_sucellus", "soul_rune", 666, 1000);
        // the_leviathan
        perfect("the_leviathan", "iron_ore", 57, 85);
        perfect("the_leviathan", "silver_ore", 57, 85);
        perfect("the_leviathan", "coal", 195, 292);
        perfect("the_leviathan", "gold_ore", 67, 101);
        perfect("the_leviathan", "adamantite_ore", 57, 85);
        perfect("the_leviathan", "runite_ore", 27, 40);
        perfect("the_leviathan", "sapphire", 25, 38);
        perfect("the_leviathan", "emerald", 25, 38);
        perfect("the_leviathan", "ruby", 25, 38);
        perfect("the_leviathan", "uncut_ruby", 37, 56);
        perfect("the_leviathan", "uncut_diamond", 37, 56);
        perfect("the_leviathan", "dragon_javelin_heads", 54, 81);
        perfect("the_leviathan", "dragon_bolts_unf", 150, 225);
        perfect("the_leviathan", "onyx_bolt_tips", 90, 135);
        perfect("the_leviathan", "raw_manta_ray", 180, 270);
        perfect("the_leviathan", "anglerfish", 4, 6);
        perfect("the_leviathan", "bronze_arrow", 63, 94);
        perfect("the_leviathan", "mithril_arrow", 63, 94);
        perfect("the_leviathan", "adamant_arrow", 63, 94);
        perfect("the_leviathan", "rune_arrow", 54, 81);
        perfect("the_leviathan", "body_rune", 180, 270);
        perfect("the_leviathan", "earth_rune", 180, 270);
        perfect("the_leviathan", "smoke_rune", 300, 450);
        perfect("the_leviathan", "soul_rune", 600, 900);
        // the_whisperer
        perfect("the_whisperer", "bronze_longsword", 16, 24);
        perfect("the_whisperer", "pure_essence", 280, 420); // the wiki writes "280; 420"
        perfect("the_whisperer", "mithril_longsword", 7, 10);
        perfect("the_whisperer", "adamant_longsword", 9, 14);
        perfect("the_whisperer", "battlestaff", 70, 105);
        perfect("the_whisperer", "dragon_plateskirt", 7, 10);
        perfect("the_whisperer", "iron_ore", 88, 133);
        perfect("the_whisperer", "coal", 303, 455);
        perfect("the_whisperer", "gold_ore", 88, 133);
        perfect("the_whisperer", "mithril_ore", 88, 133);
        perfect("the_whisperer", "adamantite_ore", 105, 157);
        perfect("the_whisperer", "runite_ore", 42, 63);
        perfect("the_whisperer", "sapphire", 39, 59);
        perfect("the_whisperer", "emerald", 39, 59);
        perfect("the_whisperer", "ruby", 39, 59);
        perfect("the_whisperer", "uncut_ruby", 58, 87);
        perfect("the_whisperer", "uncut_diamond", 58, 87);
        perfect("the_whisperer", "dragon_javelin_heads", 84, 126);
        perfect("the_whisperer", "rune_bolts_unf", 84, 126);
        perfect("the_whisperer", "raw_monkfish", 700, 1050);
        perfect("the_whisperer", "water_rune", 280, 420);
        perfect("the_whisperer", "steam_rune", 466, 700);
        perfect("the_whisperer", "chaos_rune", 140, 210);
        perfect("the_whisperer", "death_rune", 466, 700);
        perfect("the_whisperer", "soul_rune", 933, 1400);
        // vardorvis
        perfect("vardorvis", "pure_essence", 120, 180);
        perfect("vardorvis", "iron_ore", 38, 57);
        perfect("vardorvis", "silver_ore", 38, 57);
        perfect("vardorvis", "coal", 130, 195);
        perfect("vardorvis", "mithril_ore", 38, 57);
        perfect("vardorvis", "adamantite_ore", 45, 67);
        perfect("vardorvis", "runite_ore", 18, 27);
        perfect("vardorvis", "sapphire", 17, 25);
        perfect("vardorvis", "emerald", 17, 25);
        perfect("vardorvis", "ruby", 17, 25);
        perfect("vardorvis", "uncut_ruby", 25, 37);
        perfect("vardorvis", "uncut_diamond", 25, 37);
        perfect("vardorvis", "rune_javelin_heads", 36, 54);
        perfect("vardorvis", "dragon_javelin_heads", 36, 54);
        perfect("vardorvis", "dragon_dart_tip", 100, 150);
        perfect("vardorvis", "raw_shark", 200, 300);
        perfect("vardorvis", "bronze_javelin", 42, 63);
        perfect("vardorvis", "mithril_javelin", 42, 63);
        perfect("vardorvis", "adamant_javelin", 42, 63);
        perfect("vardorvis", "onyx_bolts_e", 60, 90);
        perfect("vardorvis", "mind_rune", 120, 180);
        perfect("vardorvis", "fire_rune", 120, 180);
        perfect("vardorvis", "lava_rune", 200, 300);
        perfect("vardorvis", "blood_rune", 200, 300);
        perfect("vardorvis", "soul_rune", 400, 600);
    }

    private static void perfect(String boss, String item, int normal, int perfect) {
        PERFECT.computeIfAbsent(boss, b -> new HashMap<>()).put(item, new int[]{normal, perfect});
    }

    /**
     * The perfect-kill quantity of a standard-table drop of {@code count} (the normal quantity), or {@code count} when the
     * item isn't on the boss's standard table.
     */
    public static int perfectQuantity(String bossId, String item, int count) {
        int[] q = PERFECT.getOrDefault(bossId, Map.of()).get(item);
        return q != null && q[0] == count ? q[1] : count;
    }

    /** The standard-table items with a perfect-kill quantity, item -> {normal, perfect}. */
    public static Map<String, int[]> perfectQuantities(String bossId) {
        return java.util.Collections.unmodifiableMap(PERFECT.getOrDefault(bossId, Map.of()));
    }

    public static List<BossDef> all() {
        return List.of(duke(), leviathan(), whisperer(), vardorvis());
    }

    /**
     * OSRS (Desert Treasure II - Tweaks and Drop Rates): a 1/n roll for the tradeable unique table, which gives a vestige
     * roll 3/8 (three rolls make the vestige), a chromium ingot 3/8, the boss's axe piece/weapon 1/8 and a virtus piece
     * 1/8 (each of the three equally); the awakener's orb is rolled next. Awakened kills roll the unique table at 3/n
     * ({@link AwakenedBosses#drops}). These lines describe the table (drop viewer, collection log, snapshot tests; the
     * standard lines come from {@code BossDropsTable}); a kill is rolled in the wiki's order by {@link Dt2Loot}.
     */
    static List<Drop> dt2Drops(String bossId, String vestige, String axePiece, String tablet, String quartz) {
        int n = AwakenedBosses.uniqueTableRate(bossId);
        List<Drop> d = new ArrayList<>(List.of(Drop.of("chromium_ingot", 1, 1, Math.round(8 * n / 3f)), Drop.of(vestige, 1, 1, 8 * n),
                Drop.of(axePiece, 1, 1, 8 * n), Drop.of("virtus_mask", 1, 1, 24 * n), Drop.of("virtus_robe_top", 1, 1, 24 * n),
                Drop.of("virtus_robe_bottom", 1, 1, 24 * n), Drop.of(AwakenedBosses.ORB, 1, 1, AwakenedBosses.orbRate(bossId)),
                Drop.of("runite_bar", 4, 8, 8), // approx: the standard table below is a simplification
                Drop.of("death_rune", 200, 300, 8), Drop.of("soul_rune", 150, 250, 8), Drop.of("dragon_platelegs", 1, 1, 16),
                Drop.of("coins", 30000, 90000, 4)));
        for (String clue : Dt2Loot.CLUES) {
            d.add(Drop.of(clue, 1, 1, Dt2Loot.CLUE_RATE * Dt2Loot.CLUES.size())); // 1/40 for a clue, each tier alike
        }
        // Wiki: the boss's tablet (unlocks the ring of shadows' teleport to the lair) 1/25 and its quartz 1/200, both rolled after the unique
        // table (shown as ~1/25.8 and ~1/206). Before the first quartz OSRS improves its rate from 1/200 to 1/50 over the
        // first 300 kills; that ramp isn't modelled. // approx
        d.add(Drop.of(tablet, 1, 1, TABLET_RATE));
        d.add(Drop.of(quartz, 1, 1, QUARTZ_RATE));
        d.addAll(supplies(bossId));
        return d;
    }

    // Levels, skills and defence bonuses below are the osrs-dps-calc values that MonsterStatsTable applies anyway (kept
    // equal by Dt2BossStatsTest; they were stale literals before round 30).
    static BossDef duke() {
        BossDef b = BossDef.of("duke_sucellus", "Duke Sucellus").level(758).stats(485, 300, 345, 275, 310, 1).speed(5)
                .defenceBonus(255, 45, 190, 440, 320)
                .attack(DUKE_SLAM).attack(DUKE_MAGIC).attack(DUKE_GAZE).attack(DUKE_VENTS)
                .pet("pet_baron").at("ghorrock_prison").arena(13, "ice").size(1.8f, 2.4f, 2.0f).moveSpeed(0.0);
        b.drops.addAll(dt2Drops(b.id, "magus_vestige", "eye_of_the_duke", "frozen_tablet", "ice_quartz"));
        return b;
    }

    static BossDef leviathan() {
        BossDef b = BossDef.of("the_leviathan", "The Leviathan").level(798).stats(900, 300, 360, 250, 160, 160).speed(4)
                .defenceBonus(260, 190, 230, 280, 50)
                .attack(LEVI_RANGED).attack(LEVI_MAGIC).attack(LEVI_MELEE).attack(LEVI_DEBRIS)
                .pet("pet_lilviathan").at("the_scar").arena(14, "water").size(2.2f, 2.2f, 2.0f).moveSpeed(0.0);
        b.drops.addAll(dt2Drops(b.id, "venator_vestige", "leviathans_lure", "scarred_tablet", "smoke_quartz"));
        return b;
    }

    static BossDef whisperer() {
        BossDef b = BossDef.of("the_whisperer", "The Whisperer").level(791).stats(900, 280, 280, 250, 180, 180).speed(5)
                .defenceBonus(180, 300, 220, 10, 300)
                .attack(WHISPER_MAGIC).attack(WHISPER_RANGED).attack(WHISPER_SCREECH).attack(WHISPER_SIPHON)
                .phase(50, "The Whisperer drags you into the Shadow Realm!", 1.1, false)
                .pet("pet_wisp").at("lassar_undercity").arena(13, "void").flying().size(1.8f, 2.6f, 2.0f).moveSpeed(0.18);
        b.drops.addAll(dt2Drops(b.id, "bellator_vestige", "sirens_staff", "sirenic_tablet", "shadow_quartz"));
        return b;
    }

    static BossDef vardorvis() {
        BossDef b = BossDef.of("vardorvis", "Vardorvis").level(784).stats(700, 280, 270, 215, 215, 1).speed(5)
                .defenceBonus(215, 65, 85, 580, 580)
                .attack(VARD_SLASH).attack(VARD_AXES).attack(VARD_SPIKES)
                .pet("pet_butch").at("stranglewood").arena(12, "crypt").size(0.9f, 2.4f, 1.6f).moveSpeed(0.28);
        b.drops.addAll(dt2Drops(b.id, "ultor_vestige", "executioners_axe_head", "strangled_tablet", "blood_quartz"));
        return b;
    }
}
