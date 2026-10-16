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
        BLAST_PRAYER,
        /** Ancient godsword: marks the target; 8 ticks later it takes 25 damage unless 5+ tiles away, healing the attacker. */
        BLOOD_SACRIFICE,
        /** Burning claws: three slashes in a cascade, each may set the target alight. */
        BURNING_CLAWS,
        /** Dual macuahuitl with the blood moon set: costs 25% of current hitpoints, +25% min and max hit. */
        BLOOD_INFUSION,
        /** Crimson kisten: four accuracy rolls; more successes raise the damage range (70-110% up to 130-170%). */
        BRUTAL_SWING,
        /** Adds {@code param} damage to the hit, even a miss (granite hammer). */
        FLAT_BONUS,
        /** Tonalztics of Ralos: two throws; damage drains Defence by a share of the target's Magic level. */
        DIVISION,
        /** Ursine chainmace: double accuracy and {@code param} extra damage over six seconds. */
        BEAR_DOWN,
        /** Scorching bow: binds a demon for {@code param} ticks with 5 burn damage (demons only). */
        SHACKLES,
        /** Ancient mace: drains the target's prayer by the damage and restores the attacker's by the same. */
        PRAYER_SMASH,
        /** Soulreaper axe: consumes every soul stack (+12% accuracy and +6% min and max hit each). */
        BEHEAD,
        /** A hit dealing at least {@code param}% of the max hit (Vesta's longsword's Feint, Statius's Smash). */
        MIN_DAMAGE,
        /**
         * Rod of Ivandis: traps a vampyre juvenile or juvinate below half health so it can't retaliate for {@code param}
         * ticks; every use costs one of the rod's charges.
         */
        RETAINER
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
        melee("dragon_halberd", "Sweep", 30, 1.0, 1.1, Effect.DOUBLE_HIT, 0, "+10% damage; a second hit (-25% accuracy) against large targets.");
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
        melee("abyssal_tentacle", "Binding Tentacle", 50, 1.25, 1.0, Effect.FREEZE, 8, "Binds the target for 5 seconds (and may poison).");
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
        melee("ancient_godsword", "Blood Sacrifice", 50, 2.0, 1.1, Effect.BLOOD_SACRIFICE, 25,
                "Marks the target: unless it moves 5 tiles away within 8 ticks it takes 25 damage and heals you 15% of its Hitpoints (max 25).");
        alias("ancient_godsword_or", "ancient_godsword");
        melee("saradomin_sword", "Saradomin's Lightning", 100, 1.0, 1.1, Effect.LIGHTNING, 16, "+10% damage plus a magic hit of up to 16.");
        melee("saradomins_blessed_sword", "Saradomin's Blessed Lightning", 65, 1.0, 1.25, Effect.NONE, 0, "+25% damage; rolls against magic defence.");
        melee("elder_maul", "Pulverize", 50, 1.25, 1.0, Effect.DRAIN_DEFENCE_PERCENT, 35, "+25% accuracy; lowers Defence by 35% on a hit.");
        melee("crystal_halberd", "Sweep", 30, 1.0, 1.1, Effect.DOUBLE_HIT, 0, "+10% damage; a second hit (-25% accuracy) against large targets.");
        alias("crystal_halberd_basic", "crystal_halberd");
        alias("crystal_halberd_attuned", "crystal_halberd");
        alias("crystal_halberd_perfected", "crystal_halberd");
        melee("voidwaker", "Disrupt", 50, 1.0, 1.0, Effect.VOIDWAKER, 0, "A guaranteed magic hit of 50-150% of your max hit.");
        // Rod of Ivandis (OSRS Wiki): Retainer, 10% energy, 30 seconds; one of its 10 charges per special attack, used or not.
        melee("rod_of_ivandis", "Retainer", 10, 1.0, 0.0, Effect.RETAINER, 50 /* RETAINER_TICKS */,
                "Traps a vampyre juvenile or juvinate below half health for 30 seconds; uses a charge.");
        melee("arclight", "Weaken", 50, 1.0, 1.0, Effect.WEAKEN, 5, "Drains Attack, Strength and Defence by 5% (10% vs demons).");
        melee("darklight", "Weaken", 50, 1.0, 1.0, Effect.WEAKEN, 5, "Drains Attack, Strength and Defence by 5% (10% vs demons).");
        melee("emberlight", "Weaken", 50, 1.0, 1.0, Effect.WEAKEN, 5, "Drains Attack, Strength and Defence by 5% (10% vs demons).");
        melee("barrelchest_anchor", "Sunder", 50, 2.0, 1.1, Effect.DRAIN_BY_DAMAGE, 0, "Double accuracy; drains a stat by the damage dealt.");
        melee("rune_claws", "Impale", 25, 1.1, 1.1, Effect.NONE, 0, "+10% accuracy and damage.");
        melee("brine_sabre", "Liquify", 75, 2.0, 1.0, Effect.NONE, 0, "Double accuracy (underwater).");
        melee("osmumtens_fang", "Eviscerate", 25, 1.5, 1.0, Effect.NONE, 0, "+50% accuracy with a narrowed damage range.");
        melee("dragon_scimitar_or", "Sever", 55, 1.25, 1.0, Effect.NONE, 0, "A slash with increased accuracy that disables protection prayers.");
        melee("dual_macuahuitl", "Blood Infusion", 25, 1.0, 1.0, Effect.BLOOD_INFUSION, 25,
                "With the blood moon set: sacrifice 25% of your hitpoints for +25% minimum and maximum damage.");
        melee("burning_claws", "Burning Barrage", 35, 1.0, 1.0, Effect.BURNING_CLAWS, 10, "Three fiery slashes in a cascade that may burn the target.");
        melee("soulreaper_axe", "Behead", 0, 1.0, 1.0, Effect.BEHEAD, 0, "Consumes your soul stacks: +12% accuracy and +6% damage each.");
        // Round 22: specials of weapons the mod has (wiki "Special attacks"; the DPS calculator models the first two).
        melee("granite_hammer", "Hammer Blow", 60, 1.5, 1.0, Effect.FLAT_BONUS, 5, "+50% accuracy; adds 5 damage, even to a miss.");
        melee("crimson_kisten", "Brutal Swing", 50, 1.0, 1.0, Effect.BRUTAL_SWING, 0, "Rolls accuracy four times; each success raises the damage.");
        melee("ursine_chainmace", "Bear Down", 50, 2.0, 1.0, Effect.BEAR_DOWN, 20, "Double accuracy; the target takes 20 more damage over 6 seconds.");
        melee("ancient_mace", "Favour of the War God", 100, 1.0, 1.0, Effect.PRAYER_SMASH, 0,
                "Drains the target's prayer by the damage dealt and restores yours by the same.");
        // approx: the wiki gives "-25% to +25%" damage and a 70% Defence drain (the tooltip says 75%)
        melee("statiuss_warhammer", "Smash", 35, 1.0, 1.25, Effect.MIN_DAMAGE, 25, "+25% damage (at least 25% of your max hit); lowers Defence.");
        // approx: rolling against a quarter of the target's defence is modelled as four times the accuracy
        melee("vestas_longsword", "Feint", 25, 4.0, 1.2, Effect.MIN_DAMAGE, 20, "Rolls against 25% of the target's defence; deals 20-120% of your max hit.");
        melee("dinhs_bulwark", "Shield Bash", 50, 1.2, 1.0, Effect.AREA, 0, "Hits every enemy around your target."); // approx
        instant("excalibur", "Sanctuary", 100, Effect.SKILL_BOOST, 8, "DEFENCE", "Boosts your Defence level by 8.");
        instant("staff_of_the_dead", "Power of Death", 100, Effect.POWER_OF_DEATH, 100, null, "Halves melee damage you take for one minute.");
        instant("toxic_staff_of_the_dead", "Power of Death", 100, Effect.POWER_OF_DEATH, 100, null, "Halves melee damage you take for one minute.");
        instant("staff_of_light", "Power of Death", 100, Effect.POWER_OF_DEATH, 100, null, "Halves melee damage you take for one minute.");
        instant("staff_of_balance", "Power of Death", 100, Effect.POWER_OF_DEATH, 100, null, "Halves melee damage you take for one minute.");
        // ---------------- Skilling ----------------
        instant("dragon_axe", "Lumber Up", 100, Effect.SKILL_BOOST, 3, "WOODCUTTING", "Boosts your Woodcutting level by 3.");
        instant("infernal_axe", "Lumber Up", 100, Effect.SKILL_BOOST, 3, "WOODCUTTING", "Boosts your Woodcutting level by 3.");
        instant("crystal_axe", "Lumber Up", 100, Effect.SKILL_BOOST, 3, "WOODCUTTING", "Boosts your Woodcutting level by 3.");
        instant("3rd_age_axe", "Lumber Up", 100, Effect.SKILL_BOOST, 3, "WOODCUTTING", "Boosts your Woodcutting level by 3.");
        instant("3rd_age_pickaxe", "Rock Knocker", 100, Effect.SKILL_BOOST, 3, "MINING", "Boosts your Mining level by 3.");
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
        ranged("armadyl_crossbow", "Armadyl Eye", 50, 2.0, 1.0, Effect.NONE, 0, "Double accuracy.");
        ranged("zaryte_crossbow", "Evoke", 75, 2.0, 1.0, Effect.NONE, 0, "Double accuracy; bolt effects always activate.");
        ranged("dragon_crossbow", "Annihilate", 60, 1.0, 1.2, Effect.AREA, 0, "+20% damage and splash damage around the target.");
        ranged("dragon_hunter_crossbow", "None", 0, 1.0, 1.0, Effect.NONE, 0, "");
        ranged("dragon_hunter_crossbow_b", "None", 0, 1.0, 1.0, Effect.NONE, 0, "");
        ranged("dragon_hunter_crossbow_t", "None", 0, 1.0, 1.0, Effect.NONE, 0, "");
        ranged("heavy_ballista", "Power Shot", 65, 1.25, 1.25, Effect.NONE, 0, "+25% accuracy and damage.");
        ranged("light_ballista", "Power Shot", 65, 1.25, 1.25, Effect.NONE, 0, "+25% accuracy and damage.");
        ranged("dragon_knife", "Duality", 25, 1.0, 1.0, Effect.DOUBLE_SHOT, 0, "Two knives thrown at once.");
        ranged("dragon_thrownaxe", "Momentum Throw", 25, 1.25, 1.0, Effect.NONE, 0, "+25% accuracy; your next attack comes a tick sooner.");
        ranged("webweaver_bow", "Swarm", 50, 2.0, 0.4, Effect.DOUBLE_SHOT, 4, "Four rapid shots with double accuracy, each at 40% of your max hit.");
        ranged("seercull", "Soulshot", 100, 1.0, 1.0, Effect.GUARANTEED, 0, "Lowers the target's Magic by the damage dealt.");
        ranged("rune_thrownaxe", "Chainhit", 10, 1.0, 1.0, Effect.NONE, 0, "Bounces between nearby targets.");
        ranged("morrigans_javelin", "Phantom Strike", 50, 1.5, 1.0, Effect.NONE, 0, "+50% accuracy; against players the damage is dealt again over time.");
        ranged("tonalztics_of_ralos", "Division", 50, 1.5, 1.0, Effect.DIVISION, 0, "+50% accuracy; damage lowers the target's Defence.");
        ranged("scorching_bow", "Scorching Shackles", 25, 1.0, 1.0, Effect.SHACKLES, 20, "Binds a demon for 12 seconds and burns it.");
        // ---------------- Magic (instant blast at your target) ----------------
        // The blasts' max hit, accuracy and gear effects come from PlayerHitCalc (special = true); param is the base max hit cap.
        instant("volatile_nightmare_staff", "Immolate", 55, Effect.MAGIC_BLAST, 58, null, "+50% accuracy; a blast with a base max hit of up to 58.");
        instant("eldritch_nightmare_staff", "Invocate", 55, Effect.BLAST_PRAYER, 44, null,
                "A blast (base max hit up to 44) that restores prayer by half the damage, up to 120.");
        instant("accursed_sceptre", "Condemn", 50, Effect.MAGIC_BLAST, 0, null, "+50% accuracy and damage; lowers the target's Defence and Magic by 15%.");
        instant("eye_of_ayak", "Soul Rend", 50, Effect.MAGIC_BLAST, 0, null, "Double accuracy, +30% damage; lowers the target's Magic.");
        instant("purging_staff", "Scatter Ashes", 25, Effect.MAGIC_BLAST, 0, null, "Casts your best demonbane spell at a demon.");
        instant("dawnbringer", "Pulsate", 35, Effect.MAGIC_BLAST, 150, null, "Damages Verzik Vitur.");
    }

    private SpecialAttacks() {
    }

    /** Retainer holds its vampyre for 30 seconds (50 game ticks). */
    public static final int RETAINER_TICKS = 50;

    /** Creatures the rod of Ivandis' Retainer works on: vampyre juveniles and juvinates. */
    public static boolean retainerTarget(String creatureId) {
        return creatureId != null && (creatureId.contains("vampyre_juvenile") || creatureId.contains("vampyre_juvinate"));
    }

    /** Why Retainer fails on a target (the OSRS messages), or null when it takes hold. */
    public static String retainerFailure(String creatureId, double healthFraction) {
        if (!retainerTarget(creatureId)) {
            return "This spell can only be used on vampyre juveniles or vampyre juvinates.";
        }
        if (healthFraction >= 0.5) {
            return "This spell only works against vampyres with less than half of their health remaining.";
        }
        return null;
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
        return s == null || (s.cost() <= 0 && s.effect() != Effect.BEHEAD) ? null : s;
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
        int period = lightbearer ? REGEN_TICKS / 2 : REGEN_TICKS; // wiki: 10% every 15 seconds with a lightbearer
        if (gameTick % period != 0) {
            return energy;
        }
        return Math.min(MAX_ENERGY, energy + REGEN_AMOUNT);
    }

    // ---------------- Dragon claws ----------------

    /**
     * The dragon claws damage cascade (osrs-dps-calc {@code dClawDist}): the first successful one of four accuracy rolls picks
     * a total damage range (first roll: max to 2 max - 1, then from 3/4, 1/2 and 1/4 of the max upwards) that is split into
     * the claws' hitsplats; four misses deal 1 + 1 two times in three.
     *
     * @param hits the result of each of the four accuracy rolls
     */
    public static int[] clawHits(Random random, boolean[] hits, int maxHit) {
        int max = Math.max(1, maxHit);
        for (int r = 0; r < 4; r++) {
            if (hits[r]) {
                int[] range = com.runecraft.osrs.PlayerHitCalc.clawsTotalRange(r, max, false);
                return com.runecraft.osrs.PlayerHitCalc.clawsSplit(r, randBetween(random, range[0], range[1]), false);
            }
        }
        return random.nextInt(3) < 2 ? new int[]{0, 0, 1, 1} : new int[]{0, 0, 0, 0};
    }

    /**
     * The burning claws cascade (osrs-dps-calc {@code burningClawSpec}): three accuracy rolls; four in five complete misses
     * still deal one or two 1s.
     */
    public static int[] burningClawHits(Random random, boolean[] hits, int maxHit) {
        int max = Math.max(1, maxHit);
        for (int r = 0; r < 3; r++) {
            if (hits[r]) {
                int[] range = com.runecraft.osrs.PlayerHitCalc.clawsTotalRange(r, max, true);
                int[] h = com.runecraft.osrs.PlayerHitCalc.clawsSplit(r, randBetween(random, range[0], range[1]), true);
                for (int i = 0; i < h.length; i++) {
                    h[i] = Math.max(0, h[i]);
                }
                return h;
            }
        }
        int roll = random.nextInt(5);
        return roll == 0 ? new int[]{0, 0, 0} : roll <= 2 ? new int[]{1, 0, 0} : new int[]{1, 1, 0};
    }

    /** Chance (per hitsplat) that a burning claws hit sets the target alight: 15% / 30% / 45% by the successful roll. */
    public static double burnChance(int successfulRoll) {
        return 0.15 * (successfulRoll + 1);
    }

    /**
     * Crimson kisten's Brutal Swing: with {@code successes} of four accuracy rolls passed the damage is rolled between
     * (70 + 20 (n - 1))% and (110 + 20 (n - 1))% of the max hit (osrs-dps-calc).
     */
    public static int[] kistenRange(int successes, int maxHit) {
        if (successes <= 0) {
            return new int[]{0, 0};
        }
        int i = Math.min(4, successes) - 1;
        return new int[]{maxHit * (70 + i * 20) / 100, maxHit * (110 + i * 20) / 100};
    }

    /** Ancient godsword Blood Sacrifice heal: 15% of the target's Hitpoints level, capped at 25 (15 against players). */
    public static int bloodSacrificeHeal(int targetHitpointsLevel, boolean player) {
        return Math.min(player ? 15 : 25, targetHitpointsLevel * 15 / 100);
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
