package com.runecraft.osrs;

import java.util.List;
import java.util.Set;

/**
 * Which item the quick-eat and quick-drink keys use (round 42, {@code // playability}). OSRS players click food and potions
 * in the always-open inventory; with the mouse aiming in real time, a key consumes "the best" one through the ordinary
 * eat / drink path (same delays, same effects). These rules pick it; nothing here changes what an item does.
 * <ul>
 *   <li>Quick-eat: the food that heals the most without healing past the missing hitpoints, else the one that wastes the
 *       least; nothing at full health. While the eat delay runs, a combo food (cooked karambwan) still goes down, as OSRS
 *       combo eating allows.</li>
 *   <li>Quick-drink, by need: a cure when poisoned (an anti-venom first when envenomed); a brew below half health; a prayer
 *       or restore potion once the missing prayer points are at least what one dose restores; a brew once its heal fits the
 *       missing hitpoints; otherwise a boost for the style the player fights with while that stat isn't boosted. The dose
 *       of the most-used potion goes first (fewest doses left), as players drink their partly empty vials first.</li>
 * </ul>
 */
public final class QuickConsumeRules {
    private QuickConsumeRules() {
    }

    /** Why a potion is drunk. */
    public enum Need { CURE, HEAL, PRAYER, BOOST_MELEE, BOOST_RANGED, BOOST_MAGIC, OTHER }

    /** A food in the pack: its slot, item, hitpoints healed and whether it's combo food (its own delay). */
    public record Food(int slot, String id, int heal, boolean combo) {
    }

    /** A potion in the pack: its slot, item and doses left. */
    public record Potion(int slot, String id, int doses) {
    }

    /** The player for quick-drink: hitpoints, Prayer, toxins, the style fought with (0 melee, 1 ranged, 2 magic), boosts. */
    public record State(int hp, int maxHp, int prayerPoints, int prayerLevel, boolean poisoned, boolean envenomed, int style,
            boolean meleeBoosted, boolean rangedBoosted, boolean magicBoosted) {
    }

    private static final Set<String> CURES = Set.of("antipoison", "superantipoison", "antidote_plus", "antidote_plus_plus",
            "anti_venom", "anti_venom_plus", "extended_anti_venom_plus", "sanfew_serum", "cox_antipoison_potion",
            "cox_antipoison_plus", "cox_antipoison_minus");
    private static final Set<String> HEALS = Set.of("saradomin_brew", "xerics_aid", "xerics_aid_plus", "xerics_aid_minus",
            "nectar");
    private static final Set<String> PRAYER = Set.of("prayer_potion", "super_restore", "blighted_super_restore", "sanfew_serum",
            "egniol_potion", "revitalisation_potion", "revitalisation_plus", "revitalisation_minus", "prayer_regeneration_potion",
            "ancient_brew");
    private static final Set<String> MELEE = Set.of("attack_potion", "strength_potion", "defence_potion", "combat_potion",
            "super_attack", "super_strength", "super_defence", "super_combat_potion", "divine_super_attack",
            "divine_super_strength", "divine_super_defence", "divine_super_combat", "overload", "cox_overload",
            "cox_overload_plus", "cox_overload_minus", "elder_potion", "elder_plus", "elder_minus");
    private static final Set<String> RANGED = Set.of("ranging_potion", "super_ranging", "divine_ranging", "bastion_potion",
            "divine_bastion", "twisted_potion", "twisted_plus", "twisted_minus");
    private static final Set<String> MAGIC = Set.of("magic_potion", "super_magic_potion", "divine_magic", "battlemage_potion",
            "divine_battlemage", "magic_essence", "kodai_potion", "kodai_plus", "kodai_minus");

    /** What a potion is for (a sanfew serum both cures and restores: it counts as a cure here). */
    public static Need need(String id) {
        if (CURES.contains(id)) {
            return Need.CURE;
        }
        if (HEALS.contains(id)) {
            return Need.HEAL;
        }
        if (PRAYER.contains(id)) {
            return Need.PRAYER;
        }
        if (MELEE.contains(id)) {
            return Need.BOOST_MELEE;
        }
        if (RANGED.contains(id)) {
            return Need.BOOST_RANGED;
        }
        if (MAGIC.contains(id)) {
            return Need.BOOST_MAGIC;
        }
        return Need.OTHER;
    }

    /** Whether a potion restores prayer points (a sanfew serum does as well as curing). */
    public static boolean restoresPrayer(String id) {
        return PRAYER.contains(id);
    }

