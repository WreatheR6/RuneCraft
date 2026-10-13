package com.runecraft.osrs.content.systems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Special attacks keyed by the wielded item id. Values (energy cost, accuracy and damage multipliers)
 * are from the OSRS wiki. Items that do not exist in the catalog simply never match.
 */
public final class SpecialAttacks {
    /** Which combat pipeline triggers the special. INSTANT specials fire when the special bar is clicked. */
    public enum Style { MELEE, RANGED, MAGIC, INSTANT }

    /** What happens besides the accuracy/damage multipliers. */
    public enum Effect {
        NONE,
        /** A second, independently rolled hit (dragon dagger, abyssal dagger, crystal halberd...). */
        DOUBLE_HIT,
        /** Dragon claws: four hits with the classic damage cascade. */
        CLAWS,
        /** Reduces the target's Defence by {@code param}% of its level (dragon warhammer 30, elder maul 35). */
        DRAIN_DEFENCE_PERCENT,
        /** Bandos godsword: drains Defence by the damage dealt (then Strength, Attack, Magic, Ranged). */
        DRAIN_BY_DAMAGE,
        /** Saradomin godsword: heals 50% of damage (min 10) and restores 25% as prayer (min 5). */
        HEAL_AND_PRAY,
        /** Freezes the target for {@code param} game ticks (Zamorak godsword, abyssal tentacle bind). */
        FREEZE,
        /** Heals the attacker by {@code param}% of the damage dealt (toxic blowpipe, Blood Sacrifice). */
        HEAL_PERCENT,
        /** Dragon battleaxe: drains 10% Attack/Defence/Magic/Ranged and boosts Strength by 10 + a quarter of the drain. */
        RAMPAGE,
        /** Skilling specials (dragon axe/pickaxe/harpoon): +3 levels to the skill named in {@code skill}. */
        SKILL_BOOST,
        /** Abyssal bludgeon: +0.5% damage per missing prayer point. */
        PENANCE,
        /** Ranged double shot (dark bow, magic shortbow, dragon knives): two projectiles. */
        DOUBLE_SHOT,
        /** Dark bow: two arrows each with a minimum damage of {@code param}. */
        DARK_BOW,
        /** Guaranteed hit (magic longbow Powershot, voidwaker): accuracy roll always succeeds. */
        GUARANTEED,
        /** Voidwaker: guaranteed hit between 50% and 150% of max hit. */
        VOIDWAKER,
        /** Granite maul: an immediate extra hit on the current target. */
        QUICK_SMASH,
        /** Staff of the dead: halves melee damage taken for one minute. */
        POWER_OF_DEATH,
        /** Saradomin sword: an extra magic hit of 1-16. */
        LIGHTNING,
        /** Drains the target's Attack, Strength and Defence by {@code param}% (arclight/darklight). */
        WEAKEN,
        /** Hits every enemy next to the target (dragon 2h sword, dragon halberd sweep). */
        AREA,
        /** Pushes and stuns the target for {@code param} ticks (dragon spear, zamorakian spear). */
        SHOVE,
        /** Magic special that fires a spell of max hit {@code param} at the target (nightmare staffs). */
        MAGIC_BLAST,
        /** Restores the attacker's prayer by {@code param}% of the damage (eldritch nightmare staff). */
        BLAST_PRAYER
    }

    public record Spec(String itemId, String name, int cost, Style style, double accuracy, double damage, Effect effect,
                       int param, String skill, String description) {
    }

    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();

