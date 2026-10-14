package com.runecraft.osrs.data;

import java.util.List;

/**
 * Armour set effects that depend only on what is worn: Barrows brothers' sets, Inquisitor's armour and crystal armour
 * with crystal bows. Void and obsidian live with their minigames (MinigameCombat), the berserker necklace with
 * the jewellery table.
 */
public final class SetEffects {
    private SetEffects() {
    }

    /** A Barrows brother's full set: helm, body, legs and weapon. */
    public record BarrowsSet(String brother, String helm, String body, String legs, String weapon) {
        public boolean wornBy(String head, String chest, String legsWorn, String mainHand) {
            return helm.equals(head) && body.equals(chest) && legs.equals(legsWorn) && weapon.equals(mainHand);
        }
    }

    public static final BarrowsSet DHAROK = new BarrowsSet("dharok", "dharoks_helm", "dharoks_platebody", "dharoks_platelegs",
            "dharoks_greataxe");
    public static final BarrowsSet GUTHAN = new BarrowsSet("guthan", "guthans_helm", "guthans_platebody", "guthans_chainskirt",
            "guthans_warspear");
    public static final BarrowsSet VERAC = new BarrowsSet("verac", "veracs_helm", "veracs_brassard", "veracs_plateskirt",
            "veracs_flail");
    public static final BarrowsSet TORAG = new BarrowsSet("torag", "torags_helm", "torags_platebody", "torags_platelegs",
            "torags_hammers");
    public static final BarrowsSet AHRIM = new BarrowsSet("ahrim", "ahrims_hood", "ahrims_robetop", "ahrims_robeskirt",
            "ahrims_staff");
    public static final BarrowsSet KARIL = new BarrowsSet("karil", "karils_coif", "karils_leathertop", "karils_leatherskirt",
            "karils_crossbow");
    public static final List<BarrowsSet> BARROWS = List.of(DHAROK, GUTHAN, VERAC, TORAG, AHRIM, KARIL);

    /** Chance for Guthan's (heal), Verac's (ignore defence), Torag's, Ahrim's and Karil's effects to trigger. */
    public static final double BARROWS_PROC_CHANCE = 0.25;

    /** The full Barrows set worn, or null. */
    public static BarrowsSet barrows(String head, String chest, String legs, String mainHand) {
        for (BarrowsSet s : BARROWS) {
            if (s.wornBy(head, chest, legs, mainHand)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Dharok's "Wretched strength": max hit is multiplied by 1 + (lost HP / 100) x (max HP / 100).
     * At 99 HP and 1 HP left that is x1.97.
     */
    public static double dharokMultiplier(int currentHp, int maxHp) {
        int lost = Math.max(0, maxHp - currentHp);
        return 1.0 + (lost / 100.0) * (maxHp / 100.0);
    }

    /** Ahrim's set with the amulet of the damned: a triggered spell deals 30% more (rounded down). */
    public static int ahrimDamned(int damage) {
        return damage * 13 / 10;
    }

    /** Karil's set with the amulet of the damned: a triggered shot adds a second hitsplat of half its damage. */
    public static int karilDamnedSecondHit(int damage) {
        return damage / 2;
    }

    /**
     * Inquisitor's armour: crush accuracy and damage +0.5% for the great helm and +1% each for the hauberk and
     * plateskirt (2.5% for the set). Since the July 2026 rework the mace no longer multiplies this.
     */
    public static double inquisitorCrushBonus(String head, String chest, String legs) {
        double b = 1.0;
        if ("inquisitors_great_helm".equals(head)) {
            b += 0.005;
        }
        if ("inquisitors_hauberk".equals(chest)) {
            b += 0.01;
        }
        if ("inquisitors_plateskirt".equals(legs)) {
            b += 0.01;
        }
        return b;
    }

    /** Crystal bows that benefit from crystal armour. */
    public static boolean crystalBow(String weapon) {
        return weapon != null && (weapon.equals("bow_of_faerdhinen") || weapon.startsWith("crystal_bow"));
    }

    /** Crystal armour with a crystal bow: helm +5%, body +15%, legs +10% ranged accuracy (+30% for the set). */
    public static double crystalAccuracy(String head, String chest, String legs) {
        return 1.0 + ("crystal_helm".equals(head) ? 0.05 : 0) + ("crystal_body".equals(chest) ? 0.15 : 0)
                + ("crystal_legs".equals(legs) ? 0.10 : 0);
    }

    /** Crystal armour with a crystal bow: helm +2.5%, body +7.5%, legs +5% ranged damage (+15% for the set). */
    public static double crystalDamage(String head, String chest, String legs) {
        return 1.0 + ("crystal_helm".equals(head) ? 0.025 : 0) + ("crystal_body".equals(chest) ? 0.075 : 0)
                + ("crystal_legs".equals(legs) ? 0.05 : 0);
    }
}