    /** Whether a cure also cures venom (the anti-venoms). */
    public static boolean curesVenom(String id) {
        return id.startsWith("anti_venom") || id.equals("extended_anti_venom_plus");
    }

    /** Prayer points one dose of a prayer potion restores: 7 + a quarter of the Prayer level (OSRS Wiki, Prayer potion). */
    public static int prayerRestore(int prayerLevel) {
        return 7 + prayerLevel / 4;
    }

    /** Hitpoints one dose of a Saradomin brew heals: 2 + 15% of the Hitpoints level (OSRS Wiki, Saradomin brew). */
    public static int brewHeal(int hpLevel) {
        return 2 + hpLevel * 15 / 100;
    }

    /**
     * The food to eat, as an index into {@code foods}, or -1. {@code foodReady}: the eat delay is over; {@code comboReady}:
     * combo food's own delay is over.
     */
    public static int chooseFood(List<Food> foods, int missingHp, boolean foodReady, boolean comboReady) {
        if (missingHp <= 0) {
            return -1;
        }
        int best = pickFood(foods, missingHp, foodReady, false);
        if (best < 0) {
            best = pickFood(foods, missingHp, comboReady, true);
        }
        return best;
    }

    private static int pickFood(List<Food> foods, int missing, boolean ready, boolean combo) {
        if (!ready) {
            return -1;
        }
        int fit = -1;
        int over = -1;
        for (int i = 0; i < foods.size(); i++) {
            Food f = foods.get(i);
            if (f.heal() <= 0 || f.combo() != combo) {
                continue;
            }
            if (f.heal() <= missing) {
                if (fit < 0 || f.heal() > foods.get(fit).heal()) {
                    fit = i;
                }
            } else if (over < 0 || f.heal() < foods.get(over).heal()) {
                over = i;
            }
        }
        return fit >= 0 ? fit : over;
    }

    /** The potion to drink, as an index into {@code potions}, or -1 when nothing is needed. */
    public static int choosePotion(List<Potion> potions, State s) {
        int missingHp = Math.max(0, s.maxHp() - s.hp());
        int missingPrayer = Math.max(0, s.prayerLevel() - s.prayerPoints());
        if (s.envenomed()) {
            int venom = pick(potions, p -> need(p.id()) == Need.CURE && curesVenom(p.id()));
            if (venom >= 0) {
                return venom;
            }
        }
        if (s.poisoned() || s.envenomed()) {
            int cure = pick(potions, p -> need(p.id()) == Need.CURE);
            if (cure >= 0) {
                return cure;
            }
        }
        boolean low = s.hp() * 2 <= s.maxHp();
        boolean brewFits = missingHp >= brewHeal(s.maxHp());
        if (low && brewFits) {
            int heal = pick(potions, p -> need(p.id()) == Need.HEAL);
            if (heal >= 0) {
                return heal;
            }
        }
        if (s.prayerLevel() > 0 && missingPrayer >= prayerRestore(s.prayerLevel())) {
            // an ancient brew restores prayer but drains melee stats: only when no plain restore is carried
            int prayer = pick(potions, p -> restoresPrayer(p.id()) && !p.id().equals("ancient_brew"));
            if (prayer < 0) {
                prayer = pick(potions, p -> p.id().equals("ancient_brew"));
            }
            if (prayer >= 0) {
                return prayer;
            }
        }
        if (brewFits) {
            int heal = pick(potions, p -> need(p.id()) == Need.HEAL);
            if (heal >= 0) {
                return heal;
            }
        }
        Need boost = s.style() == 2 ? Need.BOOST_MAGIC : s.style() == 1 ? Need.BOOST_RANGED : Need.BOOST_MELEE;
        boolean boosted = s.style() == 2 ? s.magicBoosted() : s.style() == 1 ? s.rangedBoosted() : s.meleeBoosted();
        if (!boosted) {
            return pick(potions, p -> need(p.id()) == boost);
        }
        return -1;
    }

    /** The matching potion with the fewest doses left (the first such slot on a tie). */
    private static int pick(List<Potion> potions, java.util.function.Predicate<Potion> matches) {
        int best = -1;
        for (int i = 0; i < potions.size(); i++) {
            Potion p = potions.get(i);
            if (matches.test(p) && (best < 0 || p.doses() < potions.get(best).doses())) {
                best = i;
            }
        }
        return best;
    }
}