    static {
        // ---------------- Melee ----------------
        melee("dragon_dagger", "Puncture", 25, 1.15, 1.15, Effect.DOUBLE_HIT, 0, "Two quick slashes with increased accuracy and damage.");
        alias("dragon_dagger_p", "dragon_dagger");
        alias("dragon_dagger_pp", "dragon_dagger");
        alias("dragon_dagger_ppp", "dragon_dagger");
        melee("dragon_scimitar", "Sever", 55, 1.25, 1.0, Effect.NONE, 0, "A slash with increased accuracy that disables protection prayers.");
        melee("dragon_longsword", "Cleave", 25, 1.0, 1.25, Effect.NONE, 0, "A powerful cleave dealing 25% more damage.");
        melee("dragon_mace", "Shatter", 25, 1.25, 1.5, Effect.NONE, 0, "A crushing blow with +25% accuracy and +50% damage.");
        melee("dragon_sword", "Wild Stab", 40, 1.25, 1.25, Effect.NONE, 0, "A stab with +25% accuracy and damage.");
        melee("dragon_warhammer", "Smash", 50, 1.0, 1.5, Effect.DRAIN_DEFENCE_PERCENT, 30, "+50% damage; lowers the target's Defence by 30% on a hit.");
        instant("dragon_battleaxe", "Rampage", 100, Effect.RAMPAGE, 0, null, "Drains Attack, Defence, Magic and Ranged by 10% to boost Strength.");
        melee("dragon_halberd", "Sweep", 30, 1.0, 1.1, Effect.DOUBLE_HIT, 0, "Two hits with +10% damage against large targets.");
        melee("dragon_2h_sword", "Powerstab", 60, 1.0, 1.0, Effect.AREA, 0, "Hits all enemies around you.");
        melee("dragon_spear", "Shove", 25, 1.0, 0.0, Effect.SHOVE, 5, "Pushes your opponent back and stuns them for 3 seconds.");
        alias("dragon_spear_p", "dragon_spear");
        melee("zamorakian_spear", "Shove", 25, 1.0, 0.0, Effect.SHOVE, 5, "Pushes your opponent back and stuns them for 3 seconds.");
        melee("zamorakian_hasta", "Shove", 25, 1.0, 0.0, Effect.SHOVE, 5, "Pushes your opponent back and stuns them for 3 seconds.");
        melee("dragon_claws", "Slice and Dice", 50, 1.0, 1.0, Effect.CLAWS, 0, "Four rapid slashes in a damage cascade.");
        instant("granite_maul", "Quick Smash", 60, Effect.QUICK_SMASH, 0, null, "An immediate extra hit on your target.");
        instant("granite_maul_or", "Quick Smash", 50, Effect.QUICK_SMASH, 0, null, "An immediate extra hit on your target.");
        melee("abyssal_whip", "Energy Drain", 50, 1.25, 1.0, Effect.NONE, 0, "+25% accuracy; transfers 10% of the target's run energy to you.");
        alias("volcanic_abyssal_whip", "abyssal_whip");
        alias("frozen_abyssal_whip", "abyssal_whip");
        melee("abyssal_tentacle", "Binding Tentacle", 50, 1.0, 1.0, Effect.FREEZE, 8, "Binds the target for 5 seconds (and may poison).");
        melee("abyssal_dagger", "Abyssal Puncture", 25, 1.25, 0.85, Effect.DOUBLE_HIT, 0, "Two hits with +25% accuracy and 15% less damage.");
        alias("abyssal_dagger_p", "abyssal_dagger");
        melee("abyssal_bludgeon", "Penance", 50, 1.0, 1.0, Effect.PENANCE, 0, "+0.5% damage for every prayer point you are missing.");
        melee("armadyl_godsword", "The Judgement", 50, 2.0, 1.375, Effect.NONE, 0, "Double accuracy and +37.5% damage.");
        alias("armadyl_godsword_or", "armadyl_godsword");
        melee("bandos_godsword", "Warstrike", 50, 2.0, 1.21, Effect.DRAIN_BY_DAMAGE, 0, "Drains the target's Defence (then other stats) by the damage dealt.");
        alias("bandos_godsword_or", "bandos_godsword");
        melee("saradomin_godsword", "Healing Blade", 50, 2.0, 1.1, Effect.HEAL_AND_PRAY, 0, "Heals 50% of the damage dealt and restores 25% as prayer.");
        alias("saradomin_godsword_or", "saradomin_godsword");
        melee("zamorak_godsword", "Ice Cleave", 50, 2.0, 1.1, Effect.FREEZE, 32, "Freezes the target for 32 ticks (19.2 seconds).");
        alias("zamorak_godsword_or", "zamorak_godsword");
        melee("ancient_godsword", "Blood Sacrifice", 50, 2.0, 1.1, Effect.HEAL_PERCENT, 25, "Marks the target; heals you for part of the damage.");
        melee("saradomin_sword", "Saradomin's Lightning", 100, 1.0, 1.1, Effect.LIGHTNING, 16, "+10% damage plus a magic hit of up to 16.");
        melee("saradomins_blessed_sword", "Saradomin's Lightning", 65, 1.0, 1.25, Effect.NONE, 0, "+25% damage, dealt as magic.");
        melee("elder_maul", "Hammer Blow", 50, 1.25, 1.0, Effect.DRAIN_DEFENCE_PERCENT, 35, "+25% accuracy; lowers Defence by 35% on a hit.");
        melee("crystal_halberd", "Sweep", 30, 1.0, 1.1, Effect.DOUBLE_HIT, 0, "Two hits with +10% damage.");
        melee("voidwaker", "Disrupt", 50, 1.0, 1.0, Effect.VOIDWAKER, 0, "A guaranteed magic hit of 50-150% of your max hit.");
        melee("arclight", "Weaken", 50, 1.0, 1.0, Effect.WEAKEN, 5, "Drains Attack, Strength and Defence by 5% (10% vs demons).");
        melee("darklight", "Weaken", 50, 1.0, 1.0, Effect.WEAKEN, 5, "Drains Attack, Strength and Defence by 5% (10% vs demons).");
        melee("emberlight", "Weaken", 50, 1.0, 1.0, Effect.WEAKEN, 5, "Drains Attack, Strength and Defence by 5% (10% vs demons).");
        melee("barrelchest_anchor", "Sunder", 50, 2.0, 1.1, Effect.DRAIN_BY_DAMAGE, 0, "Double accuracy; drains a stat by the damage dealt.");
        melee("rune_claws", "Impale", 25, 1.1, 1.1, Effect.NONE, 0, "+10% accuracy and damage.");
        melee("brine_sabre", "Liquify", 75, 2.0, 1.0, Effect.NONE, 0, "Double accuracy (underwater).");
        melee("osmumtens_fang", "Eviscerate", 25, 1.5, 1.0, Effect.NONE, 0, "+50% accuracy with a narrowed damage range.");
        melee("dragon_scimitar_or", "Sever", 55, 1.25, 1.0, Effect.NONE, 0, "A slash with increased accuracy that disables protection prayers.");
        melee("dual_macuahuitl", "Blood Moon", 25, 1.0, 1.0, Effect.DOUBLE_HIT, 0, "Two rapid hits.");
        melee("burning_claws", "Incinerate", 30, 1.0, 1.0, Effect.CLAWS, 0, "Three fiery slashes.");
        melee("soulreaper_axe", "Reap", 0, 1.0, 1.0, Effect.NONE, 0, "Consumes soul stacks.");
        instant("staff_of_the_dead", "Power of Death", 100, Effect.POWER_OF_DEATH, 100, null, "Halves melee damage you take for one minute.");
        instant("toxic_staff_of_the_dead", "Power of Death", 100, Effect.POWER_OF_DEATH, 100, null, "Halves melee damage you take for one minute.");
        instant("staff_of_light", "Power of Death", 100, Effect.POWER_OF_DEATH, 100, null, "Halves melee damage you take for one minute.");
        instant("staff_of_balance", "Power of Death", 100, Effect.POWER_OF_DEATH, 100, null, "Halves melee damage you take for one minute.");
        // ---------------- Skilling ----------------
        instant("dragon_axe", "Lumber Up", 100, Effect.SKILL_BOOST, 3, "WOODCUTTING", "Boosts your Woodcutting level by 3.");
        instant("infernal_axe", "Lumber Up", 100, Effect.SKILL_BOOST, 3, "WOODCUTTING", "Boosts your Woodcutting level by 3.");
        instant("crystal_axe", "Lumber Up", 100, Effect.SKILL_BOOST, 3, "WOODCUTTING", "Boosts your Woodcutting level by 3.");
        instant("dragon_pickaxe", "Rock Knocker", 100, Effect.SKILL_BOOST, 3, "MINING", "Boosts your Mining level by 3.");
        instant("infernal_pickaxe", "Rock Knocker", 100, Effect.SKILL_BOOST, 3, "MINING", "Boosts your Mining level by 3.");
        instant("crystal_pickaxe", "Rock Knocker", 100, Effect.SKILL_BOOST, 3, "MINING", "Boosts your Mining level by 3.");
        instant("dragon_harpoon", "Fishstabber", 100, Effect.SKILL_BOOST, 3, "FISHING", "Boosts your Fishing level by 3.");
        instant("infernal_harpoon", "Fishstabber", 100, Effect.SKILL_BOOST, 3, "FISHING", "Boosts your Fishing level by 3.");
        instant("crystal_harpoon", "Fishstabber", 100, Effect.SKILL_BOOST, 3, "FISHING", "Boosts your Fishing level by 3.");
        // ---------------- Ranged ----------------
        ranged("dark_bow", "Descent of Darkness", 55, 1.0, 1.3, Effect.DARK_BOW, 5, "Two arrows, each dealing +30% damage (minimum 5).");
        ranged("magic_shortbow", "Snapshot", 55, 1.43, 1.0, Effect.DOUBLE_SHOT, 0, "Two arrows in quick succession with increased accuracy.");
        ranged("magic_shortbow_i", "Snapshot", 50, 1.43, 1.0, Effect.DOUBLE_SHOT, 0, "Two arrows in quick succession with increased accuracy.");
        ranged("magic_longbow", "Powershot", 35, 1.0, 1.0, Effect.GUARANTEED, 0, "An arrow that is guaranteed to hit.");
        ranged("magic_comp_bow", "Powershot", 35, 1.0, 1.0, Effect.GUARANTEED, 0, "An arrow that is guaranteed to hit.");
        ranged("toxic_blowpipe", "Toxic Siphon", 50, 2.0, 1.5, Effect.HEAL_PERCENT, 50, "+50% damage, double accuracy; heals half the damage dealt.");
        ranged("armadyl_crossbow", "Armadyl Eye", 40, 2.0, 1.0, Effect.NONE, 0, "Double accuracy.");
        ranged("zaryte_crossbow", "Evoke", 75, 2.0, 1.0, Effect.NONE, 0, "Double accuracy; bolt effects always activate.");
        ranged("dragon_crossbow", "Annihilate", 60, 1.0, 1.2, Effect.AREA, 0, "+20% damage and splash damage around the target.");
        ranged("dragon_hunter_crossbow", "None", 0, 1.0, 1.0, Effect.NONE, 0, "");
        ranged("heavy_ballista", "Power Shot", 65, 1.25, 1.25, Effect.NONE, 0, "+25% accuracy and damage.");
        ranged("light_ballista", "Power Shot", 65, 1.25, 1.25, Effect.NONE, 0, "+25% accuracy and damage.");
        ranged("dragon_knife", "Duality", 25, 1.0, 1.0, Effect.DOUBLE_SHOT, 0, "Two knives thrown at once.");
        ranged("dragon_thrownaxe", "Chainhit", 25, 1.25, 1.0, Effect.NONE, 0, "+25% accuracy; bounces between targets.");
        ranged("webweaver_bow", "Arachnid's Wrath", 50, 2.0, 1.0, Effect.DOUBLE_SHOT, 0, "Four rapid shots (two simulated).");
        ranged("seercull", "Soulshot", 100, 1.0, 1.0, Effect.GUARANTEED, 0, "Lowers the target's Magic by the damage dealt.");
        ranged("rune_thrownaxe", "Chainhit", 10, 1.0, 1.0, Effect.NONE, 0, "Bounces between nearby targets.");
        ranged("morrigans_javelin", "Phantom Strike", 50, 1.0, 1.0, Effect.NONE, 0, "Deals the damage again over time.");
        // ---------------- Magic (instant blast at your target) ----------------
        instant("volatile_nightmare_staff", "Immolate", 55, Effect.MAGIC_BLAST, 66, null, "A powerful blast with a max hit of up to 66.");
        instant("eldritch_nightmare_staff", "Invocate", 55, Effect.BLAST_PRAYER, 50, null, "A blast that restores prayer equal to half the damage.");
        instant("accursed_sceptre", "Ambush", 50, Effect.MAGIC_BLAST, 32, null, "A blast with a +50% accuracy that lowers the target's stats.");
        instant("dawnbringer", "Pulsate", 35, Effect.MAGIC_BLAST, 150, null, "Damages Verzik Vitur.");
    }

    private SpecialAttacks() {
    }

    private static void melee(String id, String name, int cost, double acc, double dmg, Effect e, int param, String desc) {
        SPECS.put(id, new Spec(id, name, cost, Style.MELEE, acc, dmg, e, param, null, desc));
    }

    private static void ranged(String id, String name, int cost, double acc, double dmg, Effect e, int param, String desc) {
        if (cost <= 0) {
            return;
        }
        SPECS.put(id, new Spec(id, name, cost, Style.RANGED, acc, dmg, e, param, null, desc));
    }

    private static void instant(String id, String name, int cost, Effect e, int param, String skill, String desc) {
        SPECS.put(id, new Spec(id, name, cost, Style.INSTANT, 1.0, 1.0, e, param, skill, desc));
    }

    private static void alias(String id, String of) {
        Spec s = SPECS.get(of);
        SPECS.put(id, new Spec(id, s.name(), s.cost(), s.style(), s.accuracy(), s.damage(), s.effect(), s.param(), s.skill(), s.description()));
    }

    /** The special attack of a weapon (an LMS copy uses its base weapon's), or null. */
    public static Spec of(String itemId) {
        Spec s = SPECS.get(com.runecraft.osrs.data.EquipmentBonuses.reference(itemId));
        return s == null || s.cost() <= 0 ? null : s;
    }

    public static List<Spec> all() {
        return Collections.unmodifiableList(new ArrayList<>(SPECS.values()));
    }

    // ---------------- Energy ----------------

    public static final int MAX_ENERGY = 100;
    /** Special energy regenerates 10% every 50 game ticks (30 seconds); 25 ticks with a lightbearer. */
    public static final int REGEN_TICKS = 50;
    public static final int REGEN_AMOUNT = 10;

    public static int regen(int energy, boolean lightbearer, long gameTick) {
        int period = lightbearer ? REGEN_TICKS / 2 : REGEN_TICKS;
        if (gameTick % period != 0) {
            return energy;
        }
        return Math.min(MAX_ENERGY, energy + REGEN_AMOUNT);
    }

    // ---------------- Dragon claws ----------------

    /**
     * The dragon claws damage cascade (pre-2023 algorithm, wiki): four accuracy rolls decide which hit
     * lands first; later hits are halves of the first successful one.
     *
     * @param hits the result of each of the four accuracy rolls
     */
    public static int[] clawHits(Random random, boolean[] hits, int maxHit) {
        int max = Math.max(1, maxHit);
        if (hits[0]) {
            int h1 = randBetween(random, max / 2, max - 1);
            int h2 = h1 / 2;
            int h3 = h2 / 2;
            int h4 = h3 + 1;
            return new int[]{h1, h2, h3, h4};
        }
        if (hits[1]) {
            int h2 = randBetween(random, max * 3 / 8, max * 7 / 8);
            int h3 = h2 / 2;
            int h4 = h2 - h3;
            return new int[]{0, h2, h3, h4};
        }
        if (hits[2]) {
            int h3 = randBetween(random, max / 4, max * 3 / 4);
            int h4 = h3 + 1;
            return new int[]{0, 0, h3, h4};
        }
        if (hits[3]) {
            int h4 = randBetween(random, max / 4, max * 5 / 4);
            return new int[]{0, 0, 0, h4};
        }
        // Complete miss: two in three chance of 1-damage hits on the last two claws.
        return random.nextInt(3) < 2 ? new int[]{0, 0, 1, 1} : new int[]{0, 0, 0, 0};
    }

    private static int randBetween(Random random, int lo, int hi) {
        if (hi <= lo) {
            return Math.max(0, lo);
        }
        return lo + random.nextInt(hi - lo + 1);
    }

    /** Dragon battleaxe Rampage: strength boost = 10 + floor(sum of 10% drains / 4). */
    public static int rampageBoost(int attack, int defence, int magic, int ranged) {
        int drained = attack / 10 + defence / 10 + magic / 10 + ranged / 10;
        return 10 + drained / 4;
    }

    /** Saradomin godsword heal and prayer amounts for a given hit. */
    public static int[] sgsRestore(int damage) {
        if (damage <= 0) {
            return new int[]{0, 0};
        }
        return new int[]{Math.max(10, damage / 2), Math.max(5, damage / 4)};
    }

    /** Abyssal bludgeon Penance multiplier: +0.5% per missing prayer point. */
    public static double penance(int prayerLevel, int prayerPoints) {
        return 1.0 + Math.max(0, prayerLevel - prayerPoints) * 0.005;
    }
}
